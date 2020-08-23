package de.randombyte.fourdata.archive

import de.randombyte.fourdata.*
import de.randombyte.fourdata.JobReporter.Result.ArchiveAlreadyExists
import de.randombyte.fourdata.JobReporter.Result.TypedResult.ArchiveCreated
import de.randombyte.fourdata.JobReporter.Result.TypedResult.MessageResult.FileNotFound
import de.randombyte.fourdata.JobReporter.Result.TypedResult.MessageResult.GenericError
import de.randombyte.fourdata.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import java.io.File
import java.time.LocalDateTime

/**
 * Container of [Archive]s.
 */
class ArchivesStorage(val rootFolder: File) {

    @Serializable
    class Metadata(val lastSourceFolder: String)

    val archives: List<Archive>
        get() {
            val files = rootFolder.listFiles()
            if (files == null) {
                error("Could not list files of parent path ${rootFolder.absolutePath}! Is it a folder?")
                return emptyList()
            }

            return files
                .filter { it.isDirectory }
                .mapNotNull { Archive.fromArchiveRootFolder(it) }
        }

    private val archivesWithoutDatabase get() = archives.filter { !it.hasDatabase }

    private val allFileEntriesInDatabase: List<FileEntry>
        get() {
            // todo: maybe do computePathDuplicates() here?
            return archives.mapNotNull { it.database?.entries }.flatten()
        }

    class ArchiveEntryOccurrence(val parent: Archive, val entry: FileEntry)

/*    private fun computeDuplicates(getIdentifier: (FileEntry) -> String): Map<String, List<ArchiveEntryOccurrence>> {
        val archiveEntries = archives
            .filter { it.database != null }
            .map { archive -> archive to archive.database!!.entries }
            .toMap()

        val duplicates = mutableMapOf<String, MutableList<ArchiveEntryOccurrence>>()

        archiveEntries.forEach { (archive, entries) ->
            val otherArchiveEntries = archiveEntries.filter { (otherArchive, _) -> otherArchive != archive }

            otherArchiveEntries.forEach { (otherArchive, otherEntries) ->
                otherEntries.forEach { otherEntry ->
                    val otherEntryIdentity = getIdentifier(otherEntry)
                    if (entries.any { entry -> getIdentifier(entry) == otherEntryIdentity }) {
                        val duplicateOccurrences = duplicates[otherEntryIdentity] ?: mutableListOf()
                        // only add the "other" occurrence
                        // the "self" occurrence will be added later when the link is found the other way around
                        duplicateOccurrences += ArchiveEntryOccurrence(otherArchive, otherEntry)
                        duplicates[otherEntryIdentity] = duplicateOccurrences
                    }
                }
            }
        }

        return duplicates
    }*/

    private fun computeDuplicates(entries1: List<FileEntry>, entries2: List<FileEntry>, getIdentifier: (FileEntry) -> String): List<FileEntry> {
        val duplicates = mutableListOf<FileEntry>()

        entries1.forEach { entry1 ->
            if (entries2.any { entry2 -> getIdentifier(entry1) == getIdentifier(entry2) }) {
                duplicates += entry1
            }
        }

        return duplicates
    }

    fun createNewArchive(sourceFolder: File, jobReporter: JobReporter) = stoppableThread {
        if (archivesWithoutDatabase.isNotEmpty()) {
            jobReporter.end(GenericError("Before creating a new archive, every existing archive in this archive storage must have a database! " +
                    "Archives without a database: ${archivesWithoutDatabase.joinToString { it.archiveFile.absolutePath }}")
            )
            return@stoppableThread
        }

        val archiveName = LocalDateTimeSerializer.serialize(LocalDateTime.now())
        val newFolder = rootFolder.resolve(archiveName)
        if (newFolder.exists()) {
            jobReporter.end(ArchiveAlreadyExists)
            return@stoppableThread
        }
        newFolder.mkdirs()
        val newArchive = newFolder.resolve("$archiveName.tar")

        val newEntries = FileUtils.listFiles(sourceFolder, null, true)
            .map { file ->
                val pathWithoutRoot = File(file.path).pathWithoutRoot(sourceFolder).removePrefix(File.separator)
                FileEntry(pathWithoutRoot, file.inputStream().use { DigestUtils.md5Hex(it) })
            }
        val pathDuplicates = computeDuplicates(allFileEntriesInDatabase, newEntries, { it.path }).map { it.path }
        val newDistinctFiles = newEntries.filterNot { it.path in pathDuplicates }.map { File(it.path) }

        TarArchiver.archive(sourceFolder, newDistinctFiles, newArchive, object : JobReporter() {
            override fun onProgress(current: Int, total: Int) = jobReporter.progress(current, total)

            override fun onEnd(result: Result) {
                when (result) {
                    is Result.Success -> {
                        jobReporter.end(ArchiveCreated(Archive.fromArchiveRootFolder(newFolder)!!))
                    }
                    else -> {
                        if (result is FileNotFound) {
                            newFolder.delete()
                        }
                        jobReporter.end(result)
                    }
                }
            }
        })
    }
}
