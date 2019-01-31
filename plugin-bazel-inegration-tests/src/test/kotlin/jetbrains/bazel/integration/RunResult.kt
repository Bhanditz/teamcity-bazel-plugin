package jetbrains.bazel.integration

data class RunResult(
        val exitCode: Int = 0,
        val stdOut: List<String> = emptyList(),
        val stdErr: List<String> = emptyList(),
        val serviceMessages: List<ServiceMessage> = emptyList())