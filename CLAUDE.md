# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"Hodinátor" — a Compose Multiplatform desktop (JVM-only) time-tracking app. Start/stop a timer per project, records are stored in SQLite, listed/filtered by day, editable inline, and exportable as a monthly CSV summary. UI strings and CSV headers are in Czech; keep new user-facing text in Czech. Code comments, KDoc and log messages are in English.

## Commands

- Run: `./gradlew :desktopApp:run`
- Run with hot reload: `./gradlew :desktopApp:hotRun --auto`
- Tests: `./gradlew :shared:jvmTest` (single test: `./gradlew :shared:jvmTest --tests "cz.hodinator.TimeUtilsTest"`). Tests live in `shared/src/jvmTest/kotlin` and use `kotlin.test`; `DatabaseManagerTest` runs against a temp SQLite file.
- Package native installer for current OS: `./gradlew :desktopApp:packageDmg` (also `packageMsi`, `packageDeb`, `packagePkg`; `packageDistributionForCurrentOS`). The app bundle is named `Hodinator`.

Dependency versions live in `gradle/libs.versions.toml`. Configuration cache and build cache are enabled in `gradle.properties`.

## Architecture

Two Gradle modules:
- `desktopApp` — only `main.kt`: starts Koin with `appModule`, gets the `TimeTrackerViewModel` singleton, and opens the Compose `Window` hosting `App(viewModel)`. On window close it calls `viewModel.saveRunningTimer()` (blocking) so a running timer is not lost. Packaging config (`compose.desktop { nativeDistributions }`) lives here; `modules("java.sql")` is required so the packaged runtime includes JDBC.
- `shared` — all app code. Although it is a KMP module, the only target is `jvm()`, and `commonMain` freely uses JVM APIs (`java.sql`, `java.io.File`, AWT/Swing file dialogs). Adding a non-JVM target would require moving that code to `jvmMain` behind `expect`/`actual`.

Flow inside `shared/src/commonMain/kotlin/cz/hodinator/`:
- `DI.kt` — Koin `appModule`; `DatabaseManager` and `TimeTrackerViewModel` are singletons, wired by constructor injection.
- `data/AppFiles.kt` — per-OS paths (`~/Library/Application Support/Hodinator` on macOS, `%APPDATA%/Hodinator` on Windows, `~/.local/share/Hodinator` on Linux) and `AppLog` (macOS: `~/Library/Logs/Hodinator.log`, elsewhere next to the DB; also printed to stdout).
- `data/DatabaseManager.kt` — raw JDBC over SQLite (`records` table: `id`, `project_name`, `duration_seconds`, `timestamp` = record **start** as ISO-8601 text). Opens a new connection per call on `Dispatchers.IO`; the schema is created lazily on first use with `CREATE TABLE IF NOT EXISTS` (no migration system). Errors are thrown to the caller. Blank project names are stored as `NO_PROJECT_NAME` ("Bez projektu").
- `models/TimeModel.kt` — `TimeRecord(id, projectName, durationSeconds, startTime)` with computed `endTime`; `TimeFilter` enum.
- `ui/TimeTrackerViewModel.kt` — exposes a single `StateFlow<TimeTrackerState>`. Every mutation goes through `launchDbAction`, which writes to the DB, reloads the full list, and on failure logs and shows an error toast. `filteredRecords`/`totalSeconds` are lazy properties of the state. The running timer keeps `startTime` in memory and derives `elapsedSeconds` from the clock each second; a record is only persisted when the timer is stopped (or the window closes). Toasts are a timed `toast: Toast?` field (`isError` for failures). Saving a timer record bumps `scrollToTopRequest`, which `RecordList` observes to scroll its `LazyListState` to the top (copies don't).
- UI composables:
  - `ui/App.kt` — root screen: lays out the sections, holds dialog visibility, shows the toast.
  - `ui/screen/` — main screen sections: `TimerBar`, `FilterBar` (icon-only buttons and horizontally scrolling pills when narrow), `RecordList` (day-grouped list, inline-editable `RecordCard`).
  - `ui/dialogs/` — `ExportCsvDialog`, `DeleteConfirmDialog`, `MonthlyStatsDialog` (stat tiles + per-day bar chart drawn with plain Compose layouts/Canvas, no chart library; table toggle) and `SaveFileDialog.kt` (AWT `FileDialog` save prompt with `JFileChooser` fallback; only picks the file, the ViewModel writes it).
  - `ui/components/` — reusable pieces: `Buttons` (`SelectablePill`, `ToolbarButton`), `Cards` (`cardBackground`, `SectionCard`), `DialogScaffold` (`AppDialog`, `DialogTitle`, `DialogText`, `DialogButtons`), `TextFields` (`InlineEditableText`, `TimeInput`, `appTextFieldColors`, `withoutLineBreaks`), `Toast`.
  - `ui/theme/Colors.kt` — theme colors (dark-only `AppColorScheme`).
- `utils/TimeUtils.kt` — formatting/parsing (dates `dd.MM.yyyy`, times `HH:mm`), Czech day/month names, record filtering by `TimeFilter`, daily totals for a month.
- `utils/CsvExport.kt` — monthly CSV (UTF-8 BOM, `;` separator, RFC-4180 quoting, totals per project sorted by time).
