package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define the handwritten font family using Compose's safe, built-in Cursive font family
val PacificoFontFamily = FontFamily.Cursive

// Set of Material typography styles to start with
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PacificoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PacificoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PacificoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
)
