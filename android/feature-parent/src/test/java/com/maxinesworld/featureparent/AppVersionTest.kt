package com.maxinesworld.featureparent

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionTest {
    @Test
    fun `version label includes version name`() {
        assertEquals("Maxine's World v0.33.0", appVersionLabel("0.33.0"))
    }

    @Test
    fun `blank version falls back to app name only`() {
        assertEquals("Maxine's World", appVersionLabel(""))
        assertEquals("Maxine's World", appVersionLabel("   "))
    }
}
