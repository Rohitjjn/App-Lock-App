package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.cos
import kotlin.math.sin

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

/** A bespoke animated vault illustration: orbiting app paths around a warm safe dial. */
@Composable
fun VaultIllustration(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    val transition = rememberInfiniteTransition(label = "vault_illustration")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vault_orbit_phase"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vault_breathe"
    )
    val tertiary = MaterialTheme.colorScheme.tertiary
    val detail = if (containerColor.luminance() < 0.35f) Color.White else accent

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "An illustrated private vault with orbiting app paths"
        }
    ) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val unit = minOf(size.width, size.height)
        val orbitWidth = unit * 0.62f
        val orbitHeight = unit * 0.86f

        drawCircle(
            color = containerColor.copy(alpha = 0.64f),
            radius = unit * 0.48f,
            center = center
        )
        drawCircle(
            color = accent.copy(alpha = 0.08f),
            radius = unit * 0.34f,
            center = androidx.compose.ui.geometry.Offset(center.x + unit * 0.16f, center.y - unit * 0.18f)
        )

        // Two finely stroked ellipses give the illustration a quiet, astronomical rhythm.
        rotate(degrees = -27f, pivot = center) {
            drawOval(
                color = accent.copy(alpha = 0.42f),
                topLeft = androidx.compose.ui.geometry.Offset(center.x - orbitWidth / 2f, center.y - orbitHeight / 2f),
                size = androidx.compose.ui.geometry.Size(orbitWidth, orbitHeight),
                style = Stroke(width = unit * 0.009f)
            )
        }
        rotate(degrees = 28f, pivot = center) {
            drawOval(
                color = tertiary.copy(alpha = 0.28f),
                topLeft = androidx.compose.ui.geometry.Offset(center.x - orbitWidth * 0.44f, center.y - orbitHeight * 0.42f),
                size = androidx.compose.ui.geometry.Size(orbitWidth * 0.88f, orbitHeight * 0.84f),
                style = Stroke(width = unit * 0.007f)
            )
        }

        val safeWidth = unit * 0.45f * breathe
        val safeHeight = unit * 0.49f * breathe
        val safeTopLeft = androidx.compose.ui.geometry.Offset(
            center.x - safeWidth / 2f,
            center.y - safeHeight / 2f
        )
        val safeSize = androidx.compose.ui.geometry.Size(safeWidth, safeHeight)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(accent, accent.copy(alpha = 0.86f), tertiary),
                start = androidx.compose.ui.geometry.Offset(safeTopLeft.x, safeTopLeft.y),
                end = androidx.compose.ui.geometry.Offset(safeTopLeft.x + safeWidth, safeTopLeft.y + safeHeight)
            ),
            topLeft = safeTopLeft,
            size = safeSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.10f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.38f),
            topLeft = safeTopLeft + androidx.compose.ui.geometry.Offset(unit * 0.028f, unit * 0.028f),
            size = androidx.compose.ui.geometry.Size(safeWidth - unit * 0.056f, safeHeight - unit * 0.056f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(unit * 0.078f),
            style = Stroke(width = unit * 0.009f)
        )

        // The safe dial: concentric rings and a small, tactile center pin.
        drawCircle(
            color = Color.White.copy(alpha = 0.22f),
            radius = unit * 0.145f,
            center = center,
            style = Stroke(width = unit * 0.012f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.13f),
            radius = unit * 0.105f,
            center = center,
            style = Stroke(width = unit * 0.008f)
        )
        drawCircle(
            color = detail.copy(alpha = 0.88f),
            radius = unit * 0.035f,
            center = center
        )
        drawLine(
            color = Color.White.copy(alpha = 0.72f),
            start = androidx.compose.ui.geometry.Offset(center.x, center.y - unit * 0.035f),
            end = androidx.compose.ui.geometry.Offset(center.x + unit * 0.078f, center.y - unit * 0.09f),
            strokeWidth = unit * 0.012f
        )

        val angle = Math.toRadians(phase.toDouble())
        val orbitalRadius = unit * 0.42f
        val orbitingDot = androidx.compose.ui.geometry.Offset(
            center.x + cos(angle).toFloat() * orbitalRadius,
            center.y + sin(angle).toFloat() * orbitalRadius
        )
        drawCircle(color = Color.White.copy(alpha = 0.42f), radius = unit * 0.043f, center = orbitingDot)
        drawCircle(color = accent, radius = unit * 0.025f, center = orbitingDot)
        drawCircle(
            color = tertiary.copy(alpha = 0.78f),
            radius = unit * 0.027f,
            center = androidx.compose.ui.geometry.Offset(center.x - unit * 0.39f, center.y + unit * 0.13f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.88f),
            radius = unit * 0.012f,
            center = androidx.compose.ui.geometry.Offset(center.x + unit * 0.38f, center.y - unit * 0.19f)
        )
    }
}

/** Compatibility entry point; the illustration is now the SafeLock vault motif. */
@Composable
fun ProtectionIllustration(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    VaultIllustration(modifier = modifier, accent = accent, containerColor = containerColor)
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
