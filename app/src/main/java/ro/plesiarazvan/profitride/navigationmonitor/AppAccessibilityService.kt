package ro.plesiarazvan.profitride.navigationmonitor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import ro.plesiarazvan.profitride.overlay.OverlayService

class AppAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()?.lowercase() ?: return
        val prefs = getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("serviceEnabled", false)
        if (!serviceEnabled) return

        val hideInWaze = prefs.getBoolean("waze", true)
        val boltEnabled = prefs.getBoolean("bolt", true)
        val uberEnabled = prefs.getBoolean("uber", true)
        val reappear = prefs.getBoolean("reappear", true)

        when {
            hideInWaze && pkg.contains("waze") ->
                startOverlayAction(OverlayService.ACTION_HIDE)

            reappear && boltEnabled && (pkg.contains("mtakso") || pkg.contains("taxify") || pkg.contains("bolt")) ->
                startOverlayAction(OverlayService.ACTION_SHOW)

            reappear && uberEnabled && pkg.contains("uber") ->
                startOverlayAction(OverlayService.ACTION_SHOW)
        }
    }

    private fun startOverlayAction(action: String) {
        val i = Intent(this, OverlayService::class.java).setAction(action)
        try { startService(i) } catch (_: Exception) {}
    }

    override fun onInterrupt() {}
}
