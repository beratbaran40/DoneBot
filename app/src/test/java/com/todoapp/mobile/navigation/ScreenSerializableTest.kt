package com.todoapp.mobile.navigation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every nav destination must be `@Serializable`, because type-safe Compose Navigation resolves the
 * route through the serializer while the graph is being built — a missing annotation is not a compile
 * error and not a broken route, it is the app dying on launch with
 * "Serializer for class 'X' is not found" the moment NavGraph reaches that destination.
 *
 * That is exactly what shipped: NotificationSettings arrived without the annotation all of its
 * neighbours carry, and nothing caught it. Unit tests never build the graph, there are no Compose UI
 * tests, and the R8 keep rules for `navigation.Screen` made it look like a minification problem when
 * it was one missing line.
 *
 * Java reflection rather than kotlin-reflect, which this module does not depend on.
 */
class ScreenSerializableTest {
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `every Screen destination has a serializer`() {
        val destinations = Screen::class.java.declaredClasses
            .filter { Screen::class.java.isAssignableFrom(it) && !it.isInterface }

        // A sweep that silently found nothing would pass forever.
        assertTrue("expected to find nav destinations to check", destinations.size > 10)

        val broken = destinations.mapNotNull { klass ->
            runCatching { serializer(klass) }.exceptionOrNull()?.let { klass.simpleName }
        }

        assertTrue(
            "missing @Serializable — NavGraph will crash on launch: $broken",
            broken.isEmpty(),
        )
    }
}
