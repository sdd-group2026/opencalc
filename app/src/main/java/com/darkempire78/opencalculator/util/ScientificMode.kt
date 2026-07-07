package com.darkempire78.opencalculator.util

import android.content.Context

object ScientificMode {
    fun getScientificModeTypeDescription(
        context: Context,
        scientificModeTypes: ScientificModeTypes
    ): String {
        return when (scientificModeTypes) {
            ScientificModeTypes.OFF -> context.getString(com.darkempire78.opencalculator.R.string.settings_general_scientific_mode_hide_desc)
            ScientificModeTypes.NOT_ACTIVE -> context.getString(com.darkempire78.opencalculator.R.string.settings_general_scientific_mode_deactivate_desc)
            ScientificModeTypes.ACTIVE -> context.getString(com.darkempire78.opencalculator.R.string.settings_general_scientific_mode_desc)
        }
    }
}

enum class ScientificModeTypes {
    NOT_ACTIVE,
    ACTIVE,
    OFF;

    /**
     * Stable identifier persisted in SharedPreferences. Uses the enum constant
     * [name] rather than [ordinal] so the constants can be reordered without
     * silently remapping (and corrupting) saved preferences.
     */
    val storageKey: String get() = name

    companion object {
        /** Resolves a persisted [storageKey] back to its enum value, or null if unknown. */
        fun fromStorageKey(key: String?): ScientificModeTypes? =
            entries.firstOrNull { it.name == key }
    }
}
