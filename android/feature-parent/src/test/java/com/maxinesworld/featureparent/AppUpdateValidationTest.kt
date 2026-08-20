package com.maxinesworld.featureparent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateValidationTest {
    @Test
    fun `accepts Android APK content types`() {
        assertTrue(isAllowedUpdateContentType("application/vnd.android.package-archive"))
        assertTrue(isAllowedUpdateContentType("application/octet-stream"))
        assertTrue(isAllowedUpdateContentType(null))
    }

    @Test
    fun `rejects HTML and unrelated responses`() {
        assertFalse(isAllowedUpdateContentType("text/html"))
        assertFalse(isAllowedUpdateContentType("application/json"))
    }

    @Test
    fun `recognizes an APK zip signature`() {
        assertTrue(hasApkZipSignature(byteArrayOf(0x50, 0x4b, 0x03, 0x04)))
        assertFalse(hasApkZipSignature("<!DOCTYPE html>".toByteArray()))
    }
}
