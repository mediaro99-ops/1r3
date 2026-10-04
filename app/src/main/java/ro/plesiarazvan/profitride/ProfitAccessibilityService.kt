package ro.plesiarazvan.profitride

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class ProfitAccessibilityService : AccessibilityService() {
    private val driverPackages = setOf("ee.mtakso.driver", "com.taxify.driver", "com.ubercab.driver")
    private val wazePackages = setOf("com.waze")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString().orEmpty()
        AppState.foregroundPackage = pkg
        val store = SettingsStore(this)
        val text = buildString {
            event.text?.forEach { append(it).append(' ') }
            event.contentDescription?.let { append(it).append(' ') }
            event.source?.text?.let { append(it).append(' ') }
            event.source?.contentDescription?.let { append(it).append(' ') }
        }.lowercase()

        if (wazePackages.contains(pkg)) {
            if (store.hideInWaze) AppState.overlayService?.setOverlayVisible(false)
            return
        }

        if (driverPackages.contains(pkg)) {
            if (!AppState.pausedForRide && store.reappearOnDriverApp) {
                AppState.overlayService?.setOverlayVisible(true)
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val accepted = listOf("acceptă", "accepta", "accept", "accept ride", "acceptă cursa").any { text.contains(it) }
            val rejected = listOf("refuză", "refuza", "reject", "decline").any { text.contains(it) }

            if (accepted && store.pauseOnAccept) {
                AppState.pausedForRide = true
                AppState.overlayService?.pauseForRide()
            }
            if (rejected && store.resetOnReject) {
                AppState.pausedForRide = false
                AppState.overlayService?.resetOffer()
            }
        }

        // Detect finalization screens. We don't re-show yet; a new offer will do it automatically.
        if (text.contains("cursă finalizată") || text.contains("cursa finalizata") ||
            text.contains("ride completed") || text.contains("trip completed")) {
            AppState.pausedForRide = false
            AppState.overlayService?.resetOffer(show = false)
        }
    }

    override fun onInterrupt() = Unit
}
