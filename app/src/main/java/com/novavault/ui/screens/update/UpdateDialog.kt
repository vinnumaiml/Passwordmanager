package com.novavault.ui.screens.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavault.domain.model.UpdateCheckState
import com.novavault.domain.model.UpdateDownloadState
import com.novavault.domain.model.UpdateManifest
import com.novavault.ui.components.NovaButton
import com.novavault.ui.theme.NovaColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val isVisible by viewModel.isDialogVisible.collectAsState()
    val checkState by viewModel.checkState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val context = LocalContext.current

    val manifest = (checkState as? UpdateCheckState.UpdateAvailable)?.manifest

    if (!isVisible || manifest == null) return

    BasicAlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                viewModel.hideUpdateDialog()
            }
        },
        modifier = modifier.padding(16.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NovaColors.SurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.GlowViolet),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header Row with Icon and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = NovaColors.PrimaryAccentDim.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NovaColors.SecondaryAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = NovaColors.SecondaryAccent,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NovaColors.SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NovaColors.Border),
                    ) {
                        Text(
                            text = "v${manifest.versionName}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = NovaColors.SecondaryAccent,
                                fontFamily = FontFamily.Monospace,
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }

                    if (downloadState !is UpdateDownloadState.Downloading) {
                        IconButton(onClick = { viewModel.hideUpdateDialog() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = NovaColors.TextSecondary,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Elvarix Update Available",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A new verified build is available directly from the release repository.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaColors.TextSecondary,
                )

                // Release notes box
                if (manifest.releaseNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NovaColors.SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, NovaColors.Border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = "What\'s New:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NovaColors.SecondaryAccent,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = manifest.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaColors.TextSecondary,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Download / Verification progress
                when (val state = downloadState) {
                    is UpdateDownloadState.Idle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(
                                onClick = { viewModel.hideUpdateDialog() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "Later",
                                    color = NovaColors.TextSecondary,
                                )
                            }
                            NovaButton(
                                text = "Download & Install",
                                onClick = { viewModel.startDownload(manifest) },
                                modifier = Modifier.weight(1.5f),
                            )
                        }
                    }

                    is UpdateDownloadState.Downloading -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Downloading update…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NovaColors.TextSecondary,
                                )
                                Text(
                                    text = "${state.progressPercent}%",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = NovaColors.SecondaryAccent,
                                    ),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = NovaColors.SecondaryAccent,
                                trackColor = NovaColors.SurfaceElevated,
                            )
                        }
                    }

                    is UpdateDownloadState.Verifying -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = NovaColors.SecondaryAccent,
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaColors.TextSecondary,
                            )
                        }
                    }

                    is UpdateDownloadState.ReadyToInstall -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NovaColors.Success,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHA-256 Verified. Ready to install.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = NovaColors.Success,
                                )
                            }

                            NovaButton(
                                text = "Install Update",
                                onClick = { viewModel.installApk(context, state.apkFile) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    is UpdateDownloadState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = NovaColors.Danger,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                TextButton(
                                    onClick = { viewModel.hideUpdateDialog() },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Dismiss", color = NovaColors.TextSecondary)
                                }
                                NovaButton(
                                    text = "Retry",
                                    onClick = { viewModel.startDownload(manifest) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
