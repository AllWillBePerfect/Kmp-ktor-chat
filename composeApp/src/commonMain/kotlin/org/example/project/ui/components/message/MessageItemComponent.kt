package org.example.project.ui.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

private val CompanionMessageShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
private val MyMessageShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)

@Composable
fun MessageItemComponent(
    text: String,
    isUserMe: Boolean,
    image: Int? = null
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
    isUserMe: Boolean
) {
    val uriHandler = LocalUriHandler.current

    val styledMessage = messageFormatter(
        text = text,
        primary = isUserMe,
    )

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
        image = data.image
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
        image = data.image
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
