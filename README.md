# Hodinátor

Jednoduchá desktopová aplikace na měření času stráveného na projektech. Napíšeš, na čem pracuješ, zmáčkneš **START**,
a po **STOP** se záznam uloží. Napsaná v Kotlinu s [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
pro desktop (JVM), data se ukládají lokálně do SQLite.

## Co umí

- **Časovač** – jeden běžící časovač s názvem projektu. Při zavření okna se rozběhnutý čas automaticky uloží.
- **Seznam záznamů** seskupený po dnech, s denními součty.
- **Inline editace** – název, datum, začátek i konec záznamu jde přímo přepsat. Uloží se po opuštění pole nebo Enterem.
- **Rychlé akce** – spustit znovu stejný projekt, vytvořit kopii záznamu, smazat.
- **Filtry** – vše / dnes / tento měsíc / minulý měsíc / vlastní rozsah, s celkovým součtem.
- **Měsíční přehled** – souhrnné statistiky a graf odpracovaných hodin po dnech (případně tabulka).
- **Export do CSV** – součty za měsíc po projektech, připravené pro Excel (UTF-8 s BOM, oddělovač `;`).

## Spuštění

Potřeba je JDK 17+.

```bash
./gradlew :desktopApp:run              # spuštění
./gradlew :desktopApp:hotRun --auto    # spuštění s hot reloadem při vývoji
./gradlew :shared:jvmTest              # testy
```

## Instalační balíček

```bash
./gradlew :desktopApp:packageDistributionForCurrentOS
```

Vytvoří instalátor pro aktuální OS (`.dmg` na macOS, `.msi` na Windows, `.deb` na Linuxu)
v `desktopApp/build/compose/binaries/main/`.

## Kde jsou data

| OS      | Databáze                                                  | Log                            |
|---------|-----------------------------------------------------------|--------------------------------|
| macOS   | `~/Library/Application Support/Hodinator/time_tracker.db` | `~/Library/Logs/Hodinator.log` |
| Windows | `%APPDATA%\Hodinator\time_tracker.db`                     | `%APPDATA%\Hodinator\Hodinator.log` |
| Linux   | `~/.local/share/Hodinator/time_tracker.db`                | `~/.local/share/Hodinator/Hodinator.log` |

Záloha = zkopírovat soubor `time_tracker.db`.

## Struktura projektu

- `desktopApp` – vstupní bod (`main.kt`): inicializace Koinu, okno aplikace, konfigurace balíčků.
- `shared` – veškerý kód aplikace:
  - `data/` – přístup k SQLite (`DatabaseManager`), cesty k souborům a logování (`AppFiles`, `AppLog`)
  - `models/` – datový model záznamu a filtrů
  - `ui/` – `TimeTrackerViewModel` (stav obrazovky jako jeden `StateFlow`) a kořenová `App`; části hlavní obrazovky v `screen/`, dialogy v `dialogs/`, znovupoužitelné komponenty v `components/`, barvy v `theme/`
  - `utils/` – práce s časem (`TimeUtils`) a generování CSV (`CsvExport`)
