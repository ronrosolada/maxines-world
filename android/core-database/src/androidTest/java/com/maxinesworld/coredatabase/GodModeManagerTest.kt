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
        val manager = GodModeManager(context)
        try {
            manager.setEnabled(false)
            assertFalse(manager.isEnabled())

            manager.setEnabled(true)
            val newManagerInstance = GodModeManager(context)
            assertTrue(newManagerInstance.isEnabled())
        } finally {
            manager.setEnabled(false)
        }
    }
}
