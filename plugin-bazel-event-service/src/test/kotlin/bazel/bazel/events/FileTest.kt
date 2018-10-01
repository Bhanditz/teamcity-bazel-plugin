package bazel.bazel.events

import com.intellij.openapi.util.SystemInfo
import org.junit.Assert
import org.junit.Assume
import org.junit.Test

class FileTest {

    @Test
    fun testWindowsUri() {
        Assume.assumeTrue(SystemInfo.isWindows)

        val files = listOf(
                "file://C:/Windows/test.log",
                "file:///C:/Windows/test.log"
        )

        for (file in files) {
            val eventFile = File("name", file)
            Assert.assertEquals("C:\\Windows\\test.log", eventFile.filePath)
        }
    }
}