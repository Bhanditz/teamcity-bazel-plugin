package jetbrains.bazel.integration

import cucumber.api.java.Before
import cucumber.api.java.en.When
import org.testng.Assert
import java.io.*
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

public class EnvironmentSteps {
    private var _sandboxDirectory: File = File(".")
    private var _toolsDirectory: File = File(".")

    @Before
    fun setup() {
        var projectDirectory = File(File(EnvironmentSteps::class.java.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath(), "/../../../").canonicalFile
        if (projectDirectory.name != "plugin-bazel-inegration-tests") {
            projectDirectory = projectDirectory.parentFile
        }

        Assert.assertTrue(projectDirectory.exists(), "\"${projectDirectory}\" does not exist")
        val tempDirectory = File(projectDirectory, "/build/tmp").canonicalFile
        Assert.assertTrue(tempDirectory.exists(), "\"${tempDirectory}\" does not exist")

        _sandboxDirectory = File(tempDirectory, "/sandbox").canonicalFile
        _toolsDirectory = File(tempDirectory, "/tools").canonicalFile
        Assert.assertTrue(_toolsDirectory.exists(), "\"${_toolsDirectory}\" does not exist")

        // clean sandbox
        _sandboxDirectory.deleteRecursively()
        _sandboxDirectory.mkdirs()

        val solutionDirectory = File(projectDirectory, "/../").canonicalFile
        val libsDirectory = File(solutionDirectory, "/plugin-bazel-event-service/build/libs").canonicalFile
        val toolsDirectory = File(solutionDirectory, "/plugin-bazel-event-service/build/tools").canonicalFile
        val samplesDirectory = File(solutionDirectory, "/plugin-bazel-inegration-tests/samples").canonicalFile

        // prepare tool
        toolsDirectory.copyRecursively(_sandboxDirectory, true)
        libsDirectory.copyRecursively(_sandboxDirectory, true)
        Environment.sandboxDirectory = _sandboxDirectory
        Environment.besJar = File(_sandboxDirectory, "plugin-bazel-event-service.jar")
        Environment.samplesDirectory = samplesDirectory
    }

    @When("^use bazel (\\d+\\.\\d+\\.\\d+|default)$")
    fun useBazelStep(bazelVersion: String) {
        val version = if (bazelVersion == "default") Environment.DefaultBazelVersion else bazelVersion
        downloadAndUnzip(
                URL("https://github.com/bazelbuild/bazel/releases/download/${version}/bazel-${version}-windows-x86_64.zip"),
                File("bazel.exe"),
                _toolsDirectory)
    }

    fun downloadAndUnzip(bazelUrl: URL, toolFile: File, targetDirectory: File) {
        val zipFileName = File(targetDirectory, File(bazelUrl.path).name)
        val bazelExecutable = File(targetDirectory, zipFileName.nameWithoutExtension + "." + toolFile.extension)
        if (!bazelExecutable.exists()) {
            val buffer = ByteArray(0xffffff)
            if (!zipFileName.exists()) {
                download(bazelUrl, zipFileName, buffer)
            }

            unzip(zipFileName, _toolsDirectory, buffer)
            File(_toolsDirectory, toolFile.name).renameTo(bazelExecutable)
        }

        Environment.bazelExecutable = bazelExecutable
    }

    fun download(urlFrom: URL, fileTo: File, buffer: ByteArray) {
        BufferedInputStream(urlFrom.openStream()).use {
            val stream = it;
            fileTo.parentFile.mkdirs()
            FileOutputStream(fileTo).use {
                var bytesRead: Int
                do {
                    bytesRead = stream.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        it.write(buffer, 0, bytesRead)
                    }
                } while (bytesRead != -1)
            }
        }
    }

    fun unzip(zipFileFrom: File, directoryTo: File, buffer: ByteArray) {
        FileInputStream(zipFileFrom).use {
            ZipInputStream(BufferedInputStream(it)).use {
                var bytesRead: Int
                val stream = it
                var ze: ZipEntry?
                do {
                    ze = stream.getNextEntry()
                    if (ze != null) {
                        if (!ze.isDirectory) {
                            val destinationFile = File(directoryTo, ze.getName())
                            destinationFile.parentFile.mkdirs()
                            destinationFile.delete()
                            FileOutputStream(destinationFile).use {
                                do {
                                    bytesRead = stream.read(buffer, 0, buffer.size)
                                    if (bytesRead > 0) {
                                        it.write(buffer, 0, bytesRead)
                                    }
                                } while (bytesRead != -1)
                            }
                        }
                    }
                } while (ze != null)
            }
        }
    }
}
