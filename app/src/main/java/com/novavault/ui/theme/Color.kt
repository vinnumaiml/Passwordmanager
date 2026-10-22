package com.novavault.ui.theme

import androidx.compose.ui.graphics.Color

// ELVARIX — Futuristic Cyberpunk Security Palette
object ElvarixColors {
    // Backgrounds
    val Background       = Color(0xFF080A14)
    val SurfaceVariant   = Color(0xFF10162A) // Deep surface
    val Surface          = Color(0xFF141C33)
    val SurfaceElevated  = Color(0xFF171F35) // Elevated surface
    val CardOverlay      = Color(0x1AFFFFFF) // Glassmorphic overlay

    // Accents
    val PrimaryAccent    = Color(0xFF8B5CF6) // Royal violet
    val PrimaryAccentDim = Color(0xFF6D3FD8)
    val SecondaryAccent  = Color(0xFF43E7D0) // Electric cyan
    val SecondaryDim     = Color(0xFF22C4AD)

    // Gradient stops for aurora
    val AuroraViolet     = Color(0x358B5CF6)
    val AuroraIndigo     = Color(0x35312E81)
    val AuroraCyan       = Color(0x2543E7D0)
    val AuroraMidnight   = Color(0xFF080A14)

    // Text
    val TextPrimary      = Color(0xFFF4F6FF)
    val TextSecondary    = Color(0xFFA5B0CC)
    val TextHint         = Color(0xFF525E7A)

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
    val Divider      = Color(0x1AF4F6FF)
    val Border       = Color(0x33F4F6FF)
    val GlowViolet   = Color(0x668B5CF6)
    val GlowCyan     = Color(0x4443E7D0)
}

// Backward compatibility alias
typealias NovaColors = ElvarixColors
