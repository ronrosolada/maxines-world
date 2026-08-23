package com.maxinesworld.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.maxinesworld.app.di.MediaLibraryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import com.maxinesworld.app.ui.theme.Baloo2
import com.maxinesworld.app.ui.theme.Nunito
import com.maxinesworld.coredesignsystem.theme.MaxinesWorldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Warm the singleton catalog while the child is on onboarding/home. A
        // later subject tap then reads parsed metadata from memory immediately.
        val mediaLibrary = EntryPointAccessors
            .fromApplication(applicationContext, MediaLibraryEntryPoint::class.java)
            .mediaLibrary()
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { mediaLibrary.getCatalog() }
        }
        enableEdgeToEdge()
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
