package org.example.project.ui.components.message

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.ui.utils.PreviewWrapper

@Immutable
data class Message(
    val author: String,
    val content: String,
    val timestamp: String,
    val image: Int? = null,
    val authorImage: ImageVector = if (author == "me") Icons.Default.Preview else Icons.Default.Park,
) {
    companion object {
        fun createMyMessage(
            text: String
        ) = Message(
            author = "me",
            content = text,
            timestamp = "00:00"
        )
    }
}

private val JumpToBottomThreshold = 56.dp

@Composable
fun MessagesComponent(
    messages: List<Message>,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyColumn(
            reverseLayout = true,
            state = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            for (index in messages.indices) {
                val authorAbove = messages.getOrNull(index + 1)?.author
                val authorBelow = messages.getOrNull(index - 1)?.author

                val content = messages[index]

                val isAboveMessageAuthorDifferent = authorAbove != content.author
                val isBelowMessageAuthorDifferent = authorBelow != content.author
                item {
                    MessageComponent(
                        text = content.content,
                        isUserMe = content.author == "me",
                        isAboveMessageAuthorDifferent = isAboveMessageAuthorDifferent,
                        isBelowMessageAuthorDifferent = isBelowMessageAuthorDifferent
                    )
                }
            }
        }

        val jumpThreshold = with(LocalDensity.current) {
            JumpToBottomThreshold.toPx()
        }

        // Show the button if the first visible item is not the first one or if the offset is
        // greater than the threshold.
        val jumpToBottomButtonEnabled by remember {
            derivedStateOf {
                scrollState.firstVisibleItemIndex != 0 ||
                        scrollState.firstVisibleItemScrollOffset > jumpThreshold
            }
        }

        JumpToBottom(
            // Only show if the scroller is not at the bottom
            enabled = jumpToBottomButtonEnabled,
            onClicked = {
                scope.launch {
//                    scrollState.animateScrollToItem(0)
                    scrollState.scrollToItem(0)
//                    scrollState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd),
        )

    }
}

private enum class Visibility {
    VISIBLE,
    GONE,
}

/**
 * Shows a button that lets the user scroll to the bottom.
 */
@Composable
fun JumpToBottom(enabled: Boolean, onClicked: () -> Unit, modifier: Modifier = Modifier) {
    // Show Jump to Bottom button
    val transition = updateTransition(
        if (enabled) Visibility.VISIBLE else Visibility.GONE,
        label = "JumpToBottom visibility animation",
    )
    val bottomOffset by transition.animateDp(label = "JumpToBottom offset animation") {
        if (it == Visibility.GONE) {
            (-32).dp
        } else {
            32.dp
        }
    }
    if (bottomOffset > 0.dp) {
        ExtendedFloatingActionButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    modifier = Modifier.height(18.dp),
                    contentDescription = null,
                )
            },
            text = {
//                Text(text = "jump to bottom")
            },
            onClick = onClicked,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = modifier
                .offset(x = 0.dp, y = -bottomOffset)
                .height(36.dp)
                .padding(end = 16.dp),
        )
    }
}

@Composable
@Preview
private fun PreviewNight(
    @PreviewParameter(MessagesProvider::class)
    messages: List<Message>
) = PreviewWrapper {
    MessagesComponent(
        messages = messages,
        scrollState = rememberLazyListState(),
        modifier = Modifier
    )
}

@Composable
@Preview
private fun PreviewLight(
    @PreviewParameter(MessagesProvider::class)
    messages: List<Message>
) = PreviewWrapper(
    isDarkTheme = false
) {
    MessagesComponent(
        messages = messages,
        scrollState = rememberLazyListState(),
        modifier = Modifier
    )
}

private class MessagesProvider : PreviewParameterProvider<List<Message>> {

    override val values = sequenceOf(
        defaultMessageList
    )
}

val defaultMessageList = listOf(
    Message(
        author = "Alex",
        content = "@BestFriend",
        timestamp = "17:58",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Hey 👋",
        timestamp = "17:58",
        image = null,
    ),

    Message(
        author = "me",
        content = "Hi!",
        timestamp = "17:59",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Did you finish the Compose screen?",
        timestamp = "18:00",
        image = null,
    ),

    Message(
        author = "me",
        content = "Almost. Just polishing previews and animations.",
        timestamp = "18:01",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Nice 🔥",
        timestamp = "18:01",
        image = null,
    ),

    Message(
        author = "me",
        content = "I'm testing different message states right now.",
        timestamp = "18:02",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Make sure long messages work correctly.",
        timestamp = "18:03",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Make sure long messages work correctly.",
        timestamp = "18:03",
        image = null,
    ),


    Message(
        author = "Alex",
        content = "This is a super long message that should ideally span across multiple lines so you can verify paddings, alignment, max width constraints and overall visual appearance of the message bubble inside the chat screen.",
        timestamp = "18:03",
        image = null,
    ),

    Message(
        author = "me",
        content = "Yep, already checking that 😄",
        timestamp = "18:04",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "What about multiline content?",
        timestamp = "18:05",
        image = null,
    ),

    Message(
        author = "me",
        content = """
                    First line
                    Second line
                    Third line
                """.trimIndent(),
        timestamp = "18:05",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Looks good to me.",
        timestamp = "18:06",
        image = null,
    ),

    Message(
        author = "me",
        content = "Now adding emoji support 🚀🔥🎉",
        timestamp = "18:07",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "Compose usually handles that pretty well 👍",
        timestamp = "18:08",
        image = null,
    ),

    Message(
        author = "System",
        content = "User Alex joined the chat",
        timestamp = "18:08",
        image = null,
    ),

    Message(
        author = "me",
        content = "Testing empty message below",
        timestamp = "18:09",
        image = null,
    ),

    Message(
        author = "me",
        content = "",
        timestamp = "18:09",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "And another short one.",
        timestamp = "18:10",
        image = null,
    ),

    Message(
        author = "me",
        content = "Cool 😎",
        timestamp = "18:10",
        image = null,
    ),

    Message(
        author = "Alex",
        content = "You should also preview scrolling behavior with a large amount of items in LazyColumn.",
        timestamp = "18:11",
        image = null,
    ),

    Message(
        author = "me",
        content = "Good idea.",
        timestamp = "18:12",
        image = null,
    ),
)
