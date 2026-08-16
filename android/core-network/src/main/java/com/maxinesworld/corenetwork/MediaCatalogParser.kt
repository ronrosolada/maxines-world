package com.maxinesworld.corenetwork

import com.maxinesworld.coremodel.MediaAssessment
import com.maxinesworld.coremodel.MediaAsset
import com.maxinesworld.coremodel.MediaCatalog
import kotlinx.serialization.json.Json

/**
 * Parses and validates the optional media catalog before the app trusts any
 * download path or integrity metadata.
 */
class MediaCatalogParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    fun parse(raw: String): MediaCatalog {
        val catalog = runCatching {
            json.decodeFromString<MediaCatalog>(raw)
        }.getOrElse { error ->
            throw IllegalArgumentException("Invalid media catalog JSON: ${error.message}", error)
        }

        require(catalog.catalogVersion in SUPPORTED_CATALOG_VERSIONS) {
            "Unsupported media catalog version: ${catalog.catalogVersion}"
        }

        val ids = mutableSetOf<String>()
        catalog.media.forEach { asset ->
            require(ids.add(asset.mediaId)) {
                "Duplicate mediaId in media catalog: ${asset.mediaId}"
            }
            validateAsset(asset)
            asset.assessment?.let { validateAssessment(asset.mediaId, it) }
        }
        return catalog
    }

    private fun validateAsset(asset: MediaAsset) {
        require(asset.mediaId.matches(MEDIA_ID_PATTERN)) {
            "Invalid mediaId: ${asset.mediaId}"
        }
        require(asset.title.isNotBlank()) { "Media title must not be blank: ${asset.mediaId}" }
        require(asset.file.startsWith("media/")) {
            "Media path must remain under media/: ${asset.file}"
        }
        require(!asset.file.contains("..") && !asset.file.contains('\\') && !asset.file.startsWith('/')) {
            "Unsafe media path: ${asset.file}"
        }
        require(asset.file.endsWith(".mp4", ignoreCase = true)) {
            "Media file must be an MP4: ${asset.file}"
        }
        require(asset.sha256.matches(SHA256_PATTERN)) {
            "Invalid sha256 for ${asset.mediaId}"
        }
        require(asset.sizeBytes > 0) { "Media size must be positive: ${asset.mediaId}" }
        require(asset.durationSeconds > 0) { "Media duration must be positive: ${asset.mediaId}" }
        require(asset.width > 0 && asset.height > 0) {
            "Media dimensions must be positive: ${asset.mediaId}"
        }
        require(asset.mimeType.equals("video/mp4", ignoreCase = true)) {
            "Unsupported media MIME type for ${asset.mediaId}: ${asset.mimeType}"
        }
        require(asset.licenseStatus.isNotBlank()) {
            "Media licenseStatus must not be blank: ${asset.mediaId}"
        }
    }

    private fun validateAssessment(mediaId: String, assessment: MediaAssessment) {
        require(assessment.questionCount == assessment.items.size) {
            "Assessment questionCount does not match items for $mediaId"
        }
        require(assessment.items.size in 5..10) {
            "Assessment question count must be between 5 and 10 for $mediaId"
        }
        require(assessment.passingCorrectCount in 1..assessment.items.size) {
            "Assessment passingCorrectCount is invalid for $mediaId"
        }

        val prompts = mutableSetOf<String>()
        val sequences = assessment.items.map { it.sequence }.sorted()
        require(sequences == (1..assessment.items.size).toList()) {
            "Assessment sequences must be contiguous for $mediaId"
        }

        assessment.items.forEach { item ->
            require(item.itemId == "$mediaId-q${item.sequence.toString().padStart(2, '0')}") {
                "Invalid assessment itemId for $mediaId: ${item.itemId}"
            }
            require(item.type == "MULTIPLE_CHOICE") {
                "Unsupported assessment item type for ${item.itemId}: ${item.type}"
            }
            require(item.prompt.isNotBlank() && prompts.add(item.prompt.trim())) {
                "Assessment prompts must be unique and non-blank for $mediaId"
            }
            require(item.explanation.isNotBlank()) {
                "Assessment explanation must not be blank: ${item.itemId}"
            }
            val optionIds = item.options.map { it.id }
            require(optionIds.toSet() == setOf("a", "b", "c", "d")) {
                "Assessment options must use a, b, c, d exactly: ${item.itemId}"
            }
            require(item.options.all { it.text.isNotBlank() }) {
                "Assessment options must not be blank: ${item.itemId}"
            }
            require(item.correctOptionIds.size == 1 && item.correctOptionIds.single() in optionIds) {
                "Assessment answer key must select one listed option: ${item.itemId}"
            }
        }
    }

    private companion object {
        val SUPPORTED_CATALOG_VERSIONS = setOf(1, 2)
        val MEDIA_ID_PATTERN = Regex("^[a-z0-9][a-z0-9-]{2,63}$")
        val SHA256_PATTERN = Regex("^[a-f0-9]{64}$")
    }
}
