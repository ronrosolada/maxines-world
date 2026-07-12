package com.maxinesworld.corecontent

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Verifies content package integrity: SHA-256 checksum and ZIP-slip protection.
 * All validation methods fail with descriptive errors — never silently accept.
 */
object ContentVerifier {

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyChecksum(file: File, expectedSha256: String): VerificationResult {
        if (!file.exists()) return VerificationResult.Error("File not found: ${file.absolutePath}")
        require(expectedSha256.matches(Regex("^[a-f0-9]{64}$"))) {
            "Expected SHA-256 must be 64 lowercase hex characters"
        }
        val actual = sha256(file)
        if (!actual.equals(expectedSha256, ignoreCase = false)) {
            return VerificationResult.Error("SHA-256 mismatch. Expected: $expectedSha256, got: $actual")
        }
        return VerificationResult.Success
    }

    fun verifySize(file: File, expectedBytes: Long): VerificationResult {
        if (file.length() != expectedBytes) {
            return VerificationResult.Error("Size mismatch. Expected: $expectedBytes, got: ${file.length()}")
        }
        return VerificationResult.Success
    }

    /**
     * Extract ZIP safely to target directory, preventing path traversal.
     * Returns list of extracted file paths or throws on unsafe entries.
     */
    fun safeExtract(zipFile: File, targetDir: File, maxEntries: Int = 500, maxTotalBytes: Long = 50 * 1024 * 1024): List<File> {
        require(targetDir.exists() || targetDir.mkdirs()) { "Cannot create target directory: ${targetDir.absolutePath}" }
        val root = targetDir.canonicalFile
        val extracted = mutableListOf<File>()
        var totalBytes = 0L
        var entryCount = 0

        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                entryCount++
                if (entryCount > maxEntries) {
                    deleteAll(extracted)
                    throw SecurityException("ZIP contains more than $maxEntries entries — rejected")
                }

                val destFile = File(root, entry!!.name).canonicalFile
                // ZIP-slip prevention
                require(destFile.path == root.path || destFile.path.startsWith(root.path + File.separator)) {
                    "Unsafe ZIP entry: ${entry!!.name} resolves outside extraction root"
                }
                require(!entry!!.isDirectory) { "Directories not expected in content package: ${entry!!.name}" }

                destFile.parentFile?.mkdirs()
                val entryBytes = zis.copyTo(destFile.outputStream())
                totalBytes += entryBytes
                if (totalBytes > maxTotalBytes) {
                    deleteAll(extracted)
                    throw SecurityException("ZIP extracts more than $maxTotalBytes bytes — rejected")
                }

                extracted.add(destFile)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return extracted
    }

    private fun deleteAll(files: List<File>) {
        files.forEach { it.delete() }
    }

    private fun ZipInputStream.copyTo(output: java.io.OutputStream): Long {
        val buffer = ByteArray(8192)
        var total = 0L
        var count: Int
        while (read(buffer).also { count = it } != -1) {
            output.write(buffer, 0, count)
            total += count
        }
        output.close()
        return total
    }
}

sealed interface VerificationResult {
    data object Success : VerificationResult
    data class Error(val message: String) : VerificationResult
    val isSuccess get() = this is Success
}
