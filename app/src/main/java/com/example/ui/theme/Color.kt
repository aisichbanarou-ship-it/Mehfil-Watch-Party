package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Mehfil – Watch Party Maroon + Yellow-Cream Palette
val MehfilMainBackground = Color(0xFF5A1028)    // Main background: Deep Maroon #5A1028
val MehfilSecondary = Color(0xFF701F32)         // Secondary backgrounds and buttons: Maroon #701F32
val MehfilInputField = Color(0xFF4A0C20)        // Input fields: Deep Maroon #4A0C20

val MehfilYellowCream = Color(0xFFFFE6A3)       // All text and icons: Yellow-Cream #FFE6A3
val MehfilCreamGold = Color(0xFFFFDF85)         // Headings: Cream-Gold #FFDF85
val MehfilMutedGold = Color(0xFFB99555)         // Borders: Muted Gold #B99555

// Backward-compatible semantic bindings (all strictly adhering to the palette):
val MehfilDeepNavy = MehfilMainBackground
val MehfilSurfaceNavy = MehfilMainBackground
val MehfilCardNavy = MehfilSecondary
val MehfilBorderNavy = MehfilMutedGold

val MehfilGold = MehfilCreamGold
val MehfilGoldBright = MehfilCreamGold
val MehfilGoldDark = MehfilMutedGold
val MehfilGoldContainer = MehfilInputField

val MehfilCyanAccent = MehfilYellowCream
val MehfilCyanLight = MehfilYellowCream
val MehfilCyanDark = MehfilSecondary

val MehfilTextPrimary = MehfilYellowCream
val MehfilTextSecondary = MehfilYellowCream
val MehfilTextMuted = MehfilYellowCream

val MehfilActiveGreen = MehfilMutedGold
val MehfilAlertRed = MehfilSecondary
val MehfilWarningOrange = MehfilMutedGold
