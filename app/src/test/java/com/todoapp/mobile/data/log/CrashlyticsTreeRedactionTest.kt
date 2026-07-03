package com.todoapp.mobile.data.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the §1.7 log-hygiene contract: no email / bearer token / JWT may survive redaction into a
 * Crashlytics breadcrumb. Pure function, so this runs on the JVM without Firebase.
 */
class CrashlyticsTreeRedactionTest {

    @Test
    fun `bearer tokens are redacted so auth headers never reach Crashlytics`() {
        val out = redactLogMessage("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig")
        assertEquals("Authorization: Bearer [redacted]", out)
        assertFalse(out.contains("eyJ"))
    }

    @Test
    fun `standalone JWTs are redacted even without a Bearer prefix`() {
        val out = redactLogMessage("access=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0In0.abc-_123")
        assertTrue(out.contains("[token]"))
        assertFalse(out.contains("eyJ"))
    }

    @Test
    fun `emails are masked so PII is not sent off-device`() {
        assertEquals("login failed for [email]", redactLogMessage("login failed for berat.baran@example.com"))
    }

    @Test
    fun `a message carrying both an email and a bearer token masks both`() {
        val out = redactLogMessage("user user@mail.co retried with Bearer eyJa.b.c")
        assertFalse(out.contains("user@mail.co"))
        assertFalse(out.contains("eyJa"))
    }

    @Test
    fun `a PII-free message is returned unchanged`() {
        val msg = "TaskFetch: loaded 12 tasks in 34ms"
        assertEquals(msg, redactLogMessage(msg))
    }
}
