package cz.pavlik.timetracker.ui

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Zobrazí nativní dialog pro uložení CSV souboru a vrátí zvolený soubor (vždy s příponou `.csv`),
 * nebo `null`, pokud uživatel volbu zrušil. Pokud nativní dialog není k dispozici, použije se Swing.
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
