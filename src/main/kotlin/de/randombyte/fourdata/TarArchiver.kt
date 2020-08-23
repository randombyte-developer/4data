package de.randombyte.fourdata

import de.randombyte.fourdata.JobReporter.Result.ArchiveAlreadyExists
import de.randombyte.fourdata.JobReporter.Result.Success
import de.randombyte.fourdata.JobReporter.Result.TypedResult.MessageResult.FileNotFound
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

object TarArchiver {

    fun archive(sourceRoot: File, sourceFilesRelativeToRoot: List<File>, target: File, jobReporter: JobReporter) = stoppableThread {
        if (target.exists()) {
            jobReporter.end(ArchiveAlreadyExists)
            return@stoppableThread
        }

        try {
            target.outputStream().buffered().tar().use { outputStream ->
                // setup archive
                outputStream.apply {
                    setAddPaxHeadersForNonAsciiNames(true)
                    setBigNumberMode(BIGNUMBER_POSIX)
                    setLongFileMode(LONGFILE_POSIX)
                }

                // add entries
                sourceFilesRelativeToRoot.forEachIndexedStoppable { index, fileRelativeToRoot ->
                    val fullyQualifiedFile = sourceRoot.resolve(fileRelativeToRoot)
                    val archiveEntry = outputStream.createArchiveEntry(fullyQualifiedFile, fileRelativeToRoot.path)
                    outputStream.putArchiveEntry(archiveEntry)
                    fullyQualifiedFile.inputStream().use { it.copyTo(outputStream) }
                    outputStream.closeArchiveEntry()

                    jobReporter.progress(index + 1, sourceFilesRelativeToRoot.size)

                    return@forEachIndexedStoppable stopped
                }
            }

            if (stopped) {
                jobReporter.end(JobReporter.Result.StoppedByUser)
                return@stoppableThread
            }

            jobReporter.end(Success)
        } catch (fileNotFoundException: FileNotFoundException) {
            jobReporter.end(FileNotFound(fileNotFoundException.message ?: ""))
        }
    }
}

fun OutputStream.tar() = TarArchiveOutputStream(this)
fun InputStream.tar() = TarArchiveInputStream(this)
