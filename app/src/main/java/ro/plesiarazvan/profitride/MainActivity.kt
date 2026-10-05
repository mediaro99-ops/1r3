package ro.plesiarazvan.profitride

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import ro.plesiarazvan.profitride.feature.home.ProfitRideApp
import ro.plesiarazvan.profitride.overlay.OverlayService
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

    fun isProfitRideAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, ro.plesiarazvan.profitride.navigationmonitor.AppAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component == expected) return true
        }
        return false
    }

    /**
     * Returns true only when ProfitRide really started.
     * If a permission is missing, it opens the exact Android settings screen and returns false.
     */
    fun startProfitRide(boltEnabled: Boolean, uberEnabled: Boolean): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return false
        }
        if (!isProfitRideAccessibilityEnabled()) {
            openAccessibility()
            return false
        }

        ContextCompat.startForegroundService(this, Intent(this, MonitoringService::class.java))
        ContextCompat.startForegroundService(
            this,
            Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_SHOW)
        )

        getSharedPreferences("runtime_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("serviceEnabled", true)
            .apply()

        launchDriverApp(boltEnabled, uberEnabled)
        return true
    }

    fun stopProfitRide() {
        stopService(Intent(this, MonitoringService::class.java))
        stopService(Intent(this, OverlayService::class.java))
        getSharedPreferences("runtime_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("serviceEnabled", false)
            .apply()
    }

    private fun launchDriverApp(boltEnabled: Boolean, uberEnabled: Boolean) {
        val candidates = mutableListOf<String>()
        if (boltEnabled) candidates += listOf("ee.mtakso.driver", "com.taxify.driver")
        if (uberEnabled) candidates += "com.ubercab.driver"

        for (pkg in candidates) {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                return
            }
        }
        // No supported driver app was found. ProfitRide remains active;
        // the user can open Bolt/Uber manually.
    }
}
