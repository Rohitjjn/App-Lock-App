# Old Design Style Backup

## Overview
This file contains the configuration of the old Material 3 design template before the xAI Minimalist Dark redesign. It is stored as a reference to allow automated or manual rollback of the theme if requested by the user.

## Colors
- **Primary Accent (`AppAccent`)**: `#FF6B6B` (Vibrant pastel pinkish-red)
- **Background Light (`AppBgLight`)**: `#FFFFFF`
- **Text Light (`AppTextLight`)**: `#1A1A1A`
- **Background Dark (`AppBgDark`)**: `#1A1A1A`
- **Text Dark (`AppTextDark`)**: `#FFFFFF`
- **Surface Variant (Light)**: `#F5F5F5`
- **Surface Variant (Dark)**: `#2C2C2C`
- **Secondary (Dark)**: `PurpleGrey80` (`#CCC2DC`)
- **Tertiary (Dark)**: `Pink80` (`#EFB8C8`)
- **Secondary (Light)**: `PurpleGrey40` (`#625b71`)
- **Tertiary (Light)**: `Pink40` (`#7D5260`)

## Typography
- **Font Family**: Paid font substitute Pacifico loaded dynamically via systemic Cursive font family fallback (`FontFamily.Cursive`).
- **Standard Body Font**: `FontFamily.Default` for clean readability.
- **Hierarchy Mapping**:
  - `displayLarge`: Cursive, Normal, 40.sp limit
  - `headlineMedium`: Cursive, Normal, 28.sp limit
  - `titleLarge`: Cursive, Normal, 22.sp limit
  - `bodyLarge`: Default Sans, Normal, 16.sp, lineHeight 24.sp, letterSpacing 0.5.sp

## Component Styles
- Highly rounded corners (`shape = RoundedCornerShape(20.dp)` inside key dialogs and settings card blocks).
- Distinct categorizations in Settings inside elevated Material 3 Cards.
- Colorful, friendly icons with high-contrast tinted active indicators.
