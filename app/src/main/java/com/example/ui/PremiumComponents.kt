package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun SafeLockMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    contentDescription: String? = null
) {
    Icon(
        painter = painterResource(R.drawable.ic_safelock_mark),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

/** A small vector-drawn security vignette used in onboarding and empty states. */
@Composable
fun ProtectionIllustration(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val shieldOnColor = if (accent.luminance() > 0.20f) Color(0xFF29241F) else Color.White
    val transition = rememberInfiniteTransition(label = "shield_illustration")
    val breathe by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_breathe"
    )

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Illustration of a shield protecting your apps"
        }
    ) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val unit = minOf(size.width, size.height)
        val shieldWidth = unit * 0.52f * breathe
        val shieldHeight = unit * 0.66f * breathe

        drawCircle(
            color = containerColor.copy(alpha = 0.62f),
            radius = unit * 0.47f,
            center = center
        )
        drawCircle(
            color = accent.copy(alpha = 0.10f),
            radius = unit * 0.36f,
            center = androidx.compose.ui.geometry.Offset(center.x + unit * 0.22f, center.y - unit * 0.22f)
        )
        drawCircle(
            color = accent.copy(alpha = 0.20f),
            radius = unit * 0.035f,
            center = androidx.compose.ui.geometry.Offset(center.x - unit * 0.38f, center.y - unit * 0.18f)
        )
        drawCircle(
            color = accent.copy(alpha = 0.34f),
            radius = unit * 0.024f,
            center = androidx.compose.ui.geometry.Offset(center.x + unit * 0.38f, center.y + unit * 0.20f)
        )

        val left = center.x - shieldWidth / 2f
        val right = center.x + shieldWidth / 2f
        val top = center.y - shieldHeight * 0.49f
        val bottom = center.y + shieldHeight * 0.50f
        val shield = Path().apply {
            moveTo(center.x, top)
            cubicTo(center.x + shieldWidth * 0.18f, top + shieldHeight * 0.09f, right - shieldWidth * 0.04f, top + shieldHeight * 0.12f, right, top + shieldHeight * 0.16f)
            lineTo(right - shieldWidth * 0.05f, center.y + shieldHeight * 0.12f)
            cubicTo(right - shieldWidth * 0.10f, center.y + shieldHeight * 0.32f, center.x + shieldWidth * 0.17f, bottom - shieldHeight * 0.05f, center.x, bottom)
            cubicTo(center.x - shieldWidth * 0.17f, bottom - shieldHeight * 0.05f, left + shieldWidth * 0.10f, center.y + shieldHeight * 0.32f, left + shieldWidth * 0.05f, center.y + shieldHeight * 0.12f)
            lineTo(left, top + shieldHeight * 0.16f)
            cubicTo(left + shieldWidth * 0.04f, top + shieldHeight * 0.12f, center.x - shieldWidth * 0.18f, top + shieldHeight * 0.09f, center.x, top)
            close()
        }

        drawPath(
            path = shield,
            brush = Brush.linearGradient(
                colors = listOf(accent, accent.copy(alpha = 0.78f), tertiary),
                start = androidx.compose.ui.geometry.Offset(left, top),
                end = androidx.compose.ui.geometry.Offset(right, bottom)
            )
        )
        drawPath(
            path = shield,
            color = Color.White.copy(alpha = 0.27f),
            style = Stroke(width = unit * 0.012f)
        )

        // A compact lock glyph, kept inside the shield silhouette.
        val lockTop = center.y - unit * 0.075f
        val lockWidth = unit * 0.15f
        val lockLeft = center.x - lockWidth / 2f
        drawArc(
            color = shieldOnColor.copy(alpha = 0.9f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(lockLeft + lockWidth * 0.18f, lockTop - unit * 0.085f),
            size = androidx.compose.ui.geometry.Size(lockWidth * 0.64f, unit * 0.16f),
            style = Stroke(width = unit * 0.018f)
        )
        drawRoundRect(
            color = shieldOnColor,
            topLeft = androidx.compose.ui.geometry.Offset(lockLeft, lockTop),
            size = androidx.compose.ui.geometry.Size(lockWidth, unit * 0.12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.025f)
        )
        drawCircle(
            color = accent,
            radius = unit * 0.012f,
            center = androidx.compose.ui.geometry.Offset(center.x, lockTop + unit * 0.045f)
        )
    }
}

@Composable
fun FeatureIllustration(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ProtectionIllustration(
            modifier = Modifier.size(176.dp),
            accent = accent,
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = accent,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.size(70.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(34.dp))
            }
        }
    }
}

@Composable
fun ProtectionStatusPill(
    label: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val tone = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
    val pill = if (positive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val onPill = if (positive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = pill,
        contentColor = onPill,
        shape = CircleShape,
        modifier = modifier.defaultMinSize(minHeight = 30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (positive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(tone)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PageEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = modifier
    )
}

@Composable
fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { content() }
    )
}
