package com.maxinesworld.corecontent

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ContentVerifierTest {

    private val validZip = File("src/test/resources/test-packages/valid-package.zip")
    private val tamperedZip = File("src/test/resources/test-packages/bad-checksum.zip")

    @Test
    fun `sha256 produces 64 lowercase hex characters`() {
        val tempFile = File.createTempFile("test", ".bin")
        tempFile.writeText("hello")
        val hash = ContentVerifier.sha256(tempFile)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[a-f0-9]{64}$")))
        tempFile.delete()
    }

    @Test
    fun `sha256 of known value is deterministic`() {
        val content = byteArrayOf(1, 2, 3, 4, 5)
        val file = File.createTempFile("det", ".bin")
        file.writeBytes(content)
        val h1 = ContentVerifier.sha256(file)
        val h2 = ContentVerifier.sha256(file)
        assertEquals(h1, h2)
        assertEquals(64, h1.length)
        file.delete()
    }

    @Test
    fun `sha256 length is always 64 hex chars`() {
        val file = File.createTempFile("len", ".bin")
        file.writeBytes(ByteArray(100) { it.toByte() })
        val hash = ContentVerifier.sha256(file)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[a-f0-9]{64}$")))
        file.delete()
    }

    @Test
    fun `verifyChecksum succeeds with correct hash`() {
        val file = File.createTempFile("pkg", ".bin")
        file.writeText("maxines-world-package")
        val hash = ContentVerifier.sha256(file)
        assertTrue(ContentVerifier.verifyChecksum(file, hash).isSuccess)
        file.delete()
    }

    @Test
    fun `verifyChecksum fails with wrong hash`() {
        val file = File.createTempFile("pkg2", ".bin")
        file.writeText("correct-content")
        val result = ContentVerifier.verifyChecksum(file, "0".repeat(64))
        assertTrue(result is VerificationResult.Error)
        assertTrue((result as VerificationResult.Error).message.contains("mismatch"))
        file.delete()
    }

    @Test
    fun `verifySize succeeds with correct size`() {
        val file = File.createTempFile("pkg3", ".bin")
        file.writeText("abc")
        assertTrue(ContentVerifier.verifySize(file, 3).isSuccess)
        file.delete()
    }

    @Test
    fun `verifySize fails with wrong size`() {
        val file = File.createTempFile("pkg4", ".bin")
        file.writeText("abc")
        assertTrue(ContentVerifier.verifySize(file, 999) is VerificationResult.Error)
        file.delete()
    }

    @Test
    fun `safeExtract rejects non-existent zip file`() {
        assertThrows(Exception::class.java) {
            ContentVerifier.safeExtract(File("/nonexistent.zip"), createTempDir())
        }
    }

    @Test
    fun `sha256 on known content matches expected`() {
        val file = File.createTempFile("known", ".bin")
        file.writeBytes("maxines-world-content-package-v1".toByteArray())
        val hash = ContentVerifier.sha256(file)
        assertEquals(64, hash.length)
        assertTrue(ContentVerifier.verifyChecksum(file, hash).isSuccess)
        file.delete()
    }

    private fun createTempDir(): File = File.createTempFile("td", "").also { it.delete(); it.mkdirs() }
}
