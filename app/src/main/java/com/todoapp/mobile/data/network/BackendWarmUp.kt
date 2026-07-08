package com.todoapp.mobile.data.network

import android.os.SystemClock
import com.todoapp.mobile.BuildConfig
import com.todoapp.mobile.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Fire-and-forget backend warm-up. A Render instance that is cold (spun down, mid-deploy, or
 * restarted) starts booting the moment ANY request reaches its edge — so pinging the cheapest
 * endpoint at app-foreground gives the backend a head start before the user reaches an online
 * surface (chat, sync). On an always-on instance the ping is a no-op costwise; it exists as
 * insurance for deploy windows and any future return to a spin-down tier.
 *
 * No owned CoroutineScope on purpose (see the singleton-scope anti-pattern in CLAUDE.md):
 * callers launch [pingIfStale] from lifecycle-owned scopes and the result is ignored.
 */
@Singleton
class BackendWarmUp @Inject constructor(
    @Named("plain") private val plainClient: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val lastPingElapsedMs = AtomicLong(NEVER_PINGED)

    suspend fun pingIfStale(minIntervalMs: Long = DEFAULT_MIN_INTERVAL_MS) {
        val now = SystemClock.elapsedRealtime()
        val last = lastPingElapsedMs.get()
        if (last != NEVER_PINGED && now - last < minIntervalMs) return
        // Claim the slot before the (slow) call so concurrent callers don't double-ping.
        if (!lastPingElapsedMs.compareAndSet(last, now)) return
        withContext(ioDispatcher) {
            runCatching {
                val request = Request.Builder()
                    .url(BuildConfig.BASE_URL + LIVENESS_PATH)
                    .get()
                    .build()
                plainClient.newCall(request).execute().use { response ->
                    Timber.tag(TAG).d("warm-up ping -> HTTP %d", response.code)
                }
            }.onFailure {
                // Expected while the backend is actually waking (that's the point of the ping).
                Timber.tag(TAG).d(it, "warm-up ping failed")
            }
        }
    }

    companion object {
        // JVM-only liveness probe — deliberately NOT /actuator/health: that group's DB indicator
        // would wake the autosuspended Neon compute on every app open and defeat the cost strategy.
        private const val LIVENESS_PATH = "actuator/health/liveness"
        private const val DEFAULT_MIN_INTERVAL_MS = 5 * 60 * 1000L
        private const val NEVER_PINGED = -1L
        private const val TAG = "BackendWarmUp"
    }
}
