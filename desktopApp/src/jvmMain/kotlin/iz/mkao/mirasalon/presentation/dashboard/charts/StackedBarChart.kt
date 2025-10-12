package iz.mkao.mirasalon.presentation.dashboard.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iz.mkao.mirasalon.core.designsystem.theme.*

data class AppointmentBarData(val day: String, val confirmed: Float, val cancelled: Float)

@Composable
fun StackedBarChart(
    data: List<AppointmentBarData>,
    modifier: Modifier = Modifier,
    maxValue: Float = 15f,
    yAxisLabel: String = "Value",
    primaryColor: Color = MiraCoral,
    secondaryColor: Color = MiraCoral
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = MiraTextSecondary)


    var startAnim by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 1200)
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
            val label = if (tick == 0f) "0" else "${tick.toInt()}"
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


        val barWidth = 24.dp.toPx()
        val spacePerDay = plotWidth / data.size

        data.forEachIndexed { i, dayData ->
            val xCenter = plotLeft + spacePerDay * i + spacePerDay / 2


            val primaryHeight = (dayData.confirmed / maxValue) * plotHeight * animatedProgress
            val secondaryHeight = (dayData.cancelled / maxValue) * plotHeight * animatedProgress

            val barLeft = xCenter - barWidth/2
            val barRight = xCenter + barWidth/2


            drawRect(
                color = primaryColor,
                topLeft = Offset(barLeft, plotBottom - primaryHeight),
                size = Size(barWidth, primaryHeight)
            )


            if (dayData.cancelled > 0) {
                val secondaryTop = plotBottom - primaryHeight - secondaryHeight
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(barLeft, secondaryTop),
                    size = Size(barWidth, secondaryHeight)
                )
            }


            val measured = textMeasurer.measure(dayData.day, labelStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = dayData.day,
                style = labelStyle,
                topLeft = Offset(xCenter - measured.size.width / 2f, plotBottom + 8.dp.toPx()),
            )
        }
    }
}
