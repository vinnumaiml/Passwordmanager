package com.novavault

import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.UpdateManifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testManifestJsonDeserialization() {
        val rawJson = """
            {
                "versionCode": 2,
                "versionName": "1.1.0",
                "apkUrl": "https://github.com/vinnumaiml/Passwordmanager/releases/latest/download/Elvarix.apk",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "releaseNotes": "Elvarix 1.1.0: Complete rebrand and in-app update system",
                "mandatory": false,
                "minVersionCode": 1
            }
        """.trimIndent()

        val manifest = json.decodeFromString<UpdateManifest>(rawJson)

        assertEquals(2, manifest.versionCode)
        assertEquals("1.1.0", manifest.versionName)
        assertEquals("https://github.com/vinnumaiml/Passwordmanager/releases/latest/download/Elvarix.apk", manifest.apkUrl)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", manifest.sha256)
        assertFalse(manifest.mandatory)
        assertEquals(1, manifest.minVersionCode)
    }

    @Test
    fun testManifestRoundTripSerialization() {
        val original = UpdateManifest(
            versionCode = 2,
            versionName = "1.1.0",
            apkUrl = "https://example.com/Elvarix.apk",
            sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            releaseNotes = "New features",
            mandatory = true,
            minVersionCode = 1,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<UpdateManifest>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun testUpdateCheckLogic() {
        val installedVersionCode = 1
        val manifestV2 = UpdateManifest(
            versionCode = 2,
            versionName = "1.1.0",
            apkUrl = "https://example.com/Elvarix.apk",
            sha256 = "abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234abcd1234",
        )

        val isUpdateAvailable = manifestV2.versionCode > installedVersionCode
        assertTrue("Manifest with versionCode 2 should be recognized as an update over 1", isUpdateAvailable)

        val state = if (isUpdateAvailable) {
            UpdateCheckState.UpdateAvailable(manifestV2)
        } else {
            UpdateCheckState.UpToDate
        }

        assertTrue(state is UpdateCheckState.UpdateAvailable)
        assertEquals(2, (state as UpdateCheckState.UpdateAvailable).manifest.versionCode)
    }

    @Test
    fun testSha256ChecksumFormat() {
        val validHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val shaRegex = Regex("^[a-fA-F0-9]{64}$")

        assertTrue("Valid 64-char hex string must match SHA-256 pattern", shaRegex.matches(validHash))
        assertFalse("Truncated hash must fail validation", shaRegex.matches("e3b0c442"))
    }

    @Test
    fun testUpdateV3OverV2Installed() {
        val installedVersionCode = 2
        val manifestV3 = UpdateManifest(
            versionCode = 3,
            versionName = "1.2.0",
            apkUrl = "https://raw.githubusercontent.com/vinnumaiml/Passwordmanager/main/website/Elvarix.apk",
            sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            releaseNotes = "Elvarix v1.2.0: In-app update notifications and installer",
        )

        val isUpdateAvailable = manifestV3.versionCode > installedVersionCode
        assertTrue("Manifest with versionCode 3 should be recognized as an update over 2", isUpdateAvailable)

        val state = if (isUpdateAvailable) {
            UpdateCheckState.UpdateAvailable(manifestV3)
        } else {
            UpdateCheckState.UpToDate
        }

        assertTrue(state is UpdateCheckState.UpdateAvailable)
        assertEquals(3, (state as UpdateCheckState.UpdateAvailable).manifest.versionCode)
        assertEquals("1.2.0", (state as UpdateCheckState.UpdateAvailable).manifest.versionName)
    }
}
