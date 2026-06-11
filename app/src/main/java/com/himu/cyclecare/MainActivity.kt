package com.himu.cyclecare

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.himu.cyclecare.ui.CycleCareApp
import com.himu.cyclecare.ui.theme.CycleCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CycleCareTheme { NotificationAwareApp() }
        }
    }
}

@Composable
private fun NotificationAwareApp() {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    CycleCareApp(onRequestNotifications = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) })
}
