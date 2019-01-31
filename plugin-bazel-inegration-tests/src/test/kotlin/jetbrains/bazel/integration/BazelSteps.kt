package jetbrains.bazel.integration

import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import devteam.rx.Disposable
import devteam.rx.use
import io.cucumber.datatable.DataTable
import org.testng.Assert
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

public class BazelSteps {
    private val _args: MutableList<String> = mutableListOf()
    private val _targets: MutableList<String> = mutableListOf()
    private var _runResult: RunResult = RunResult()

    @When("^add the argument (.+)$")
    fun addArgumentStep(argument: String) = _args.add(argument)

    @When("^add the target (.+)$")
    fun addProjectStep(argument: String) = _targets.add(argument)

    @When("^run in (.+)$")
    fun runStep(scenario: String) {
        try {
            val cleanResult = run(scenario, listOf("clean", "--expunge"), emptyList())
            Assert.assertEquals(cleanResult.exitCode, 0)
            _runResult = run(scenario, _args, _targets)
            run(scenario, listOf("clean", "--expunge"), emptyList())
        }
        finally {
            _args.clear()
            _targets.clear()
        }
    }

    @Then("^the exit code is (\\d+)$")
    fun checkExitCodeStep(expectedExitCode: Int) = Assert.assertEquals(_runResult.exitCode, expectedExitCode)


    @Then("^the stdErr output is empty$")
    fun checkStdErrIsEmptyStep() = Assert.assertEquals(_runResult.stdErr.size, 0)

    @Then("^the result contains all service messages like$")
    fun checkContainsAllServiceMessagesStep(table: DataTable) {
        val expected = ServiceMessages.convert(table).toSet()
        val actual = _runResult.serviceMessages.toSet()
        Assert.assertTrue(actual.containsAll(expected))
    }

    @Then("^the result does not contain any service messages like$")
    fun checkDoesNotContainAnyServiceMessagesStep(table: DataTable) {
        val expected = ServiceMessages.convert(table).toSet()
        val actual = _runResult.serviceMessages.toSet()
        Assert.assertTrue(actual.intersect(expected).isEmpty())
    }

    companion object {
        fun run(scenario: String, args: List<String>, targets: List<String>): RunResult {
            Environment.validate()

            val cmdArgs = mutableListOf<String>()
            cmdArgs.add(Environment.bazelExecutable.canonicalPath)
            cmdArgs.addAll(args)
            if (targets.any()) {
                cmdArgs.add("--")
                cmdArgs.addAll(targets)
            }

            val argsFile = File(Environment.sandboxDirectory, "args${cmdArgs.hashCode()}.txt")
            argsFile.delete()
            argsFile.appendText(cmdArgs.joinToString(System.getProperty("line.separator")))
            val scenarioDirectory = File(Environment.samplesDirectory, scenario)
            if (!scenarioDirectory.exists() || !scenarioDirectory.isDirectory) {
                Assert.fail("Samples directory \"${scenarioDirectory}\" was not found for scenario \"${scenario}\".")
            }

            val processArgs = listOf(
                    Environment.javaExecutable.canonicalPath,
                    "-jar",
                    Environment.besJar.canonicalPath,
                    "-c=${argsFile.canonicalFile}")

            var runningCmd = processArgs.joinToString(" ") { "\"${it}\"" }

            val process = ProcessBuilder(processArgs)
                    .directory(scenarioDirectory)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            val stdOut = mutableListOf<String>()
            val stdErr = mutableListOf<String>()
            val serviceMessages = mutableListOf<ServiceMessage>()

            ActiveReader(process.inputStream.bufferedReader()) { line ->
                stdOut.add(line)
                ServiceMessages.tryParseServiceMessage(line)?.let {
                    serviceMessages.add(it)
                }
            }.use {
                ActiveReader(process.errorStream.bufferedReader()) { line ->
                    stdErr.add(line)
                }.use { }
            }

            Assert.assertTrue(process.waitFor(2, TimeUnit.MINUTES), "Timeout while waiting the process ${runningCmd} in the directory \"${scenarioDirectory}\".")

            return RunResult(
                    process.exitValue(),
                    stdOut,
                    stdErr,
                    serviceMessages)
        }

        private class ActiveReader(reader: BufferedReader, action: (line: String) -> Unit) : Disposable {
            private val _tread: Thread = object : Thread() {
                override fun run() {
                    do {
                        val line = reader.readLine()
                        if (!line.isNullOrBlank()) {
                            action(line)
                        }
                    } while (line != null)
                }
            }

            init {
                _tread.start()
            }

            override fun dispose() = _tread.join()
        }
    }
}