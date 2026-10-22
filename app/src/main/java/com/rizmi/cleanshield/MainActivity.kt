package com.rizmi.cleanshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rizmi.cleanshield.ui.navigation.CleanShieldMainApp
import com.rizmi.cleanshield.ui.theme.CleanShieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanShieldTheme {
                CleanShieldMainApp()
            }
        }
    }
}