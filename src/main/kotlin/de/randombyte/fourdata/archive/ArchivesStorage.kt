package de.randombyte.fourdata.archive

import de.randombyte.fourdata.archive.Archive.ArchiveEntry
import de.randombyte.fourdata.error
import kotlinx.serialization.Serializable
import java.io.File

/**
 * Container of [Archive]s.
 */
class ArchivesStorage(val rootFolder: File) {

    @Serializable
    class Metadata(val lastSourceFolder: String)

    val archives: List<Archive> get() {
        val files = rootFolder.listFiles()
        if (files == null) {
            error("Could not list files of parent path ${rootFolder.absolutePath}! Is it a folder?")
            return emptyList()
        }

        return files
            .filter { it.isDirectory }
            .mapNotNull { Archive.fromArchiveRootFolder(it) }
    }

    val archivesWithoutDatabase get() = archives.filter { !it.hasDatabase }

    val allArchiveEntriesInDatabase: List<ArchiveEntry>
        get() {
            // todo: maybe do computePathDuplicates() here?
            return archives.mapNotNull { it.database?.entries }.flatten()
        }

    class ArchiveEntryOccurrence(val parent: Archive, val entry: ArchiveEntry)

    private fun computeDuplicates(getIdentifier: (ArchiveEntry) -> String): Map<String, List<ArchiveEntryOccurrence>> {
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
    }

    fun computePathDuplicates() = computeDuplicates { it.path }
    fun computeMd5Duplicates() = computeDuplicates { it.md5Sum }

    /**
     * @return success
     */
    fun createNewArchive(sourceFolder: File): Boolean {
        if (archivesWithoutDatabase.isNotEmpty()) {
            error("Before creating a new archive, every existing archive in this archive storage must have a database! " +
                    "Archives without a database: ${archivesWithoutDatabase.joinToString { it.archiveFile.absolutePath }}")
            return false
        }

        // foreach sourcefolder.files check if path exists in allArchiveEntriesInDatabase
    }

}
