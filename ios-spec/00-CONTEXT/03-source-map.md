# Android Source Map

A lookup table, not a read-through. Consult it when a task says "read the source" and you need to find where something lives.

All paths are relative to the repo root. `:app` sources live under `app/src/main/java/com/todoapp/mobile/`, abbreviated below as `app/…/`.

---

## 1. Scale

| Module / layer | Files | LOC |
|---|---|---|
| `:app` src/main | 535 | 67,982 |
| — `ui/` | 290 | 48,405 |
| — `data/` | 143 | 12,397 |
| — `domain/` | 67 | 2,482 |
| — `navigation/` | 8 | 2,077 |
| — `common/` | 12 | 1,061 |
| — `di/` | 8 | 710 |
| — root | 7 | 850 |
| `:app` src/test | 36 | 4,918 |
| `:app` src/androidTest | 2 | 453 |
| `:uikit` src/main | 93 | 16,455 |
| **Total (all modules)** | **672** | **90,005** |

---

## 2. Entry points and root

| Path | LOC | Role |
|---|---|---|
| `app/…/Application.kt` | ~300 | Firebase reconcile, App Check, notification channels, Places init, WorkManager config, Coil `ImageLoaderFactory`, alarm resweep |
| `app/…/MainActivity.kt` | — | Splash, edge-to-edge, window size class, push-intent forwarding → `MainContent()`. Deliberately thin. |
| `app/…/MainViewModel.kt` | — | Auth event stream, deep-link parsing, `clearLocalSession()` (lines ~257–294), journal orphan claim |
| `app/…/MainContent.kt` | — | Scaffold host, NavGraph, merged pixel-icon map |
| `app/…/MainContract.kt`, `ThemeViewModel.kt`, `StrictModeConfig.kt` | — | |

---

## 3. Navigation

| Path | LOC | Note |
|---|---|---|
| `app/…/navigation/NavGraph.kt` | 1,128 | **Largest file in the repo.** 43 `composable<Screen.X>` blocks, per-route `navEffect` collection, per-app locale switching (lines ~273–285) |
| `app/…/navigation/Screen.kt` | 181 | 43 `@Serializable` destinations. Plain `interface Screen`, not sealed. **R8-name-sensitive.** |
| `app/…/navigation/AppDestination.kt` | 358 | Drives `bottomBarItems` (5) + `topBarItems` (34) + `hasInfoDialog` |
| `app/…/navigation/TDBottomBar.kt` / `TDNavigationRail.kt` | 144 / 92 | `TD`-prefixed but live in `:app`, not `:uikit` |
| `app/…/navigation/ThemedApp.kt` / `ThemeChangeReveal.kt` / `CurrentRouteTracker.kt` | 34 / 108 / 32 | |

Deep links are **not** declared in the nav graph — they are manifest intent filters on `MainActivity` parsed by `MainViewModel.onPushIntent` into a sealed `DeepLink`.

---

## 4. Domain (`app/…/domain/`)

Almost platform-free. **Only two Android leaks in the entire layer:**
- `domain/repository/AlarmSoundPreferences.kt:3` — `android.net.Uri`
- `domain/security/Authenticator.kt:3` — `androidx.fragment.app.FragmentActivity`

| Area | Contents |
|---|---|
| `domain/model/` | 17 models: `Task`, `Subtask`, `TaskCategory`, `Recurrence` + `RecurrenceRule`, `RecurrenceProgress`, `AlarmItem`, `DayMode`, `Group`, `GroupMember`, `GroupTask`, `GroupActivity`, `Invitation`, `Notification`, `ChatMessage`, `JournalEntry`, `Pomodoro`, `ThemePreference` |
| `domain/repository/` | 25 interfaces — 11 data repos + 14 preference repos. `AuthRepository` is declared inside `UserRepository.kt`. |
| `domain/usecase/` | 8 use cases total, incl. `ComputeHealthPointsUseCase` (+ pure `HealthPointsCalculator`), `SetTaskCompletionUseCase`, `ObserveOverdueSummaryUseCase`, `location/`, `security/` |
| `domain/alarm/` | `AlarmScheduler` (interface, `MAX_REMINDER_SLOTS = 8`), `RescheduleAllAlarmsUseCase`, `BuildDailyPlanAlarmItem` |
| `domain/engine/` | `PomodoroEngine` |
| `domain/` other | `ambience/`, `analytics/`, `location/`, `security/`, `update/`, `constants/` |

