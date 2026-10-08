package cz.hodinator.ui.dialogs

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


/**
 * Shows a native save dialog for a CSV file and returns the chosen file (always with a `.csv` extension),
 * or `null` if the user cancelled. Falls back to Swing when the native dialog isn't available.
 */
fun chooseCsvSaveFile(title: String, defaultFileName: String): File? {
    val selected = try {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply {
            file = defaultFileName
            isVisible = true
        }
        val directory = dialog.directory ?: return null
        val fileName = dialog.file ?: return null
        File(directory, fileName)
    } catch (_: Exception) {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            selectedFile = File(defaultFileName)
            fileFilter = FileNameExtensionFilter("CSV soubory (*.csv)", "csv")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
        chooser.selectedFile ?: return null
    }
    return if (selected.extension.equals("csv", ignoreCase = true)) selected else File(selected.path + ".csv")
}
