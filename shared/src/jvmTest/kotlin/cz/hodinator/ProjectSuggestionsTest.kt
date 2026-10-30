package cz.hodinator

import cz.hodinator.models.NO_PROJECT_NAME
import cz.hodinator.models.TimeRecord
import cz.hodinator.utils.ProjectSuggestions
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSuggestionsTest {
    private fun record(name: String) = TimeRecord(projectName = name, durationSeconds = 60, startTime = Instant.parse("2026-01-01T00:00:00Z"))

    @Test fun collectsDistinctNamesInRecordOrder() {
        val records = listOf("B", "A", "B", NO_PROJECT_NAME, "C").map(::record)
        assertEquals(listOf("B", "A", "C"), ProjectSuggestions.projectNames(records))
    }

    @Test fun matchesAllWordsIgnoringCaseAndDiacritics() {
        val names = listOf("Udělat záznam akcí v KLP", "Schůzka KLP", "Oběd")
        assertEquals(listOf("Udělat záznam akcí v KLP"), ProjectSuggestions.suggest(names, "dělat"))
        assertEquals(listOf("Udělat záznam akcí v KLP"), ProjectSuggestions.suggest(names, "DELAT  klp"))
        assertEquals(listOf("Udělat záznam akcí v KLP", "Schůzka KLP"), ProjectSuggestions.suggest(names, "klp"))
        assertEquals(emptyList(), ProjectSuggestions.suggest(names, "  "))
    }

    @Test fun skipsExactMatchAndRespectsLimit() {
        assertEquals(emptyList(), ProjectSuggestions.suggest(listOf("Oběd"), "obed"))
        assertEquals(listOf("a1", "a2"), ProjectSuggestions.suggest(listOf("a1", "a2", "a3"), "a", limit = 2))
    }
}
