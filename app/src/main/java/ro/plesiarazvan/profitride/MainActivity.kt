package ro.plesiarazvan.profitride

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ro.plesiarazvan.profitride.feature.home.ProfitRideApp
import ro.plesiarazvan.profitride.service.MonitoringService
import ro.plesiarazvan.profitride.ui.theme.ProfitRideTheme

class MainActivity : ComponentActivity() {
    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { ProfitRideTheme { ProfitRideApp(this) } }
    }

    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
    }

    fun openAccessibility() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun startMonitoring() {
        ContextCompat.startForegroundService(this, Intent(this, MonitoringService::class.java))
    }

    fun stopMonitoring() {
        stopService(Intent(this, MonitoringService::class.java))
    }
}
