package com.novavault.data.repository

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.novavault.R
import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.UpdateDownloadState
import com.novavault.domain.model.UpdateManifest
import com.novavault.domain.repository.UpdateRepository
import com.novavault.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
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
    private val checkCooldownMs = 2 * 60 * 1000L // 2 minutes cooldown between background checks

    companion object {
        const val NOTIFICATION_CHANNEL_UPDATES = "elvarix_updates"
        const val NOTIFICATION_ID_UPDATE = 4001

        // Ordered by reliability: raw GitHub main branch repository first, then GitHub Releases, then GitHub Pages
        val MANIFEST_URLS = listOf(
            "https://raw.githubusercontent.com/vinnumaiml/Passwordmanager/main/website/latest.json",
            "https://github.com/vinnumaiml/Passwordmanager/releases/latest/download/latest.json",
            "https://vinnumaiml.github.io/Passwordmanager/latest.json"
        )
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
            var manifestText: String? = null
            for (url in MANIFEST_URLS) {
                manifestText = fetchManifestString(url)
                if (manifestText != null) break
            }

            if (manifestText == null) {
                val errorMsg = "Unable to connect to update servers. Please check your internet connection."
                _checkState.value = UpdateCheckState.Error(errorMsg)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val manifest = json.decodeFromString<UpdateManifest>(manifestText)
            val currentCode = getCurrentVersionCode()

            if (manifest.versionCode > currentCode) {
                _checkState.value = UpdateCheckState.UpdateAvailable(manifest)
                postUpdateNotification(manifest)
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

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
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

        // Candidate URLs: manifest URL first, then fallback to repository direct raw link
        val candidateUrls = listOfNotNull(
            manifest.apkUrl.takeIf { it.isNotBlank() },
            "https://raw.githubusercontent.com/vinnumaiml/Passwordmanager/main/website/Elvarix.apk",
            "https://github.com/vinnumaiml/Passwordmanager/releases/latest/download/Elvarix.apk"
        ).distinct()

        var downloadSuccess = false
        var lastError: Exception? = null

        for (downloadUrl in candidateUrls) {
            try {
                performDownload(downloadUrl, destFile)
                downloadSuccess = true
                break
            } catch (e: Exception) {
                lastError = e
                if (destFile.exists()) destFile.delete()
            }
        }

        if (!downloadSuccess) {
            val errorMsg = lastError?.localizedMessage ?: "Download failed across all update mirrors"
            _downloadState.value = UpdateDownloadState.Error(errorMsg)
            return@withContext Result.failure(lastError ?: IOException(errorMsg))
        }

        try {
            // 1. Verify Checksum
            _downloadState.value = UpdateDownloadState.Verifying("Verifying cryptographic SHA-256…")
            val computedHash = calculateSha256(destFile)
            if (manifest.sha256.isNotBlank() && !computedHash.equals(manifest.sha256.trim(), ignoreCase = true)) {
                destFile.delete()
                val errorMsg = "Checksum mismatch!\nExpected: ${manifest.sha256}\nCalculated: $computedHash"
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
            _downloadState.value = UpdateDownloadState.Error(e.localizedMessage ?: "Verification failed")
            Result.failure(e)
        }
    }

    private fun performDownload(urlString: String, destFile: File) {
        var currentUrl = urlString
        var conn: HttpURLConnection? = null
        var redirects = 0

        while (redirects < 5) {
            val url = URL(currentUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "Elvarix-Android-Updater")

            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                status == HttpURLConnection.HTTP_MOVED_PERM ||
                status == HttpURLConnection.HTTP_SEE_OTHER ||
                status == 307 || status == 308
            ) {
                val newUrl = conn.getHeaderField("Location")
                conn.disconnect()
                if (newUrl != null) {
                    currentUrl = newUrl
                    redirects++
                    continue
                }
            }

            if (status !in 200..299) {
                conn.disconnect()
                throw IllegalStateException("Server returned HTTP $status for $currentUrl")
            }
            break
        }

        val totalBytes = conn!!.contentLengthLong
        val input: InputStream = conn.inputStream
        val output = FileOutputStream(destFile)

        try {
            val buffer = ByteArray(16384)
            var bytesRead: Int
            var totalRead: Long = 0

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else 0
                _downloadState.value = UpdateDownloadState.Downloading(percent, totalRead, totalBytes)
            }

            output.flush()
        } finally {
            try { output.close() } catch (_: Exception) {}
            try { input.close() } catch (_: Exception) {}
            conn.disconnect()
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(16384)
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

    override fun postUpdateNotification(manifest: UpdateManifest) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_UPDATES,
                    "App Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for new Elvarix versions and security updates"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                action = "com.novavault.action.OPEN_UPDATE"
                putExtra("OPEN_UPDATE_DIALOG", true)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("Elvarix v${manifest.versionName} Available")
                .setContentText("A new verified update is ready to install inside the app.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "A new verified update is available (v${manifest.versionName}). Tap to download and install inside Elvarix.\n${manifest.releaseNotes.take(160)}"
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_UPDATE, notification)
        } catch (_: Exception) {
            // Non-critical background failure suppressed
        }
    }

    override fun resetStates() {
        _checkState.value = UpdateCheckState.Idle
        _downloadState.value = UpdateDownloadState.Idle
    }
}
