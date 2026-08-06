---
id: 20-07
title: Room → Room KMP
layer: data
status: TODO
depends_on: [20-05]
blocks: [20-11]
parallel_safe: false
estimate: 35h
reversible: false
owner_files:
  - app/src/main/java/com/todoapp/mobile/data/source/local/**
  - app/src/main/java/com/todoapp/mobile/data/model/entity/**
  - app/src/androidTest/java/com/todoapp/mobile/**
  - app/schemas/**
  - gradle/libs.versions.toml
  - app/build.gradle.kts
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - "diff -q app/schemas/*/30.json <the regenerated 30.json>"
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Move Room to its multiplatform configuration without changing the database schema by a single byte, and without adding the bundled SQLite native library to the Android bundle.

## 2. Why this way

**This is the highest-consequence task in the migration.** Every other one-way door costs developer time if it goes wrong. This one can corrupt or destroy data for people who already have the app installed.

The good news, verified in this repo: **Room 2.8.4 already supports KMP.** The 15 entities, 15 DAOs and 122 DAO methods are not rewritten — they are re-targeted. The work is:

1. Apply the `androidx.room` Gradle plugin (replaces the `ksp { arg("room.schemaLocation", …) }` wiring).
2. Add `@ConstructedBy` + an `expect object … : RoomDatabaseConstructor`.
3. Change `SupportSQLiteDatabase` → `SQLiteConnection` in the migration callbacks.
4. Choose the right driver **per platform**.

**The schema is the contract.** There are 30 exported JSONs in `app/schemas/`, `1.json` through `30.json`. After the port, the regenerated `30.json` must be **byte-identical** to the committed one. If it differs, the port changed the schema, and every existing install is at risk of a failed migration or silent data loss. This diff is not a nicety — it is the gate.

**The driver choice is a size trap with a 3–4 MiB blast radius.** `BundledSQLiteDriver` packages a native SQLite `.so` per ABI. On iOS that is required. On Android it is not — the platform ships SQLite — and adding it there would blow the AAB ceiling in one step. This is the single most-copied mistake from KMP sample projects.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/source/local/AppDatabase.kt` (158 LOC) | `version = 30`, the `@AutoMigration` list, and the **4** `AutoMigrationSpec` classes: `Migration1To2Spec`, `Migration3To4Spec`, `Migration4To5Spec` (two `@DeleteColumn` on `groups`), `Migration27To28Spec` (`@DeleteColumn` on `journal_entries.mood`) |
| `data/source/local/Migrations.kt` (41 LOC) | `MIGRATION_12_13` and `MIGRATION_25_26`. The latter **dedups duplicate `remote_id` group rows before creating a unique index** — it is data surgery, not DDL. |
| `data/model/entity/` (15 files) | Entities. `TaskEntity.kt:75-80` holds the `SyncStatus` enum with `defaultValue = "PENDING_CREATE"`. |
| `data/source/local/*Dao.kt` (15 files) | 122 methods: 92 `@Query`, 20 `@Insert`, 5 `@Update`, 5 `@Delete`, 1 `@Transaction` |
| `data/source/local/StringListConverter` | The one type converter |
| `app/schemas/` | 30 JSONs. **The contract.** |
| `app/build.gradle.kts` | `ksp { arg("room.schemaLocation", …) }` and the androidTest asset wiring |
| `app/src/androidTest/…/MigrationTest.kt` | Instrumented; tests against the exported schemas. Needs a device. |
| `di/LocalStorageModule.kt` → now a Koin module | Database construction |

## 4. Target

- `gradle/libs.versions.toml` — `androidx.sqlite` 2.4.0 → 2.5.x; add `sqlite-bundled` **for iOS only**; add the Room Gradle plugin
- `app/build.gradle.kts` — apply the Room plugin, `room { schemaDirectory(…) }`, drop the KSP schema arg
- `data/source/local/AppDatabase.kt` — `@ConstructedBy`, `expect object AppDatabaseConstructor`
- `data/source/local/Migrations.kt` — `SQLiteConnection` API
- The 4 `AutoMigrationSpec` classes — same change
- Koin database definition — driver selection
- `app/src/androidTest/…/MigrationTest.kt` — extended to a full v1→v30 chain

## 5. Steps

1. **Snapshot the contract before touching anything.**
   ```bash
   cp -r app/schemas /tmp/schemas-before
   shasum -a 256 app/schemas/*/30.json
   ```

2. **Bump `androidx.sqlite` to 2.5.x** and apply the `androidx.room` Gradle plugin. Replace the `ksp { arg("room.schemaLocation", …) }` wiring with `room { schemaDirectory("$projectDir/schemas") }`.

3. **Add the constructor.** `@ConstructedBy(AppDatabaseConstructor::class)` on `AppDatabase`, plus the `expect object`. With only `androidTarget()` declared, one `actual` satisfies it — the ratchet working as intended.

4. **Port the migration callbacks.** `SupportSQLiteDatabase` → `SQLiteConnection`; `db.execSQL(...)` → `connection.execSQL(...)`. `MIGRATION_25_26` also *reads* rows to dedup — port its query loop to `connection.prepare(...)` / `step()` / `getText(...)` carefully. **This one deserves its own test.**

5. **Select drivers per platform.**
   - Android: `AndroidSQLiteDriver()` — the platform SQLite, exactly what ships today.
   - iOS (later, `20-13`): `BundledSQLiteDriver()`.
   - **`sqlite-bundled` must not appear on the Android target's dependency list.**

6. **Regenerate the schema and diff.**
   ```bash
   ./gradlew :app:kspDebugKotlin   # or the Room plugin's generation task
   diff -u /tmp/schemas-before/*/30.json app/schemas/*/30.json
   ```
   **Any difference stops this task.** Investigate until the diff is empty. Do not commit a changed schema and do not bump the version to "fix" it.

7. **Extend `MigrationTest`** to walk v1 → v30 with real data, not just adjacent pairs. It is instrumented and needs a device.

8. **Full gate, then measure the AAB.** Expect ~0 change. If it jumped by megabytes, `sqlite-bundled` leaked onto Android.

9. **Ship to Play internal testing with a real, old database** before proceeding. Take a device (or an emulator image) holding a v22-era database, install the new build over it, and verify the app opens with data intact. This is the only test that exercises the real upgrade path.

## 6. Code skeleton

```kotlin
// data/source/local/AppDatabase.kt
@Database(
    entities = [ /* 15 unchanged */ ],
    version = 30,
    autoMigrations = [ /* unchanged, including the 4 specs */ ],
    exportSchema = true,
)
@TypeConverters(StringListConverter::class)
@ConstructedBy(AppDatabaseConstructor::class)     // NEW
abstract class AppDatabase : RoomDatabase() { /* 15 DAO accessors unchanged */ }

