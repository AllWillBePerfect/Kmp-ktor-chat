package org.example.project.ui.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.example.project.ui.utils.PreviewWrapper
import org.example.project.v2.core.models.SyncStatus

@Composable
fun MessageComponent(
    text: String,
    isUserMe: Boolean,
    isAboveMessageAuthorDifferent: Boolean,
    isBelowMessageAuthorDifferent: Boolean,
    readCount: Int,
    syncStatus: SyncStatus,
    createdAt: String?
) {
    val borderColor = if (isUserMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    val spaceBetweenAuthors =
        if (isAboveMessageAuthorDifferent) Modifier.padding(top = 8.dp) else Modifier

    Row(
        modifier = spaceBetweenAuthors.fillMaxWidth(),
        horizontalArrangement = if (isUserMe) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {

        if (isUserMe) {
            MessageBubble(
                text = text,
                isUserMe = true,
                isBelowMessageAuthorDifferent = isBelowMessageAuthorDifferent,
                readCount = readCount,
                syncStatus = syncStatus,
                createdAt = createdAt
            )

//            AvatarOrSpacer(
//                showAvatar = isBelowMessageAuthorDifferent,
//                borderColor = borderColor
//            )
            Spacer(Modifier.width(16.dp))
        } else {

            //todo вернуть
            /*AvatarOrSpacer(
                showAvatar = isBelowMessageAuthorDifferent,
                borderColor = borderColor
            )*/
            //todo убрать
            Spacer(Modifier.width(16.dp))

            MessageBubble(
                text = text,
                isUserMe = false,
                isBelowMessageAuthorDifferent = isBelowMessageAuthorDifferent,
                readCount = readCount,
                syncStatus = syncStatus,
                createdAt = createdAt
            )
        }
    }
}

@Composable
private fun RowScope.MessageBubble(
    text: String,
    isUserMe: Boolean,
    isBelowMessageAuthorDifferent: Boolean,
    readCount: Int,
    syncStatus: SyncStatus,
    createdAt: String?
) {
    MessageSpacing(
        modifier = Modifier
//            .padding(horizontal = 16.dp)
            then(
                if (isUserMe) Modifier.padding(start = 16.dp) else Modifier.padding(end = 16.dp)
            )
            .weight(1f, fill = false),
        text = text,
        isUserMe = isUserMe,
        isBelowMessageAuthorDifferent = isBelowMessageAuthorDifferent,
        readCount = readCount,
        syncStatus = syncStatus,
        createdAt = createdAt

    )
}

@Composable
private fun RowScope.AvatarOrSpacer(
    showAvatar: Boolean,
    borderColor: Color
) {
    if (showAvatar)
    {
        Image(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .align(Alignment.Bottom)
                .clickable(onClick = { })
                .padding(horizontal = 16.dp)
                .size(42.dp)
                .border(1.5.dp, borderColor, CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .clip(CircleShape)
            ,
            imageVector = Icons.Default.VerifiedUser,
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
    } else {
        Spacer(modifier = Modifier.width(74.dp))
    }
}

@Composable
private fun MessageSpacing(
    modifier: Modifier,
    text: String,
    isUserMe: Boolean,
    isBelowMessageAuthorDifferent: Boolean,
    readCount: Int,
    syncStatus: SyncStatus,
    createdAt: String?
) {
    Column(
        modifier = modifier
    ) {
        MessageItemComponent(
            text = text,
            isUserMe = isUserMe,
            readCount = readCount,
            syncStatus = syncStatus,
            createdAt = createdAt
        )
        if (isBelowMessageAuthorDifferent) {
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
@Preview
private fun PreviewNight() = PreviewWrapper {
   Column {
       MessageComponent(
           text = "Hello",
           isUserMe = true,
           isAboveMessageAuthorDifferent = false,
           isBelowMessageAuthorDifferent = true,
           readCount = 0,
           syncStatus = SyncStatus.SYNC_NEEDED,
           createdAt = ""
       )

       MessageComponent(
           text = "Hello",
           isUserMe = true,
           isAboveMessageAuthorDifferent = true,
           isBelowMessageAuthorDifferent = false,
           readCount = 0,
           syncStatus = SyncStatus.SYNC_NEEDED,
           createdAt = ""
       )
   }
}

@Composable
@Preview
private fun PreviewLight() = PreviewWrapper(
    isDarkTheme = false
) {
    MessageComponent(
        text = "Hello",
        isUserMe = true,
        isAboveMessageAuthorDifferent = true,
        isBelowMessageAuthorDifferent = true,
        readCount = 0,
        syncStatus = SyncStatus.SYNC_NEEDED,
        createdAt = ""
    )
}

