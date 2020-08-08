package de.randombyte.fourdata.gui

import de.randombyte.fourdata.FourData
import de.randombyte.fourdata.JobReporter
import de.randombyte.fourdata.TarArchiver.Result
import de.randombyte.fourdata.TarArchiver.Result.ArchiveAlreadyExists
import de.randombyte.fourdata.TarArchiver.Result.Success
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import kotlin.system.exitProcess

class Gui(val fourData: FourData) {

    companion object {
        private const val COMPONENT_HEIGHT = 20
    }

    val window = JFrame()
    lateinit var sourceFolderSelectionPanel: FolderSelectionPanel
    lateinit var archivesFolderSelectionPanel: FolderSelectionPanel

    init {
        setup()
        window.isVisible = true
    }

    private fun setup() {
        window.setSize(400, 300)
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
            sourceFolderSelectionPanel = addComponent(FolderSelectionPanel("Backup folder:", COMPONENT_HEIGHT))
            archivesFolderSelectionPanel = addComponent(FolderSelectionPanel("Archives folder:", COMPONENT_HEIGHT))

            create<JButton> {
                text = "Start Backup"
                addActionListener {
                    fourData.backup(
                        File("/home/randombyte/Downloads/tmp/source"),
                        File("/home/randombyte/Downloads/tmp/archives"),
                        object : JobReporter() {
                            override fun onProgress(current: Int, total: Int) {
                                println("Progress: $current / $total")
                            }

                            override fun onEnd(result: Result) {
                                when (result) {
                                    is ArchiveAlreadyExists -> {
                                        println("Archive already exists!")
                                    }
                                    else -> {
                                        println("Done")
                                    }
                                }
                            }
                        }
                    )
                    //fourData.startBackup(sourceFolderSelectionPanel.folder, archivesFolderSelectionPanel.folder)
                }
            }

            create<JButton> {
                text = "Update database"
                addActionListener {
                    fourData.updateExternalFileEntriesDatabase(
                        File("/home/randombyte/Downloads/tmp/tmp.tar"),
                        File("/home/randombyte/Downloads/tmp/tmp.json"),
                        object : JobReporter() {
                            override fun onProgress(current: Int, total: Int) {
                                println("Progress: $current / $total")
                            }

                            override fun onEnd(result: Result) {
                                when (result) {
                                    is Result.ArchiveNotFound -> {
                                        println("Archive not found!")
                                    }
                                    is Success -> {
                                        println("Done")
                                    }
                                }
                            }
                        }
                    )
                    //fourData.startBackup(sourceFolderSelectionPanel.folder, archivesFolderSelectionPanel.folder)
                }
            }
        }
    }
}
