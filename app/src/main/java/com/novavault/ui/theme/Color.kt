package com.novavault.ui.theme

import androidx.compose.ui.graphics.Color

// ELVARIX — Precision Mobile Studio Design System Palette
object ElvarixColors {
    // Dark Theme Foundation
    val Background       = Color(0xFF0B0D12) // Primary dark background
    val SecondaryBackground = Color(0xFF11151D) // Secondary dark surface
    val SurfaceVariant   = Color(0xFF11151D) // Card / component surface
    val Surface          = Color(0xFF141A24) // Base container surface
    val SurfaceElevated  = Color(0xFF181D27) // Elevated surface / modals
    val CardOverlay      = Color(0x0DFFFFFF) // Very subtle layered sheen

    // Brand Accents
    val PrimaryAccent    = Color(0xFFA78BFA) // Soft radiant violet
    val PrimaryAccentDim = Color(0xFF8B5CF6) // Deep violet
    val SecondaryAccent  = Color(0xFF67E8D1) // Clean luminous mint / cyan
    val SecondaryDim     = Color(0xFF45D1BA) // Restrained cyan

    // Ambient Lighting Stops
    val AuroraViolet     = Color(0x1EA78BFA)
    val AuroraIndigo     = Color(0x1E3B426B)
    val AuroraCyan       = Color(0x1867E8D1)
    val AuroraMidnight   = Color(0xFF0B0D12)

    // Typography
    val TextPrimary      = Color(0xFFF4F5F8) // High contrast primary text
    val TextSecondary    = Color(0xFFA5ADBC) // Readable secondary text
    val TextMuted        = Color(0xFF778296) // Metadata and caption text
    val TextHint         = Color(0xFF556075) // Input placeholders

    // Accessible Semantic Indicators
    val Success          = Color(0xFF10B981) // Accessible Emerald Green
    val SuccessDim       = Color(0x2A10B981)
    val Warning          = Color(0xFFF59E0B) // Accessible Amber
    val WarningDim       = Color(0x2AF59E0B)
    val Danger           = Color(0xFFEF4444) // Accessible Red
    val DangerDim        = Color(0x2AEF4444)

    // Strength Meter Tokens
    val StrengthVeryWeak   = Color(0xFFEF4444)
    val StrengthWeak       = Color(0xFFF87171)
    val StrengthFair       = Color(0xFFF59E0B)
    val StrengthStrong     = Color(0xFF34D399)
    val StrengthVeryStrong = Color(0xFF10B981)

    // Structural Dividers and Borders
    val Divider      = Color(0xFF181F2C)
    val Border       = Color(0xFF222938)
    val BorderFocused = Color(0xFFA78BFA)
    val GlowViolet   = Color(0x40A78BFA)
    val GlowCyan     = Color(0x3367E8D1)

    // Light Theme Palette
    object Light {
        val Background       = Color(0xFFF8FAFC)
        val SurfaceVariant   = Color(0xFFFFFFFF)
        val Surface          = Color(0xFFFFFFFF)
        val SurfaceElevated  = Color(0xFFF1F5F9)
        val TextPrimary      = Color(0xFF0F172A)
        val TextSecondary    = Color(0xFF475569)
        val TextMuted        = Color(0xFF64748B)
        val TextHint         = Color(0xFF94A3B8)
        val PrimaryAccent    = Color(0xFF7C3AED)
        val SecondaryAccent  = Color(0xFF0D9488)
        val Divider          = Color(0xFFE2E8F0)
        val Border           = Color(0xFFCBD5E1)
    }
}

// Backward compatibility alias
typealias NovaColors = ElvarixColors
