package com.maxinesworld.app

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression guard for the 2026-08-06 launch crash: the bundled Baloo 2 / Nunito
 * "TTF" files were accidentally committed as HTML documents (Google Fonts page),
 * so every fresh install crashed with "Could not load font" the moment the first
 * OutlinedTextField rendered. These tests fail fast if any bundled font resource
 * is missing or not a loadable typeface.
 */
@RunWith(AndroidJUnit4::class)
class FontsLoadableTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun baloo2Bold_loadsAsTypeface() {
        assertNotNull(ResourcesCompat.getFont(context, R.font.baloo2_bold))
    }

    @Test
    fun nunitoRegular_loadsAsTypeface() {
        assertNotNull(ResourcesCompat.getFont(context, R.font.nunito_regular))
    }

    @Test
    fun nunitoBold_loadsAsTypeface() {
        assertNotNull(ResourcesCompat.getFont(context, R.font.nunito_bold))
    }
}
