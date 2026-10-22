package com.novavault.ui.screens.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.UpdateDownloadState
import com.novavault.domain.model.UpdateManifest
import com.novavault.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    val checkState: StateFlow<UpdateCheckState> = updateRepository.checkState
    val downloadState: StateFlow<UpdateDownloadState> = updateRepository.downloadState

    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch {
            updateRepository.checkForUpdates(force)
        }
    }

    fun startDownload(manifest: UpdateManifest) {
        viewModelScope.launch {
            updateRepository.downloadUpdate(manifest)
        }
    }

    fun installApk(context: Context, apkFile: java.io.File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!updateRepository.canInstallApk()) {
                // Request unknown app install permission
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val installIntent = updateRepository.getInstallIntent(apkFile)
        context.startActivity(installIntent)
    }

    fun dismissUpdate() {
        updateRepository.resetStates()
    }
}
