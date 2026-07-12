package com.maxinesworld.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.maxinesworld.app.ui.theme.Baloo2
import com.maxinesworld.app.ui.theme.Nunito
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaxinesWorldTheme(
                displayFont = Baloo2,
                bodyFont = Nunito
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    MaxinesNavGraph(navController)
                }
            }
        }
    }
}
