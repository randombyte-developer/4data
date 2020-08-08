package de.randombyte.fourdata

import de.randombyte.fourdata.ArchiveEntriesDatabase.FileEntry
import de.randombyte.fourdata.TarArchiver.Result.Success
import de.randombyte.fourdata.TarArchiver.Result.TypedResult.MessageResult.FileNotFound
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

object TarArchiver {

    sealed class Result {
        object Success : Result()
        object StoppedByUser : Result()
        object ArchiveAlreadyExists : Result()
        object ArchiveNotFound : Result()

        sealed class TypedResult<T>(val result: T) : Result() {
            sealed class MessageResult(message: String) : TypedResult<String>(message) {
                class FileNotFound(message: String) : MessageResult(message)
                class CanNotReadArchiveEntry(message: String) : MessageResult(message)
            }

            class FileEntries(val entries: List<FileEntry>) : TypedResult<List<FileEntry>>(entries)
        }
    }

    fun archive(source: File, target: File, jobReporter: JobReporter) = stoppableThread {
        if (target.exists()) {
            jobReporter.end(Result.ArchiveAlreadyExists)
            return@stoppableThread
        }

        try {
            val files = FileUtils.listFiles(source, null, true)

            target.outputStream().buffered().tar().use { outputStream ->
                // setup archive
                outputStream.apply {
                    setAddPaxHeadersForNonAsciiNames(true)
                    setBigNumberMode(BIGNUMBER_POSIX)
                    setLongFileMode(LONGFILE_POSIX)
                }

                // add entries
                files.forEachIndexedStoppable { index, file ->
                    val archiveEntry = outputStream.createArchiveEntry(file, file.pathWithoutRoot(source))
                    outputStream.putArchiveEntry(archiveEntry)
                    file.inputStream().use { it.copyTo(outputStream) }
                    outputStream.closeArchiveEntry()

                    jobReporter.progress(index + 1, files.size)

                    return@forEachIndexedStoppable stopped
                }
            }

            jobReporter.end(Success)
        } catch (fileNotFoundException: FileNotFoundException) {
            jobReporter.end(FileNotFound(fileNotFoundException.message ?: ""))
        }
    }
}

fun OutputStream.tar() = TarArchiveOutputStream(this)
fun InputStream.tar() = TarArchiveInputStream(this)
fun File.pathWithoutRoot(root: File) = canonicalPath.removePrefix(root.canonicalPath)