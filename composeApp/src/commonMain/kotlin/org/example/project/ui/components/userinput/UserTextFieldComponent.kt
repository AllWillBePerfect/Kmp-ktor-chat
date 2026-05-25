package org.example.project.ui.components.userinput

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.utils.PreviewWrapper

@Composable
fun BoxScope.UserTextFieldComponent(
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType,
    focusState: Boolean,
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    onMessageSent: (String) -> Unit,
) {
    var lastFocusState by remember { mutableStateOf(false) }
    BasicTextField(
        modifier = modifier
            .padding(start = 32.dp)
            .align(Alignment.CenterStart)
            .onFocusChanged { state ->
                if (lastFocusState != state.isFocused) {
                    onTextFieldFocused(state.isFocused)
                }
                lastFocusState = state.isFocused
            },
        value = textFieldValue,
        onValueChange = { onTextChanged(it) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions {
            if (textFieldValue.text.isNotBlank()) onMessageSent(textFieldValue.text)
        },
        maxLines = 1,
        cursorBrush = SolidColor(LocalContentColor.current),
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current)
    )

    val disableContentColor =
        MaterialTheme.colorScheme.onSurfaceVariant
    if (textFieldValue.text.isEmpty() && !focusState) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
            ,
            text = "Введите текст",
            style = MaterialTheme.typography.bodyLarge.copy(color = disableContentColor),
        )
    }
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    Box() {
        UserTextFieldComponent (
            keyboardType = KeyboardType.Text,
            focusState = false,
            textFieldValue = textState,
            onTextChanged = {},
            onTextFieldFocused = {},
            onMessageSent = {}
        )
    }
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    Box() {
        UserTextFieldComponent (
            keyboardType = KeyboardType.Text,
            focusState = false,
            textFieldValue = textState,
            onTextChanged = {},
            onTextFieldFocused = {},
            onMessageSent = {}
        )
    }
}