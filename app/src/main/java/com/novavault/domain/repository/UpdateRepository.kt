package com.novavault.domain.repository

import android.content.Intent
import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.UpdateDownloadState
import com.novavault.domain.model.UpdateManifest
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface UpdateRepository {
    val checkState: StateFlow<UpdateCheckState>
    val downloadState: StateFlow<UpdateDownloadState>

    suspend fun checkForUpdates(force: Boolean = false): Result<UpdateManifest?>
    suspend fun downloadUpdate(manifest: UpdateManifest): Result<File>
    fun canInstallApk(): Boolean
    fun getInstallIntent(apkFile: File): Intent
    fun postUpdateNotification(manifest: UpdateManifest)
    fun resetStates()
}
