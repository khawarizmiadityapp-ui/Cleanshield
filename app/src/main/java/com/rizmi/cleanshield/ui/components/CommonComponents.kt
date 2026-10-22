package com.rizmi.cleanshield.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizmi.cleanshield.ui.theme.CyberNavyDark
import com.rizmi.cleanshield.ui.theme.CyanAccent
import com.rizmi.cleanshield.ui.theme.EmeraldActive
import com.rizmi.cleanshield.ui.theme.GlassBackgroundDark
import com.rizmi.cleanshield.ui.theme.GlassBorder
import com.rizmi.cleanshield.ui.theme.GlassBorderSubtle
import com.rizmi.cleanshield.ui.theme.GlassHighlight
import com.rizmi.cleanshield.ui.theme.LiquidGlowBlue
import com.rizmi.cleanshield.ui.theme.LiquidGlowCyan
import com.rizmi.cleanshield.ui.theme.LiquidGlowEmerald
import com.rizmi.cleanshield.ui.theme.LiquidGlowPurple
import com.rizmi.cleanshield.ui.theme.TextMuted
import com.rizmi.cleanshield.ui.theme.TextPrimary
import com.rizmi.cleanshield.ui.theme.TextSecondary

/**
 * Liquid Glass Background container that renders dynamic ambient glowing mesh orbs
 * behind frosted translucent cards.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GlassBackgroundDark,
                        CyberNavyDark,
                        Color(0xFF04060C)
                    )
                )
            )
    ) {
        // Fluid ambient glowing orbs behind the glass surface
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Top-left cyan liquid orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LiquidGlowCyan, Color.Transparent),
                    center = Offset(width * 0.12f, height * 0.10f),
                    radius = width * 0.55f
                ),
                radius = width * 0.55f,
                center = Offset(width * 0.12f, height * 0.10f)
            )

            // Center-right emerald protection energy orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LiquidGlowEmerald, Color.Transparent),
                    center = Offset(width * 0.88f, height * 0.36f),
                    radius = width * 0.52f
                ),
                radius = width * 0.52f,
                center = Offset(width * 0.88f, height * 0.36f)
            )

            // Lower-left deep indigo fluid aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LiquidGlowPurple, Color.Transparent),
                    center = Offset(width * 0.22f, height * 0.78f),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(width * 0.22f, height * 0.78f)
            )

            // Lower-right subtle blue aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LiquidGlowBlue, Color.Transparent),
                    center = Offset(width * 0.90f, height * 0.88f),
                    radius = width * 0.45f
                ),
                radius = width * 0.45f,
                center = Offset(width * 0.90f, height * 0.88f)
            )
        }

        // Foreground content with liquid glass layering
        content()
    }
}

/**
 * Frosted Liquid Glass Card with specular highlight border and translucent fill.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    accentGlow: Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    accentGlow?.copy(alpha = 0.6f) ?: GlassHighlight,
                    borderColor,
                    GlassBorderSubtle
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x38202F49),
                            Color(0x1F141F33)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * Backwards-compatible CyberCard elevated to Liquid Glass styling.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    backgroundColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    borderColor.copy(alpha = 0.7f),
                    GlassBorderSubtle
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x331E2C44),
                            Color(0x1A141F33)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * Interactive Liquid Glass Shield Core Button with convex glass refraction,
 * specular reflection, and pulsating energy ripple.
 */
@Composable
fun LiquidPowerCore(
    isRunning: Boolean,
    isStarting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = EmeraldActive
    val idleColor = CyanAccent
    val targetColor = if (isRunning) activeColor else idleColor

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(600),
        label = "coreColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "liquidPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRunning) 1400 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isRunning) 0.5f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRunning) 1400 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing fluid aura
        Box(
            modifier = Modifier
                .size(136.dp * pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = haloAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Middle frosted glass ring
        Box(
            modifier = Modifier
                .size(122.dp)
                .clip(CircleShape)
                .background(Color(0x1F1E293B))
                .border(
                    BorderStroke(
                        1.5.dp,
                        Brush.verticalGradient(
                            listOf(
                                animatedColor.copy(alpha = 0.65f),
                                GlassBorderSubtle
                            )
                        )
                    ),
                    CircleShape
                )
        )

        // Inner liquid glass convex orb
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = if (isRunning) 0.35f else 0.15f),
                            Color(0xCC090F1C)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                GlassHighlight,
                                animatedColor.copy(alpha = 0.8f),
                                Color(0x22FFFFFF)
                            )
                        )
                    ),
                    CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Glass reflection highlight arc on top of the dome
            Canvas(modifier = Modifier.size(86.dp)) {
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99FFFFFF),
                            Color(0x22FFFFFF),
                            Color.Transparent
                        )
                    ),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            // Power / Shield Icon in core
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Toggle Protection",
                tint = animatedColor,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

/**
 * Frosted Glass Metric Card with glowing icon pill.
 */
@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    GlassBorder,
                    Color(0x0FFFFFFF)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x331E2B42),
                            Color(0x1F141F33)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Frosted Glass Status Badge.
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    isPulsing: Boolean = false
) {
    val alpha = if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val animatedAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else {
        1.0f
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    color.copy(alpha = 0.7f),
                    color.copy(alpha = 0.2f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = color
            )
        }
    }
}
