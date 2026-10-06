package ro.plesiarazvan.profitride.navigationmonitor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ro.plesiarazvan.profitride.overlay.OverlayService
import kotlin.math.max
import kotlin.math.min

class AppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()?.lowercase() ?: return
        val prefs = getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("serviceEnabled", false)) return

        val hideInWaze = prefs.getBoolean("waze", true)
        val boltEnabled = prefs.getBoolean("bolt", true)
        val uberEnabled = prefs.getBoolean("uber", true)
        val reappear = prefs.getBoolean("reappear", true)

        when {
            hideInWaze && pkg.contains("waze") -> {
                prefs.edit().putString("currentProvider", "Waze").apply()
                startOverlayAction(OverlayService.ACTION_HIDE)
            }

            boltEnabled &&
                (pkg.contains("mtakso") || pkg.contains("taxify") || pkg.contains("bolt")) -> {
                prefs.edit().putString("currentProvider", "Bolt").apply()
                if (reappear) startOverlayAction(OverlayService.ACTION_SHOW)
                updateSafeBounds()
            }

            uberEnabled && pkg.contains("uber") -> {
                prefs.edit().putString("currentProvider", "Uber").apply()
                if (reappear) startOverlayAction(OverlayService.ACTION_SHOW)
                updateSafeBounds()
            }
        }
    }

    /**
     * Finds the original driver's offer controls and tells ProfitRide which
     * vertical area is safe. The overlay may move/expand only inside this band.
     *
     * We intentionally use generic visible text so this keeps working across
     * minor Bolt/Uber UI changes.
     */
    private fun updateSafeBounds() {
        val root = rootInActiveWindow ?: return
        val all = ArrayList<AccessibilityNodeInfo>()
        collect(root, all)

        val reject = findFirst(all, listOf("refuz", "decline", "respinge"))
        val accept = findFirst(all, listOf("accept", "acceptă", "accepta"))
        val payment = findFirst(all, listOf("numerar", "cash", "card"))
        val fare = findFareNode(all)

        val dm = resources.displayMetrics
        val screenH = dm.heightPixels
        val margin = (8f * dm.density).toInt()

        val rejectRect = reject?.let(::boundsOf)
        val fareRect = fare?.let(::boundsOf)
        val paymentRect = payment?.let(::boundsOf)
        val acceptRect = accept?.let(::boundsOf)

        // Top: below the reject control if visible, otherwise below status/top UI.
        var safeTop = rejectRect?.bottom?.plus(margin)
            ?: (screenH * 0.09f).toInt()

        // Bottom: above the beginning of the original offer card.
        // Fare/payment are usually at the top of that card. Accept is only fallback.
        val candidates = listOfNotNull(
            fareRect?.top,
            paymentRect?.top,
            acceptRect?.top
        )
        var safeBottom = if (candidates.isNotEmpty()) {
            candidates.minOrNull()!! - margin
        } else {
            (screenH * 0.55f).toInt()
        }

        // Defensive limits: never return an unusable band.
        safeTop = safeTop.coerceIn(0, screenH - margin)
        safeBottom = safeBottom.coerceIn(safeTop + (96f * dm.density).toInt(), screenH)

        startService(
            Intent(this, OverlayService::class.java)
                .setAction(OverlayService.ACTION_SAFE_BOUNDS)
                .putExtra(OverlayService.EXTRA_SAFE_TOP, safeTop)
                .putExtra(OverlayService.EXTRA_SAFE_BOTTOM, safeBottom)
        )
    }

    private fun collect(
        node: AccessibilityNodeInfo,
        out: MutableList<AccessibilityNodeInfo>
    ) {
        out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collect(it, out) }
        }
    }

    private fun findFirst(
        nodes: List<AccessibilityNodeInfo>,
        needles: List<String>
    ): AccessibilityNodeInfo? {
        return nodes.firstOrNull { node ->
            val text = buildString {
                append(node.text ?: "")
                append(' ')
                append(node.contentDescription ?: "")
            }.lowercase()
            needles.any { text.contains(it) }
        }
    }

    private fun findFareNode(
        nodes: List<AccessibilityNodeInfo>
    ): AccessibilityNodeInfo? {
        val regex = Regex("""\b\d{1,4}[,.]\d{2}\s*(lei|ron)\b""", RegexOption.IGNORE_CASE)
        return nodes.firstOrNull { node ->
            val text = buildString {
                append(node.text ?: "")
                append(' ')
                append(node.contentDescription ?: "")
            }
            regex.containsMatchIn(text)
        }
    }

    private fun boundsOf(node: AccessibilityNodeInfo): Rect {
        val r = Rect()
        node.getBoundsInScreen(r)
        return r
    }

    private fun startOverlayAction(action: String) {
        try {
            startService(Intent(this, OverlayService::class.java).setAction(action))
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() {}
}
