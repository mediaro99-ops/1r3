package ro.plesiarazvan.profitride.overlay

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import ro.plesiarazvan.profitride.MainActivity
import ro.plesiarazvan.profitride.R
import kotlin.math.roundToInt

class OverlayService : Service() {

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

    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private var params: WindowManager.LayoutParams? = null
    private var hiddenByNavigation = false
    private var compact = false
    private val prefs by lazy { getSharedPreferences("overlay_pos", MODE_PRIVATE) }

    // References updated in-place. No overlay recreation = no flicker.
    private var fullContent: LinearLayout? = null
    private var waitingText: TextView? = null
    private var compactContent: LinearLayout? = null

    private var fareValue: TextView? = null
    private var profitValue: TextView? = null
    private var profitKmValue: TextView? = null
    private var timeValue: TextView? = null
    private var timeSub: TextView? = null
    private var distanceValue: TextView? = null
    private var distanceSub: TextView? = null
    private var grossHourValue: TextView? = null
    private var grossKmValue: TextView? = null

    private var fuelValue: TextView? = null
    private var vehicleValue: TextView? = null
    private var rentValue: TextView? = null
    private var otherValue: TextView? = null
    private var totalCostValue: TextView? = null

    private var evaluationValue: TextView? = null
    private var evaluationReason: TextView? = null
    private var tipValue: TextView? = null

    private var compactMain: TextView? = null
    private var compactSub: TextView? = null
    private var compactEval: TextView? = null

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
            ACTION_WAITING -> {
                hiddenByNavigation = false
                ensureView()
                setWaiting()
            }

            ACTION_UPDATE -> {
                hiddenByNavigation = false
                ensureView()
                updateOffer(intent)
            }

            ACTION_HIDE -> {
                hiddenByNavigation = true
                root?.visibility = View.GONE
            }

            ACTION_SHOW -> {
                hiddenByNavigation = false
                root?.visibility = View.VISIBLE
            }

            ACTION_RESET -> {
                ensureView()
                setWaiting()
            }

            ACTION_STOP -> {
                removeOverlay()
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun ensureView() {
        if (root != null) return
        if (!Settings.canDrawOverlays(this)) return

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val desired = (390 * density).toInt()
        val width = minOf(desired, screenWidth - (16 * density).toInt())

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(14), dp(12), dp(14), dp(12)
            )
            background = rounded(
                fill = "#0D151C",
                stroke = "#2B3943",
                radius = 20f
            )
            elevation = dp(6).toFloat()
        }

