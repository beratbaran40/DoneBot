package com.todoapp.mobile.di

import com.todoapp.mobile.data.source.remote.api.ToDoApi
import com.todoapp.mobile.data.source.remote.api.TodoAuthApi
import com.todoapp.mobile.data.source.remote.authenticator.TokenRefreshAuthenticator
import com.todoapp.mobile.data.source.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import okhttp3.Dispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideTokenRefreshMutex(): Mutex = Mutex()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        return Retrofit
            .Builder()
            .baseUrl(com.todoapp.mobile.BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    @Named("token")
    fun provideTokenRetrofit(
        @Named("plain") plainOkHttpClient: OkHttpClient,
    ): Retrofit {
        val json =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }
        return Retrofit
            .Builder()
            .baseUrl(com.todoapp.mobile.BuildConfig.BASE_URL)
            .client(plainOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // Bare client for token refresh + backend warm-up pings. Deliberately NO AuthInterceptor (it
    // would attach the stale Bearer to /auth/refresh — refresh isn't in its noAuthPaths) and NO
    // TokenRefreshAuthenticator (a 401 on the refresh call itself would re-enter authenticate()
    // while the outer call already holds the non-reentrant refresh Mutex inside runBlocking —
    // a deadlocked OkHttp thread). Without an explicit client the token Retrofit used OkHttp
    // defaults (10s, no call timeout), which silently killed refreshes during cold backend wakes.
    // Timeouts are shorter than the main client's because authenticate() blocks an OkHttp thread.
    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient {
        val dispatcher = Dispatcher().apply { maxRequestsPerHost = 16 }
        return OkHttpClient
            .Builder()
            .dispatcher(dispatcher)
            // Explicit timeouts so a half-open socket can't leave a request hanging forever
            // (endless spinner + leaked call). Read/call are deliberately generous so the DoneBot
            // chat's server-side Vertex function-call loop, photo uploads, and a cold Neon wake
            // aren't cut off; tighten only if these stay comfortably under the limit in practice.
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .authenticator(tokenRefreshAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideTDApi(retrofit: Retrofit): ToDoApi = retrofit.create(ToDoApi::class.java)

    @Provides
    @Singleton
    fun provideTokenApi(
        @Named("token") retrofit: Retrofit,
    ): TodoAuthApi = retrofit.create(TodoAuthApi::class.java)
}
