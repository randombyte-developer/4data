@file:UseSerializers(LocalDateTimeSerializer::class)
package de.randombyte.fourdata.archive

import de.randombyte.fourdata.JobReporter
import de.randombyte.fourdata.TarArchiver
import de.randombyte.fourdata.serialization.LocalDateTimeSerializer
import de.randombyte.fourdata.serialization.json
import de.randombyte.fourdata.stoppableThread
import de.randombyte.fourdata.tar
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.FileNotFoundException
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
    class Database(val lastUpdated: LocalDateTime, val entries: List<ArchiveEntry>)

    @Serializable
    class ArchiveEntry(val path: String, val md5Sum: String) {
        fun isSamePath(other: ArchiveEntry) = path == other.path
        fun isSameMd5Sum(other: ArchiveEntry) = md5Sum == other.md5Sum
    }

    val hasDatabase get() = databaseFile.exists()

    val database: Database? get() {
        if (!hasDatabase) return null
        return json.parse(Database.serializer(), databaseFile.readText())
    }

    fun updateDatabase(jobReporter: JobReporter) {
        collectEntriesFromArchiveFile(object : JobReporter() {
            override fun onProgress(current: Int, total: Int) = jobReporter.progress(current, total)

            override fun onEnd(result: TarArchiver.Result) {
                when (result) {
                    is TarArchiver.Result.TypedResult.FileEntries -> {
                        val archiveDatabase = Database(
                            LocalDateTime.now(),
                            result.entries
                        )
                        val databaseString = json.stringify(Database.serializer(), archiveDatabase)
                        databaseFile.writeText(databaseString)
                        jobReporter.end(TarArchiver.Result.Success)
                    }
                    else -> jobReporter.end(result)
                }
            }
        })
    }

    private fun collectEntriesFromArchiveFile(jobReporter: JobReporter) =
        stoppableThread {
            val fileEntries = mutableListOf<ArchiveEntry>()

            var error = false

            try {
                archiveFile.inputStream().buffered().tar().use { inputStream ->
                    var archiveEntry: org.apache.commons.compress.archivers.ArchiveEntry? = inputStream.nextTarEntry
                    while (archiveEntry != null) {
                        if (!inputStream.canReadEntryData(archiveEntry)) {
                            jobReporter.end(
                                TarArchiver.Result.TypedResult.MessageResult.CanNotReadArchiveEntry(
                                    "Can't read archive entry '${archiveEntry.name}' of size ${archiveEntry.size}!"
                                )
                            )
                            error = true
                            break
                        }

                        if (!archiveEntry.isDirectory) {
                            fileEntries += ArchiveEntry(
                                archiveEntry.name,
                                DigestUtils.md5Hex(inputStream)
                            )
                            jobReporter.progress(
                                fileEntries.size,
                                JobReporter.UNKNOWN_TOTAL
                            )
                        }

                        archiveEntry = inputStream.nextTarEntry
                    }
                }
            } catch (fileNotFoundException: FileNotFoundException) {
                jobReporter.end(TarArchiver.Result.ArchiveNotFound)
                error = true
            }

            if (!error) jobReporter.end(
                TarArchiver.Result.TypedResult.FileEntries(
                    fileEntries
                )
            )
        }
}