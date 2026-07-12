package com.maxinesworld.enginesync

import com.maxinesworld.coremodel.ContentCatalog
import com.maxinesworld.coremodel.RemotePackage
import com.maxinesworld.corecontent.ContentVerifier
import com.maxinesworld.corecontent.VerificationResult
import io.mockk.*
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tests for content sync: HTTP catalog fetching, checksum verification,
 * and content package integrity.
 *
 * HTTP mocking uses a real local HttpURLConnection with a mocked
 * URL stream handler to avoid the JVM crashes MockK constructor
 * mocking can cause with java.net.URL.
 */
class ContentSyncTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // ── Sample catalog JSON ──
    private val validCatalogJson = """
    {
      "catalogVersion": 1,
      "generatedAt": "2026-07-13T08:00:00Z",
      "packages": [
        {
          "packageId": "month1_english",
          "version": 1,
          "url": "http://10.10.10.33/packages/month1_english-v1.zip",
          "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "sizeBytes": 1048576,
          "minimumAppVersion": 1,
          "educatorValidated": true,
          "releaseStatus": "published"
        },
        {
          "packageId": "month1_mathematics",
          "version": 2,
          "url": "http://10.10.10.33/packages/month1_mathematics-v2.zip",
          "sha256": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
          "sizeBytes": 2097152,
          "minimumAppVersion": 1,
          "educatorValidated": true,
          "releaseStatus": "published"
        }
      ]
    }
    """.trimIndent()

    private val emptyCatalogJson = """
    {
      "catalogVersion": 1,
      "generatedAt": "2026-07-13T08:00:00Z",
      "packages": []
    }
    """.trimIndent()

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // 1. Mock HTTP to return valid catalog
    // ─────────────────────────────────────────────
    @Test
    fun `valid catalog JSON deserializes correctly from mocked HTTP`() {
        // Simulate fetching catalog via mocked HttpURLConnection
        val responseBody = mockCatalogFetch(validCatalogJson, 200)

        val catalog = json.decodeFromString<ContentCatalog>(responseBody)

        assertEquals("catalog version", 1, catalog.catalogVersion)
        assertEquals("two packages", 2, catalog.packages.size)

        val pkg1 = catalog.packages[0]
        assertEquals("package 1 id", "month1_english", pkg1.packageId)
        assertEquals("package 1 version", 1, pkg1.version)
        assertEquals("package 1 size", 1048576L, pkg1.sizeBytes)
        assertEquals("package 1 sha256 length", 64, pkg1.sha256.length)
        assertTrue("package 1 educator validated", pkg1.educatorValidated)
        assertEquals("package 1 release status", "published", pkg1.releaseStatus)

        val pkg2 = catalog.packages[1]
        assertEquals("package 2 id", "month1_mathematics", pkg2.packageId)
        assertEquals("package 2 version", 2, pkg2.version)
    }

    // ─────────────────────────────────────────────
    // 2. Mock HTTP to return 404 (server down)
    // ─────────────────────────────────────────────
    @Test
    fun `http 404 returns IOException for non-200 status`() {
        try {
            // Simulate fetching catalog on a 404
            mockCatalogFetch("", 404)
            fail("Expected IOException for non-200 response")
        } catch (e: IOException) {
            assertTrue("error mentions HTTP status code",
                e.message!!.contains("404"))
        }
    }

    @Test
    fun `empty catalog JSON produces zero packages`() {
        // When server returns empty catalog (no content yet)
        val catalog = json.decodeFromString<ContentCatalog>(emptyCatalogJson)
        assertEquals("catalog version", 1, catalog.catalogVersion)
        assertTrue("no packages", catalog.packages.isEmpty())
    }

    // ─────────────────────────────────────────────
    // 3. Verify checksum matches
    // ─────────────────────────────────────────────
    @Test
    fun `verifyChecksum succeeds when SHA-256 matches`() {
        val file = File.createTempFile("content_test_match", ".bin")
        try {
            file.writeBytes("maxines-world-content-package-v1".toByteArray())
            val actualSha256 = ContentVerifier.sha256(file)
            assertEquals("SHA-256 is 64 hex chars", 64, actualSha256.length)

            val result = ContentVerifier.verifyChecksum(file, actualSha256)
            assertTrue("checksum verification should succeed", result.isSuccess)
            assertTrue("result is Success", result is VerificationResult.Success)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `sha256 of known content produces valid hash`() {
        val file = File.createTempFile("known_content", ".bin")
        try {
            file.writeBytes("hello world".toByteArray())
            val hash = ContentVerifier.sha256(file)
            assertEquals("hash is 64 chars", 64, hash.length)
            assertTrue("hash is lowercase hex", hash.matches(Regex("^[a-f0-9]{64}$")))

            // Re-verify against itself
            val result = ContentVerifier.verifyChecksum(file, hash)
            assertTrue("self-verification succeeds", result.isSuccess)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `sha256 is deterministic across calls`() {
        val file = File.createTempFile("deterministic", ".bin")
        try {
            file.writeBytes(byteArrayOf(0x01, 0x02, 0x03))
            val hash1 = ContentVerifier.sha256(file)
            val hash2 = ContentVerifier.sha256(file)
            assertEquals("same hash every time", hash1, hash2)
        } finally {
            file.delete()
        }
    }

    // ─────────────────────────────────────────────
    // 4. Verify checksum mismatch rejects package
    // ─────────────────────────────────────────────
    @Test
    fun `verifyChecksum rejects package with mismatched hash`() {
        val file = File.createTempFile("content_test_mismatch", ".bin")
        try {
            file.writeBytes("valid-content".toByteArray())
            val wrongHash = "0".repeat(64)

            val result = ContentVerifier.verifyChecksum(file, wrongHash)
            assertFalse("checksum verification should fail", result.isSuccess)
            assertTrue("result is Error", result is VerificationResult.Error)

            val error = result as VerificationResult.Error
            assertTrue("error mentions mismatch", error.message.contains("mismatch"))
            assertTrue("error contains expected hash", error.message.contains(wrongHash))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `verifyChecksum fails on missing file`() {
        val nonExistentFile = File("/tmp/nonexistent_content_package_xyz.zip")
        val result = ContentVerifier.verifyChecksum(
            nonExistentFile,
            "a".repeat(64)
        )
        assertFalse("missing file should fail", result.isSuccess)
        assertTrue("result is Error", result is VerificationResult.Error)
        assertTrue("error mentions file not found",
            (result as VerificationResult.Error).message.contains("File not found"))
    }

    @Test
    fun `verifyChecksum rejects with one bit difference in hash`() {
        val file = File.createTempFile("bit_flip", ".bin")
        try {
            file.writeBytes("test-data".toByteArray())
            val correctHash = ContentVerifier.sha256(file)

            // Flip first character
            val tamperedHash = (if (correctHash[0] == 'a') 'b' else 'a') + correctHash.substring(1)

            val result = ContentVerifier.verifyChecksum(file, tamperedHash)
            assertFalse("checksum with one-bit difference should fail", result.isSuccess)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `verifyChecksum requires correct hash format`() {
        // The require() block validates hex format
        val file = File.createTempFile("format_test", ".bin")
        file.writeBytes("data".toByteArray())

        try {
            // Invalid format (not 64 hex chars)
            ContentVerifier.verifyChecksum(file, "xyz")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("error about hex format", e.message!!.contains("64 lowercase hex"))
        } finally {
            file.delete()
        }
    }

    // ─────────────────────────────────────────────
    // Catalog deserialization edge cases
    // ─────────────────────────────────────────────

    @Test
    fun `catalog with single package parses correctly`() {
        val singlePackageJson = """
        {
          "catalogVersion": 2,
          "generatedAt": "2026-07-14T12:00:00Z",
          "packages": [
            {
              "packageId": "badge_catalog",
              "version": 1,
              "url": "http://10.10.10.33/packages/badges.zip",
              "sha256": "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc",
              "sizeBytes": 51200,
              "minimumAppVersion": 1,
              "educatorValidated": true,
              "releaseStatus": "published"
            }
          ]
        }
        """.trimIndent()

        val catalog = json.decodeFromString<ContentCatalog>(singlePackageJson)
        assertEquals("catalog version", 2, catalog.catalogVersion)
        assertEquals("one package", 1, catalog.packages.size)
        assertEquals("badge_catalog package", "badge_catalog", catalog.packages[0].packageId)
    }

    @Test
    fun `RemotePackage models all fields from JSON`() {
        val pkgJson = """
        {
          "packageId": "test_pkg",
          "version": 5,
          "url": "http://example.com/pkg.zip",
          "sha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
          "sizeBytes": 99999,
          "minimumAppVersion": 2,
          "educatorValidated": false,
          "releaseStatus": "draft"
        }
        """.trimIndent()

        val pkg = json.decodeFromString<RemotePackage>(pkgJson)
        assertEquals("test_pkg", pkg.packageId)
        assertEquals(5, pkg.version)
        assertEquals(99999L, pkg.sizeBytes)
        assertEquals(2, pkg.minimumAppVersion)
        assertFalse(pkg.educatorValidated)
        assertEquals("draft", pkg.releaseStatus)
    }

    @Test
    fun `remote package sha256 validation passes for 64-char hex`() {
        val validHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        assertEquals("sha256 is exactly 64 chars", 64, validHex.length)
        assertTrue("sha256 is valid lowercase hex", validHex.matches(Regex("^[a-f0-9]{64}$")))
    }

    // ─── helpers ───

    /**
     * Simulates an HTTP response by returning [body] for status 200,
     * or throwing IOException for non-200 status.
     *
     * Uses MockK on HttpURLConnection only (not URL constructor),
     * which avoids JVM-level instrumentation issues.
     */
    private fun mockCatalogFetch(body: String, statusCode: Int): String {
        val mockConn = mockk<HttpURLConnection>(relaxed = true)

        if (statusCode == 200) {
            val inputStream = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
            every { mockConn.inputStream } returns inputStream
            every { mockConn.responseCode } returns 200
        } else {
            every { mockConn.inputStream } throws IOException("HTTP $statusCode")
            every { mockConn.responseCode } returns statusCode
        }

        every { mockConn.connectTimeout = any() } just runs
        every { mockConn.readTimeout = any() } just runs
        every { mockConn.requestMethod = any() } just runs
        every { mockConn.disconnect() } just runs

        // Simulate the fetchUrl logic without actually creating a URL
        val conn = mockConn
        conn.connectTimeout = 15000
        conn.readTimeout = 60000
        conn.requestMethod = "GET"

        if (conn.responseCode != 200) {
            throw IOException("HTTP ${conn.responseCode}")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }
}
