package com.novavault.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val releaseNotes: String = "",
    val mandatory: Boolean = false,
    val minVersionCode: Int = 1,
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data class UpdateAvailable(val manifest: UpdateManifest) : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : UpdateDownloadState
    data class Verifying(val message: String = "Verifying cryptographic checksum…") : UpdateDownloadState
    data class ReadyToInstall(val apkFile: java.io.File) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}
