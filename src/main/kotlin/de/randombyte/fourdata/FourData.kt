package de.randombyte.fourdata

import de.randombyte.fourdata.TarArchiver.Result.ArchiveAlreadyExists
import de.randombyte.fourdata.TarArchiver.Result.TypedResult.MessageResult.FileNotFound
import de.randombyte.fourdata.archive.ArchivesStorage
import de.randombyte.fourdata.gui.Gui
import de.randombyte.fourdata.serialization.LocalDateTimeSerializer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    val fourData = FourData()
    val gui = Gui(fourData)
}

class FourData() {

    val archivesStorage = ArchivesStorage

    fun backup(sourceFolder: File, archivesFolder: File, jobReporter: JobReporter) {
        val archiveName = LocalDateTimeSerializer.serialize(LocalDateTime.now())
        val newFolder = archivesFolder.resolve(archiveName)
        if (newFolder.exists()) {
            jobReporter.end(ArchiveAlreadyExists)
            return
        }
        newFolder.mkdirs()
        val newArchive = newFolder.resolve("$archiveName.tar")

        TarArchiver.archive(sourceFolder, newArchive, object : JobReporter() {
            override fun onProgress(current: Int, total: Int) = jobReporter.progress(current, total)

            override fun onEnd(result: TarArchiver.Result) {
                when (result) {
                    is FileNotFound -> {
                        newFolder.delete()
                    }
                }

                jobReporter.end(result)
            }
        })
    }

    fun updateExternalFileEntriesDatabase(archive: File, database: File, jobReporter: JobReporter) {
        ArchiveEntriesDatabase.update(archive, database, jobReporter)
    }
}