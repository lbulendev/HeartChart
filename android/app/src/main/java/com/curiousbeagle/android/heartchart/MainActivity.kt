package com.curiousbeagle.android.heartchart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.curiousbeagle.android.heartchart.ui.MainScreen
import com.curiousbeagle.android.heartchart.ui.theme.HeartChartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HeartChartApplication
        setContent {
            HeartChartTheme {
                MainScreen(monitor = app.monitor, settings = app.settings)
            }
        }
    }
}
