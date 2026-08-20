package com.maxinesworld.coredatabase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GodModeManagerTest {
    @Test
    fun enabledStatePersistsAcrossManagerInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val childId = "god-mode-test-child"
        val manager = GodModeManager(context)
        try {
            manager.setEnabled(childId, false)
            assertFalse(manager.isEnabledNow(childId))

            manager.setEnabled(childId, true)
            val newManagerInstance = GodModeManager(context)
            assertTrue(newManagerInstance.isEnabledNow(childId))
        } finally {
            manager.setEnabled(childId, false)
        }
    }
}
