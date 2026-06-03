package org.example.project.v2.ui.components.channels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.example.project.v2.core.models.Message
import org.example.project.v2.core.models.SyncStatus

@Composable
fun MessageReadStatusIcon(
    message: Message,
    isMessageRead: Boolean,
    modifier: Modifier = Modifier,
) {
    when (message.syncStatus) {
        SyncStatus.IN_PROGRESS,
        SyncStatus.SYNC_NEEDED,
        -> Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = "Pending",
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SyncStatus.COMPLETED -> {
            if (isMessageRead) {
                Icon(
                    imageVector = Icons.Filled.DoneAll,
                    contentDescription = "Read",
                    modifier = modifier,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Sent",
                    modifier = modifier,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
