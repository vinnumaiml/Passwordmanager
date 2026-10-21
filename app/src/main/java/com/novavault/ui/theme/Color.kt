package com.novavault.ui.theme

import androidx.compose.ui.graphics.Color

// NOVA VAULT dark-first color system
object NovaColors {
    // Backgrounds
    val Background       = Color(0xFF080B16)
    val SurfaceVariant   = Color(0xFF11182A)
    val Surface          = Color(0xFF161E35)
    val SurfaceElevated  = Color(0xFF1C2540)
    val CardOverlay      = Color(0x1AFFFFFF)   // glass effect

    // Accents
    val PrimaryAccent    = Color(0xFF8B5CF6)   // violet
    val PrimaryAccentDim = Color(0xFF6D3FD8)
    val SecondaryAccent  = Color(0xFF45E5D1)   // cyan/teal
    val SecondaryDim     = Color(0xFF2BBFA9)

    // Gradient stops for aurora
    val AuroraViolet     = Color(0x308B5CF6)
    val AuroraIndigo     = Color(0x302A3BB8)
    val AuroraCyan       = Color(0x2045E5D1)
    val AuroraMidnight   = Color(0xFF080B16)

    // Text
    val TextPrimary      = Color(0xFFF5F7FF)
    val TextSecondary    = Color(0xFF9AA7C2)
    val TextHint         = Color(0xFF4E5A77)

    // Semantic
    val Danger           = Color(0xFFFF453A)
    val DangerDim        = Color(0x33FF453A)
    val Warning          = Color(0xFFFFB340)
    val WarningDim       = Color(0x33FFB340)
    val Success          = Color(0xFF32D74B)
    val SuccessDim       = Color(0x3332D74B)

    // Strength meter
    val StrengthVeryWeak   = Color(0xFFFF453A)
    val StrengthWeak       = Color(0xFFFF6961)
    val StrengthFair       = Color(0xFFFFB340)
    val StrengthStrong     = Color(0xFF34C759)
    val StrengthVeryStrong = Color(0xFF32D74B)

    // Divider / border
    val Divider      = Color(0x1AF5F7FF)
    val Border       = Color(0x33F5F7FF)
    val GlowViolet   = Color(0x668B5CF6)
    val GlowCyan     = Color(0x4445E5D1)
}
