package org.example.project.ui.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.example.project.ui.utils.PreviewWrapper
import org.example.project.ui.utils.SymbolAnnotationType
import org.example.project.ui.utils.messageFormatter
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus
import org.example.project.v2.ui.components.channels.MessageReadStatusIcon

private val CompanionMessageShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
private val MyMessageShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)

@Composable
fun MessageItemComponent(
    text: String,
    isUserMe: Boolean,
    image: Int? = null,
    readCount: Int,
    syncStatus: SyncStatus,
    createdAt: String?
) {
    val backgroundColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val messageShape = if (isUserMe) {
        MyMessageShape
    } else {
        CompanionMessageShape
    }

    Column {
        Surface(
            color = backgroundColor,
            shape = messageShape,
        ) {
            ClickableMessage(
                text = text,
                isUserMe = isUserMe,
                readCount = readCount,
                syncStatus = syncStatus,
                createdAt = createdAt
            )
        }

        image?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = backgroundColor,
                shape = messageShape,
            ) {
                Image(
                    imageVector = Icons.Default.Preview,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(160.dp),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ClickableMessage(
    text: String,
    isUserMe: Boolean,
    readCount: Int,
    syncStatus: SyncStatus,
    createdAt: String?
) {
    val uriHandler = LocalUriHandler.current

    val styledMessage = messageFormatter(
        text = text,
        primary = isUserMe,
    )

    Row {
        ClickableText(
            text = styledMessage,
            style = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
            modifier = Modifier.padding(16.dp),
            onClick = {
                styledMessage
                    .getStringAnnotations(start = it, end = it)
                    .firstOrNull()
                    ?.let { annotation ->
                        when (annotation.tag) {
                            SymbolAnnotationType.LINK.name -> uriHandler.openUri(annotation.item)
                            SymbolAnnotationType.PERSON.name -> {}
                            else -> Unit
                        }
                    }
            },
        )
        Column(
            Modifier.padding(end = 16.dp)
        ) {
            if (isUserMe && readCount > 0) {
                Row {
                    MessageReadStatusIcon(
                        syncStatus = syncStatus,
                        isMessageRead = true,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (readCount == 1) "Read" else "$readCount",
                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            } else if (isUserMe) {
                Row {
                    MessageReadStatusIcon(
                        syncStatus = syncStatus,
                        isMessageRead = false,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Sent",
                        style = MaterialTheme.typography.labelSmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            createdAt
                ?.toChatTimeLabel()
                ?.let { Text(it) }
        }
    }
}

@Composable
fun MessageReadStatusIcon(
    syncStatus: SyncStatus,
    isMessageRead: Boolean,
    modifier: Modifier = Modifier,
) {
    when (syncStatus) {
        SyncStatus.IN_PROGRESS,
        SyncStatus.SYNC_NEEDED,
            -> Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = "Pending",
            modifier = modifier,
//            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SyncStatus.COMPLETED -> {
            if (isMessageRead) {
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = "Read",
                    modifier = modifier,
//                    tint = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Sent",
                    modifier = modifier,
//                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SyncStatus.FAILED_PERMANENTLY -> Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = "Failed",
            modifier = modifier,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

private fun String.toChatTimeLabel(): String? {
    val timePart = substringAfter('T', missingDelimiterValue = "")
    if (timePart.length < 5) return null
    val hours = timePart.substring(0, 2)
    val minutes = timePart.substring(3, 5)
    return if (hours.all(Char::isDigit) && minutes.all(Char::isDigit)) {
        "$hours:$minutes"
    } else {
        null
    }
}



@Composable
@Preview
private fun MessageItemComponentPreviewNight(
    @PreviewParameter(MessageItemPreviewProvider::class)
    data: MessageItemPreviewData
) = PreviewWrapper {
    MessageItemComponent(
        text = data.text,
        isUserMe = data.isUserMe,
        image = data.image,
        readCount = 0,
        syncStatus = SyncStatus.SYNC_NEEDED,
        createdAt = ""
    )
}

@Composable
@Preview
private fun MessageItemComponentPreviewLight(
    @PreviewParameter(MessageItemPreviewProvider::class)
    data: MessageItemPreviewData
) = PreviewWrapper(
    isDarkTheme = false
) {
    MessageItemComponent(
        text = data.text,
        isUserMe = data.isUserMe,
        image = data.image,
        readCount = 0,
        syncStatus = SyncStatus.SYNC_NEEDED,
        createdAt = ""
    )
}

private data class MessageItemPreviewData(
    val text: String,
    val isUserMe: Boolean,
    val image: Int?
)

private class MessageItemPreviewProvider : PreviewParameterProvider<MessageItemPreviewData> {

    override val values = sequenceOf(
        MessageItemPreviewData(
            text = "Hello",
            isUserMe = true,
            image = null
        ),

        MessageItemPreviewData(
            text = "Hi 👋",
            isUserMe = false,
            image = null
        ),

        MessageItemPreviewData(
            text = "How are you doing today?",
            isUserMe = false,
            image = null
        ),

        MessageItemPreviewData(
            text = "I'm fine, thanks! Working on my Compose UI.",
            isUserMe = true,
            image = null
        ),

        MessageItemPreviewData(
            text = "This is a very long message preview to check how the bubble behaves when the text spans across multiple lines in the layout.",
            isUserMe = false,
            image = null
        ),

        MessageItemPreviewData(
            text = "Nice!",
            isUserMe = true,
            image = null
        ),

        MessageItemPreviewData(
            text = "",
            isUserMe = false,
            image = null
        ),

        MessageItemPreviewData(
            text = "Message with emoji 😄🔥🚀",
            isUserMe = true,
            image = null
        ),

        MessageItemPreviewData(
            text = "Multiline message\nSecond line\nThird line",
            isUserMe = false,
            image = null
        )
    )
}
