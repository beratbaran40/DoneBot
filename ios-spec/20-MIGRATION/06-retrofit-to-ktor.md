---
id: 20-06
title: Retrofit + OkHttp `Authenticator` → Ktor 3
layer: data
status: TODO
depends_on: [20-05]
blocks: [20-11]
parallel_safe: false
estimate: 35h
reversible: true
owner_files:
  - app/src/main/java/com/todoapp/mobile/data/source/remote/**
  - app/src/main/java/com/todoapp/mobile/di/NetworkModule.kt
  - app/src/test/java/com/todoapp/mobile/**
  - gradle/libs.versions.toml
  - app/build.gradle.kts
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - ./gradlew :app:bundleRelease
---

## 1. Goal

Replace Retrofit 3 + the OkHttp `Authenticator` with a Ktor 3 client, preserving all 52 endpoints, the `BaseResponse<T?>` envelope, and — critically — the exact 401 refresh semantics.

## 2. Why this way

Retrofit is JVM-only; it does not exist in `commonMain`. Ktor uses a platform engine behind one shared API: OkHttp on Android (so the OkHttp you already ship is reused), Darwin on iOS.

**The refresh logic is the real work, not the 52 endpoints.** Today's flow is subtle and was clearly hard-won:

- `AuthInterceptor` attaches `Bearer` to everything except `/auth/register|login|google`.
- `TokenRefreshAuthenticator` gives up after 2 prior responses; takes a `@Singleton Mutex` so concurrent 401s serialize; performs an **idempotency check** (if the stored token differs from the one on the failed request, another thread already refreshed → just retry); retries the refresh call **once** after 500 ms if it comes back `Unauthorized`; and only calls `forceLogout()` when the failure is `Unauthorized` **and** a refresh token is still on disk.
- A second, `@Named("token")` Retrofit with no interceptor and no authenticator exists solely so the refresh call cannot recurse — the deadlock this avoids is documented in `NetworkModule.kt`.

Ktor's `Auth` plugin with `bearer { loadTokens; refreshTokens }` provides single-flight refresh natively, which subsumes the `Mutex` and the idempotency check. That is a genuine simplification — but **only if the remaining behaviours are ported deliberately**, not assumed.

**Two options for the endpoint layer.** Hand-written Ktor request functions, or **Ktorfit**, which keeps Retrofit-style annotations (`@GET`, `@POST`, `@Path`, `@Query`, `@Body`, `@Multipart`) via KSP on top of Ktor. Ktorfit turns `ToDoApi.kt` from a rewrite into an import swap for most of its 311 lines. **Evaluate Ktorfit first**; fall back to hand-written only if it cannot express something. Record the choice in `DECISIONS.md`.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `data/source/remote/api/ToDoApi.kt` (311 LOC) | 52 endpoints. Note `@HTTP(hasBody = true)` on `DELETE devices/fcm-token` — a DELETE *with* a body. |
| `data/source/remote/interceptor/AuthInterceptor.kt` | The three no-auth paths; the "token null but send anyway" behaviour |
| `data/source/remote/authenticator/TokenRefreshAuthenticator.kt` | Every rule listed in §2. Read it line by line. |
| `di/NetworkModule.kt` | Two clients; timeouts 15s connect / 60s read / 60s write / 90s call; `maxRequestsPerHost = 16`; the deadlock comment |
| `data/model/network/response/BaseResponse.kt` | The envelope |
| `common/Extensions.kt` (~lines 54, 103, 125, 163-184) | `handleRequest` / `handleEmptyRequest`; HTTP → `DomainException` mapping. **401 and 403 both collapse to `Unauthorized`** — deliberate. |
| `data/network/BackendWarmUp.kt` | Warm-up ping for the Render cold start |
| `app/src/test/…/TokenRefreshAuthenticatorTest.kt` | The behavioural contract you must preserve |
| `app/src/main/res/xml/network_security_config.xml` | Cleartext allowed only for `10.0.2.2`/localhost; user CAs in debug only |

## 4. Target

- `data/source/remote/api/ToDoApi.kt` — Ktorfit interface or Ktor client class
- `data/source/remote/api/TodoAuthApi.kt` — refresh/logout, on a client with **no** auth plugin
- `data/source/remote/HttpClientFactory.kt` *(new)* — client construction, `expect`-ready
- `di/NetworkModule.kt` — collapses substantially
- `AuthInterceptor.kt`, `TokenRefreshAuthenticator.kt` — deleted, behaviour moved into the `Auth` plugin
- `TokenRefreshAuthenticatorTest.kt` — rewritten, **same assertions**

## 5. Steps

1. **Evaluate Ktorfit.** If it handles the annotation set — including multipart uploads (`users/me/avatar`, `tasks/{taskId}/photos`, `family-groups/{groupId}/avatar`) and the DELETE-with-body — use it. Record the decision.

2. **Write down the current refresh behaviour as a checklist** before deleting anything. Each line becomes a test:
   - no `Bearer` on `/auth/register`, `/auth/login`, `/auth/google`, `/auth/refresh`, `/auth/logout`
   - concurrent 401s trigger exactly **one** refresh
   - a 401 whose request carried a stale token retries with the current token, no refresh call
   - refresh returning `Unauthorized` is retried **once** after 500 ms
   - `forceLogout()` only when refresh fails as `Unauthorized` **and** a refresh token is on disk
   - network/server failures during refresh **keep** the session
   - give up after 2 prior responses

3. **Build the client factory.** Content negotiation with the existing kotlinx-serialization `Json` config (`ignoreUnknownKeys = true`, `isLenient = true`, `encodeDefaults = true`), the same timeouts, logging in debug only.

4. **Port the `Auth` plugin.** `bearer { loadTokens { }; refreshTokens { }; sendWithoutRequest { } }`. `sendWithoutRequest` is where the no-auth path list goes.

5. **Port the 52 endpoints.** Keep method names and signatures identical so `RemoteDataSource` implementations are untouched.

6. **Keep the second client for refresh.** Ktor's `Auth` plugin does not by itself prevent the refresh call from recursing through the same plugin; a separate client with no `Auth` installed is the clear, safe equivalent of today's `@Named("token")` Retrofit.

7. **Rewrite `TokenRefreshAuthenticatorTest`** against the checklist from step 2. It tests behaviour, not implementation.

8. **Full gate, then measure the AAB immediately.** This is the single biggest size risk in the migration (+0.3…+0.8 MiB projected). If it blows the ceiling, the fallback is an `expect`/`actual` HTTP layer keeping Retrofit on Android — expensive (a duplicated 52-endpoint client) but real. Record the number before deciding anything.

## 6. Code skeleton

```kotlin
// data/source/remote/HttpClientFactory.kt
fun createHttpClient(
    engine: HttpClientEngine,
    tokens: SessionPreferences,
    authApi: TodoAuthApi,
    isDebug: Boolean,
): HttpClient = HttpClient(engine) {
    expectSuccess = false                    // BaseResponse carries its own error codes

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 90_000        // generous: Vertex loop, photo upload, Neon cold wake
        socketTimeoutMillis = 60_000
    }

    install(Auth) {
        bearer {
            loadTokens { BearerTokens(tokens.accessToken() ?: return@loadTokens null, tokens.refreshToken().orEmpty()) }

            // Ktor serialises refresh internally — this subsumes the @Singleton Mutex
            // and the stale-token idempotency check in TokenRefreshAuthenticator.
            refreshTokens {
                val stored = tokens.refreshToken() ?: return@refreshTokens null
                // One retry after 500 ms on Unauthorized: guards a transient edge/WAF 403.
                // NOTE: Extensions.kt collapses 401 AND 403 into DomainException.Unauthorized.
                val result = authApi.refresh(stored).orRetryOnceAfter(500)
                result.onUnauthorized {
                    if (tokens.refreshToken() != null) authRepository.forceLogout()
                }
                result.toBearerTokens()
            }

            // The only requests that must never carry a Bearer.
            sendWithoutRequest { request ->
                request.url.encodedPath !in NO_AUTH_PATHS
            }
        }
    }
}

private val NO_AUTH_PATHS = setOf("/auth/register", "/auth/login", "/auth/google")
```

```kotlin
// The refresh client installs NO Auth plugin — the same reason NetworkModule keeps a
// separate @Named("token") Retrofit today. Without this, refresh recurses into itself.
fun createAuthHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(/* same Json */) }
    install(HttpTimeout) { connectTimeoutMillis = 15_000; requestTimeoutMillis = 60_000 }
}
```

## 7. Acceptance

- [ ] All 52 endpoints ported; every `RemoteDataSource` compiles unchanged
- [ ] `TokenRefreshAuthenticatorTest` rewritten, covering **all seven** behaviours from step 2, and passing
- [ ] `./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug` passes
- [ ] `:app:bundleRelease` measured and recorded — flag immediately if it exceeds the ceiling
- [ ] Multipart uploads work: avatar, task photo, group avatar (manual)
- [ ] `DELETE devices/fcm-token` still sends its body and the server accepts it
- [ ] Unit-returning endpoints still succeed with `data: null` (the `handleEmptyRequest` path)
- [ ] Manual: log in, let the access token expire, make a request → transparent refresh, no logout
- [ ] Manual: log in on a device with the network off → request fails, **session survives**
- [ ] `AuthInterceptor.kt` and `TokenRefreshAuthenticator.kt` deleted

## 8. Pitfalls

- **`expectSuccess = false` is mandatory.** Ktor defaults to throwing on non-2xx. This API returns meaningful bodies with error codes on non-2xx, and `Extensions.kt` maps them. Leaving the default on turns every handled error into an exception.
- **401 and 403 both mean `Unauthorized` here.** `Extensions.kt` collapses them deliberately — an edge/WAF sometimes returns 403 for an expired token. Preserving only the 401 path silently breaks refresh behind certain networks.
- **Never let the refresh call carry a Bearer, and never let it recurse.** A separate client with no `Auth` plugin. This is the deadlock `NetworkModule.kt` documents.
- **`forceLogout()` is conditional.** Only on `Unauthorized` **and** a refresh token still on disk. Logging out on a network error strands users offline — a real bug this codebase already fixed once.
- **DELETE with a body.** `DELETE devices/fcm-token` uses `@HTTP(hasBody = true)`. Ktor supports it; many HTTP layers strip it. Test it explicitly.
- **Timeouts are generous on purpose.** 60s read / 90s call exist because of the Vertex AI chat loop, photo uploads, and Render/Neon cold starts. Do not "tighten them to sensible values."
- **Keep `maxRequestsPerHost = 16`** on the OkHttp engine. There is a documented incident where stale syncs saturated the per-host pool and login hung behind them.
- **Measure the AAB before celebrating.** This is the largest single size risk in the migration.
- **`BackendWarmUp` must keep working.** It pings on foreground to wake the Render dyno; it uses the plain client.

## 9. Verification

```bash
# 1. Refresh semantics
./gradlew :app:testDebugUnitTest --tests '*TokenRefresh*'

# 2. Full gate
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# 3. Size — the big risk
./gradlew :app:bundleRelease && ls -l app/build/outputs/bundle/release/*.aab

# 4. Retrofit is gone
grep -rn "retrofit2" app/src/main --include="*.kt" && echo "RETROFIT REMAINS" || echo "clean"

# 5. Manual, on a device against the live backend
#    log in → create/edit/delete a task → group task → chat message
#    upload an avatar, a task photo, a group avatar
#    register then unregister an FCM token (the DELETE-with-body)
#    wait for the access token to expire, then act → transparent refresh
#    turn off the network, act, turn it back on → session survived
```
