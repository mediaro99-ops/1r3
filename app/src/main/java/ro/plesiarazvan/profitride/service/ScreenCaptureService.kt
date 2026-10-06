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
import ro.plesiarazvan.profitride.core.calculation.TripCalculator
import ro.plesiarazvan.profitride.core.model.DriverProfile
import ro.plesiarazvan.profitride.core.model.TripOffer
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

        val fareCandidates = Regex(
            """(?<!\d)(\d{1,3}[.,]\d{2})\s*(?:lei|RON)\b""",
            RegexOption.IGNORE_CASE
        ).findAll(normalized)
            .mapNotNull { parseDecimal(it.groupValues[1]) }
            .filter { it in 2.0..500.0 }
            .toList()

        val segmentCandidates = Regex(
            """(?<!\d)(\d{1,2})\s*min\s*[•·\-]?\s*(\d{1,2}(?:[.,]\d{1,2})?)\s*km\b""",
            RegexOption.IGNORE_CASE
        ).findAll(normalized)
            .mapNotNull { m ->
                val minutes = m.groupValues[1].toIntOrNull()
                val km = parseDecimal(m.groupValues[2])
                if (
                    minutes != null &&
                    km != null &&
                    minutes in 1..60 &&
                    km > 0.05 &&
                    km <= 80.0
                ) Pair(minutes, km) else null
            }
            .toList()

        val now = System.currentTimeMillis()

        if (fareCandidates.isEmpty() || segmentCandidates.size < 2) {
            if (lastOfferAt > 0 && now - lastOfferAt > 5000L) {
                sendOverlayReset()
                lastOfferAt = 0L
                lastSignature = ""
            }
            return
        }

        val fare = fareCandidates.first()
        val (pickupMin, pickupKm) = segmentCandidates[0]
        val (tripMin, tripKm) = segmentCandidates[1]

        val totalKm = pickupKm + tripKm
        val totalMin = pickupMin + tripMin

        if (totalKm <= 0.1 || totalKm > 120.0) return
        if (totalMin < 2 || totalMin > 120) return

        val signature = "$fare|$pickupMin|$pickupKm|$tripMin|$tripKm"
        lastOfferAt = now

        val prefs = getSharedPreferences("runtime_settings", MODE_PRIVATE)

        val paymentType = when {
            normalized.contains("Numerar", true) ||
                normalized.contains("Cash", true) -> "Numerar"
            normalized.contains("Card", true) -> "Card"
            else -> null
        }

        val currentProvider = prefs.getString("currentProvider", "Bolt") ?: "Bolt"

        val offer = TripOffer(
            platform = currentProvider,
            fare = fare,
            pickupDistanceKm = pickupKm,
            pickupDurationMin = pickupMin,
            tripDistanceKm = tripKm,
            tripDurationMin = tripMin,
            paymentType = paymentType
        )

        val profile = DriverProfile(
            fuelType = prefs.getString("fuelType", "GPL") ?: "GPL",
            fuelPrice = prefDouble(prefs, "fuelPrice", 4.66),
            fuelConsumptionPer100 = prefDouble(prefs, "fuelConsumption", 10.0),
            maintenancePerKm = prefDouble(prefs, "maint", 0.0),
            rentOrRateMonthly = prefDouble(prefs, "monthly", 800.0),
            insuranceMonthly = prefDouble(prefs, "insurance", 300.0),
            phoneMonthly = prefDouble(prefs, "phone", 50.0),
            otherMonthly = prefDouble(prefs, "other", 100.0),
            fixedAllocationMode =
                prefs.getString("fixedAllocationMode", "Per km") ?: "Per km",
            monthlyKm = prefDouble(prefs, "monthlyKm", 5000.0),
            hoursPerDay = max(1.0, prefDouble(prefs, "hpd", 8.0)),
            daysPerWeek = max(1.0, prefDouble(prefs, "dpw", 6.0)),
            minProfitPerTrip = prefDouble(prefs, "profit", 5.0),
            minProfitPerKm = prefDouble(prefs, "profitKm", 0.80),
            minProfitPerHour = prefDouble(prefs, "profitHour", 30.0),
            minGrossPerKm = prefDouble(prefs, "grossKm", 2.0),
            maxPickupKm = prefDouble(prefs, "maxPickupKm", 3.0),
            maxPickupMinutes =
                prefDouble(prefs, "maxPickupMinutes", 10.0).toInt()
        )

        val analysis = TripCalculator.calculate(offer, profile)

        if (signature != lastSignature) {
            lastSignature = signature

            sendOverlayUpdate(offer, analysis)

            val voiceEnabled = prefs.getBoolean("voice", true)
            val speakOnlyGood = prefs.getBoolean("onlyGood", false)
            val goodEnough =
                analysis.recommendation.name == "ACCEPTA" ||
                analysis.recommendation.name == "ACCEPTABIL"

            if (voiceEnabled && (!speakOnlyGood || goodEnough)) {
                tts.speakAmount(
                    fare,
                    prefs.getFloat("volume", 0.7f),
                    prefs.getFloat("rate", 1.0f)
                )
            }
        }
    }

    private fun sendOverlayUpdate(
        offer: TripOffer,
        analysis: ro.plesiarazvan.profitride.core.model.TripAnalysis
    ) {
        val i = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_UPDATE

            putExtra(OverlayService.EXTRA_PLATFORM, offer.platform)
            putExtra(OverlayService.EXTRA_PAYMENT_TYPE, offer.paymentType ?: "")

            putExtra(OverlayService.EXTRA_FARE, analysis.grossFare)
            putExtra(OverlayService.EXTRA_PICKUP_MIN, offer.pickupDurationMin)
            putExtra(OverlayService.EXTRA_PICKUP_KM, offer.pickupDistanceKm)
            putExtra(OverlayService.EXTRA_TRIP_MIN, offer.tripDurationMin)
            putExtra(OverlayService.EXTRA_TRIP_KM, offer.tripDistanceKm)

            putExtra(OverlayService.EXTRA_GROSS_KM, analysis.grossPerKm)
            putExtra(OverlayService.EXTRA_GROSS_HOUR, analysis.grossPerHour)
            putExtra(OverlayService.EXTRA_PROFIT_KM, analysis.profitPerKm)
            putExtra(OverlayService.EXTRA_PROFIT_HOUR, analysis.profitPerHour)

            putExtra(OverlayService.EXTRA_FUEL_COST, analysis.fuelCost)
            putExtra(OverlayService.EXTRA_VEHICLE_COST, analysis.vehicleCost)
            putExtra(OverlayService.EXTRA_RENT_COST, analysis.rentOrRateCost)
            putExtra(OverlayService.EXTRA_OTHER_COST, analysis.otherFixedCost)
            putExtra(OverlayService.EXTRA_TOTAL_COST, analysis.totalCost)
            putExtra(OverlayService.EXTRA_PROFIT, analysis.netProfit)

            putExtra(
                OverlayService.EXTRA_RECOMMENDATION,
                analysis.recommendation.name
            )
            putExtra(
                OverlayService.EXTRA_REASON,
                analysis.recommendationReason
            )
            putExtra(
                OverlayService.EXTRA_TIP,
                analysis.contextualTip
            )
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
