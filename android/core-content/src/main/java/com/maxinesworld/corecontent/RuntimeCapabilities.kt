package com.maxinesworld.corecontent

/**
 * Runtime capabilities advertised by this version of the Android app.
 * Used to check compatibility with content packages before downloading.
 * Schema version 1 — frozen 2026-07-13.
 */
object RuntimeCapabilities {
    const val APP_VERSION_CODE = 9  // maps to v0.9.x
    val SUPPORTED_SCHEMA_VERSIONS = setOf(1)
    val ACTIVITY_CAPABILITIES = setOf(
        "ANIMATED_EXPLANATION_V1",
        "MULTIPLE_CHOICE_V1",
        "SORT_AND_CLASSIFY_V1",
        "HOTSPOT_IMAGE_V1",
        "MATCHING_PAIRS_V1",
        "SEQUENCE_BUILDER_V1",
        "INTERACTIVE_SPEC_V1"
    )

    fun isCompatible(minimumAppVersionCode: Int, schemaVersion: Int, requiredCapabilities: Set<String>): Boolean =
        minimumAppVersionCode <= APP_VERSION_CODE &&
            schemaVersion in SUPPORTED_SCHEMA_VERSIONS &&
            ACTIVITY_CAPABILITIES.containsAll(requiredCapabilities)
}

/**
 * Package states for installed content.
 */
enum class ContentPackageState {
    STAGING, VALIDATING, ACTIVE, SUPERSEDED, QUARANTINED, FAILED
}

/**
 * Represents the result of a package compatibility check.
 */
sealed interface CompatibilityResult {
    data object Compatible : CompatibilityResult
    data class Incompatible(val reasons: List<String>) : CompatibilityResult

    companion object {
        fun check(appVersion: Int, schema: Int, capabilities: Set<String>): CompatibilityResult {
            val reasons = mutableListOf<String>()
            if (appVersion > RuntimeCapabilities.APP_VERSION_CODE)
                reasons.add("Package requires app version $appVersion, installed is ${RuntimeCapabilities.APP_VERSION_CODE}")
            if (schema !in RuntimeCapabilities.SUPPORTED_SCHEMA_VERSIONS)
                reasons.add("Unsupported schema version $schema (supported: ${RuntimeCapabilities.SUPPORTED_SCHEMA_VERSIONS})")
            val missing = capabilities - RuntimeCapabilities.ACTIVITY_CAPABILITIES
            if (missing.isNotEmpty())
                reasons.add("Missing runtime capabilities: ${missing.joinToString(", ")}")
            return if (reasons.isEmpty()) Compatible else Incompatible(reasons)
        }
    }
}
