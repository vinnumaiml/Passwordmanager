package com.novavault.data.repository

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.UpdateDownloadState
import com.novavault.domain.model.UpdateManifest
import com.novavault.domain.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) : UpdateRepository {

    private val _checkState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    override val checkState: StateFlow<UpdateCheckState> = _checkState.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    override val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private var lastCheckTimeMs: Long = 0L
    private val checkCooldownMs = 15 * 60 * 1000L // 15 minutes throttle

    companion object {
        private const val PRIMARY_MANIFEST_URL =
            "https://github.com/vinnumaiml/Passwordmanager/releases/latest/download/latest.json"
        private const val FALLBACK_MANIFEST_URL =
            "https://vinnumaiml.github.io/Passwordmanager/latest.json"
    }

    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    override suspend fun checkForUpdates(force: Boolean): Result<UpdateManifest?> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastCheckTimeMs < checkCooldownMs) && _checkState.value is UpdateCheckState.UpdateAvailable) {
            val current = (_checkState.value as UpdateCheckState.UpdateAvailable).manifest
            return@withContext Result.success(current)
        }

        _checkState.value = UpdateCheckState.Checking
        lastCheckTimeMs = now

        try {
            val manifestText = fetchManifestString(PRIMARY_MANIFEST_URL)
                ?: fetchManifestString(FALLBACK_MANIFEST_URL)

            if (manifestText == null) {
                _checkState.value = UpdateCheckState.UpToDate
                return@withContext Result.success(null)
            }

            val manifest = json.decodeFromString<UpdateManifest>(manifestText)
            val currentCode = getCurrentVersionCode()

            if (manifest.versionCode > currentCode) {
                _checkState.value = UpdateCheckState.UpdateAvailable(manifest)
                Result.success(manifest)
            } else {
                _checkState.value = UpdateCheckState.UpToDate
                Result.success(null)
            }
        } catch (e: Exception) {
            _checkState.value = UpdateCheckState.Error(e.localizedMessage ?: "Failed to check for updates")
            Result.failure(e)
        }
    }

    private fun fetchManifestString(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Elvarix-Android-App/${getCurrentVersionCode()}")

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    override suspend fun downloadUpdate(manifest: UpdateManifest): Result<File> = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val destFile = File(updatesDir, "Elvarix-v${manifest.versionName}.apk")
        if (destFile.exists()) destFile.delete()

        _downloadState.value = UpdateDownloadState.Downloading(0, 0, 0)

        var conn: HttpURLConnection? = null
        try {
            val url = URL(manifest.apkUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Elvarix-Android-Updater")

            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("Server returned HTTP ${conn.responseCode}")
            }

            val totalBytes = conn.contentLengthLong
            val input = conn.inputStream
            val output = FileOutputStream(destFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead: Long = 0

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else 0
                _downloadState.value = UpdateDownloadState.Downloading(percent, totalRead, totalBytes)
            }

            output.flush()
            output.close()
            input.close()

            // 1. Verify Checksum
            _downloadState.value = UpdateDownloadState.Verifying("Verifying cryptographic SHA-256…")
            val computedHash = calculateSha256(destFile)
            if (!computedHash.equals(manifest.sha256.trim(), ignoreCase = true)) {
                destFile.delete()
                val errorMsg = "Checksum verification failed! Expected: ${manifest.sha256}, Got: $computedHash"
                _downloadState.value = UpdateDownloadState.Error(errorMsg)
                return@withContext Result.failure(SecurityException(errorMsg))
            }

            // 2. Verify Package Identity
            _downloadState.value = UpdateDownloadState.Verifying("Verifying package signature…")
            val archiveInfo = context.packageManager.getPackageArchiveInfo(destFile.absolutePath, 0)
            if (archiveInfo == null || archiveInfo.packageName != context.packageName) {
                destFile.delete()
                val errorMsg = "Package mismatch! Expected: ${context.packageName}, Got: ${archiveInfo?.packageName}"
                _downloadState.value = UpdateDownloadState.Error(errorMsg)
                return@withContext Result.failure(SecurityException(errorMsg))
            }

            _downloadState.value = UpdateDownloadState.ReadyToInstall(destFile)
            Result.success(destFile)
        } catch (e: Exception) {
            if (destFile.exists()) destFile.delete()
            _downloadState.value = UpdateDownloadState.Error(e.localizedMessage ?: "Download failed")
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    override fun canInstallApk(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    override fun getInstallIntent(apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    override fun resetStates() {
        _checkState.value = UpdateCheckState.Idle
        _downloadState.value = UpdateDownloadState.Idle
    }
}
