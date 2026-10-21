package com.novavault.ui.screens.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novavault.ui.components.NovaButton
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AutofillSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NovaVaultTheme {
                AuroraBackground {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars)
                            .padding(24.dp),
                    ) {
                        Text(
                            text = "Nova Vault Autofill",
                            style = MaterialTheme.typography.headlineMedium,
                            color = NovaColors.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nova Vault securely autofills usernames, passwords, and TOTP codes in your apps and web browsers. Your credentials never leave your device unencrypted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaColors.TextSecondary,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        NovaButton(
                            text = "Done",
                            onClick = { finish() },
                        )
                    }
                }
            }
        }
    }
}
