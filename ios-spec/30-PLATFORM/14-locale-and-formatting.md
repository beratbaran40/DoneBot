---
id: 30-14
title: Locale control & platform formatting
layer: platform
status: TODO
depends_on: [20-04, 20-13, 30-00]
blocks: [40-settings-01]
parallel_safe: true
estimate: 12h
reversible: true
owner_files:
  - shared/domain/src/commonMain/**/locale/**
  - shared/data/src/androidMain/**/locale/**
  - shared/data/src/iosMain/**/locale/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

In-app language switching on iOS, and locale-aware month/weekday names — the gap `kotlinx-datetime` leaves open.

## 2. Why this way

**Two related problems, one task.**

*In-app language override.* DoneBot lets the user pick EN or TR inside the app, independent of the system language. Android does this with `AppCompatDelegate.setApplicationLocales` / `LocaleManager`. iOS has no direct equivalent: the system language is chosen in Settings. The workable approach is to write `AppleLanguages` into `UserDefaults` and tell the user a restart is needed — plus override CMP's resource locale so the *content* switches immediately even if system-formatted values do not.

*Localized date names.* `kotlinx-datetime` formats but does not localize. `20-04` introduced `PlatformFormatting` as a stub; this task implements it properly. It matters more than it sounds: **Turkish is half the user base**, and the failure mode is silent — month names simply come out English.

**Both must honour the in-app override, not the system locale.** A user with a Turkish phone who picked English must see English month names. Reading `NSLocale.current` would silently ignore their choice.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `navigation/NavGraph.kt` (~273-285) | The Android locale switch, API 33+ vs older |
| `data/repository/LanguageRepositoryImpl.kt` | Where the choice is persisted |
| `domain/model/LanguagePreference` | The value type |
| `app/src/main/res/xml/locales_config.xml` | `en`, `tr` |
| `ui/settings/TDLanguageSelector` | The picker |
| `uikit/…/util/TimeFormat.kt` | `DateFormat.is24HourFormat` |
| `shared/domain/…/platform/PlatformFormatting.kt` | The stub from `20-04` |
| Call sites of `DateTimeFormatter` (19) and `TextStyle` (7) | What must produce localized output |

## 4. Target

- `shared/domain/…/locale/LocaleController.kt`
- `shared/domain/…/platform/PlatformFormatting.kt` — the real implementations
- `shared/data/androidMain/…/` — `AppCompatDelegate`, `java.time` `TextStyle`
- `shared/data/iosMain/…/` — `AppleLanguages`, `NSDateFormatter`
- `iosApp/Info.plist` — `CFBundleLocalizations`

## 5. Steps

1. **Define `LocaleController`** — apply, read current, and a `requiresRestart` flag so the UI can say so honestly.

2. **Android: wrap the existing switch.** No behaviour change.

3. **iOS: write `AppleLanguages`** into `UserDefaults` **and** override CMP's resource locale so app strings switch immediately. Surface the restart requirement in the UI rather than pretending it is instant.

4. **Add `CFBundleLocalizations`** = `["en", "tr"]`. Without it iOS will not consider the app localized for Turkish.

5. **Implement `PlatformFormatting` on Android** with `java.time.format.TextStyle` + `java.util.Locale`, built from the **app's** locale, not the system's.

6. **Implement it on iOS** with `NSDateFormatter.monthSymbols` / `shortMonthSymbols` / `weekdaySymbols` / `shortWeekdaySymbols`, and an `NSDateFormatter` probe for 12/24-hour.

7. **Mind `NSDateFormatter` weekday indexing.** Its arrays are **Sunday-first**; `kotlinx.datetime.DayOfWeek` is ISO **Monday-first**. Converting wrong shifts every weekday label by one — a bug that looks like a translation error.

8. **Cache the formatters.** `NSDateFormatter` construction is expensive and these are called per calendar cell.

## 6. Code skeleton

```kotlin
// shared/domain/…/locale/LocaleController.kt
enum class AppLocale(val tag: String) { EN("en"), TR("tr") }

interface LocaleController {
    fun apply(locale: AppLocale)
    fun current(): AppLocale
    val requiresRestart: Boolean   // Android false, iOS true for system-formatted values
}
```

```kotlin
// shared/domain/…/platform/PlatformFormatting.kt
enum class NameStyle { FULL, SHORT, NARROW }

expect fun monthName(month: Month, style: NameStyle, locale: AppLocale): String
expect fun dayOfWeekName(day: DayOfWeek, style: NameStyle, locale: AppLocale): String
expect fun uses24HourClock(): Boolean
```

```kotlin
// shared/data/iosMain/…/PlatformFormatting.ios.kt
private val formatterCache = mutableMapOf<String, NSDateFormatter>()   // construction is expensive

actual fun dayOfWeekName(day: DayOfWeek, style: NameStyle, locale: AppLocale): String {
    val symbols = formatter(locale).let {
        if (style == NameStyle.FULL) it.weekdaySymbols else it.shortWeekdaySymbols
    }
    // NSDateFormatter arrays are SUNDAY-first; kotlinx.datetime.DayOfWeek is ISO Monday-first
    // (MONDAY = 1 … SUNDAY = 7). Getting this wrong shifts every label by one, which reads
    // as a translation bug rather than an indexing bug.
    val index = day.isoDayNumber % 7      // MONDAY(1)->1 … SATURDAY(6)->6, SUNDAY(7)->0
    return symbols[index] as String
}
```

## 7. Acceptance

- [ ] `LocaleController` and `PlatformFormatting` in `:shared:domain`; both platforms registered
- [ ] Android language switching unchanged
- [ ] iOS: switching to Turkish switches app strings immediately
- [ ] iOS: the UI states the restart requirement honestly where it applies
- [ ] `CFBundleLocalizations` = `["en", "tr"]`
- [ ] Month names correct in EN and TR on both platforms
- [ ] **Weekday names correct — verify Monday actually says Monday**, not Sunday
- [ ] Short and full styles both correct
- [ ] 12h/24h follows the device setting on both platforms
- [ ] Formatting honours the **in-app** locale, not the system locale — verify with a Turkish phone set to English in-app
- [ ] Formatters are cached; no per-cell construction
- [ ] Calendar, week strip, month navigator and date pickers all show correct localized names

## 8. Pitfalls

- **Sunday-first vs Monday-first.** The single most likely defect in this task, and it presents as a translation bug.
- **Do not read `NSLocale.current` for formatting.** It ignores the in-app override, so a user who chose English on a Turkish phone gets Turkish month names.
- **Missing `CFBundleLocalizations` means iOS does not consider the app Turkish**, which affects both the App Store listing and system-formatted values.
- **`NSDateFormatter` is expensive.** Cache per locale.
- **The restart requirement must be stated, not hidden.** A language switch that half-works with no explanation reads as a bug.
- **`CalendarGrid.kt` is Monday-first.** `CalendarGridTest` guards the grid; it does not guard the *labels*.
- **Turkish has locale-specific casing** (dotted/dotless i). Do not apply `.uppercase()` without a locale — `NARROW` styles are where this bites.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# On a device, all four combinations
#   system EN + app EN · system EN + app TR · system TR + app EN · system TR + app TR
#   calendar month names, week strip weekday labels, month navigator, date pickers
#   confirm the first column of the calendar grid is Monday and is labelled Monday
#   a 12h-locale device shows 12h times; a 24h device shows 24h
```
