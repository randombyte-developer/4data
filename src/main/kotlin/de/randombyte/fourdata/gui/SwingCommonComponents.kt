package de.randombyte.fourdata.gui

import java.awt.Dimension
import java.io.File
import javax.swing.*

class FolderSelectionPanel(text: String, height: Int) : JPanel() {

    lateinit var folderPathTextField: JTextField

    init {
        horizontalLayout {
            create<JLabel> {
                size = Dimension(100, height)
                setText(text)
            }

            folderPathTextField = create {
                columns = 18
                size = Dimension(100, height)
            }

            create<JButton> {
                size = Dimension(30, height)
                setText("...")
                addActionListener {
                    JFileChooser().apply {
                        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                        if (showOpenDialog(this@FolderSelectionPanel) == JFileChooser.APPROVE_OPTION) {
                            folderPathTextField.text = selectedFile.toString()
                        }
                    }
                }
            }
        }
    }

    val folder: File get() =  File(folderPathTextField.text)
}