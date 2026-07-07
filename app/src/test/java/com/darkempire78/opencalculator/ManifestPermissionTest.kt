package com.darkempire78.opencalculator

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestPermissionTest {

    private val manifest = readProjectFile("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml")
    private val mainActivity = readProjectFile(
        "src/main/java/com/darkempire78/opencalculator/activities/MainActivity.kt",
        "app/src/main/java/com/darkempire78/opencalculator/activities/MainActivity.kt"
    )

    @Test
    fun manifestDoesNotRequestUnusedSensitiveLockScreenPermissions() {
        assertFalse(
            "Lock-screen access is handled by MainActivity window flags; do not add overlay permission unless an overlay runtime path is introduced.",
            manifest.contains("android.permission.SYSTEM_ALERT_WINDOW")
        )
        assertFalse(
            "OpenCalc does not post full-screen notifications; do not add full-screen intent permission unless that feature is introduced.",
            manifest.contains("android.permission.USE_FULL_SCREEN_INTENT")
        )
    }

    @Test
    fun lockScreenBehaviorUsesActivityWindowFlags() {
        assertTrue(manifest.contains("android:showOnLockScreen=\"true\""))
        assertTrue(manifest.contains("android:turnScreenOn=\"true\""))
        assertTrue(mainActivity.contains("setShowWhenLocked(true)"))
        assertTrue(mainActivity.contains("setTurnScreenOn(true)"))
        assertTrue(mainActivity.contains("WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED"))
        assertTrue(mainActivity.contains("WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON"))
    }

    private fun readProjectFile(vararg paths: String): String {
        val file = paths.map(::File).firstOrNull(File::isFile)
        requireNotNull(file) { "Unable to find project file from candidates: ${paths.joinToString()}" }
        return file.readText()
    }
}
