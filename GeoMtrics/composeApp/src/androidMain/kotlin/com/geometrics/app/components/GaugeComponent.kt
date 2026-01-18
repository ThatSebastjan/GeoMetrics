package com.geometrics.app.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.drawscope.rotate
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
@Preview
fun GaugeComponent(
    value: MutableState<Float>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 20.dp,
    startColor: Color = Color(0xFF00C853),
    endColor: Color = Color(0xFF00B0FF),
    backgroundColor: Color = Color(0xFFEEEEEE),
    showTicks: Boolean = false,
    tickCount: Int = 10
) {
    val clamped = value.value.coerceIn(0f, 100f)
    val fraction = animateFloatAsState(targetValue = clamped / 100f).value

    val startAngle = 135f
    val totalSweep = 270f
    val sweep = totalSweep * fraction

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokePx = strokeWidth.toPx()
            val radius = (kotlin.math.min(this.size.width, this.size.height) / 2f) - strokePx / 2f
            val center = this.center

            // background arc
            drawArc(
                color = backgroundColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
            )

            // create a sweep gradient that finishes its transition at the current sweep angle
            val gradientEndFraction = (sweep / 360f).coerceIn(0f, 1f)
            val brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0.0f to startColor,
                    gradientEndFraction to endColor,
                    1.0f to endColor
                ),
                center = center
            )
            rotate(startAngle, pivot = center) {
            drawArc(
                brush = brush,
                    startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
            )
            }
            // dot at the start of the gradient to ensure a clean rounded start
            val startAngleRad = Math.toRadians(startAngle.toDouble())
            val dotX = center.x + radius * cos(startAngleRad).toFloat()
            val dotY = center.y + radius * sin(startAngleRad).toFloat()


            drawCircle(
                color = startColor,
                radius = strokePx / 2f,
                center = Offset(dotX, dotY)
            )

            if (showTicks && tickCount > 0) {
                val tickLength = strokePx * 0.45f
                val tickWidth = strokePx * 0.08f
                val step = totalSweep / (tickCount - 1).coerceAtLeast(1)
                for (i in 0 until tickCount) {
                    val angleDeg = startAngle + i * step
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val outerX = center.x + radius * cos(angleRad).toFloat()
                    val outerY = center.y + radius * sin(angleRad).toFloat()
                    val innerX = center.x + (radius - tickLength) * cos(angleRad).toFloat()
                    val innerY = center.y + (radius - tickLength) * sin(angleRad).toFloat()

                    drawLine(
                        color = backgroundColor.copy(alpha = 0.9f),
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = tickWidth
                    )
                }
            }
        }

        Text(
            text = "${clamped.roundToInt()}%",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = (size.value * 0.18).sp,
            textAlign = TextAlign.Center
        )
    }
}