// NEW — Room's KSP generates the actual per target.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
```

```kotlin
// data/source/local/Migrations.kt — API change only, SQL untouched
internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {   // was: SupportSQLiteDatabase
        connection.execSQL("...")                          // SQL string is unchanged
    }
}
```

```kotlin
// Koin — Android definition. THE DRIVER CHOICE IS THE SIZE TRAP.
single {
    Room.databaseBuilder<AppDatabase>(
        context = androidContext(),
        name = androidContext().getDatabasePath("todo_database").absolutePath,
    )
        // AndroidSQLiteDriver = the platform SQLite already on every device.
        // BundledSQLiteDriver here would package a native .so per ABI: +3-4 MiB,
        // instantly blowing the AAB ceiling. iOS uses Bundled; Android never does.
        .setDriver(AndroidSQLiteDriver())
        .setQueryCoroutineContext(get<CoroutineDispatcher>(named("io")))
        .addMigrations(MIGRATION_12_13, MIGRATION_25_26)
        .build()
}
```

## 7. Acceptance

- [ ] Regenerated `app/schemas/…/30.json` is **byte-identical** to the pre-task file (`diff` empty)
- [ ] All 30 schema JSONs unchanged; no new schema file created
- [ ] `AppDatabase.version` is still **30** — not bumped
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] `MigrationTest` passes on a device, extended to a full v1→v30 chain
- [ ] `MIGRATION_25_26`'s dedup behaviour has a dedicated test case
- [ ] `:app:bundleRelease` within **±100 KiB** of the previous measurement
- [ ] `./gradlew :app:dependencies` shows **no** `sqlite-bundled` on any Android configuration
- [ ] Installed over a real pre-migration build: app opens, tasks/groups/journal all intact
- [ ] All 122 DAO methods compile without signature changes

## 8. Pitfalls

- **`sqlite-bundled` on Android: +3–4 MiB.** The single most-copied mistake from KMP samples. `AndroidSQLiteDriver` on Android, `BundledSQLiteDriver` on iOS.
- **Do not bump the database version.** The port must be schema-neutral. Bumping to escape a diff hides a real change and ships a migration nobody wrote.
- **Do not regenerate or "tidy" the older schema JSONs.** They are the historical contract that `MigrationTest` validates against. Only `30.json` is regenerated, and it must come out identical.
- **`MIGRATION_25_26` is data surgery.** It dedups duplicate `remote_id` group rows *before* creating a unique index. Porting the DDL and dropping the dedup loop produces a constraint violation on exactly the users who hit the original bug.
- **The 4 `AutoMigrationSpec` classes have callbacks too.** `Migration1To2Spec`, `Migration3To4Spec`, `Migration4To5Spec`, `Migration27To28Spec`. Missing one leaves a compile error at best and a skipped migration step at worst.
- **The database file path must not change.** `getDatabasePath("todo_database")` — a different name or directory means every user starts with an empty database and their local-only journal is gone forever.
- **`MigrationTest` is instrumented.** It cannot run in `testDebugUnitTest`. If you have no device, mark the task `BLOCKED` for that check rather than deleting the test.
- **Journal data is irreplaceable.** `journal_entries` is local-only with no backend copy and is deliberately not wiped on logout. A failed migration destroys it permanently. Treat this table as the one with no undo.
- **Room's KSP is order-sensitive after a plugin change.** If generation misbehaves: `./gradlew --stop && ./gradlew :app:clean`.

## 9. Verification

```bash
# 1. THE GATE — schema is byte-identical
cp -r app/schemas /tmp/schemas-before      # before starting
# … after the port …
diff -ru /tmp/schemas-before app/schemas && echo "SCHEMA IDENTICAL" || echo "STOP — SCHEMA CHANGED"

# 2. No bundled SQLite on Android
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -i "sqlite-bundled" \
  && echo "SIZE TRAP HIT" || echo "clean"

# 3. Full gate + size
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 4. Migrations, on a device
./gradlew :app:connectedDebugAndroidTest --tests '*MigrationTest*'

# 5. The real upgrade path — irreplaceable, do not skip
#    Install the last pre-migration build, create tasks + a journal entry with a photo,
#    then install this build over it. Everything must still be there.
```
