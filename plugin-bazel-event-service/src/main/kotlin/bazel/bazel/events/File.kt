package bazel.bazel.events

import com.intellij.openapi.util.SystemInfo
import java.net.URI

class File private constructor(val name: String) {
    private var fileContent: ByteArray? = null
    private var fileUri: String? = null

    /**
     * File with URI.
     *
     * @name - identifier indicating the nature of the file (e.g., "stdout", "stderr")
     * @uri - location where the contents of the file can be found. The string is encoded according to RFC2396.
     */
    constructor(name: String, uri: String) : this(name) {
        fileUri = uri
    }

    /**
     * File with contents.
     *
     * @name - identifier indicating the nature of the file (e.g., "stdout", "stderr")
     * @content - The contents of the file, if they are guaranteed to be short.
     */
    constructor(name: String, content: ByteArray) : this(name) {
        fileContent = content
    }

    val content: String
        get() =
            when {
                fileContent != null -> fileContent!!.contentToString()
                !fileUri.isNullOrBlank() -> getFile(fileUri!!).readText()
                else -> ""
            }

    val filePath: String by lazy {
        val file = if (!fileUri.isNullOrBlank()) {
            getFile(fileUri!!)
        } else {
            val tempFile = java.io.File.createTempFile("bazel-event-file", "tmp")
            tempFile.writeText(content)
            tempFile.deleteOnExit()
            tempFile
        }
        file.canonicalPath
    }

    companion object {
        val empty = File("", "")
        private val FILE_SCHEMA = Regex("(file\\:\\/\\/)([a-z]+\\:\\/.*)", RegexOption.IGNORE_CASE)

        private fun normalizeURI(uri: String): String {
            if (!SystemInfo.isWindows) return uri
            return FILE_SCHEMA.matchEntire(uri)?.let {
                val (_, path) = it.destructured
                return "file:////$path"
            } ?: uri
        }

        private fun getFile(uri: String): java.io.File {
            return java.io.File(URI(normalizeURI(uri)))
        }
    }
}