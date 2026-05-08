package org.example.project.ui.screens.chats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.utils.PreviewWrapper

@Composable
fun RoundBadgeComponent(
    count: Int,
    modifier: Modifier = Modifier
) {

    val displayCount = remember(count) {
        when {
            count > 9999 -> "9999+"
            count < 0 -> "0"
            else -> count.toString()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(90))
            .background(MaterialTheme.colorScheme.primary)
            .defaultMinSize(
                minWidth = 24.dp,
                minHeight = 20.dp
            )
            .padding(horizontal = 2.dp)
        ,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayCount,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private class BadgeCountProvider : PreviewParameterProvider<Int> {
    override val values = sequenceOf(
        1,
        9,
        27,
        345,
        4113,
        10000
    )
}

@Composable
@Preview
private fun PreviewNight(
    @PreviewParameter(BadgeCountProvider::class)
    count: Int
) = PreviewWrapper {

    RoundBadgeComponent(
        count = count
    )
}

@Composable
@Preview
private fun PreviewLight(
    @PreviewParameter(BadgeCountProvider::class)
    count: Int
) = PreviewWrapper(
    isDarkTheme = false
) {
    RoundBadgeComponent(
        count = count
    )
}