package com.forestglass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                lock = { try { startLockTask() } catch (_: Exception) {} },
                unlock = { try { stopLockTask() } catch (_: Exception) {} }
            )
        }
    }
}
