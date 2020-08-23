@file:UseSerializers(LocalDateTimeSerializer::class)
package de.randombyte.fourdata.archive

import de.randombyte.fourdata.JobReporter
import de.randombyte.fourdata.JobReporter.Result.ArchiveNotFound
import de.randombyte.fourdata.JobReporter.Result.Success
import de.randombyte.fourdata.JobReporter.Result.TypedResult.FileEntries
import de.randombyte.fourdata.JobReporter.Result.TypedResult.MessageResult.CanNotReadArchiveEntry
import de.randombyte.fourdata.serialization.LocalDateTimeSerializer
import de.randombyte.fourdata.serialization.json
import de.randombyte.fourdata.stoppableThread
import de.randombyte.fourdata.tar
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class Archive(val timestamp: LocalDateTime, val archiveFile: File, val databaseFile: File) {

    companion object {
        fun fromArchiveRootFolder(rootFolder: File): Archive? {
            // test if it is a valid timestamp
            val timestamp = try {
                LocalDateTimeSerializer.deserialize(rootFolder.name)
            } catch (dateTimeParseException: DateTimeParseException) {
                return null
            }

            val archiveFile = rootFolder.resolve("${rootFolder.name}.tar")
            if (!archiveFile.exists()) return null

            // doesn't have to exist
            val databaseFile = rootFolder.resolve("${rootFolder.name}.json")

            return Archive(timestamp, archiveFile, databaseFile)
        }
    }

    @Serializable
    class Database(val lastUpdated: LocalDateTime, val entries: List<FileEntry>)

    val hasDatabase get() = databaseFile.exists()

    val database: Database? get() {
        if (!hasDatabase) return null
        return json.parse(Database.serializer(), databaseFile.readText())
    }

    fun updateDatabase(jobReporter: JobReporter) {
        collectEntriesFromArchiveFile(object : JobReporter() {
            override fun onProgress(current: Int, total: Int) = jobReporter.progress(current, total)

            override fun onEnd(result: Result) {
                when (result) {
                    is FileEntries -> {
                        val archiveDatabase = Database(LocalDateTime.now(), result.entries)
                        val databaseString = json.stringify(Database.serializer(), archiveDatabase)
                        databaseFile.writeText(databaseString)
                        jobReporter.end(Success)
                    }
                    else -> jobReporter.end(result)
                }
            }
        })
    }

    private fun collectEntriesFromArchiveFile(jobReporter: JobReporter) = stoppableThread {
        val fileEntries = mutableListOf<FileEntry>()

        if (!archiveFile.exists()) {
            jobReporter.end(ArchiveNotFound)
            return@stoppableThread
        }
        archiveFile.inputStream().buffered().tar().use { inputStream ->
            var archiveEntry: ArchiveEntry? = inputStream.nextTarEntry
            while (archiveEntry != null) {
                if (!inputStream.canReadEntryData(archiveEntry)) {
                    jobReporter.end(CanNotReadArchiveEntry("Can't read archive entry '${archiveEntry.name}' of size ${archiveEntry.size}!"))
                    return@stoppableThread
                }

                if (!archiveEntry.isDirectory) {
                    fileEntries += FileEntry(archiveEntry.name, DigestUtils.md5Hex(inputStream))
                    jobReporter.progress(fileEntries.size, JobReporter.UNKNOWN_TOTAL)
                }

                archiveEntry = inputStream.nextTarEntry
            }
        }

        jobReporter.end(FileEntries(fileEntries))
    }
}