**Key file:** `domain/model/Recurrence.kt` — `firesOn` + `clampedDayOfMonth` is the shared predicate behind the task list *and* both platforms' schedulers. Locked by `RecurrenceTest`, `RecurrenceProgressTest`, `GroupTaskRecurrenceTest`.

---

## 5. Data (`app/…/data/`)

| Package | Files | LOC | Note |
|---|---|---|---|
| `data/repository/` | 26 | 4,657 | `TaskRepositoryImpl` ~1,590 LOC — sync state machine, `comparableFields()` (21 fields), conflict rules |
| `data/source/` | 39 | 2,791 | local (Room) + remote (Retrofit) |
| `data/model/` | 39 | 1,210 | 15 entities + DTOs |
| `data/notification/` | 7 | 712 | |
| `data/alarm/` | 4 | 527 | |
| `data/mapper/` | 8 | 451 | |
| `data/ambience/` | 3 | 406 | |
| `data/ai/LocalIntentClassifier.kt` | 1 | 373 | On-device chat intents |
| `data/engine/PomodoroEngineImpl.kt` | 1 | 290 | |
| `data/auth/` | 2 | 242 | `GoogleSignInManager`, `TokenCipher` |
| `data/worker/` | 3 | 121 | `SyncWorker`, `FetchTasksWorker`, `RescheduleAlarmsWorker` |

### 5.1 Room

`data/source/local/AppDatabase.kt` — **version 30**, 15 entities, auto-migrations 1→30 with **4 `AutoMigrationSpec`s** (`Migration1To2Spec`, `Migration3To4Spec`, `Migration4To5Spec` with two `@DeleteColumn` on `groups`, `Migration27To28Spec` with `@DeleteColumn` on `journal_entries.mood`), plus **2 manual migrations** in `Migrations.kt` (`MIGRATION_12_13`, `MIGRATION_25_26`). **30 schema JSONs** (`1.json`…`30.json`) exported to `app/schemas/`. Room 2.8.4, `androidx.sqlite` 2.4.0.

15 entities (in `data/model/entity/`, not `source/local/`): `TaskEntity`, `SubtaskEntity`, `TaskReminderEntity`, `SubtaskDailyCompletionEntity`, `TaskDailyCompletionEntity`, `PomodoroEntity`, `GroupEntity`, `GroupTaskEntity`, `GroupSubtaskEntity`, `GroupTaskDailyCompletionEntity`, `GroupMemberEntity`, `GroupActivityEntity`, `PendingPhotoEntity`, `ChatMessageEntity`, `JournalEntryEntity`.

15 DAOs, 122 methods total (92 `@Query`, 20 `@Insert`, 5 `@Update`, 5 `@Delete`).

`SyncStatus` (`TaskEntity.kt:75-80`): `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`, `SYNCED`.

### 5.2 Remote

`data/source/remote/api/ToDoApi.kt` — 311 LOC, **52 endpoints** (50 on `ToDoApi`, 2 on `TodoAuthApi`). Universal envelope `BaseResponse<T?> { code, message, data, errorCode }`.

`data/source/remote/interceptor/AuthInterceptor.kt` — Bearer injection, skips `/auth/register|login|google`.
`data/source/remote/authenticator/TokenRefreshAuthenticator.kt` — 401 handling, `@Singleton Mutex`, idempotency check, single retry.
`data/source/remote/fcm/TDFireBaseMessagingService.kt` (342) + `PushPayload.kt` (214, 10 payload types).

### 5.3 Sync-critical facts

- Conflict rule: a remote row overwrites local **only when local is `SYNCED` and actually differs**. Pending local CRUD always wins. **There is no `updatedAt` / last-write-wins.**
- "Differs" = `comparableFields()` — an explicit 21-field list (`TaskRepositoryImpl.kt:1187-1209`). A field missing from that list silently drops off-device edits. This has already happened once, for the four location columns.
- Idempotency key: client-generated `clientTaskId` UUID.
- Push and pull are serialized by a `@Singleton syncMutex`.
- Local-only entities (never sync): `SubtaskDailyCompletionEntity`, `PomodoroEntity`, `ChatMessageEntity`, **`JournalEntryEntity`**.

