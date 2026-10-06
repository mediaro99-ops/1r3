package ro.plesiarazvan.profitride.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import ro.plesiarazvan.profitride.MainActivity
import ro.plesiarazvan.profitride.R
import ro.plesiarazvan.profitride.ui.theme.ProfitRideTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class OverlayService : LifecycleService() {

    companion object {
        const val ACTION_WAITING = "ro.plesiarazvan.profitride.WAITING_OVERLAY"
        const val ACTION_UPDATE = "ro.plesiarazvan.profitride.UPDATE_OVERLAY"
        const val ACTION_HIDE = "ro.plesiarazvan.profitride.HIDE_OVERLAY"
        const val ACTION_SHOW = "ro.plesiarazvan.profitride.SHOW_OVERLAY"
        const val ACTION_RESET = "ro.plesiarazvan.profitride.RESET_OVERLAY"
        const val ACTION_STOP = "ro.plesiarazvan.profitride.STOP_OVERLAY"
        const val ACTION_SAFE_BOUNDS = "ro.plesiarazvan.profitride.SAFE_BOUNDS"

        const val EXTRA_SAFE_TOP = "safe_top"
        const val EXTRA_SAFE_BOTTOM = "safe_bottom"

        const val EXTRA_PLATFORM = "platform"
        const val EXTRA_PAYMENT_TYPE = "payment_type"
        const val EXTRA_FARE = "fare"
        const val EXTRA_PICKUP_MIN = "pickup_min"
        const val EXTRA_PICKUP_KM = "pickup_km"
        const val EXTRA_TRIP_MIN = "trip_min"
        const val EXTRA_TRIP_KM = "trip_km"
        const val EXTRA_GROSS_KM = "gross_km"
        const val EXTRA_GROSS_HOUR = "gross_hour"
        const val EXTRA_PROFIT_KM = "profit_km"
        const val EXTRA_PROFIT_HOUR = "profit_hour"
        const val EXTRA_FUEL_COST = "fuel_cost"
        const val EXTRA_VEHICLE_COST = "vehicle_cost"
        const val EXTRA_RENT_COST = "rent_cost"
        const val EXTRA_OTHER_COST = "other_cost"
        const val EXTRA_TOTAL_COST = "total_cost"
        const val EXTRA_PROFIT = "profit"
        const val EXTRA_RECOMMENDATION = "recommendation"
        const val EXTRA_REASON = "reason"
        const val EXTRA_TIP = "tip"
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var overlayOwners: OverlayViewTreeOwner? = null

    private var overlayState by mutableStateOf(OverlayUiState())
    private var displayMode by mutableStateOf(OverlayDisplayMode.COMPACT)
    private var maxHeightDp by mutableFloatStateOf(420f)

    private var hiddenByNavigation = false
    private var safeTopPx = 0
    private var safeBottomPx = 0
    private var lastOfferKey = ""

    private val prefs by lazy {
        getSharedPreferences("overlay_pos", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val h = resources.displayMetrics.heightPixels
        safeTopPx = (h * 0.09f).toInt()
        safeBottomPx = (h * 0.56f).toInt()
        updateMaxHeight()

        createChannel()
        startForeground(
            2201,
            NotificationCompat.Builder(this, "profitride")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ProfitRide este activ")
                .setContentText("Așteaptă următoarea ofertă.")
                .setOngoing(true)
                .build()
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_WAITING -> {
                hiddenByNavigation = false
                ensureView()
                overlayState = OverlayUiState(mode = OverlayMode.WAITING)
                displayMode = OverlayDisplayMode.COMPACT
                applyModeLayout()
                composeView?.visibility = View.VISIBLE
            }

            ACTION_UPDATE -> {
                hiddenByNavigation = false
                ensureView()

                val next = intent.toUiState()
                val key = buildOfferKey(next)

                // Every NEW ride starts in COMPACT mode, as requested.
                if (key != lastOfferKey) {
                    lastOfferKey = key
                    displayMode = OverlayDisplayMode.COMPACT
                }

                overlayState = next
                applyModeLayout()
                composeView?.visibility = View.VISIBLE
            }

            ACTION_SAFE_BOUNDS -> {
                safeTopPx = intent.getIntExtra(EXTRA_SAFE_TOP, safeTopPx)
                safeBottomPx = intent.getIntExtra(EXTRA_SAFE_BOTTOM, safeBottomPx)
                updateMaxHeight()
                clampIntoSafeArea()
            }

            ACTION_HIDE -> {
                hiddenByNavigation = true
                overlayOwners?.pause()
                composeView?.visibility = View.GONE
            }

            ACTION_SHOW -> {
                hiddenByNavigation = false
                overlayOwners?.resume()
                composeView?.visibility = View.VISIBLE
                clampIntoSafeArea()
            }

            ACTION_RESET -> {
                ensureView()
                lastOfferKey = ""
                overlayState = OverlayUiState(mode = OverlayMode.WAITING)
                displayMode = OverlayDisplayMode.COMPACT
                applyModeLayout()
                if (!hiddenByNavigation) {
                    composeView?.visibility = View.VISIBLE
                }
            }

            ACTION_STOP -> {
                removeOverlay()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun ensureView() {
        if (composeView != null) return
        if (!Settings.canDrawOverlays(this)) return

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val margin = (12f * density).roundToInt()
        val maxWidth = (460f * density).roundToInt()

        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        layoutParams = WindowManager.LayoutParams(
            min(screenWidth - margin * 2, maxWidth),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", margin)
            y = prefs.getInt("y", safeTopPx).coerceAtLeast(safeTopPx)
        }

        val owners = OverlayViewTreeOwner().apply { resume() }
        overlayOwners = owners

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owners)
            setViewTreeSavedStateRegistryOwner(owners)
            setViewTreeViewModelStoreOwner(owners)

            setContent {
                ProfitRideTheme {
                    ProfitRideOverlay(
                        state = overlayState,
                        displayMode = displayMode,
                        maxHeightDp = maxHeightDp,
                        onMinimize = {
                            displayMode = OverlayDisplayMode.MINIMIZED
                            applyModeLayout()
                        },
                        onDetails = {
                            displayMode = OverlayDisplayMode.EXPANDED
                            applyModeLayout()
                        },
                        onHideDetails = {
                            displayMode = OverlayDisplayMode.COMPACT
                            applyModeLayout()
                        },
                        onRestoreCompact = {
                            displayMode = OverlayDisplayMode.COMPACT
                            applyModeLayout()
                        },
                        onSettings = {
                            startActivity(
                                Intent(
                                    this@OverlayService,
                                    MainActivity::class.java
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        onDrag = { dx, dy ->
                            moveBy(dx, dy)
                        }
                    )
                }
            }
        }

        windowManager.addView(composeView, layoutParams)
        clampIntoSafeArea()
    }

    private fun applyModeLayout() {
        val p = layoutParams ?: return
        val view = composeView ?: return
        val d = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels

        p.width = when (displayMode) {
            OverlayDisplayMode.MINIMIZED ->
                min((330f * d).roundToInt(), screenW - (24f * d).roundToInt())
            else ->
                min((460f * d).roundToInt(), screenW - (24f * d).roundToInt())
        }

        // Expanded is explicitly limited to the Accessibility-derived safe area.
        p.height = if (displayMode == OverlayDisplayMode.EXPANDED) {
            max(1, safeBottomPx - safeTopPx)
        } else {
            WindowManager.LayoutParams.WRAP_CONTENT
        }

        if (displayMode == OverlayDisplayMode.EXPANDED) {
            p.y = safeTopPx
        }

        try {
            windowManager.updateViewLayout(view, p)
        } catch (_: Exception) {
        }

        clampIntoSafeArea()
    }

    private fun updateMaxHeight() {
        val d = resources.displayMetrics.density
        maxHeightDp =
            ((safeBottomPx - safeTopPx).coerceAtLeast((96f * d).toInt())) / d
    }

    private fun clampIntoSafeArea() {
        val p = layoutParams ?: return
        val view = composeView ?: return

        val screenW = resources.displayMetrics.widthPixels
        val viewW = if (view.width > 0) view.width else p.width
        val viewH = if (view.height > 0) view.height else 0

        p.x = p.x.coerceIn(0, max(0, screenW - viewW))

        val maxY = if (viewH > 0) {
            max(safeTopPx, safeBottomPx - viewH)
        } else {
            safeBottomPx
        }
        p.y = p.y.coerceIn(safeTopPx, maxY)

        try {
            windowManager.updateViewLayout(view, p)
        } catch (_: Exception) {
        }
    }

    private fun moveBy(dx: Float, dy: Float) {
        val p = layoutParams ?: return
        val view = composeView ?: return

        p.x += dx.roundToInt()
        p.y += dy.roundToInt()

        val screenW = resources.displayMetrics.widthPixels
        val viewW = if (view.width > 0) view.width else p.width
        val viewH = if (view.height > 0) view.height else 0

        p.x = p.x.coerceIn(0, max(0, screenW - viewW))

        val maxY = if (viewH > 0) {
            max(safeTopPx, safeBottomPx - viewH)
        } else safeBottomPx

        p.y = p.y.coerceIn(safeTopPx, maxY)

        windowManager.updateViewLayout(view, p)

        prefs.edit()
            .putInt("x", p.x)
            .putInt("y", p.y)
            .apply()
    }

    private fun buildOfferKey(s: OverlayUiState): String =
        "${s.platform}|${s.fare}|${s.pickupMinutes}|${s.pickupKm}|${s.tripMinutes}|${s.tripKm}"

    private fun Intent.toUiState(): OverlayUiState =
        OverlayUiState(
            mode = OverlayMode.OFFER_READY,
            platform = getStringExtra(EXTRA_PLATFORM).orEmpty(),
            paymentType = getStringExtra(EXTRA_PAYMENT_TYPE).orEmpty(),
            fare = getDoubleExtra(EXTRA_FARE, 0.0),
            pickupMinutes = getIntExtra(EXTRA_PICKUP_MIN, 0),
            pickupKm = getDoubleExtra(EXTRA_PICKUP_KM, 0.0),
            tripMinutes = getIntExtra(EXTRA_TRIP_MIN, 0),
            tripKm = getDoubleExtra(EXTRA_TRIP_KM, 0.0),
            grossPerKm = getDoubleExtra(EXTRA_GROSS_KM, 0.0),
            grossPerHour = getDoubleExtra(EXTRA_GROSS_HOUR, 0.0),
            profitPerKm = getDoubleExtra(EXTRA_PROFIT_KM, 0.0),
            profitPerHour = getDoubleExtra(EXTRA_PROFIT_HOUR, 0.0),
            fuelCost = getDoubleExtra(EXTRA_FUEL_COST, 0.0),
            maintenanceCost = getDoubleExtra(EXTRA_VEHICLE_COST, 0.0),
            rentRateCost = getDoubleExtra(EXTRA_RENT_COST, 0.0),
            otherCost = getDoubleExtra(EXTRA_OTHER_COST, 0.0),
            totalCost = getDoubleExtra(EXTRA_TOTAL_COST, 0.0),
            netProfit = getDoubleExtra(EXTRA_PROFIT, 0.0),
            recommendation = getStringExtra(EXTRA_RECOMMENDATION).orEmpty(),
            tip = getStringExtra(EXTRA_TIP).orEmpty()
        )

    private fun removeOverlay() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        composeView = null
        overlayOwners?.destroy()
        overlayOwners = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        "profitride",
                        "ProfitRide",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }
}
