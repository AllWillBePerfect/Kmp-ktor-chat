package org.example.project.ui.components.userinput

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.example.project.ui.utils.PreviewWrapper
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun RecordingIndicatorComponent(swipeOffset: () -> Float) {
    var duration by remember { mutableStateOf(Duration.ZERO) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            duration += 1.seconds
        }
    }
    Row(
        Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")

        val animatedPulse = infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                tween(2000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
        Box(
            Modifier
                .size(56.dp)
                .padding(24.dp)
                .graphicsLayer {
                    scaleX = animatedPulse.value
                    scaleY = animatedPulse.value
                }
                .clip(CircleShape)
                .background(Color.Red),
        )
        Text(
            duration.toComponents { minutes, seconds, _ ->
                val min = minutes.toString().padStart(2, '0')
                val sec = seconds.toString().padStart(2, '0')
                "$min:$sec"
            },
            Modifier.alignByBaseline(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .alignByBaseline()
                .clipToBounds(),
        ) {
            val swipeThreshold = with(LocalDensity.current) { 200.dp.toPx() }
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = swipeOffset() / 2
                        alpha = 1 - (swipeOffset().absoluteValue / swipeThreshold)
                    },
                textAlign = TextAlign.Center,
                text = "\u25C0 Swipe to cancel",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
    RecordingIndicatorComponent(
        { 0.1f }
    )
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    RecordingIndicatorComponent(
        { 1f }
    )
}

