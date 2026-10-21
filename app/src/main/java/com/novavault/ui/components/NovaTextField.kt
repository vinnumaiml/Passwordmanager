package com.novavault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.novavault.ui.theme.NovaColors
import com.novavault.ui.theme.NovaShapes
import com.novavault.ui.theme.NovaTypography

/**
 * Styled text field for NOVA VAULT.
 *
 * Wraps [OutlinedTextField] with the app colour scheme.  Key behaviours:
 *  • Focus state drives an animated border glow that transitions from
 *    [NovaColors.Border] → [NovaColors.PrimaryAccent].
 *  • Error state overrides the border to [NovaColors.Danger] and shows an
 *    inline error message below the field.
 *  • Password variant adds a visibility-toggle trailing icon by default;
 *    callers may still supply their own [trailingContent] to override.
 *
 * @param value          Current field value.
 * @param onValueChange  Value change callback.
 * @param label          Floating label / placeholder text.
 * @param modifier        Applied to the outermost [Column].
 * @param isPassword     When `true` text is masked and a visibility toggle appears.
 * @param isError        Marks the field as invalid (red border + error text).
 * @param errorMessage   Optional error description shown below the field.
 * @param leadingIcon    Optional icon shown on the left side of the field.
 * @param trailingContent Custom trailing slot; overrides the password eye icon.
 * @param keyboardOptions Forwarded to the underlying [OutlinedTextField].
 * @param keyboardActions Forwarded to the underlying [OutlinedTextField].
 * @param enabled        Whether the field accepts input.
 * @param singleLine     Whether the field grows to multiple lines.
 */
@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    // Animated border colour: error > focused > normal
    val unfocusedBorderColor by animateColorAsState(
        targetValue = when {
            isError -> NovaColors.Danger
            else    -> NovaColors.Border
        },
        animationSpec = tween(durationMillis = 200),
        label = "unfocusedBorder",
    )
    val focusedBorderColor by animateColorAsState(
        targetValue = when {
            isError -> NovaColors.Danger
            else    -> NovaColors.PrimaryAccent
        },
        animationSpec = tween(durationMillis = 200),
        label = "focusedBorder",
    )

    val visualTransformation = if (isPassword && !passwordVisible)
        PasswordVisualTransformation()
    else
        VisualTransformation.None

    Column(modifier = modifier) {
        OutlinedTextField(
            value            = value,
            onValueChange    = onValueChange,
            label            = {
                Text(
                    text  = label,
                    style = NovaTypography.bodyMedium,
                )
            },
            modifier         = Modifier.fillMaxWidth(),
            enabled          = enabled,
            singleLine       = singleLine,
            isError          = isError,
            visualTransformation = visualTransformation,
            keyboardOptions  = keyboardOptions,
            keyboardActions  = keyboardActions,
            shape            = NovaShapes.medium,
            leadingIcon      = leadingIcon?.let {
                {
                    Icon(
                        imageVector        = it,
                        contentDescription = null,
                        tint               = if (isError) NovaColors.Danger else NovaColors.TextSecondary,
                    )
                }
            },
            trailingIcon = when {
                trailingContent != null -> trailingContent
                isPassword -> {
                    {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.VisibilityOff
                                else
                                    Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible)
                                    "Hide password"
                                else
                                    "Show password",
                                tint = NovaColors.TextSecondary,
                            )
                        }
                    }
                }
                else -> null
            },
            colors = OutlinedTextFieldDefaults.colors(
                // Text
                focusedTextColor      = NovaColors.TextPrimary,
                unfocusedTextColor    = NovaColors.TextPrimary,
                disabledTextColor     = NovaColors.TextHint,
                errorTextColor        = NovaColors.TextPrimary,
                // Container
                focusedContainerColor   = NovaColors.SurfaceVariant,
                unfocusedContainerColor = NovaColors.Surface,
                disabledContainerColor  = NovaColors.Surface,
                errorContainerColor     = NovaColors.DangerDim,
                // Cursor
                cursorColor      = NovaColors.PrimaryAccent,
                errorCursorColor = NovaColors.Danger,
                // Border
                focusedBorderColor   = focusedBorderColor,
                unfocusedBorderColor = unfocusedBorderColor,
                disabledBorderColor  = NovaColors.Divider,
                errorBorderColor     = NovaColors.Danger,
                // Label
                focusedLabelColor   = if (isError) NovaColors.Danger else NovaColors.PrimaryAccent,
                unfocusedLabelColor = NovaColors.TextHint,
                disabledLabelColor  = NovaColors.TextHint,
                errorLabelColor     = NovaColors.Danger,
                // Placeholder
                focusedPlaceholderColor   = NovaColors.TextHint,
                unfocusedPlaceholderColor = NovaColors.TextHint,
                // Leading / trailing
                focusedLeadingIconColor    = NovaColors.PrimaryAccent,
                unfocusedLeadingIconColor  = NovaColors.TextSecondary,
                focusedTrailingIconColor   = NovaColors.TextSecondary,
                unfocusedTrailingIconColor = NovaColors.TextHint,
            ),
        )

        // Inline error message
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text     = errorMessage,
                style    = NovaTypography.bodySmall,
                color    = NovaColors.Danger,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}
