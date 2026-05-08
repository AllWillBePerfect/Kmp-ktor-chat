package org.example.project

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.learn.broadcast.CustomBroadcastEvent
import org.example.project.learn.broadcast.sendCustomBroadcastEvent
import org.example.project.ui.App

class MainActivity : ComponentActivity() {

    private lateinit var screenReceiver: ScreenReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")

        setContent {
            App()
        }

    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
//        screenReceiver = ScreenReceiver()
//        val filter = IntentFilter().apply {
//            addAction(Intent.ACTION_SCREEN_ON)
//            addAction(Intent.ACTION_SCREEN_OFF)
//        }
//        this.registerReceiver(screenReceiver, filter)
        sendCustomBroadcastEvent(this, CustomBroadcastEvent.FirstEvent)

    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")

    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")

    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop")
//        unregisterReceiver(screenReceiver)
        sendCustomBroadcastEvent(this, CustomBroadcastEvent.SecondEvent("hello"))
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("MainActivity", "onRestart")

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")

    }


}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra("level", -1)
        Log.d("Battery", "Level: $level")
    }
}

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> Log.d("Screen", "ON")
            Intent.ACTION_SCREEN_OFF -> Log.d("Screen", "OFF")
        }
    }
}