package com.maxinesworld.featurelessonplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NarrationPreferencesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun restoreDefault() = runBlocking {
        NarrationPreferences.setEnabled(context, true)
    }

    @Test
    fun narrationPreferencePersistsAndDefaultsOn() = runBlocking {
        NarrationPreferences.setEnabled(context, false)
        assertFalse(NarrationPreferences.enabled(context).first())

        NarrationPreferences.setEnabled(context, true)
        assertTrue(NarrationPreferences.enabled(context).first())
    }
}