---

## 6. UI (`app/…/ui/`) — 39 top-level packages

| Package | Files | LOC | | Package | Files | LOC |
|---|---|---|---|---|---|---|
| `groups` | 42 | 9,380 | | `invitations` | 4 | 637 |
| `journal` | 29 | 4,075 | | `topbar` | 3 | 607 |
| `home` | 14 | 3,510 | | `addpomodorotimer` | 3 | 598 |
| `common` | 33 | 3,478 | | `overlay` | 2 | 500 |
| `settings` | 20 | 2,759 | | `onboarding` | 4 | 441 |
| `pomodoro` | 20 | 2,737 | | `permissions` | 3 | 378 |
| `creationhub` | 10 | 2,118 | | `pomodorosummary` | 3 | 376 |
| `chat` | 6 | 1,979 | | `planyourday` | 3 | 359 |
| `details` | 7 | 1,961 | | `changepassword` | 4 | 353 |
| `search` | 6 | 1,453 | | `resetpassword` | 4 | 345 |
| `notifications` | 11 | 1,412 | | `alarmsounds` | 3 | 325 |
| `activity` | 5 | 1,229 | | `forgotpassword` | 3 | 307 |
| `calendar` | 4 | 1,217 | | `banner` | 3 | 296 |
| `profile` | 8 | 1,080 | | `pomodorolaunch` | 3 | 293 |
| `filteredtasks` | 4 | 1,043 | | `auth` | 2 | 262 |
| `register` | 5 | 971 | | `blockedusers` | 3 | 228 |
| `login` | 4 | 704 | | `appcolors` | 3 | 228 |
| | | | | `webview` | 3 | 221 |
| | | | | `update` | 3 | 199 |
| | | | | `splash` | 1 | 161 |
| | | | | `licenses` | 1 | 134 |
| | | | | `security` | 1 | 51 |

Sub-packages: `groups/{createnewgroup,groupdetail,groupsettings,grouptaskdetail,invitemember,managemembers,memberprofile,transferownership}`, `journal/{camera,entry}`, `profile/avatarcrop`.

46 `*Screen.kt` / 46 `*ViewModel.kt` pairs.

### 6.1 Hand-drawn Canvas surfaces in `:app` (~3,524 LOC)

| Path | LOC |
|---|---|
| `ui/journal/camera/PolaroidComponentsCanvas.kt` | 577 |
| `ui/journal/camera/PolaroidBodyCanvas.kt` | 381 |
| `ui/common/components/ImageCropOverlay.kt` | 299 |
| `ui/journal/camera/PolaroidCameraBody.kt` | 245 |
| `ui/pomodoro/ambience/{Fireplace,Rain,Handpan}Scene.kt` + common | ~486 |
| `ui/journal/camera/LiveCameraPreview.kt` | 176 | ← the only CameraX-bound file
| `ui/pomodoro/PomodoroTimerRing.kt` | 141 |
| `ui/journal/camera/PolaroidBodyMetrics.kt` | 132 |
| `navigation/ThemeChangeReveal.kt` | 108 |

Compose Canvas drawing ports to CMP unchanged. Only `LiveCameraPreview.kt` needs a platform implementation.

---

## 7. `:uikit`

| Dir | Files | LOC |
|---|---|---|
| `components/` | 73 | 13,999 |
| `theme/` | 9 | 1,621 |
| `modifier/` | 4 | 369 |
| `image/` | 3 | 298 |
| `previews/` | 1 | 79 |
| `extensions/` | 2 | 70 |
| `util/` | 1 | 19 |

**Namespace/R class is `com.example.uikit`** (fixed to `com.todoapp.uikit` in `20-MIGRATION/10`).

Theme: `Color.kt` (647 — 45-field `TDColor` × 6 palette factories), `Style.kt` (240 — `TDStyle`/`TDShapes`/`TDMotion`, `SteppedEasing`), `Type.kt` (273 — 15 text styles), `TDTheme.kt` (125 — **has Android imports**: `Activity`, `enableEdgeToEdge`, `LocalView.isInEditMode`), `PixelCornerShape.kt` (119), `PolaroidColors.kt` (103), `PaletteKit.kt` (70), `ComponentColors.kt` (41).

