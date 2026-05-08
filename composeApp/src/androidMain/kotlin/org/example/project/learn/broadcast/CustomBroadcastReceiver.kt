package org.example.project.learn.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.util.Log

const val ACTION_CUSTOM_EVENT = "com.example.MY_ACTION"
const val EXTRA_EVENT = "extra_event"

class CustomBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = intent.getParcelableCompat<CustomBroadcastEvent>(EXTRA_EVENT) ?: return

        when (event) {
            CustomBroadcastEvent.FirstEvent -> {
                Log.d("CustomBroadcastReceiver", "FirstEvent: ${event.uid}")
            }

            is CustomBroadcastEvent.SecondEvent -> {
                Log.d("CustomBroadcastReceiver", "SecondEvent: ${event.uid}")
            }
        }

    }
}


inline fun <reified T : Parcelable> Intent.getParcelableCompat(key: String): T? {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}


fun sendCustomBroadcastEvent(context: Context, event: CustomBroadcastEvent) {
    val intent = Intent(ACTION_CUSTOM_EVENT).apply {
        putExtra(EXTRA_EVENT, event)
        `package` = context.packageName
    }
    context.sendBroadcast(intent)
}