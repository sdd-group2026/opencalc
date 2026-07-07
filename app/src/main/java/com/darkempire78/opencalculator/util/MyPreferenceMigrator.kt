import android.content.SharedPreferences
import com.darkempire78.opencalculator.util.ScientificModeTypes

/**
 * Normalises the scientific-mode SharedPreferences value to the current stable
 * string representation (the enum constant name), migrating any legacy format.
 *
 * Storage history for this preference:
 *   - Legacy boolean flag (scientific mode on/off by default)
 *   - Enum *ordinal* stored as Int (fragile: breaks if the enum is reordered)
 *   - Current: enum constant name stored as String (stable across reordering)
 */
object MyPreferenceMigrator {

    private val DEFAULT = ScientificModeTypes.OFF

    /**
     * Reads the scientific-mode preference, migrating any legacy representation
     * to the current stable string key, persisting the normalised value, and
     * returning the resolved [ScientificModeTypes].
     */
    fun getScientificMode(sharedPreferences: SharedPreferences, key: String): ScientificModeTypes {
        val mode = when (val value = sharedPreferences.all[key]) {
            // Current format: the enum constant name.
            is String -> ScientificModeTypes.fromStorageKey(value) ?: DEFAULT
            // Legacy: boolean flag (true == scientific mode active by default).
            is Boolean -> if (value) ScientificModeTypes.ACTIVE else ScientificModeTypes.NOT_ACTIVE
            // Legacy: enum ordinal stored as Int. Mapped by index ONCE, here, only
            // to migrate old installs - never used for ongoing storage.
            is Int -> ScientificModeTypes.entries.getOrNull(value) ?: DEFAULT
            else -> DEFAULT
        }
        save(sharedPreferences, key, mode)
        return mode
    }

    /** Persists [mode] using its stable string [ScientificModeTypes.storageKey]. */
    private fun save(sharedPreferences: SharedPreferences, key: String, mode: ScientificModeTypes) {
        sharedPreferences.edit()
            .putString(key, mode.storageKey)
            .apply()
    }
}
