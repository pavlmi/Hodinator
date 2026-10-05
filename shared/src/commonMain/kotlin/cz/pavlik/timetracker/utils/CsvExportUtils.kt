package cz.pavlik.timetracker.utils

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


object CsvExportUtils {
    fun saveCsvFile(defaultFileName: String, content: String): File? {
        try {
            val fileDialog = FileDialog(null as Frame?, "Uložit CSV měsíční přehled", FileDialog.SAVE).apply {
                file = defaultFileName
                isMultipleMode = false
                isVisible = true
            }

            val dir = fileDialog.directory
            val filename = fileDialog.file

            if (dir != null && filename != null) {
                val finalFileName =
                    if (!filename.endsWith(".csv", ignoreCase = true)) "$filename.csv"
                    else filename

                val targetFile = File(dir, finalFileName)
                targetFile.writeText(content, Charsets.UTF_8)
                return targetFile
            } else return null
        } catch (_: Throwable) {
            return saveWithJFileChooser(defaultFileName, content)
        }
    }

    private fun saveWithJFileChooser(defaultFileName: String, content: String): File? {
        val fileChooser = JFileChooser().apply {
            dialogTitle = "Uložit CSV měsíční přehled"
            selectedFile = File(defaultFileName)
            fileFilter = FileNameExtensionFilter("CSV soubory (*.csv)", "csv")
        }
        val userSelection = fileChooser.showSaveDialog(null)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            val selected = fileChooser.selectedFile ?: return null
            val dir = selected.parentFile ?: fileChooser.currentDirectory ?: File(".")
            val finalFileName =
                if (!selected.name.endsWith(".csv", ignoreCase = true)) "${selected.name}.csv"
                else selected.name

            val targetFile = File(dir, finalFileName)
            targetFile.writeText(content, Charsets.UTF_8)
            return targetFile
        }
        return null
    }
}
