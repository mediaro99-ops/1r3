package ro.plesiarazvan.profitride.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import ro.plesiarazvan.profitride.MainActivity
import ro.plesiarazvan.profitride.R
import ro.plesiarazvan.profitride.ui.theme.ProfitRideTheme
import kotlin.math.roundToInt

class OverlayService : LifecycleService() {

    companion object {
        const val ACTION_WAITING = "ro.plesiarazvan.profitride.WAITING_OVERLAY"
        const val ACTION_UPDATE = "ro.plesiarazvan.profitride.UPDATE_OVERLAY"
        const val ACTION_HIDE = "ro.plesiarazvan.profitride.HIDE_OVERLAY"
        const val ACTION_SHOW = "ro.plesiarazvan.profitride.SHOW_OVERLAY"
        const val ACTION_RESET = "ro.plesiarazvan.profitride.RESET_OVERLAY"
        const val ACTION_STOP = "ro.plesiarazvan.profitride.STOP_OVERLAY"

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

    private var overlayState by mutableStateOf(OverlayUiState())
    private var compact by mutableStateOf(false)
    private var hiddenByNavigation = false

    private val prefs by lazy {
        getSharedPreferences("overlay_pos", MODE_PRIVATE)
    }

    override fun onCreate() {
        super.onCreate()

        windowManager =
            getSystemService(WINDOW_SERVICE) as WindowManager

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
                overlayState = OverlayUiState(
                    mode = OverlayMode.WAITING
                )
                composeView?.visibility =
                    android.view.View.VISIBLE
            }

            ACTION_UPDATE -> {
                hiddenByNavigation = false
                ensureView()
                overlayState = intent.toUiState()
                composeView?.visibility =
                    android.view.View.VISIBLE
            }

            ACTION_HIDE -> {
                hiddenByNavigation = true
                composeView?.visibility =
                    android.view.View.GONE
            }

            ACTION_SHOW -> {
                hiddenByNavigation = false
                composeView?.visibility =
                    android.view.View.VISIBLE
            }

            ACTION_RESET -> {
                ensureView()
                overlayState = OverlayUiState(
                    mode = OverlayMode.WAITING
                )
                if (!hiddenByNavigation) {
                    composeView?.visibility =
                        android.view.View.VISIBLE
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

        val horizontalMarginPx =
            (12f * density).roundToInt()

        val maxWidthPx =
            (460f * density).roundToInt()

        val targetWidth =
            minOf(
                screenWidth - horizontalMarginPx * 2,
                maxWidthPx
            )

        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

        layoutParams = WindowManager.LayoutParams(
            targetWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_SECURE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", horizontalMarginPx)
            y = prefs.getInt(
                "y",
                (52f * density).roundToInt()
            )
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)

            setContent {
                ProfitRideTheme {
                    ProfitRideOverlay(
                        state = overlayState,
                        compact = compact,
                        onToggleCompact = {
                            applyCompactMode(!compact)
                        },
                        onSettings = {
                            val open = Intent(
                                this@OverlayService,
                                MainActivity::class.java
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(open)
                        },
                        onDrag = { dx, dy ->
                            moveBy(dx, dy)
                        }
                    )
                }
            }
        }

        windowManager.addView(
            composeView,
            layoutParams
        )
    }

    private fun applyCompactMode(value: Boolean) {
        compact = value

        val p = layoutParams ?: return
        val view = composeView ?: return

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels

        p.width = if (compact) {
            minOf(
                (330f * density).roundToInt(),
                screenWidth - (24f * density).roundToInt()
            )
        } else {
            minOf(
                (460f * density).roundToInt(),
                screenWidth - (24f * density).roundToInt()
            )
        }

        windowManager.updateViewLayout(view, p)
    }

    private fun moveBy(
        dx: Float,
        dy: Float
    ) {
        val p = layoutParams ?: return
        val view = composeView ?: return

        p.x += dx.roundToInt()
        p.y += dy.roundToInt()

        windowManager.updateViewLayout(view, p)

        prefs.edit()
            .putInt("x", p.x)
            .putInt("y", p.y)
            .apply()
    }

    private fun Intent.toUiState(): OverlayUiState {
        return OverlayUiState(
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
    }

    private fun removeOverlay() {
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
            }
        }
        composeView = null
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
