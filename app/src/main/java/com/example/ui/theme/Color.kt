package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SafeLock's warm, editorial palette. The light surfaces borrow the softness of
 * paper and porcelain; the dark surfaces stay warm instead of falling to pure black.
 */
val SafeLockCanvas = Color(0xFFF6F2ED)
val SafeLockSurface = Color(0xFFFFFCF8)
val SafeLockSurfaceRaised = Color(0xFFF0EAE3)
val SafeLockSurfaceDeep = Color(0xFFE9E1D8)
val SafeLockOutline = Color(0xFFE2D8CF)
val SafeLockInk = Color(0xFF29241F)
val SafeLockInkMuted = Color(0xFF746B63)
val SafeLockClay = Color(0xFFB64D32)
val SafeLockClayContainer = Color(0xFFF5DDD3)
val SafeLockSage = Color(0xFF536B5B)
val SafeLockGold = Color(0xFFB87B3B)

val SafeLockDarkCanvas = Color(0xFF171512)
val SafeLockDarkSurface = Color(0xFF201D19)
val SafeLockDarkSurfaceRaised = Color(0xFF2A2621)
val SafeLockDarkSurfaceDeep = Color(0xFF342F29)
val SafeLockDarkOutline = Color(0xFF49413A)
val SafeLockDarkInk = Color(0xFFF4EEE7)
val SafeLockDarkInkMuted = Color(0xFFC2B8AD)
val SafeLockDarkClay = Color(0xFFF0A184)
val SafeLockDarkClayContainer = Color(0xFF573326)
val SafeLockDarkSage = Color(0xFFB8C9B8)
val SafeLockDarkGold = Color(0xFFE0B377)

// Backward-compatible token names used by older screen implementations. They now
// resolve to the SafeLock palette, so this visual migration doesn't alter behavior.
val XaiCanvas = SafeLockDarkCanvas
val XaiCanvasSoft = SafeLockDarkSurfaceRaised
val XaiCanvasCard = SafeLockDarkSurface
val XaiCanvasMid = SafeLockDarkSurfaceDeep
val XaiHairline = SafeLockDarkOutline
val XaiWhite = SafeLockDarkInk
val XaiOnWhite = SafeLockDarkCanvas
val XaiMuteText = SafeLockDarkInkMuted
val XaiBodyText = SafeLockDarkInkMuted
val XaiLightCanvas = SafeLockCanvas
val XaiLightCanvasSoft = SafeLockSurfaceRaised
val XaiLightCanvasCard = SafeLockSurface
val XaiLightCanvasMid = SafeLockSurfaceDeep
val XaiLightHairline = SafeLockOutline
val XaiBlack = SafeLockInk
val XaiLightMuteText = SafeLockInkMuted
val XaiLightBodyText = SafeLockInk
val XaiSunsetOrange = SafeLockClay
val XaiSunsetSoft = Color(0xFFF0B7A0)
val XaiDuskPurple = Color(0xFF765A83)
val XaiTwilight = Color(0xFFD4BEDA)
val XaiBreeze = Color(0xFFB2C6C2)

val Purple80 = XaiTwilight
val PurpleGrey80 = XaiMuteText
val Pink80 = XaiBreeze
val Purple40 = XaiDuskPurple
val PurpleGrey40 = XaiMuteText
val Pink40 = XaiSunsetOrange
val AppAccent = SafeLockClay
val AppTextLight = SafeLockDarkInkMuted
val AppBgLight = SafeLockDarkCanvas
val AppTextDark = SafeLockDarkInk
val AppBgDark = SafeLockDarkCanvas
