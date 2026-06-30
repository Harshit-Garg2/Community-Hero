package com.example

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.CommunityHeroApp
import com.example.ui.CommunityHeroViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MainActivity : ComponentActivity() {
  private val viewModel: CommunityHeroViewModel by viewModels()

  private val requestPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { _ ->
    // Permission result handling
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Firebase and App Check
    try {
      FirebaseApp.initializeApp(this)
      val appCheck = FirebaseAppCheck.getInstance()
      if (BuildConfig.DEBUG) {
        appCheck.installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance()
        )
        Log.d("MainActivity", "Firebase App Check initialized with Debug Provider.")
      } else {
        appCheck.installAppCheckProviderFactory(
          PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        Log.d("MainActivity", "Firebase App Check initialized with Play Integrity Provider.")
      }
    } catch (e: Exception) {
      Log.w("MainActivity", "Firebase App Check initialization failed: ${e.message}")
    }

    // Request notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          CommunityHeroApp(viewModel = viewModel)
        }
      }
    }
  }
}
