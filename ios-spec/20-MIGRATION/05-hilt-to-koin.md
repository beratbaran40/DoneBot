---
id: 20-05
title: Hilt → Koin 4
layer: data
status: TODO
depends_on: [20-03]
blocks: [20-06, 20-07, 20-08]
parallel_safe: false
estimate: 55h
reversible: false
owner_files:
  - app/src/main/java/com/todoapp/mobile/di/**
  - app/src/main/java/com/todoapp/mobile/**
  - app/src/test/java/com/todoapp/mobile/di/**
  - gradle/libs.versions.toml
  - app/build.gradle.kts
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:testDebugUnitTest --tests '*KoinModulesTest*'
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Replace Hilt with Koin 4 across the whole app, and **restore the safety Hilt provided** with a module-verification test that runs in CI.

## 2. Why this way

Hilt is JVM/Android-only. Every shared module that needs injection — data, repositories, ViewModels — is blocked on this. It must precede Ktor and Room because both rewire the graph.

**This task trades a compile-time guarantee for a runtime one, and that trade is only acceptable if you pay for it in the same commit.** Hilt validates the object graph at build time: a missing binding is a compile error. Koin resolves at runtime: a missing binding is a crash on whichever screen happens to need it, possibly one you never opened.

Koin ships `checkModules()` / `verify()`, which walks every definition and asserts its dependencies resolve. A `KoinModulesTest` calling it lands in `app/src/test`, which is already in the CI gate (`testDebugUnitTest`). **The check is not lost — it moves from the compiler to the test suite.** Writing it in a follow-up task is how a codebase ends up with a runtime DI crash three weeks later.

Volume: 8 modules, 43 `@Binds`, 34 `@Provides`, 22 `@Inject constructor`, 45 `@HiltViewModel`, 56 `hiltViewModel()` call sites, 3 `@HiltWorker`, 4 `@AndroidEntryPoint` services.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `app/…/di/LocalStorageModule.kt` (244 LOC) | Room, DataStore, EncryptedSharedPreferences, `GoogleSignInManager`. The corruption-recovery path around `EncryptedSharedPreferences` is behaviour, not boilerplate — preserve it. |
| `app/…/di/NetworkModule.kt` | Two Retrofit instances (main + `@Named("token")`), the `@Singleton Mutex`, OkHttp timeouts |
| `app/…/di/RepositoryModule.kt` | ~33 `@Binds` |
| `app/…/di/DispatcherModule.kt` | `@IoDispatcher` / `@DefaultDispatcher` / `@MainDispatcher` qualifiers |
| `app/…/di/{Analytics,AlarmManager,NotificationService,Places}Module.kt` | The remaining four. `PlacesModule`'s laziness is deliberate — `Places.initialize` only runs when a key is present. |
| `app/…/Application.kt` | `@HiltAndroidApp`, `Configuration.Provider`, `HiltWorkerFactory` |
| `app/…/data/worker/*.kt` | 3 `@HiltWorker` classes |
| `app/…/data/notification/*.kt`, `ui/overlay/OverlayService.kt`, `data/source/remote/fcm/*.kt` | 4 `@AndroidEntryPoint` services |
| `app/…/navigation/NavGraph.kt` | 12 `hiltViewModel()` calls |
| `app/src/test/…/` | Existing VM tests construct ViewModels directly with MockK — most survive untouched |

## 4. Target

- `app/…/di/*.kt` — 8 Hilt modules become Koin modules
- `app/…/di/KoinModules.kt` *(new)* — aggregation
- `app/src/test/…/di/KoinModulesTest.kt` *(new)* — **required in this task**
- `Application.kt` — `startKoin { }`
- All 45 ViewModels, 3 workers, 4 services, 56 call sites
- `gradle/libs.versions.toml`, `app/build.gradle.kts` — Koin in, Hilt out

## 5. Steps

1. **Add Koin**, keep Hilt. Both can coexist briefly; that is what makes this migratable module by module.
   `koin-core`, `koin-android`, `koin-compose`, `koin-compose-viewmodel`, `koin-androidx-workmanager`, `koin-test` (testImplementation).

2. **Write `KoinModulesTest` first**, against an empty module list. It fails until modules exist; that is the point — the safety net is in place before the wire is cut.

3. **Convert modules in dependency order:** `DispatcherModule` → `LocalStorageModule` → `NetworkModule` → `AnalyticsModule` → `AlarmManagerModule` → `NotificationServiceModule` → `PlacesModule` → `RepositoryModule`. Gate green between each.

   | Hilt | Koin |
   |---|---|
   | `@Provides @Singleton fun x(): T` | `single { … }` |
   | `@Binds fun bind(impl: Impl): Iface` | `single<Iface> { Impl(get()) }` |
   | `@Provides` (unscoped) | `factory { … }` |
   | `@Qualifier @IoDispatcher` | `named("io")` |
   | `@ApplicationContext Context` | `androidContext()` |
   | `@HiltViewModel` + `hiltViewModel()` | `viewModel { }` + `koinViewModel()` |
   | `@HiltWorker` + `HiltWorkerFactory` | `worker { }` + `KoinWorkerFactory` |
   | `@AndroidEntryPoint` field injection | `by inject()` |

4. **`Application.kt`**: remove `@HiltAndroidApp`, add `startKoin { androidContext(this@Application); workManagerFactory(); modules(appModules) }`. **WorkManager on-demand init must keep working** — the default initializer is removed in the manifest and `Configuration.Provider` supplies the factory.

5. **ViewModels**: drop `@HiltViewModel`, keep constructor parameters, register `viewModel { XyzViewModel(get(), get()) }`. `SavedStateHandle` comes from Koin's `viewModel` scope.

6. **Call sites**: `hiltViewModel()` → `koinViewModel()` in all 56 places, including the 12 in `NavGraph.kt`.

7. **Remove Hilt** — plugin, dependencies, KSP processor, every remaining annotation. Then `./gradlew --stop && ./gradlew :app:clean` before rebuilding; stale Hilt codegen is a known failure mode in this repo.

8. **Run `KoinModulesTest`.** It must pass with every module registered. Then the full gate, then measure the AAB.

## 6. Code skeleton

```kotlin
// app/…/di/RepositoryModule.kt — before/after shape
// Hilt:
//   @Binds abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
val repositoryModule = module {
    single<TaskRepository> { TaskRepositoryImpl(get(), get(), get(named("io"))) }
    single<TaskSyncRepository> { TaskSyncRepositoryImpl(get(), androidContext()) }
    // … ~33 bindings
}

val dispatcherModule = module {
    single(named("io")) { Dispatchers.IO }
    single(named("default")) { Dispatchers.Default }
    single(named("main")) { Dispatchers.Main }
}

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    // … 45 ViewModels
}

val appModules = listOf(
    dispatcherModule, localStorageModule, networkModule, analyticsModule,
    alarmModule, notificationModule, placesModule, repositoryModule, viewModelModule,
)
```

```kotlin
// app/src/test/…/di/KoinModulesTest.kt — REQUIRED IN THIS TASK
//
// Hilt validated the object graph at compile time. Koin resolves at runtime, so
// without this test a missing binding becomes a crash on a screen nobody opened.
// This restores the gate: testDebugUnitTest is already in CI.
class KoinModulesTest {
    @Test
    fun `every definition resolves`() {
        appModules.forEach { it.verify(extraTypes = listOf(SavedStateHandle::class, Context::class)) }
    }
}
```

```kotlin
// Application.kt
override fun onCreate() {
    super.onCreate()
    startKoin {
        androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
        androidContext(this@DoneBotApplication)
        workManagerFactory()
        modules(appModules)
    }
    // … existing Timber.plant, Firebase reconcile, channels, Places, alarm resweep
}
```

## 7. Acceptance

- [ ] `KoinModulesTest` exists, registers **every** module, and passes
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] Zero Hilt references: `grep -rn "dagger\|Hilt\|@Inject" app/src/main --include="*.kt"` returns nothing
- [ ] Hilt plugin and dependencies removed from `app/build.gradle.kts` and the catalog
- [ ] All 3 workers run — verified by `SyncWorkerTest` and `FetchTasksWorkerTest`
- [ ] All 4 services start and resolve their dependencies (manual: fire an alarm, start pomodoro, receive a push)
- [ ] `:app:bundleRelease` measured and recorded — expect **−0.2…−0.5 MiB**
- [ ] Manual smoke: launch, log in, create a task, complete it, open every bottom-bar tab
- [ ] `MainViewModelLogoutTest` passes unchanged

## 8. Pitfalls

- **Do not defer `KoinModulesTest`.** It is the entire justification for accepting runtime DI. A later task is not good enough.
- **`verify()` needs `extraTypes`** for things Koin cannot construct — `SavedStateHandle`, `Context`, `WorkerParameters`. Without them it reports false failures and you will be tempted to weaken the test. Add the types instead.
- **Singleton scope is not the default.** Hilt's `@Singleton` maps to `single`; a plain `@Provides` maps to `factory`. Getting this backwards silently creates multiple Room databases, multiple OkHttp clients, or multiple `PomodoroEngine` instances. `PomodoroEngineImpl` in particular holds a `CoroutineScope` — two of them is a real bug.
- **`PlacesModule`'s laziness is load-bearing.** `Places.initialize` runs in `Application.onCreate` only when `MAPS_API_KEY` is present, and the client is built on first injection. `single { }` is lazy by default in Koin, which matches — but do not "eagerly initialize for performance."
- **`@Named("token")` Retrofit.** `NetworkModule` deliberately builds a second, plain client with no interceptor and no authenticator, to avoid a token-refresh deadlock. Preserve the distinction as two differently-`named` definitions. Collapsing them reintroduces the deadlock.
- **`androidx.work` on-demand init.** The manifest removes `WorkManagerInitializer`. If `workManagerFactory()` is missing from `startKoin`, workers fail to construct at runtime and nothing fails at build time.
- **Stale codegen after Hilt removal.** `./gradlew --stop && ./gradlew :app:clean`. Documented in `CLAUDE.md`; expect it.
- **Koin's `viewModel { }` needs `koin-compose-viewmodel`** for `koinViewModel()` in Compose. `koin-android` alone gives you the Android-only variant and the Compose call site will not resolve.
- **Do not convert everything in one commit.** Module-by-module with a green gate between each is what makes a mistake findable.

## 9. Verification

```bash
# 1. The graph resolves
./gradlew :app:testDebugUnitTest --tests '*KoinModulesTest*'

# 2. No Hilt left
grep -rn "dagger\|Hilt\|@Inject\|@AndroidEntryPoint" app/src/main --include="*.kt" && echo "HILT REMAINS" || echo "clean"
grep -n "hilt" app/build.gradle.kts gradle/libs.versions.toml && echo "HILT DEPS REMAIN" || echo "clean"

# 3. Full gate + size
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 4. Manual, on a device — the runtime-resolution surfaces Hilt used to prove at compile time
#    launch → log in → create task → complete → all five bottom-bar tabs
#    fire a reminder (alarm receiver + notification service)
#    start a pomodoro (foreground service)
#    receive a push (FCM service)
#    trigger a sync (worker)
```
