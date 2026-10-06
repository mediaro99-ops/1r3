package ro.plesiarazvan.profitride

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.media.projection.MediaProjectionManager
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
import ro.plesiarazvan.profitride.service.ScreenCaptureService
import ro.plesiarazvan.profitride.ui.theme.ProfitRideTheme

class MainActivity : ComponentActivity() {
    private val notifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private var onCaptureStarted: (() -> Unit)? = null

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == Activity.RESULT_OK && data != null) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, MonitoringService::class.java)
                )

                val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                }
                ContextCompat.startForegroundService(this, captureIntent)

                getSharedPreferences("runtime_settings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("serviceEnabled", true)
                    .apply()

                onCaptureStarted?.invoke()
            } else {
                startService(
                    Intent(this, OverlayService::class.java)
                        .setAction(OverlayService.ACTION_RESET)
                )
            }
            onCaptureStarted = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { ProfitRideTheme { ProfitRideApp(this) } }
    }

    fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    fun openAccessibility() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun isProfitRideAccessibilityEnabled(): Boolean {
        val expected = ComponentName(
            this,
            ro.plesiarazvan.profitride.navigationmonitor.AppAccessibilityService::class.java
        )
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (ComponentName.unflattenFromString(splitter.next()) == expected) return true
        }
        return false
    }

    fun startProfitRide(onStarted: () -> Unit) {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        // Arată imediat ProfitRide în modul "Așteaptă ofertă".
        ContextCompat.startForegroundService(
            this,
            Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_WAITING)
        )

        // Apoi cere permisiunea Android oficială de captură ecran.
        onCaptureStarted = onStarted
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        captureLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    fun stopProfitRide() {
        stopService(Intent(this, ScreenCaptureService::class.java))
        stopService(Intent(this, MonitoringService::class.java))
        stopService(Intent(this, OverlayService::class.java))

        getSharedPreferences("runtime_settings", MODE_PRIVATE)
            .edit()
            .putBoolean("serviceEnabled", false)
            .apply()
    }
}
