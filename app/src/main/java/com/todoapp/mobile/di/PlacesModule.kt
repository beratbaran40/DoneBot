package com.todoapp.mobile.di

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlacesModule {
    // Places is initialized in Application.onCreate (only when MAPS_API_KEY is present), and the
    // location picker only opens once Places.isInitialized() is true. @Provides is lazy — Hilt
    // creates the client on first injection (i.e. when the picker's ViewModel is built), so this
    // never runs before initialization.
    @Provides
    @Singleton
    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient = Places.createClient(context)
}
