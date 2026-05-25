package org.example.project.ui.components.userinput

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.components.userinput.buttons.RecordButtonComponent
import org.example.project.ui.components.userinput.buttons.SendButtonComponent
import org.example.project.ui.utils.PreviewWrapper

@Composable
fun UserInputComponent(
    keyboardType: KeyboardType = KeyboardType.Text,
    textFieldValue: TextFieldValue,
    focusState: Boolean,
    keyboardShown: Boolean,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    onMessageSent: (String) -> Unit,

) {
    val swipeOffset = remember { mutableFloatStateOf(0f) }
    var isRecordingMessage by remember { mutableStateOf(false) }
    /// TODO: change isBlankInput
    val isBlankInput = textFieldValue.text.isBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.End,
    ) {

        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = null,
            tint = LocalContentColor.current,
            modifier = Modifier
                .sizeIn(minWidth = 56.dp, minHeight = 6.dp)
                .padding(18.dp)
                .clickable(onClick = {})

        )

        AnimatedContent(
            targetState = isRecordingMessage,
            label = "text-field",
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) { recording ->
            Box(Modifier.fillMaxSize()) {
                if (recording) {
                    RecordingIndicatorComponent { swipeOffset.floatValue }
                } else {
                    UserTextFieldComponent(
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = keyboardType,
                        focusState = focusState,
                        textFieldValue = textFieldValue,
                        onTextChanged = onTextChanged,
                        onTextFieldFocused = onTextFieldFocused,
                        onMessageSent = onMessageSent
                    )
                }
            }

        }

        AnimatedContent(isBlankInput) { isTrue ->
            if (isTrue) {
                RecordButtonComponent(
                    recording = isRecordingMessage,
                    swipeOffset = { swipeOffset.floatValue },
                    onSwipeOffsetChange = { offset -> swipeOffset.floatValue = offset },
                    onStartRecording = {
                        val consumed = !isRecordingMessage
                        isRecordingMessage = true
                        consumed
                    },
                    onFinishRecording = {
                        // handle end of recording
                        isRecordingMessage = false
                    },
                    onCancelRecording = {
                        isRecordingMessage = false
                    },
                    modifier = Modifier.fillMaxHeight(),
                )
            } else {
                SendButtonComponent(
                    textFieldValue = textFieldValue,
                    onMessageSent = onMessageSent
                )

            }
        }


    }
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    UserInputComponent(
        textFieldValue = textState,
        focusState = false,
        keyboardShown = false,
        onTextChanged = {},
        onTextFieldFocused = {},
        onMessageSent = {}
    )
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    UserInputComponent(
        textFieldValue = textState,
        focusState = false,
        keyboardShown = false,
        onTextChanged = {},
        onTextFieldFocused = {},
        onMessageSent = {}
    )
}