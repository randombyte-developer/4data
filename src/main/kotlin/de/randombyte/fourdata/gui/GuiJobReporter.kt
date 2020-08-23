package de.randombyte.fourdata.gui

import de.randombyte.fourdata.JobReporter
import de.randombyte.fourdata.JobReporter.Result.TypedResult.MessageResult
import javax.swing.JProgressBar
import javax.swing.JTextArea

open class GuiJobReporter(val progressBar: JProgressBar, val messagesTextBox: JTextArea) : JobReporter() {
    override fun onProgress(current: Int, total: Int) {
        progressBar.maximum = total
        progressBar.value = current
    }

    override fun onEnd(result: Result) {
        when (result) {
            is MessageResult -> {
                messagesTextBox.append("${result.message}\n")
            }
            else -> {
                messagesTextBox.append("${result::class.simpleName}\n")
            }
        }
    }
}