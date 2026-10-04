package ro.plesiarazvan.profitride

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import kotlin.math.roundToInt

class CaptureOverlayService : Service(), TextToSpeech.OnInitListener {
    companion object {
        const val ACTION_START = "profitride.START"
        const val ACTION_STOP = "profitride.STOP"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
        private const val CHANNEL_ID = "profitride_capture"
        private const val NOTIFICATION_ID = 3110
    }

    private lateinit var wm: WindowManager
    private var overlay: LinearLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val handler = Handler(Looper.getMainLooper())
    private var lastCaptureAt = 0L
    private var processing = false
    private var lastSpokenAmount = -1.0
    private lateinit var tts: TextToSpeech

    private lateinit var amountView: TextView
    private lateinit var pickupView: TextView
    private lateinit var rideView: TextView
    private lateinit var totalView: TextView
    private lateinit var kmView: TextView
    private lateinit var hourView: TextView
    private lateinit var profitView: TextView
    private lateinit var statusView: TextView

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        tts = TextToSpeech(this, this)
        createNotificationChannel()
        AppState.overlayService = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                ensureOverlay()
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (code == Activity.RESULT_OK && data != null && projection == null) {
                    startProjection(code, data)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "ProfitRide analiză", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_profitride)
            .setContentTitle("ProfitRide activ")
            .setContentText("Analizează automat ofertele Bolt/Uber")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    private fun startProjection(code: Int, data: Intent) {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        projection?.createVirtualDisplay(
            "ProfitRideCapture", width, height, density,
            0, reader!!.surface, null, handler
        )
        reader?.setOnImageAvailableListener({ r ->
            val now = System.currentTimeMillis()
            if (processing || now - lastCaptureAt < 850) {
                r.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            lastCaptureAt = now
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            processing = true
            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                bitmap.recycle()
                processBitmap(cropped)
            } catch (_: Exception) {
                image.close()
                processing = false
            }
        }, handler)
    }

