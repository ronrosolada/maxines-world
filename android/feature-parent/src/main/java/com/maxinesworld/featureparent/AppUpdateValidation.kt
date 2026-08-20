package com.maxinesworld.featureparent

internal fun isAllowedUpdateContentType(contentType: String?): Boolean {
    val normalized = contentType?.substringBefore(';')?.trim()?.lowercase()
    return normalized == null || normalized.isBlank() || normalized in setOf(
        "application/vnd.android.package-archive",
        "application/octet-stream",
        "binary/octet-stream",
    )
}

internal fun hasApkZipSignature(bytes: ByteArray): Boolean =
    bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte() &&
        bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
