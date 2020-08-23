package de.randombyte.fourdata.gui

import de.randombyte.fourdata.FourData
import de.randombyte.fourdata.JobReporter.Result.TypedResult.ArchiveCreated
import de.randombyte.fourdata.archive.Archive
import de.randombyte.fourdata.archive.ArchivesStorage
import de.randombyte.fourdata.error
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import kotlin.system.exitProcess

class Gui(val fourData: FourData) {

    companion object {
        private const val COMPONENT_HEIGHT = 20
    }

    val window = JFrame()
    lateinit var sourceFolderSelectionPanel: FolderSelectionPanel
    lateinit var archivesFolderSelectionPanel: FolderSelectionPanel
    lateinit var archiveFolderSelectionPanel: FolderSelectionPanel
    lateinit var messagesTextbox: JTextArea
    lateinit var progressBar: JProgressBar

    init {
        setup()
        window.isVisible = true
    }

    private fun setup() {
        window.setSize(500, 400)
        window.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        window.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exitProcess(0)
                if (JOptionPane.showConfirmDialog(
                        window,
                        "Close?",
                        "Close?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                    ) == JOptionPane.YES_OPTION) {
                    exitProcess(0)
                }
            }
        })

        window.contentPane = JPanel().verticalLayout {
            sourceFolderSelectionPanel = addComponent(FolderSelectionPanel("Source folder:", COMPONENT_HEIGHT))
            archivesFolderSelectionPanel = addComponent(FolderSelectionPanel("Archives storage folder:", COMPONENT_HEIGHT))
            archiveFolderSelectionPanel = addComponent(FolderSelectionPanel("Archive folder:", COMPONENT_HEIGHT))

            create<JButton> {
                text = "Start Backup"
                addActionListener {
                    if (!sourceFolderSelectionPanel.folder.exists()) {
                        error("Select a valid source folder!")
                        return@addActionListener
                    }
                    if (!archivesFolderSelectionPanel.folder.exists()) {
                        error("Select a valid destination folder!")
                        return@addActionListener
                    }

                    val archivesStorage = ArchivesStorage(archivesFolderSelectionPanel.folder)
                    archivesStorage.createNewArchive(sourceFolderSelectionPanel.folder, object : GuiJobReporter(progressBar, messagesTextbox) {
                        override fun onEnd(result: Result) {
                            super.onEnd(result)

                            when (result) {
                                is ArchiveCreated -> {
                                    result.archive.updateDatabase(GuiJobReporter(progressBar, messagesTextBox))
                                }
                            }
                        }
                    })
                }
            }

            create<JButton> {
                text = "Update database"
                addActionListener {

                    var validArchive = true
                    if (!archiveFolderSelectionPanel.folder.exists()) validArchive = false
                    val archive = Archive.fromArchiveRootFolder(archiveFolderSelectionPanel.folder)
                    if (archive == null) validArchive = false

                    if (!validArchive) {
                        error("Select a valid archive folder!")
                        return@addActionListener
                    }

                    archive!!.updateDatabase(GuiJobReporter(progressBar, messagesTextbox))
                }
            }

            messagesTextbox = create<JTextArea> {
                rows = 5
                columns = 20
                isEditable = false
            }

            progressBar = create<JProgressBar> {
                minimum = 0
                maximum = 1
                isEnabled = false
                value = 0
            }
        }
    }
}
