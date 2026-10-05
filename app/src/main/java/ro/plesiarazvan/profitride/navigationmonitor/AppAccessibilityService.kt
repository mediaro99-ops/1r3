package ro.plesiarazvan.profitride.navigationmonitor

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import ro.plesiarazvan.profitride.overlay.OverlayService

class AppAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()?.lowercase() ?: return
        when {
            pkg.contains("waze") -> startOverlayAction(OverlayService.ACTION_HIDE)
            pkg.contains("bolt") || pkg.contains("uber") -> startOverlayAction(OverlayService.ACTION_SHOW)
        }
    }
    private fun startOverlayAction(action:String) {
        val i = Intent(this, OverlayService::class.java).setAction(action)
        try { startService(i) } catch (_:Exception) {}
    }
    override fun onInterrupt() {}
}
