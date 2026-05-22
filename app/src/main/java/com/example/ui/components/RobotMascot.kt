package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MascotState

@Composable
fun RobotMascot(
    state: MascotState,
    speechBubbleText: String?,
    onBubbleSpeechRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant breathing bounce transition
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_float")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Eye blink animation
    var triggerBlink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3000..6000).random().toLong())
            triggerBlink = true
            delay(150)
            triggerBlink = false
        }
    }

    val eyeScaleY by animateFloatAsState(
        targetValue = if (triggerBlink) 0.05f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "eye_blink"
    )

    // Gear eye rotation for THINKING state
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gear_rotation"
    )

    // State dependent colors and features
    val glowColor = when (state) {
        MascotState.IDLE -> Color(0xFF386663) // Spruce Green
        MascotState.THINKING -> Color(0xFFD97706) // Saffron Clay Orange
        MascotState.ALERT -> Color(0xFFC2410C) // Terracotta Red
        MascotState.HAPPY -> Color(0xFF4D7C0F) // Leaf Green
        MascotState.SPEAKING -> Color(0xFF0F766E) // Forest Teal Green
    }

    val stateLabel = when (state) {
        MascotState.IDLE -> "Observando..."
        MascotState.THINKING -> "Revisando..."
        MascotState.ALERT -> "Dica Ortográfica!"
        MascotState.HAPPY -> "Perfeito!"
        MascotState.SPEAKING -> "Falando..."
    }

    // Mascot bouncing container
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Speech Bubble on the Left, adaptive
        if (!speechBubbleText.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { translationY = bounceOffset }
                    .padding(end = 12.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.5.dp, glowColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                    .clickable { onBubbleSpeechRequested() }
                    .padding(12.dp)
                    .testTag("mascot_speech_bubble")
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🤖 SpellBot • $stateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = glowColor
                        )
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Speak guidance",
                            tint = glowColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = speechBubbleText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Mascot Visuals
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer { translationY = bounceOffset }
                .testTag("mascot_robot_body"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)

                // 1. Draw glowing background shadow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = size.width / 2.3f
                    ),
                    radius = size.width / 2.1f
                )

                // 2. Ears / Antenna Nodes
                // Left Ear Antena
                drawRoundRect(
                    color = Color(0xFFA2AD9C),
                    topLeft = Offset(size.width * 0.15f, size.height * 0.35f),
                    size = Size(size.width * 0.12f, size.height * 0.3f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawCircle(color = glowColor, radius = 6f, center = Offset(size.width * 0.21f, size.height * 0.32f))

                // Right Ear Antena
                drawRoundRect(
                    color = Color(0xFFA2AD9C),
                    topLeft = Offset(size.width * 0.73f, size.height * 0.35f),
                    size = Size(size.width * 0.12f, size.height * 0.3f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawCircle(color = glowColor, radius = 6f, center = Offset(size.width * 0.79f, size.height * 0.32f))

                // 3. Main Head Plate
                val headWidth = size.width * 0.52f
                val headHeight = size.height * 0.46f
                val headLeft = (size.width - headWidth) / 2
                val headTop = size.height * 0.28f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF386663), Color(0xFF224341))
                    ),
                    topLeft = Offset(headLeft, headTop),
                    size = Size(headWidth, headHeight),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                // Head glossy border
                drawRoundRect(
                    color = Color(0xFFDCE5E0).copy(alpha = 0.4f),
                    topLeft = Offset(headLeft, headTop),
                    size = Size(headWidth, headHeight),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 3f)
                )

                // 4. LED Screen Lens for eyes
                val screenWidth = headWidth * 0.82f
                val screenHeight = headHeight * 0.5f
                val screenLeft = headLeft + (headWidth - screenWidth) / 2
                val screenTop = headTop + headHeight * 0.12f

                drawRoundRect(
                    color = Color(0xFF1C1C17),
                    topLeft = Offset(screenLeft, screenTop),
                    size = Size(screenWidth, screenHeight),
                    cornerRadius = CornerRadius(16f, 16f)
                )

                // 5. Draw Eyes based on status
                val eyeY = screenTop + screenHeight / 2
                val eyeSpacing = screenWidth * 0.28f
                val leftEyeX = center.x - eyeSpacing
                val rightEyeX = center.x + eyeSpacing
                val eyeRadius = size.width * 0.05f

                when (state) {
                    MascotState.THINKING -> {
                        // Drawing spinning crosses / gear eyes
                        // We will simulate eye shape using canvas rotatable drawings or simple rotating lines
                        val rRadius = eyeRadius * 1.1f
                        
                        // Let's paint bright yellow gears
                        drawCircle(color = Color(0x33D97706), radius = rRadius * 1.5f, center = Offset(leftEyeX, eyeY))
                        drawCircle(color = Color(0x33D97706), radius = rRadius * 1.5f, center = Offset(rightEyeX, eyeY))

                        // Left rotating cross line
                        val radianOffset = Math.toRadians(rotationDegrees.toDouble()).toFloat()
                        val lDx = Math.cos(radianOffset.toDouble()).toFloat() * rRadius
                        val lDy = Math.sin(radianOffset.toDouble()).toFloat() * rRadius

                        drawLine(
                            color = glowColor,
                            start = Offset(leftEyeX - lDx, eyeY - lDy),
                            end = Offset(leftEyeX + lDx, eyeY + lDy),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = glowColor,
                            start = Offset(leftEyeX + lDy, eyeY - lDx),
                            end = Offset(leftEyeX - lDy, eyeY + lDx),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        // Right rotating cross line
                        drawLine(
                            color = glowColor,
                            start = Offset(rightEyeX - lDx, eyeY - lDy),
                            end = Offset(rightEyeX + lDx, eyeY + lDy),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = glowColor,
                            start = Offset(rightEyeX + lDy, eyeY - lDx),
                            end = Offset(rightEyeX - lDy, eyeY + lDx),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                    MascotState.HAPPY -> {
                        // Curved happy eyes ^ ^
                        val pathLeft = Path().apply {
                            moveTo(leftEyeX - eyeRadius, eyeY + eyeRadius * 0.4f)
                            quadraticTo(leftEyeX, eyeY - eyeRadius * 1.2f, leftEyeX + eyeRadius, eyeY + eyeRadius * 0.4f)
                        }
                        drawPath(path = pathLeft, color = glowColor, style = Stroke(width = 6f, cap = StrokeCap.Round))

                        val pathRight = Path().apply {
                            moveTo(rightEyeX - eyeRadius, eyeY + eyeRadius * 0.4f)
                            quadraticTo(rightEyeX, eyeY - eyeRadius * 1.2f, rightEyeX + eyeRadius, eyeY + eyeRadius * 0.4f)
                        }
                        drawPath(path = pathRight, color = glowColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                    }
                    MascotState.ALERT -> {
                        // Wide open alarmed eyes / squinted focus
                        drawCircle(color = glowColor, radius = eyeRadius * 1.1f, center = Offset(leftEyeX, eyeY))
                        drawCircle(color = glowColor, radius = eyeRadius * 1.1f, center = Offset(rightEyeX, eyeY))
                        // Draw exclamation nodes inside
                        drawCircle(color = Color.Black, radius = 4f, center = Offset(leftEyeX, eyeY))
                        drawCircle(color = Color.Black, radius = 4f, center = Offset(rightEyeX, eyeY))
                        // Worried robotic eyebrows
                        drawLine(
                            color = glowColor,
                            start = Offset(leftEyeX - eyeRadius, eyeY - eyeRadius * 1.5f),
                            end = Offset(leftEyeX + eyeRadius * 0.8f, eyeY - eyeRadius * 0.9f),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = glowColor,
                            start = Offset(rightEyeX + eyeRadius, eyeY - eyeRadius * 1.5f),
                            end = Offset(rightEyeX - eyeRadius * 0.8f, eyeY - eyeRadius * 0.9f),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                    else -> {
                        // MascotState.IDLE or SPEAKING, normal eyes with periodic blink
                        drawRoundRect(
                            color = glowColor,
                            topLeft = Offset(leftEyeX - eyeRadius, eyeY - eyeRadius * eyeScaleY),
                            size = Size(eyeRadius * 2, eyeRadius * 2 * eyeScaleY),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = glowColor,
                            topLeft = Offset(rightEyeX - eyeRadius, eyeY - eyeRadius * eyeScaleY),
                            size = Size(eyeRadius * 2, eyeRadius * 2 * eyeScaleY),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                }

                // 6. Dynamic Mouth
                val mouthWidth = screenWidth * 0.35f
                val mouthY = headTop + headHeight * 0.74f

                when (state) {
                    MascotState.HAPPY -> {
                        // Wide glowing smile
                        val pathMouth = Path().apply {
                            moveTo(center.x - mouthWidth / 2, mouthY)
                            quadraticTo(center.x, mouthY + 12f, center.x + mouthWidth / 2, mouthY)
                        }
                        drawPath(path = pathMouth, color = glowColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
                    }
                    MascotState.ALERT -> {
                        // Small surprised circle mouth 'o'
                        drawCircle(
                            color = glowColor,
                            radius = 6f,
                            center = Offset(center.x, mouthY)
                        )
                    }
                    MascotState.SPEAKING -> {
                        // Sound wave lip motion
                        val pathVoice = Path().apply {
                            moveTo(center.x - mouthWidth / 2, mouthY)
                            quadraticTo(center.x - mouthWidth / 4, mouthY - 10f, center.x, mouthY + 6f)
                            quadraticTo(center.x + mouthWidth / 4, mouthY - 10f, center.x + mouthWidth / 2, mouthY)
                        }
                        drawPath(path = pathVoice, color = glowColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                    }
                    MascotState.THINKING -> {
                        // Flat line mouth
                        drawLine(
                            color = Color(0xFFA2AD9C),
                            start = Offset(center.x - mouthWidth / 3, mouthY),
                            end = Offset(center.x + mouthWidth / 3, mouthY),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                    else -> {
                        // IDLE comfortable state
                        val pathMouthStr = Path().apply {
                            moveTo(center.x - mouthWidth / 2, mouthY)
                            quadraticTo(center.x, mouthY + 5f, center.x + mouthWidth / 2, mouthY)
                        }
                        drawPath(path = pathMouthStr, color = Color(0xFFA2AD9C), style = Stroke(width = 4f, cap = StrokeCap.Round))
                    }
                }

                // 7. Mini light antenna dot
                drawRoundRect(
                    color = Color(0xFFA2AD9C),
                    topLeft = Offset(center.x - 4f, headTop - 12f),
                    size = Size(8f, 14f)
                )
                drawCircle(
                    color = glowColor,
                    radius = 8f,
                    center = Offset(center.x, headTop - 15f)
                )
            }
        }
    }
}

// Delayed helper
suspend fun delay(milis: Long) {
    kotlinx.coroutines.delay(milis)
}
