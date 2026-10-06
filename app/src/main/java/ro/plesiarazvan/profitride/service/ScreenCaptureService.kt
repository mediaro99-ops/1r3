package ro.plesiarazvan.profitride.service

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import ro.plesiarazvan.profitride.R
import ro.plesiarazvan.profitride.overlay.OverlayService
import ro.plesiarazvan.profitride.tts.TtsManager
import kotlin.math.max

class ScreenCaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_RESULT_DATA = "resultData"
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    private val tts by lazy { TtsManager(this) }

    private var lastFrameAt = 0L
    private var lastOfferAt = 0L
    private var lastSignature = ""

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            2301,
            NotificationCompat.Builder(this, "screen_capture")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ProfitRide analizează ecranul")
                .setContentText("Așteaptă o ofertă Bolt/Uber.")
                .setOngoing(true)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED

        @Suppress("DEPRECATION")
        val data: Intent? =
            if (Build.VERSION.SDK_INT >= 33) {
                intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
            } else {
                intent?.getParcelableExtra(EXTRA_RESULT_DATA)
            }

        if (resultCode != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startProjection(resultCode, data)
        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val manager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = manager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        handlerThread = HandlerThread("ProfitRideCapture").also { it.start() }
        handler = Handler(handlerThread!!.looper)

        mediaProjection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            },
            handler
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ProfitRideScreen",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val now = System.currentTimeMillis()
            val image = try { reader.acquireLatestImage() } catch (_: Exception) { null }
            if (image == null) return@setOnImageAvailableListener

            if (now - lastFrameAt < 900L) {
                image.close()
                return@setOnImageAvailableListener
            }
            lastFrameAt = now

            val bitmap = try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val paddedWidth = width + rowPadding / pixelStride
                val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
                padded.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
                if (cropped !== padded) padded.recycle()
                cropped
            } catch (_: Exception) {
                null
            } finally {
                image.close()
            }

            if (bitmap != null) analyze(bitmap)
        }, handler)
    }

    private fun analyze(bitmap: Bitmap) {
        // IMPORTANT:
        // Nu analizăm tot ecranul. Overlay-ul ProfitRide stă sus și ar putea fi
        // recitit de OCR, creând valori false. Oferta Bolt/Uber este în mod normal
        // în jumătatea de jos, așa că analizăm doar zona relevantă.
        val top = (bitmap.height * 0.43f).toInt().coerceIn(0, bitmap.height - 1)
        val bottom = (bitmap.height * 0.96f).toInt().coerceIn(top + 1, bitmap.height)
        val roiHeight = bottom - top

        val roi = try {
            Bitmap.createBitmap(bitmap, 0, top, bitmap.width, roiHeight)
        } catch (_: Exception) {
            bitmap
        }

        recognizer.process(InputImage.fromBitmap(roi, 0))
            .addOnSuccessListener { handleText(it.text) }
            .addOnCompleteListener {
                if (roi !== bitmap) roi.recycle()
                bitmap.recycle()
            }
    }

    private fun handleText(raw: String) {
        val normalized = raw
            .replace('\n', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        // Suma: în oferta Bolt apare ca "9,82 lei".
        val fareCandidates = Regex(
            """(?<!\d)(\d{1,3}[.,]\d{2})\s*(?:lei|RON)\b""",
            RegexOption.IGNORE_CASE
        ).findAll(normalized)
            .mapNotNull { parseDecimal(it.groupValues[1]) }
            .filter { it in 2.0..500.0 }
            .toList()

        // Segmentele valide trebuie să fie exact de forma:
        // 6 min • 2.8 km
        // 7 min • 3.5 km
        val segmentCandidates = Regex(
            """(?<!\d)(\d{1,2})\s*min\s*[•·\-]?\s*(\d{1,2}(?:[.,]\d{1,2})?)\s*km\b""",
            RegexOption.IGNORE_CASE
        ).findAll(normalized)
            .mapNotNull { m ->
                val minutes = m.groupValues[1].toIntOrNull()
                val km = parseDecimal(m.groupValues[2])
                if (minutes != null && km != null &&
                    minutes in 1..60 && km > 0.05 && km <= 80.0
                ) Pair(minutes, km) else null
            }
            .toList()

        val now = System.currentTimeMillis()

        if (fareCandidates.isEmpty() || segmentCandidates.size < 2) {
            if (lastOfferAt > 0 && now - lastOfferAt > 2200L) {
                sendOverlayReset()
                lastOfferAt = 0L
                lastSignature = ""
            }
            return
        }

        // În ROI, prima sumă plauzibilă și primele două perechi min/km sunt oferta.
        val fare = fareCandidates.first()
        val (pickupMin, pickupKm) = segmentCandidates[0]
        val (tripMin, tripKm) = segmentCandidates[1]

        // Protecții suplimentare împotriva OCR-ului absurd.
        val totalKm = pickupKm + tripKm
        val totalMin = pickupMin + tripMin
        if (totalKm <= 0.1 || totalKm > 120.0) return
        if (totalMin < 2 || totalMin > 120) return

        val signature = "$fare|$pickupMin|$pickupKm|$tripMin|$tripKm"
        lastOfferAt = now

        val prefs = getSharedPreferences("runtime_settings", MODE_PRIVATE)

        // Profilul REAL salvat de utilizator.
        val fuelPrice = prefDouble(prefs, "fuelPrice", 4.66)
        val consumption = prefDouble(prefs, "fuelConsumption", 10.0)
        val maintenancePerKm = prefDouble(prefs, "maint", 0.0)

        val monthlyCosts =
            prefDouble(prefs, "monthly", 800.0) +
            prefDouble(prefs, "insurance", 300.0) +
            prefDouble(prefs, "phone", 50.0) +
            prefDouble(prefs, "other", 100.0)

        val hoursPerDay = max(1.0, prefDouble(prefs, "hpd", 8.0))
        val daysPerWeek = max(1.0, prefDouble(prefs, "dpw", 6.0))

        val minKm = prefDouble(prefs, "km", 2.0)
        val minHour = prefDouble(prefs, "hour", 50.0)
        val minProfit = prefDouble(prefs, "profit", 5.0)

        val ronKm = fare / totalKm
        val ronHour = fare / (totalMin / 60.0)

        // Aceeași formulă funcționează pentru GPL/benzină/motorină sau electric:
        // consum/100 × preț unitate × km.
        val energyCost = totalKm * consumption / 100.0 * fuelPrice

        val monthlyHours = hoursPerDay * daysPerWeek * 4.33
        val fixedCost = if (monthlyHours > 0.0) {
            monthlyCosts / (monthlyHours * 60.0) * totalMin
        } else 0.0

        val maintenanceCost = totalKm * maintenancePerKm
        val profit = fare - energyCost - fixedCost - maintenanceCost

        val good =
            ronKm >= minKm &&
            ronHour >= minHour &&
            profit >= minProfit

        sendOverlayUpdate(
            fare, pickupMin, pickupKm, tripMin, tripKm,
            ronKm, ronHour, profit, good
        )

        if (signature != lastSignature) {
            lastSignature = signature

            val voiceEnabled = prefs.getBoolean("voice", true)
            val speakOnlyGood = prefs.getBoolean("onlyGood", false)
            if (voiceEnabled && (!speakOnlyGood || good)) {
                tts.speakAmount(
                    fare,
                    prefs.getFloat("volume", 0.7f),
                    prefs.getFloat("rate", 1.0f)
                )
            }
        }
    }

    private fun sendOverlayUpdate(
        fare: Double,
        pickupMin: Int,
        pickupKm: Double,
        tripMin: Int,
        tripKm: Double,
        ronKm: Double,
        ronHour: Double,
        profit: Double,
        good: Boolean
    ) {
        val i = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE
            putExtra(OverlayService.EXTRA_FARE, fare)
            putExtra(OverlayService.EXTRA_PICKUP_MIN, pickupMin)
            putExtra(OverlayService.EXTRA_PICKUP_KM, pickupKm)
            putExtra(OverlayService.EXTRA_TRIP_MIN, tripMin)
            putExtra(OverlayService.EXTRA_TRIP_KM, tripKm)
            putExtra(OverlayService.EXTRA_RON_KM, ronKm)
            putExtra(OverlayService.EXTRA_RON_HOUR, ronHour)
            putExtra(OverlayService.EXTRA_PROFIT, profit)
            putExtra(OverlayService.EXTRA_GOOD, good)
        }
        androidx.core.content.ContextCompat.startForegroundService(this, i)
    }

    private fun sendOverlayReset() {
        androidx.core.content.ContextCompat.startForegroundService(
            this,
            Intent(this, OverlayService::class.java).setAction(OverlayService.ACTION_RESET)
        )
    }

    private fun prefDouble(
        prefs: android.content.SharedPreferences,
        key: String,
        default: Double
    ): Double {
        if (!prefs.contains(key)) return default
        return try {
            java.lang.Double.longBitsToDouble(prefs.getLong(key, 0L))
        } catch (_: Exception) {
            default
        }
    }

    private fun parseDecimal(s: String): Double? = s.replace(',', '.').toDoubleOrNull()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        "screen_capture",
                        "ProfitRide analiză ecran",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }

    override fun onDestroy() {
        imageReader?.setOnImageAvailableListener(null, null)
        virtualDisplay?.release()
        mediaProjection?.stop()
        recognizer.close()
        tts.shutdown()
        handlerThread?.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
