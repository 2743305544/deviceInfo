package com.shiyi.deviceinfo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shiyi.deviceinfo.ui.screens.HomeScreen
import com.shiyi.deviceinfo.ui.theme.DeviceInfoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeviceInfoTheme {
                HomeScreen()
            }
        }
    }
}