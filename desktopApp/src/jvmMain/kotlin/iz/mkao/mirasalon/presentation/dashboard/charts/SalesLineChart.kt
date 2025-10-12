package iz.mkao.mirasalon.presentation.dashboard.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.*

@Composable
fun SalesLineChart(
    primaryData: List<Float>,
    secondaryData: List<Float>? = null,
    dayLabels: List<String>,
    dayStartIndices: List<Int>,
    modifier: Modifier = Modifier,
    maxValue: Float = 60f,
    yAxisLabel: String = "Value",
    primaryColor: Color = MiraCoral,
    secondaryColor: Color = MiraCoral,
    isCurrency: Boolean = true
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = MiraTextSecondary)


    var startAnim by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1500)
    )
    LaunchedEffect(Unit) {
        startAnim = true
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Canvas(modifier = modifier) {
        val leftAxisWidth = 56.dp.toPx()
        val bottomAxisHeight = 22.dp.toPx()
        val plotLeft = leftAxisWidth
        val plotRight = size.width
        val plotBottom = size.height - bottomAxisHeight
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom

        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f), 0f)


        val step = maxValue / 3
        listOf(0f, step, step * 2, maxValue).forEach { tick ->
            val y = plotBottom - (tick / maxValue) * plotHeight
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = pathEffect,
            )
            val label = if (tick == 0f) {
                if (isCurrency) "$0" else "0"
            } else {
                if (isCurrency) {
                    if (tick >= 1000f) "$${(tick / 1000).toInt()}k" else "$${tick.toInt()}"
                } else {
                    if (tick >= 1000f) "${(tick / 1000).toInt()}k" else "${tick.toInt()}"
                }
            }
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                style = labelStyle,
                topLeft = Offset(26.dp.toPx(), y - measured.size.height / 2f),
            )
        }


        val sideX = 10.dp.toPx()
        val sideY = plotHeight / 2
        withTransform({
            rotate(-90f, Offset(sideX, sideY))
        }) {
            val sideLabelStyle = TextStyle(fontSize = 12.sp, color = MiraTextSecondary)
            val sideMeasured = textMeasurer.measure(yAxisLabel, sideLabelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = yAxisLabel,
                style = sideLabelStyle,
                topLeft = Offset(sideX - sideMeasured.size.width / 2f, sideY - sideMeasured.size.height / 2f)
            )
        }


        dayStartIndices.forEachIndexed { idx, sampleIndex ->
            val x = plotLeft + plotWidth * sampleIndex / (primaryData.size - 1f)
            val measured = textMeasurer.measure(dayLabels[idx], labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = dayLabels[idx],
                style = labelStyle,
                topLeft = Offset(x - measured.size.width / 2f, plotBottom + 8.dp.toPx()),
            )
        }

        fun pointsFor(data: List<Float>): List<Offset> {
            val n = data.size
            return data.mapIndexed { i, v ->
                val x = plotLeft + plotWidth * i / (n - 1f)
                val y = plotBottom - (v / maxValue) * plotHeight
                Offset(x, y)
            }
        }

        drawSmoothCurve(pointsFor(primaryData), primaryColor, animatedProgress)
        secondaryData?.let {
            drawSmoothCurve(pointsFor(it), secondaryColor, animatedProgress)
        }
    }
}

private fun DrawScope.drawSmoothCurve(points: List<Offset>, color: Color, progress: Float) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val p0 = points[if (i == 0) i else i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]

            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f
            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f

            cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
    }

    val segmentPath = Path()
    val pathMeasure = PathMeasure()
    pathMeasure.setPath(path, false)
    val length = pathMeasure.length
    pathMeasure.getSegment(0f, length * progress, segmentPath, true)

    drawPath(
        path = segmentPath,
        color = color,
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
    )
}
