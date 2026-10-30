package cz.hodinator.utils

import cz.hodinator.models.NO_PROJECT_NAME
import cz.hodinator.models.TimeRecord
import java.text.Normalizer


object ProjectSuggestions {
    /** Distinct project names, most recently used first (records are expected newest first). */
    fun projectNames(records: List<TimeRecord>): List<String> =
        records.map { it.projectName }.distinct().filter { it != NO_PROJECT_NAME }

    /** Names containing every word of [query], ignoring case and diacritics. */
    fun suggest(projectNames: List<String>, query: String, limit: Int = 8): List<String> {
        val simpleQuery = simplify(query)
        if (simpleQuery.isEmpty()) return emptyList()
        val words = simpleQuery.split(' ')
        return projectNames.asSequence()
            .filter { name -> simplify(name).let { simple -> simple != simpleQuery && words.all { it in simple } } }
            .take(limit)
            .toList()
    }

    private val DIACRITICS = Regex("\\p{M}+")
    private val WHITESPACE = Regex("\\s+")

    private fun simplify(text: String) =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(DIACRITICS, "").replace(WHITESPACE, " ").trim().lowercase()
}
