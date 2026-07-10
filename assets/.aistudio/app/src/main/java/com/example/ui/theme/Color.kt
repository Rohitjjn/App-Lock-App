package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// xAI Design Tokens (The "engineered-cosmic" near-black canvas)
val XaiCanvas = Color(0xFF0A0A0A)       // Near-black page background
val XaiCanvasSoft = Color(0xFF1A1C20)   // Lighter dark fill for hovered/pressed items
val XaiCanvasCard = Color(0xFF191919)   // Charcoal card fill inside product-feature list items
val XaiCanvasMid = Color(0xFF363A3F)    // Nested surfaces and code style dialog surfaces
val XaiHairline = Color(0xFF212327)     // 1px solid dividers/borders on dark surfaces

// Brand Core Typography Colors
val XaiWhite = Color(0xFFFFFFFF)
val XaiOnWhite = Color(0xFF0A0A0A)
val XaiMuteText = Color(0xFF7D8187)
val XaiBodyText = Color(0xFFDADBDF)

// xAI Light Theme Tokens
val XaiLightCanvas = Color(0xFFF9F9FB)     // Off-white background
val XaiLightCanvasSoft = Color(0xFFEDEDF0) // Slightly darker soft background
val XaiLightCanvasCard = Color(0xFFFFFFFF) // Pure white card surfaces
val XaiLightCanvasMid = Color(0xFFE2E4E9)  // Mid-gray for nested components
val XaiLightHairline = Color(0xFFD2D4D8)   // 1px border light lines

val XaiBlack = Color(0xFF0A0A0A)
val XaiLightMuteText = Color(0xFF6B7280)   // Charcoal body/mute text
val XaiLightBodyText = Color(0xFF1F2937)   // Dark text

// Cosmic Accent Tokens
val XaiSunsetOrange = Color(0xFFFF7A17)
val XaiSunsetSoft = Color(0xFFFFC285)
val XaiDuskPurple = Color(0xFF7C3AED)
val XaiTwilight = Color(0xFFC4B5FD)
val XaiBreeze = Color(0xFFA0C3EC)

// Backward-compatible bindings (mapped directly to the premium xAI palette)
val Purple80 = XaiTwilight
val PurpleGrey80 = XaiMuteText
val Pink80 = XaiBreeze

val Purple40 = XaiDuskPurple
val PurpleGrey40 = XaiMuteText
val Pink40 = XaiSunsetOrange

val AppAccent = XaiSunsetOrange
val AppTextLight = XaiBodyText
val AppBgLight = XaiCanvas
val AppTextDark = XaiWhite
val AppBgDark = XaiCanvas
