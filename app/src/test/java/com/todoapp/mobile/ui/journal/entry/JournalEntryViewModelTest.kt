package com.todoapp.mobile.ui.journal.entry

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.todoapp.mobile.data.storage.JournalPhotoStorage
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.repository.JournalRepository
import com.todoapp.mobile.navigation.NavigationEffect
import com.todoapp.mobile.ui.journal.entry.JournalEntryContract.UiAction
import com.todoapp.mobile.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The entry editor has no explicit save button — leaving the screen IS the save. Both exit
 * affordances (system back and the top-bar arrow) funnel into [UiAction.OnBackPress], so these
 * lock the persistence contract that both depend on.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalEntryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<JournalRepository>(relaxed = true) {
        coEvery { upsertEntry(any()) } returns 1L
    }
    private val photoStorage = mockk<JournalPhotoStorage>(relaxed = true)

    private fun buildViewModel(entryId: Long = 0L) = JournalEntryViewModel(
        savedStateHandle = SavedStateHandle(mapOf("entryId" to entryId)),
        journalRepository = repository,
        photoStorage = photoStorage,
    )

    @Test
    fun `back press persists a written entry`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()

        viewModel.onAction(UiAction.OnContentChange("bugun kosuya ciktim"))
        viewModel.onAction(UiAction.OnBackPress)
        advanceUntilIdle()

        val saved = slot<JournalEntry>()
        coVerify(exactly = 1) { repository.upsertEntry(capture(saved)) }
        assertEquals("bugun kosuya ciktim", saved.captured.content)
        // The title is derived from the first line, so the list has something to render.
        assertEquals("bugun kosuya ciktim", saved.captured.title)
    }

    @Test
    fun `back press navigates back`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()

        viewModel.navEffect.test {
            viewModel.onAction(UiAction.OnContentChange("kisa not"))
            viewModel.onAction(UiAction.OnBackPress)
            advanceUntilIdle()

            assertTrue(awaitItem() is NavigationEffect.Back)
            expectNoEvents()
        }
    }

    @Test
    fun `a second back press while the save is in flight does not duplicate the entry`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()

        viewModel.onAction(UiAction.OnContentChange("cift dokunma"))
        // Both presses land before the suspending upsert resolves — the guard must swallow the second.
        viewModel.onAction(UiAction.OnBackPress)
        viewModel.onAction(UiAction.OnBackPress)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.upsertEntry(any()) }
    }

    @Test
    fun `back press on an untouched entry saves nothing`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()

        viewModel.onAction(UiAction.OnBackPress)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.upsertEntry(any()) }
    }

    @Test
    fun `a photo with no text is still worth saving`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val viewModel = buildViewModel()
        val path = "/data/journal_photos/shot.jpg"

        viewModel.onAction(UiAction.OnPhotoCapturedFromCamera(path))
        viewModel.onAction(UiAction.OnBackPress)
        advanceUntilIdle()

        val saved = slot<JournalEntry>()
        coVerify(exactly = 1) { repository.upsertEntry(capture(saved)) }
        assertEquals(listOf(path), saved.captured.photoPaths)
        coVerify(exactly = 0) { photoStorage.deletePhoto(any()) }
    }
}