        container.addView(buildHeader())
        waitingText = tv(
            "Aștept cursă...",
            17f,
            "#AEB8C2",
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(20))
        }
        container.addView(waitingText)

        fullContent = buildFullContent().apply {
            visibility = View.GONE
        }
        container.addView(fullContent)

        compactContent = buildCompactContent().apply {
            visibility = View.GONE
        }
        container.addView(compactContent)

        val type =
            if (Build.VERSION.SDK_INT >= 26)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", dp(8))
            y = prefs.getInt("y", dp(46))
        }

        makeDraggable(container)

        root = container
        wm.addView(container, params)
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        row.addView(tv("R", 25f, "#35F58A", true))

        row.addView(
            tv("  Profit", 18f, "#FFFFFF", true)
        )
        row.addView(
            tv("Ride", 18f, "#35F58A", true)
        )

        row.addView(
            Space(this),
            LinearLayout.LayoutParams(0, 1, 1f)
        )

        row.addView(
            iconButton("⚙") {
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(i)
            }
        )

        row.addView(
            iconButton("−") {
                toggleCompact()
            }
        )

        return row
    }

    private fun buildFullContent(): LinearLayout {
        val full = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val firstRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val fareCard = infoCard("#111C24", "#24343E").apply {
            addView(label("Plată cursă"))
            fareValue = value("0,00 lei", 27f, "#FFFFFF")
            addView(fareValue)
            addView(sub("NET, taxe incluse"))
        }

        val profitCard = infoCard("#0F241A", "#1E5D3A").apply {
            addView(label("Profit estimat", "#5FEA9C"))
            profitValue = value("0,00 lei", 30f, "#5FEA9C")
            addView(profitValue)
            profitKmValue = sub("0,00 lei/km PROFIT", "#E8FFF1")
            addView(profitKmValue)
        }

        firstRow.addView(
            fareCard,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginEnd = dp(5) }
        )
        firstRow.addView(
            profitCard,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(5) }
        )
        full.addView(firstRow)

        full.addView(space(9))

        val metrics = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded("#0F1921", "#273640", 14f)
        }

        metrics.addView(metricCard("⏱", "Timp total").also {
            timeValue = it.first
            timeSub = it.second
        }.third)

        metrics.addView(metricCard("▰", "Distanță totală").also {
            distanceValue = it.first
            distanceSub = it.second
        }.third)

        metrics.addView(metricCard("◉", "Câștig/oră").also {
            grossHourValue = it.first
        }.third)

        metrics.addView(metricCard("⌖", "Plată/km").also {
            grossKmValue = it.first
        }.third)

        full.addView(metrics)
        full.addView(space(10))

        val costsEval = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }

        val costs = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(sectionTitle("Costuri estimate cursă"))

            costRow("⛽", "Combustibil").also {
                fuelValue = it.second
                addView(it.first)
            }

            costRow("🚗", "Uzură + întreținere").also {
                vehicleValue = it.second
                addView(it.first)
            }

            costRow("▤", "Chirie / Rată").also {
                rentValue = it.second
                addView(it.first)
            }

            costRow("⚙", "Alte costuri").also {
                otherValue = it.second
                addView(it.first)
            }

            addView(divider())

            costRow(
                "",
                "Total costuri cursă",
                bold = true
            ).also {
                totalCostValue = it.second
                addView(it.first)
            }
        }

        costsEval.addView(
            costs,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.45f
            ).apply { marginEnd = dp(8) }
        )

        val eval = infoCard("#111C24", "#2B3943").apply {
            addView(sectionTitle("Evaluare"))
            evaluationValue = value("● ANALIZEZ", 17f, "#FFCC5C")
            addView(evaluationValue)
            evaluationReason = sub(
                "Aștept date complete.",
                "#AEB8C2"
            )
            addView(evaluationReason)
        }

        costsEval.addView(
            eval,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, .85f
            )
        )

        full.addView(costsEval)
        full.addView(space(10))
        full.addView(divider())
        full.addView(space(7))

        val tipRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tipRow.addView(tv("💡", 16f, "#FFCC5C", false))
        tipValue = tv(
            "  Sfat: Aștept o ofertă completă.",
            12f,
            "#C1CAD2",
            false
        )
        tipRow.addView(
            tipValue,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        full.addView(tipRow)

        return full
    }

    private fun buildCompactContent(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(3))

            compactMain = tv(
                "Aștept cursă...",
                18f,
                "#FFFFFF",
                true
            )
            addView(compactMain)

            compactSub = tv(
                "",
                12f,
                "#AEB8C2",
                false
            )
            addView(compactSub)

            compactEval = tv(
                "",
                12f,
                "#FFCC5C",
                true
            )
            addView(compactEval)
        }
    }

    private fun updateOffer(i: Intent) {
        waitingText?.visibility = View.GONE

        if (compact) {
            fullContent?.visibility = View.GONE
            compactContent?.visibility = View.VISIBLE
        } else {
            fullContent?.visibility = View.VISIBLE
            compactContent?.visibility = View.GONE
        }

        val fare = i.getDoubleExtra(EXTRA_FARE, 0.0)
        val pickupMin = i.getIntExtra(EXTRA_PICKUP_MIN, 0)
        val pickupKm = i.getDoubleExtra(EXTRA_PICKUP_KM, 0.0)
        val tripMin = i.getIntExtra(EXTRA_TRIP_MIN, 0)
        val tripKm = i.getDoubleExtra(EXTRA_TRIP_KM, 0.0)

        val grossKm = i.getDoubleExtra(EXTRA_GROSS_KM, 0.0)
        val grossHour = i.getDoubleExtra(EXTRA_GROSS_HOUR, 0.0)
        val profitKm = i.getDoubleExtra(EXTRA_PROFIT_KM, 0.0)
        val profitHour = i.getDoubleExtra(EXTRA_PROFIT_HOUR, 0.0)

        val fuel = i.getDoubleExtra(EXTRA_FUEL_COST, 0.0)
        val vehicle = i.getDoubleExtra(EXTRA_VEHICLE_COST, 0.0)
        val rent = i.getDoubleExtra(EXTRA_RENT_COST, 0.0)
        val other = i.getDoubleExtra(EXTRA_OTHER_COST, 0.0)
        val totalCost = i.getDoubleExtra(EXTRA_TOTAL_COST, 0.0)
        val profit = i.getDoubleExtra(EXTRA_PROFIT, 0.0)

        val recommendation =
            i.getStringExtra(EXTRA_RECOMMENDATION) ?: "ACCEPTABIL"
        val reason =
            i.getStringExtra(EXTRA_REASON) ?: ""
        val tip =
            i.getStringExtra(EXTRA_TIP) ?: ""

        val totalMin = pickupMin + tripMin
        val totalKm = pickupKm + tripKm

        val recColor = recommendationColor(recommendation)
        val recLabel = when (recommendation) {
            "ACCEPTA" -> "● ACCEPTĂ"
            "ACCEPTABIL" -> "● ACCEPTABIL"
            "SLABA" -> "● SLABĂ"
            else -> "● RESPINGE"
        }

        fareValue?.text = "${fmt(fare)} lei"

        profitValue?.text = "${fmt(profit)} lei"
        profitValue?.setTextColor(Color.parseColor(recColor))

        profitKmValue?.text =
            "${fmt(profitKm)} lei/km PROFIT"

        timeValue?.text = "$totalMin min"
        timeSub?.text = "$pickupMin min + $tripMin min"

        distanceValue?.text = "${fmt(totalKm)} km"
        distanceSub?.text =
            "${fmt(pickupKm)} km + ${fmt(tripKm)} km"

        grossHourValue?.text =
            "${grossHour.roundToInt()} lei"
        grossKmValue?.text =
            "${fmt(grossKm)} lei"

        fuelValue?.text = "${fmt(fuel)} lei"
        vehicleValue?.text = "${fmt(vehicle)} lei"
        rentValue?.text = "${fmt(rent)} lei"
        otherValue?.text = "${fmt(other)} lei"
        totalCostValue?.text = "${fmt(totalCost)} lei"

        evaluationValue?.text = recLabel
        evaluationValue?.setTextColor(Color.parseColor(recColor))
        evaluationReason?.text = reason

        tipValue?.text = "  Sfat: $tip"

        compactMain?.text =
            "${fmt(fare)} lei  →  ${signed(profit)} lei profit"
        compactSub?.text =
            "${fmt(totalKm)} km • $totalMin min • ${fmt(profitKm)} lei profit/km"
        compactEval?.text = recLabel
        compactEval?.setTextColor(Color.parseColor(recColor))

        root?.visibility =
            if (hiddenByNavigation) View.GONE else View.VISIBLE
    }

    private fun setWaiting() {
        waitingText?.visibility = View.VISIBLE
        waitingText?.text = "ProfitRide activ • Aștept cursă..."
        fullContent?.visibility = View.GONE
        compactContent?.visibility = View.GONE
        root?.visibility =
            if (hiddenByNavigation) View.GONE else View.VISIBLE
    }

    private fun toggleCompact() {
        compact = !compact
        waitingText?.let {
            if (it.visibility == View.VISIBLE) return
        }

        fullContent?.visibility =
            if (compact) View.GONE else View.VISIBLE
        compactContent?.visibility =
            if (compact) View.VISIBLE else View.GONE

        params?.let { p ->
            p.width = if (compact) dp(300) else {
                val screenWidth = resources.displayMetrics.widthPixels
                minOf(dp(390), screenWidth - dp(16))
            }
            root?.let { wm.updateViewLayout(it, p) }
        }
    }

    private fun metricCard(
        icon: String,
        title: String
    ): Triple<TextView, TextView, LinearLayout> {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(8), dp(5), dp(8))
        }

        box.addView(tv("$icon  $title", 10f, "#B9C3CC", false))

        val main = tv("—", 16f, "#FFFFFF", true).apply {
            gravity = Gravity.CENTER
        }
        box.addView(main)

        val secondary = tv("", 10f, "#8F9BA6", false).apply {
            gravity = Gravity.CENTER
        }
        box.addView(secondary)

        box.layoutParams = LinearLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            1f
        )

        return Triple(main, secondary, box)
    }

    private fun costRow(
        icon: String,
        title: String,
        bold: Boolean = false
    ): Pair<View, TextView> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(3), 0, dp(3))
        }

        row.addView(tv(icon, 12f, "#CBD4DB", false))

        row.addView(
            tv(
                if (icon.isBlank()) title else "  $title",
                11f,
                if (bold) "#FFFFFF" else "#B8C2CB",
                bold
            ),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val value = tv(
            "0,00 lei",
            11f,
            "#FFFFFF",
            true
        )
        row.addView(value)

        return row to value
    }

    private fun infoCard(
        fill: String,
        stroke: String
    ) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(11), dp(9), dp(11), dp(9))
        background = rounded(fill, stroke, 15f)
    }

    private fun label(
        text: String,
        color: String = "#B9C3CC"
    ) = tv(text, 11f, color, false)

    private fun value(
        text: String,
        size: Float,
        color: String
    ) = tv(text, size, color, true)

    private fun sub(
        text: String,
        color: String = "#9BA7B1"
    ) = tv(text, 10f, color, false)

    private fun sectionTitle(text: String) =
        tv(text, 13f, "#FFFFFF", true)

    private fun divider() =
        View(this).apply {
            setBackgroundColor(Color.parseColor("#26343E"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }

    private fun space(h: Int) =
        Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, dp(h))
        }

    private fun iconButton(
        text: String,
        action: () -> Unit
    ) = TextView(this).apply {
        this.text = text
        textSize = 18f
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor("#D8E0E6"))
        setPadding(dp(8), dp(4), dp(8), dp(4))
        setOnClickListener { action() }
    }

    private fun tv(
        text: String,
        size: Float,
        color: String,
        bold: Boolean
    ) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(
        fill: String,
        stroke: String,
        radius: Float
    ) = android.graphics.drawable.GradientDrawable().apply {
        shape =
            android.graphics.drawable.GradientDrawable.RECTANGLE
        cornerRadius = radius * resources.displayMetrics.density
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor(stroke))
    }

    private fun recommendationColor(rec: String): String =
        when (rec) {
            "ACCEPTA" -> "#54E58E"
            "ACCEPTABIL" -> "#F2C45C"
            "SLABA" -> "#E9964A"
            else -> "#E26067"
        }

    private fun signed(v: Double): String =
        if (v >= 0) "+${fmt(v)}" else fmt(v)

    private fun fmt(v: Double): String =
        String.format(
            java.util.Locale.US,
            "%.2f",
            v
        ).replace('.', ',')

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).roundToInt()

    private fun makeDraggable(view: View) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false

        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = params?.x ?: 0
                    startY = params?.y ?: 0
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY

                    if (kotlin.math.abs(dx) > dp(4) ||
                        kotlin.math.abs(dy) > dp(4)
                    ) moved = true

                    params?.let { p ->
                        p.x = startX + dx.roundToInt()
                        p.y = startY + dy.roundToInt()
                        root?.let {
                            wm.updateViewLayout(it, p)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    params?.let {
                        prefs.edit()
                            .putInt("x", it.x)
                            .putInt("y", it.y)
                            .apply()
                    }
                    !moved
                }

                else -> false
            }
        }
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

    private fun removeOverlay() {
        root?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        root = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
