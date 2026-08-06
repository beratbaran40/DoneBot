package com.todoapp.mobile.data.source.remote.fcm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser is the seam between the backend's data-only push and everything the client does with
 * it — deep link, sync, banner. A field that fails to parse does not crash: the push is simply
 * dropped, or shown with no way to open what it is about.
 */
class PushPayloadParserTest {
    @Test
    fun `a push with no type is not ours`() {
        // Firebase console messages and anything else arrive without one; the service falls back to
        // the notification block rather than inventing a payload.
        assertNull(PushPayloadParser.parse(mapOf("title" to "hi")))
    }

    @Test
    fun `task assigned carries the ids the deep link needs`() {
        val payload = PushPayloadParser.parse(
            mapOf("type" to "task_assigned", "groupId" to "3", "taskId" to "9", "title" to "t", "body" to "b"),
        )

        val assigned = payload as PushPayload.TaskAssigned
        assertEquals(3L, assigned.groupId)
        assertEquals(9L, assigned.taskId)
    }

    @Test
    fun `a group task push without its ids is dropped rather than shown`() {
        // Showing it would produce a notification that opens nothing — worse than staying silent,
        // because the user taps it expecting the task.
        listOf("task_assigned", "task_completed", "task_due_soon").forEach { type ->
            assertNull(type, PushPayloadParser.parse(mapOf("type" to type, "groupId" to "3")))
            assertNull(type, PushPayloadParser.parse(mapOf("type" to type, "taskId" to "9")))
        }
    }

    @Test
    fun `an invitation parses without a group id`() {
        // The invitee is not a member yet, so the server has no group context to hand them; the push
        // deep-links to the invitations list instead.
        val payload = PushPayloadParser.parse(
            mapOf("type" to "invitation_received", "invitationId" to "5", "inviterName" to "Ada"),
        )

        val invitation = payload as PushPayload.InvitationReceived
        assertEquals(5L, invitation.invitationId)
        assertNull(invitation.groupId)
    }

    @Test
    fun `silent only counts when it is exactly true`() {
        fun silentOf(value: String?) = (
            PushPayloadParser.parse(
                buildMap {
                    put("type", "group_task_changed")
                    put("groupId", "3")
                    value?.let { put("silent", it) }
                },
            ) as PushPayload.GroupTaskChanged
            ).silent

        assertTrue(silentOf("true"))
        assertEquals(false, silentOf("TRUE"))
        assertEquals(false, silentOf("1"))
        assertEquals(false, silentOf(null))
    }

    @Test
    fun `an unrecognised type survives as Unknown instead of vanishing`() {
        // Forward compatibility: a type this build predates should still reach the user as a generic
        // notice, because the server already decided it was worth telling them about.
        val payload = PushPayloadParser.parse(mapOf("type" to "something_new", "title" to "t", "body" to "b"))

        val unknown = payload as PushPayload.Unknown
        assertEquals("something_new", unknown.type)
        assertEquals("t", unknown.title)
    }

    @Test
    fun `a non-numeric id is treated as absent`() {
        assertNull(PushPayloadParser.parse(mapOf("type" to "task_assigned", "groupId" to "abc", "taskId" to "9")))
    }
}
