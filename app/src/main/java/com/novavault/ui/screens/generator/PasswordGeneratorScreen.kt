package com.novavault.ui.screens.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.novavault.domain.usecase.vault.GeneratePasswordUseCase
import com.novavault.domain.usecase.vault.PasswordOptions
import com.novavault.domain.usecase.vault.PasswordStrength
import com.novavault.ui.components.NovaButton
import com.novavault.ui.components.PasswordStrengthIndicator
import com.novavault.ui.theme.AuroraBackground
import com.novavault.ui.theme.NovaColors
import com.novavault.util.SecureClipboard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

enum class GeneratorMode { PASSWORD, PASSPHRASE }

@HiltViewModel
class PasswordGeneratorViewModel @Inject constructor(
    private val passwordUseCase: GeneratePasswordUseCase,
) : ViewModel() {

    var mode by mutableStateOf(GeneratorMode.PASSWORD)
    var length by mutableFloatStateOf(18f)
    var includeUppercase by mutableStateOf(true)
    var includeLowercase by mutableStateOf(true)
    var includeDigits by mutableStateOf(true)
    var includeSymbols by mutableStateOf(true)
    var excludeAmbiguous by mutableStateOf(false)

    var wordCount by mutableFloatStateOf(4f)
    var separator by mutableStateOf("-")

    var generatedResult by mutableStateOf("")

    init {
        generate()
    }

    fun generate() {
        generatedResult = if (mode == GeneratorMode.PASSWORD) {
            passwordUseCase.generatePassword(
                PasswordOptions(
                    length = length.toInt(),
                    uppercase = includeUppercase,
                    lowercase = includeLowercase,
                    digits = includeDigits,
                    symbols = includeSymbols,
                    excludeAmbiguous = excludeAmbiguous,
                )
            )
        } else {
            passwordUseCase.generatePassphrase(
                wordCount = wordCount.toInt(),
                separator = separator,
            )
        }
    }

    val strength: PasswordStrength
        get() = passwordUseCase.estimateStrength(generatedResult)
}

@Composable
fun PasswordGeneratorScreen(
    navController: NavController,
    viewModel: PasswordGeneratorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    AuroraBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NovaColors.TextPrimary,
                    )
                }
                Text(
                    text = "Password Generator",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaColors.TextPrimary,
                )
                IconButton(onClick = {
                    viewModel.generate()
                    copied = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = NovaColors.SecondaryAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Output Display Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NovaColors.SurfaceVariant)
                    .border(1.dp, NovaColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = viewModel.generatedResult,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            lineHeight = 28.sp,
                        ),
                        color = NovaColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordStrengthIndicator(strength = viewModel.strength)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NovaButton(
                            text = if (copied) "Copied!" else "Copy Password",
                            onClick = {
                                SecureClipboard.copyAndScheduleClear(
                                    context = context,
                                    text = viewModel.generatedResult,
                                    label = "Generated Password",
                                    clearAfterSeconds = 30,
                                    scope = scope,
                                )
                                copied = true
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovaColors.SurfaceElevated)
                    .padding(4.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.mode = GeneratorMode.PASSWORD
                            viewModel.generate()
                            copied = false
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewModel.mode == GeneratorMode.PASSWORD) NovaColors.PrimaryAccent else NovaColors.SurfaceElevated,
                ) {
                    Text(
                        text = "Password",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaColors.TextPrimary,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            viewModel.mode = GeneratorMode.PASSPHRASE
                            viewModel.generate()
                            copied = false
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewModel.mode == GeneratorMode.PASSPHRASE) NovaColors.PrimaryAccent else NovaColors.SurfaceElevated,
                ) {
                    Text(
                        text = "Passphrase",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaColors.TextPrimary,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options
            if (viewModel.mode == GeneratorMode.PASSWORD) {
                // Length Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Length", color = NovaColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("${viewModel.length.toInt()} characters", color = NovaColors.SecondaryAccent, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = viewModel.length,
                    onValueChange = {
                        viewModel.length = it
                        viewModel.generate()
                        copied = false
                    },
                    valueRange = 8f..64f,
                    colors = SliderDefaults.colors(
                        thumbColor = NovaColors.SecondaryAccent,
                        activeTrackColor = NovaColors.PrimaryAccent,
                    ),
                )

                Spacer(modifier = Modifier.height(14.dp))

                GeneratorToggle(
                    title = "Uppercase Letters (A-Z)",
                    checked = viewModel.includeUppercase,
                    onCheckedChange = {
                        viewModel.includeUppercase = it
                        viewModel.generate()
                    },
                )

                GeneratorToggle(
                    title = "Lowercase Letters (a-z)",
                    checked = viewModel.includeLowercase,
                    onCheckedChange = {
                        viewModel.includeLowercase = it
                        viewModel.generate()
                    },
                )

                GeneratorToggle(
                    title = "Numbers (0-9)",
                    checked = viewModel.includeDigits,
                    onCheckedChange = {
                        viewModel.includeDigits = it
                        viewModel.generate()
                    },
                )

                GeneratorToggle(
                    title = "Symbols (!@#$%^&*)",
                    checked = viewModel.includeSymbols,
                    onCheckedChange = {
                        viewModel.includeSymbols = it
                        viewModel.generate()
                    },
                )

                GeneratorToggle(
                    title = "Exclude Ambiguous (0, O, l, 1)",
                    checked = viewModel.excludeAmbiguous,
                    onCheckedChange = {
                        viewModel.excludeAmbiguous = it
                        viewModel.generate()
                    },
                )
            } else {
                // Passphrase Word Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Word Count", color = NovaColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("${viewModel.wordCount.toInt()} words", color = NovaColors.SecondaryAccent, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Slider(
                    value = viewModel.wordCount,
                    onValueChange = {
                        viewModel.wordCount = it
                        viewModel.generate()
                        copied = false
                    },
                    valueRange = 3f..8f,
                    steps = 4,
                    colors = SliderDefaults.colors(
                        thumbColor = NovaColors.SecondaryAccent,
                        activeTrackColor = NovaColors.PrimaryAccent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun GeneratorToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = NovaColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NovaColors.TextPrimary,
                checkedTrackColor = NovaColors.PrimaryAccent,
            ),
        )
    }
}
