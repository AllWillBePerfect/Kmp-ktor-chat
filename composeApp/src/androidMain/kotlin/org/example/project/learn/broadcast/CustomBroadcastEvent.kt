package org.example.project.learn.broadcast

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

sealed class CustomBroadcastEvent(
    val uid: String = UUID.randomUUID().toString()
) : Parcelable {

    @Parcelize
    data object FirstEvent : CustomBroadcastEvent()

    @Parcelize
    data class SecondEvent(val someId: String) : CustomBroadcastEvent()
}