    private fun processBitmap(bitmap: Bitmap) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                bitmap.recycle()
                val offer = BoltOfferParser.parse(result.text)
                if (offer != null) {
                    // A new offer means the previous ride is over and analysis can resume.
                    AppState.pausedForRide = false
                    setOverlayVisible(true)
                    showOffer(offer)
                }
            }
            .addOnCompleteListener { processing = false }
    }

    private fun ensureOverlay() {
        if (overlay != null) return
        val store = SettingsStore(this)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded("#091318", "#22E77A", 18f, 1)
            elevation = dp(10).toFloat()
        }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_profitride); layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)) }
        val title = TextView(this).apply { text = "ProfitRide"; setTextColor(Color.WHITE); textSize = 16f; setTypeface(typeface, Typeface.BOLD); setPadding(dp(6),0,0,0) }
        statusView = TextView(this).apply { text = "LIVE"; setTextColor(Color.rgb(34,231,122)); textSize = 11f; gravity = Gravity.END }
        header.addView(logo); header.addView(title, LinearLayout.LayoutParams(0, dp(32), 1f)); header.addView(statusView)
        card.addView(header)

        amountView = text("0,00 lei", 30f, Color.rgb(34,231,122), true)
        card.addView(amountView)

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pickupView = miniBox("0 min\n0,0 km\npână la client")
        rideView = miniBox("0 min\n0,0 km\ncursă")
        row1.addView(pickupView, LinearLayout.LayoutParams(0, dp(72), 1f).apply { marginEnd = dp(5) })
        row1.addView(rideView, LinearLayout.LayoutParams(0, dp(72), 1f))
        card.addView(row1)

        totalView = text("Total: 0 min • 0,0 km", 13f, Color.WHITE, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(6),0,dp(6)) }
        card.addView(totalView)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        kmView = metricBox("0,00\nlei/km")
        hourView = metricBox("0\nlei/oră")
        profitView = metricBox("0,00\nlei profit")
        row2.addView(kmView, LinearLayout.LayoutParams(0, dp(58), 1f).apply { marginEnd = dp(4) })
        row2.addView(hourView, LinearLayout.LayoutParams(0, dp(58), 1f).apply { marginEnd = dp(4) })
        row2.addView(profitView, LinearLayout.LayoutParams(0, dp(58), 1f))
        card.addView(row2)

        overlay = card
        val params = WindowManager.LayoutParams(
            dp(255), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = store.overlayX
            y = store.overlayY
        }
        overlayParams = params
        wm.addView(card, params)
        makeDraggable(card, params)
        resetOffer()
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var startX = 0; var startY = 0; var downX = 0f; var downY = 0f
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { startX = params.x; startY = params.y; downX = e.rawX; downY = e.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - downX).roundToInt()
                    params.y = startY + (e.rawY - downY).roundToInt()
                    wm.updateViewLayout(view, params); true
                }
                MotionEvent.ACTION_UP -> {
                    val s = SettingsStore(this)
                    s.overlayX = params.x; s.overlayY = params.y
                    true
                }
                else -> false
            }
        }
    }

    fun setOverlayVisible(visible: Boolean) {
        handler.post { overlay?.visibility = if (visible) View.VISIBLE else View.GONE }
    }

    fun pauseForRide() {
        AppState.pausedForRide = true
        // During an accepted ride ProfitRide disappears entirely. Waze therefore stays clean.
        setOverlayVisible(false)
    }

    fun resetOffer(show: Boolean = true) {
        handler.post {
            amountView.text = "0,00 lei"
            pickupView.text = "0 min\n0,0 km\npână la client"
            rideView.text = "0 min\n0,0 km\ncursă"
            totalView.text = "Total: 0 min • 0,0 km"
            kmView.text = "0,00\nlei/km"
            hourView.text = "0\nlei/oră"
            profitView.text = "0,00\nlei profit"
            statusView.text = "AȘTEAPTĂ"
            lastSpokenAmount = -1.0
            if (show && !AppState.pausedForRide) setOverlayVisible(true)
        }
    }

    private fun showOffer(o: OfferData) {
        val m = ProfitEngine.calculate(this, o)
        handler.post {
            amountView.text = String.format(Locale.US, "%.2f lei", o.amount).replace('.', ',')
            pickupView.text = String.format(Locale.US, "%.0f min\n%.1f km\npână la client", o.pickupMin, o.pickupKm).replace('.', ',')
            rideView.text = String.format(Locale.US, "%.0f min\n%.1f km\ncursă", o.rideMin, o.rideKm).replace('.', ',')
            totalView.text = String.format(Locale.US, "Total: %.0f min • %.1f km", o.totalMin, o.totalKm).replace('.', ',')
            kmView.text = String.format(Locale.US, "%.2f\nlei/km", m.netPerKm).replace('.', ',')
            hourView.text = String.format(Locale.US, "%.0f\nlei/oră", m.netPerHour)
            profitView.text = String.format(Locale.US, "%.2f\nlei profit", m.profit).replace('.', ',')
            statusView.text = if (m.worthIt) "MERITĂ" else "SUB PRAG"
            amountView.setTextColor(if (m.worthIt) Color.rgb(34,231,122) else Color.rgb(255,78,94))
            profitView.background = rounded(if (m.worthIt) "#0A5C36" else "#5A1820", if (m.worthIt) "#22E77A" else "#FF4E5E", 10f, 1)
            speakAmount(o.amount)
        }
    }

    private fun speakAmount(amount: Double) {
        val store = SettingsStore(this)
        if (!store.voiceEnabled || kotlin.math.abs(amount - lastSpokenAmount) < 0.01) return
        lastSpokenAmount = amount
        val whole = amount.toInt()
        val bani = ((amount - whole) * 100).roundToInt()
        val phrase = if (bani == 0) "$whole lei" else "$whole lei și $bani bani"
        tts.setSpeechRate(store.speechRate)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, store.voiceVolume) }
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, params, "offer_amount")
        if (store.vibrateOnOffer) {
            val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= 26) vib.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vib.vibrate(90)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ro", "RO")
            tts.setSpeechRate(1.0f)
        }
    }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun miniBox(t: String) = text(t, 12.5f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER
        background = rounded("#12344A", "#2E9EFF", 10f, 1)
    }

    private fun metricBox(t: String) = text(t, 12f, Color.WHITE, true).apply {
        gravity = Gravity.CENTER
        background = rounded("#17232B", "#314854", 10f, 1)
    }

    private fun rounded(fill: String, stroke: String, radius: Float, strokeWidth: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radius.toInt()).toFloat()
        setColor(Color.parseColor(fill))
        setStroke(dp(strokeWidth), Color.parseColor(stroke))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        AppState.overlayService = null
        try { overlay?.let { wm.removeView(it) } } catch (_: Exception) {}
        reader?.close(); projection?.stop(); recognizer.close(); tts.stop(); tts.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}