`monochromeStyle() = defaultStyle()` (`Style.kt:155`) — MONOCHROME differs from ORIGINAL **only in colour**. There are two geometry systems, not three.

Component difficulty tiers are catalogued in `50-DESIGN-SYSTEM/`.

---

## 8. DI (`app/…/di/`, 8 modules, 710 LOC)

`LocalStorageModule` (244) · `NetworkModule` · `RepositoryModule` (~33 `@Binds`) · `DispatcherModule` · `AnalyticsModule` · `AlarmManagerModule` · `NotificationServiceModule` · `PlacesModule`.

Volume: 45 `@HiltViewModel`, 56 `hiltViewModel()` call sites, 43 `@Binds`, 34 `@Provides`, 22 `@Inject constructor`, 3 `@HiltWorker`, 4 `@AndroidEntryPoint` services.

---

## 9. Resources

| Kind | `:app` | `:uikit` |
|---|---|---|
| Strings | 1,135 + 9 plurals × EN/TR | 73 × EN/TR |
| Vector drawables | 88 `ic_*` + `logo_text.xml` | 143 `ic_*` |
| Raster (nodpi WebP) | 9 (1,164 KB) | 15 (1,940 KB) |
| Fonts | — | 6 TTF (Poppins ×4, Pixelify Sans ×2), 688 KB |
| Lottie | — | `raw/confetti` (129 KB, extension-less JSON) |
| Audio | `raw/ambience_{fireplace,rain,handpan}.ogg` (472 KB) | — |

Call-site volume: 1,346 `R.string.*` (1,024 unique) · 647 `R.drawable.*` (244 unique) · 844 `stringResource(` · 291 `tdPainter(` · 20 `painterResource(` · 5 `pluralStringResource(` · 479 `TDText(`.

**~184 string keys are defined but never referenced** — deleted during `20-MIGRATION/09`.

`values-night/` does **not** move to common: `app/…/values-night/colors.xml` (cold-start splash) and `uikit/…/values-night/themes.xml` (pre-Compose window theme) are deliberately Android-only.

---

## 10. Tests

36 unit test files (4,918 LOC) in `app/src/test/`, 2 instrumented (`AccountSwitchIsolationTest`, `MigrationTest`).

Stack: JUnit4 + MockK + Turbine + `kotlinx-coroutines-test` + Robolectric + `androidx.work.testing`. Assertions via `org.junit.Assert`. Shared `MainDispatcherRule` in `app/src/test/…/util/`.

**Regression shields for the migration** — these must keep passing with unchanged semantics:
`RecurrenceTest`, `RecurrenceProgressTest`, `GroupTaskRecurrenceTest`, `CalendarGridTest`, `DayTapOutcomeTest`, `SubtaskTest`, `HealthPointsCalculatorTest`, `AlarmRequestCodesTest`, `TaskAlarmLifecycleTest`, `TokenRefreshAuthenticatorTest`, `SyncWorkerTest`, `FetchTasksWorkerTest`, `MainViewModelLogoutTest`, `PaletteStyleTest`, `PixelIconMapTest`.

---

## 11. Outside this repo

| What | Where |
|---|---|
| Backend (Spring Boot + Kotlin, 159 files) | `~/AndroidStudioProjects/ToDoBackend` → `github.com/beratbaran40/DoneBot-Backend`, deployed on Render at `https://donebot-backend.onrender.com/` |
| Admin panel (React 19 + Vite) | `~/AndroidStudioProjects/DoneBot-Admin`, Vercel |
| Architecture diagrams (12 mermaid) | `thesis/figures/*.mmd` — Room ERD, REST endpoint table, auth flow, FCM flow, WorkManager, pomodoro state machine, DoneBot dual-path |
| Visual reference (102 PNG, 23 screens) | `docs/screenshots/` — `NN_{state}_{lang}_{theme}.png`. Gitignored; local only. |
| Release playbook | `donebot prod/PRODUCTION_READINESS_RAPOR.md` (3,352 lines) |
| Signing keystore | `~/donebot-upload.jks` (valid to 2053). `keystore.properties` is **absent** and must be created for a signed release. |
