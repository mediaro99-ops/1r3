package ro.plesiarazvan.profitride.overlay

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import ro.plesiarazvan.profitride.R
import kotlin.math.roundToInt

class OverlayService : Service() {
    companion object {
        const val ACTION_UPDATE = "ro.plesiarazvan.profitride.UPDATE_OVERLAY"
        const val ACTION_HIDE = "ro.plesiarazvan.profitride.HIDE_OVERLAY"
        const val ACTION_SHOW = "ro.plesiarazvan.profitride.SHOW_OVERLAY"
        const val ACTION_RESET = "ro.plesiarazvan.profitride.RESET_OVERLAY"
        const val ACTION_STOP = "ro.plesiarazvan.profitride.STOP_OVERLAY"

        const val EXTRA_FARE = "fare"
        const val EXTRA_PICKUP_MIN = "pickup_min"
        const val EXTRA_PICKUP_KM = "pickup_km"
        const val EXTRA_TRIP_MIN = "trip_min"
        const val EXTRA_TRIP_KM = "trip_km"
        const val EXTRA_RON_KM = "ron_km"
        const val EXTRA_RON_HOUR = "ron_hour"
        const val EXTRA_PROFIT = "profit"
        const val EXTRA_GOOD = "good"
    }

    private lateinit var wm: WindowManager
    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var hiddenByNavigation = false
    private var lastOffer: Intent? = null
    private val prefs by lazy { getSharedPreferences("overlay_pos", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> {
                lastOffer = Intent(intent)
                if (!hiddenByNavigation) showOffer(intent)
            }
            ACTION_HIDE -> {
                hiddenByNavigation = true
                view?.visibility = View.GONE
            }
            ACTION_SHOW -> {
                hiddenByNavigation = false
                view?.visibility = View.VISIBLE
            }
            ACTION_RESET -> {
                lastOffer = null
                remove()
            }
            ACTION_STOP -> {
                remove()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showOffer(data: Intent) {
        if (!Settings.canDrawOverlays(this)) return

        val fare = data.getDoubleExtra(EXTRA_FARE, 0.0)
        val pickupMin = data.getIntExtra(EXTRA_PICKUP_MIN, 0)
        val pickupKm = data.getDoubleExtra(EXTRA_PICKUP_KM, 0.0)
        val tripMin = data.getIntExtra(EXTRA_TRIP_MIN, 0)
        val tripKm = data.getDoubleExtra(EXTRA_TRIP_KM, 0.0)
        val ronKm = data.getDoubleExtra(EXTRA_RON_KM, 0.0)
        val ronHour = data.getDoubleExtra(EXTRA_RON_HOUR, 0.0)
        val profit = data.getDoubleExtra(EXTRA_PROFIT, 0.0)
        val good = data.getBooleanExtra(EXTRA_GOOD, false)

        remove()
        val density = resources.displayMetrics.density
        val accent = if (good) "#35F58A" else "#FF5A63"

        fun tv(text: String, size: Float, color: String = "#FFFFFF", bold: Boolean = false) =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.parseColor(color))
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14*density).toInt(), (11*density).toInt(), (14*density).toInt(), (11*density).toInt())
            background = roundedBg("#091117", accent, 18f)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(tv("R", 21f, "#35F58A", true))
        header.addView(tv("  ProfitRide", 15f, "#FFFFFF", true))
        header.addView(Space(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
        box.addView(header)

        box.addView(tv("${fmt(fare)} lei", 30f, accent, true).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        val segmentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        segmentRow.addView(
            segment("${pickupMin} min", "${fmt(pickupKm)} km", "până la client"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (4*density).toInt()
            }
        )
        segmentRow.addView(
            segment("${tripMin} min", "${fmt(tripKm)} km", "cursă"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (4*density).toInt()
            }
        )
        box.addView(segmentRow)

        val totalMin = pickupMin + tripMin
        val totalKm = pickupKm + tripKm
        box.addView(tv("Total: $totalMin min • ${fmt(totalKm)} km", 13f, "#FFFFFF", true).apply {
            gravity = Gravity.CENTER
            setPadding(0, (7*density).toInt(), 0, (5*density).toInt())
        })

        val metrics = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        metrics.addView(metric(fmt(ronKm), "lei/km", false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        metrics.addView(metric(ronHour.roundToInt().toString(), "lei/oră", false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins((4*density).toInt(), 0, (4*density).toInt(), 0)
        })
        metrics.addView(metric(fmt(profit), "lei profit", good), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        box.addView(metrics)

        box.addView(tv("🔊  Voce: ${fmt(fare)} lei", 11f, "#DCE7EC").apply {
            setPadding(0, (7*density).toInt(), 0, 0)
        })

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            (326*density).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 20)
            y = prefs.getInt("y", 110)
        }

        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        box.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = params!!.x
                    startY = params!!.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params!!.x = startX + (e.rawX - downX).roundToInt()
                    params!!.y = startY + (e.rawY - downY).roundToInt()
                    wm.updateViewLayout(box, params!!)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit().putInt("x", params!!.x).putInt("y", params!!.y).apply()
                    true
                }
                else -> false
            }
        }

        view = box
        wm.addView(box, params)
    }

    private fun segment(a: String, b: String, c: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val d = resources.displayMetrics.density
        setPadding((7*d).toInt(), (5*d).toInt(), (7*d).toInt(), (5*d).toInt())
        background = roundedBg("#102434", "#2A95E8", 11f)
        addView(simpleTv(a, 15f, "#FFFFFF", true))
        addView(simpleTv(b, 14f, "#FFFFFF", true))
        addView(simpleTv(c, 10f, "#C8D8E6", false))
    }

    private fun metric(value: String, label: String, positive: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val d = resources.displayMetrics.density
        setPadding((6*d).toInt(), (5*d).toInt(), (6*d).toInt(), (5*d).toInt())
        background = roundedBg(
            if (positive) "#0D5A35" else "#15212A",
            if (positive) "#35F58A" else "#273A45",
            9f
        )
        addView(simpleTv(value, 15f, if (positive) "#35F58A" else "#FFFFFF", true))
        addView(simpleTv(label, 9f, "#FFFFFF", false))
    }

    private fun simpleTv(text: String, size: Float, color: String, bold: Boolean) =
        TextView(this).apply {
            this.text = text
            textSize = size
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(color))
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun fmt(v: Double): String =
        String.format(java.util.Locale.US, "%.2f", v).replace('.', ',')

    private fun roundedBg(fill: String, stroke: String, radius: Float) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radius * resources.displayMetrics.density
            setColor(Color.parseColor(fill))
            setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor(stroke))
        }

    private fun remove() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel("profitride", "ProfitRide", NotificationManager.IMPORTANCE_LOW)
                )
        }
    }

    override fun onDestroy() {
        remove()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
