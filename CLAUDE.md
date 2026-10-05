# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Hodinátor" — a Compose Multiplatform desktop (JVM-only) time-tracking app. Start/stop a timer per project, records are stored in SQLite, listed/filtered by day, editable inline, and exportable as a monthly CSV summary. UI strings, comments and CSV headers are in Czech; keep new user-facing text in Czech.

## Commands

- Run: `./gradlew :desktopApp:run`
- Run with hot reload: `./gradlew :desktopApp:hotRun --auto`
- Tests: `./gradlew :shared:jvmTest` (single test: `./gradlew :shared:jvmTest --tests "cz.pavlik.timetracker.SomeTest"`). No test sources exist yet — they would go in `shared/src/jvmTest/kotlin` (or `commonTest`).
- Package native installer for current OS: `./gradlew :desktopApp:packageDmg` (also `packageMsi`, `packageDeb`, `packagePkg`; `packageDistributionForCurrentOS`).

Dependency versions live in `gradle/libs.versions.toml`. Configuration cache and build cache are enabled in `gradle.properties`.

## Architecture

Two Gradle modules:
- `desktopApp` — only `main.kt`: starts Koin with `appModule`, opens the Compose `Window` hosting `App()`. Packaging config (`compose.desktop { nativeDistributions }`) lives here; `modules("java.sql")` is required so the packaged runtime includes JDBC.
- `shared` — all app code. Although it is a KMP module, the only target is `jvm()`, and `commonMain` freely uses JVM APIs (`java.sql`, `java.io.File`, AWT/Swing file dialogs). Adding a non-JVM target would require moving that code to `jvmMain` behind `expect`/`actual`.

Flow inside `shared/src/commonMain/kotlin/cz/pavlik/timetracker/`:
- `DI.kt` — Koin `appModule`; `DatabaseManager` is a singleton.
- `data/DatabaseManager.kt` — raw JDBC over SQLite (`records` table: `id`, `project_name`, `duration_seconds`, `timestamp` as ISO-8601 text). Opens a new connection per call on `Dispatchers.IO`; errors are caught and logged, never thrown. Blank project names become `"Bez projektu"`. Schema is created with `CREATE TABLE IF NOT EXISTS` — there is no migration system.
  - DB location: `~/Library/Application Support/Hodinator/time_tracker.db` (macOS), `%APPDATA%/Hodinator` (Windows), `~/.local/share/Hodinator` (Linux). On first run it copies an old dev DB from `./time_tracker.db` or `.../cz.pavlik.timetracker/` if present.
  - Log file: `~/Library/Logs/Hodinator.log` (via `logApp`, also printed to stdout).
- `ui/ViewModel.kt` — `TimeTrackerViewModel` (gets `DatabaseManager` via `KoinComponent.inject`) exposes a single `StateFlow<TimeTrackerState>`. Every mutation writes to the DB then calls `loadRecords()` to reload the full list. Filtering (`filteredRecords`, `totalSeconds`) is computed in the state class from `currentFilter` + custom date strings. The running timer is an in-memory coroutine ticking `elapsedSeconds`; a record is only persisted when the timer is stopped. Toasts are a timed `toastMessage` field.
- `ui/Screen.kt` — all composables (`App` → `TimeTrackerScreen` with timer bar, filter bar, day-grouped record list, inline editing, CSV export and delete dialogs). Theme colors are in `ui/utils/Colors.kt` (dark-only `customColorScheme`).
- `utils/TimeUtils.kt` — formatting/parsing (dates as `dd.MM.yyyy`, times `HH:mm`), record filtering by `TimeFilter`, and monthly CSV generation (UTF-8 BOM, `;` separator, totals grouped by project). `utils/CsvExportUtils.kt` shows an AWT `FileDialog` save prompt, falling back to `JFileChooser`.
