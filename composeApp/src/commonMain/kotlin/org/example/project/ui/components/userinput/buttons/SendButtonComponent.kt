package org.example.project.ui.components.userinput.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun SendButtonComponent(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    onMessageSent: (String) -> Unit,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.Send,
        contentDescription = null,
        tint = LocalContentColor.current,
        modifier = modifier
            .sizeIn(minWidth = 56.dp, minHeight = 6.dp)
            .padding(18.dp)
            .clickable(onClick = { onMessageSent(textFieldValue.text) })

    )
}