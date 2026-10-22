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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    val checkState: StateFlow<UpdateCheckState> = updateRepository.checkState
    val downloadState: StateFlow<UpdateDownloadState> = updateRepository.downloadState

    private val _isDialogVisible = MutableStateFlow(false)
    val isDialogVisible: StateFlow<Boolean> = _isDialogVisible.asStateFlow()

    init {
        viewModelScope.launch {
            checkState.collect { state ->
                if (state is UpdateCheckState.UpdateAvailable) {
                    _isDialogVisible.value = true
                }
            }
        }
    }

    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch {
            val result = updateRepository.checkForUpdates(force)
            if (result.isSuccess && result.getOrNull() != null) {
                _isDialogVisible.value = true
            }
        }
    }

    fun showUpdateDialog() {
        _isDialogVisible.value = true
    }

    fun hideUpdateDialog() {
        _isDialogVisible.value = false
    }

    fun startDownload(manifest: UpdateManifest) {
        viewModelScope.launch {
            updateRepository.downloadUpdate(manifest)
        }
    }

    fun installApk(context: Context, apkFile: java.io.File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!updateRepository.canInstallApk()) {
                // Open system Unknown App Sources settings for Elvarix
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
        _isDialogVisible.value = false
    }
}
