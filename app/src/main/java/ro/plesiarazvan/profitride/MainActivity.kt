package ro.plesiarazvan.profitride

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var store: SettingsStore
    private val roundedTypeface by lazy { Typeface.create("sans-serif-rounded", Typeface.NORMAL) }

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK && r.data != null) {
            val i = Intent(this, CaptureOverlayService::class.java).apply {
                action = CaptureOverlayService.ACTION_START
                putExtra(CaptureOverlayService.EXTRA_RESULT_CODE, r.resultCode)
                putExtra(CaptureOverlayService.EXTRA_RESULT_DATA, r.data)
            }
            ContextCompat.startForegroundService(this, i)
            Toast.makeText(this, "ProfitRide este activ. Poți intra în Bolt/Uber.", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SettingsStore(this)
        buildShell()
        showGeneral()
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
            setBackgroundColor(Color.parseColor("#081117"))
        }

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_profitride); layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)) }
        val titleWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8),0,0,0) }
        titleWrap.addView(label("ProfitRide", 26f, Color.WHITE, true))
        titleWrap.addView(label("Asistentul tău în ridesharing", 13f, Color.parseColor("#A9B7C0")))
        header.addView(logo)
        header.addView(titleWrap, LinearLayout.LayoutParams(0, dp(60), 1f))
        val stop = button("OPREȘTE", "#15232C", "#A9B7C0").apply {
            textSize = 11f
            setOnClickListener {
                startService(Intent(this@MainActivity, CaptureOverlayService::class.java).apply { action = CaptureOverlayService.ACTION_STOP })
                Toast.makeText(this@MainActivity, "ProfitRide oprit", Toast.LENGTH_SHORT).show()
            }
        }
        header.addView(stop, LinearLayout.LayoutParams(dp(90), dp(42)))
        root.addView(header)

        val tabs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val tabRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,dp(8),0,dp(8)) }
        listOf(
            "General" to { showGeneral() },
            "Costuri" to { showCosts() },
            "Praguri" to { showThresholds() },
            "Sunet" to { showVoice() },
            "Info" to { showInfo() }
        ).forEach { (name, action) ->
            tabRow.addView(button(name, "#12212A", "#FFFFFF").apply { setOnClickListener { action() } }, LinearLayout.LayoutParams(dp(92), dp(42)).apply { marginEnd = dp(6) })
        }
        tabs.addView(tabRow)
        root.addView(tabs)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(4), 0, dp(18)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun showGeneral() {
        content.removeAllViews()
        sectionTitle("Setări generale", "Configurezi o dată, apoi ProfitRide lucrează automat.")

        addSwitchCard("Pornire automată", "Pornești analiza cu un singur buton; setările rămân salvate.", true, false) { }
        addSwitchCard("Se ascunde automat în Waze", "Activ implicit. Când Waze se deschide pentru navigație, afișajul ProfitRide dispare complet.", store.hideInWaze, true) { store.hideInWaze = it }
        addSwitchCard("Resetare la refuz", "Când refuzi oferta, valorile revin la 0 și aplicația așteaptă cursa următoare.", store.resetOnReject, true) { store.resetOnReject = it }
        addSwitchCard("Pauză după accept", "După acceptarea unei curse, ProfitRide se ascunde și nu te deranjează în timpul deplasării.", store.pauseOnAccept, true) { store.pauseOnAccept = it }
        addSwitchCard("Reapare automat", "La următoarea ofertă Bolt/Uber, cardul revine și calculează automat.", store.reappearOnDriverApp, true) { store.reappearOnDriverApp = it }
        addSwitchCard("Vibrație la ofertă", "Vibrație scurtă când este detectată o ofertă nouă.", store.vibrateOnOffer, true) { store.vibrateOnOffer = it }

        content.addView(actionCard("ACTIVEAZĂ MODUL AUTOMAT", "ProfitRide are nevoie de accesibilitate ca să știe când ești în Bolt/Uber/Waze.", "Accesibilitate") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(actionCard("PORNEȘTE ANALIZA", "Acordă permisiunile și pornește captura ecranului. Apoi intră în Bolt sau Uber.", "START") { startAnalysis() })
    }

    private fun showCosts() {
        content.removeAllViews()
        sectionTitle("Setări costuri", "Cheltuieli și consum. Valorile intră automat în profitul NET.")
        infoCard("Costuri fixe lunare", "Total curent: ${formatValue(store.fixedTotal, "RON/lună")}")
        addValueCard("Rată / Chirie", "Cost lunar pentru mașină", store.carPayment, "RON") { store.carPayment = it.toFloat() }
        addValueCard("Asigurări / Taxe", "RCA, CASCO, taxe și alte obligații", store.insuranceTaxes, "RON") { store.insuranceTaxes = it.toFloat() }
        addValueCard("Telefon / Date", "Abonament și internet folosit pentru muncă", store.phoneData, "RON") { store.phoneData = it.toFloat() }
        addValueCard("Alte costuri fixe", "Orice alte cheltuieli lunare", store.otherFixed, "RON") { store.otherFixed = it.toFloat() }
        addFuelTypeCard()
        addValueCard("Preț combustibil / energie", "Prețul pentru ${store.fuelType}", store.fuelPrice, if (store.fuelType == "Electric") "RON/kWh" else "RON/L") { store.fuelPrice = it.toFloat() }
        addValueCard("Consum mediu", "Consum real al mașinii tale", store.consumption, "L/100 km") { store.consumption = it.toFloat() }
        addValueCard("Mentenanță per km", "Revizii, anvelope, uzură și consumabile", store.maintenancePerKm, "RON/km") { store.maintenancePerKm = it.toFloat() }
        addValueCard("Program de lucru", "Ore estimate pe săptămână", store.weeklyHours, "ore/săpt.") { store.weeklyHours = it.toFloat() }
        addValueCard("Kilometri săptămânali", "Km estimați pentru împărțirea costurilor fixe", store.weeklyKm, "km/săpt.") { store.weeklyKm = it.toFloat() }
        addValueCard("Ținta ta", "Profit lunar dorit", store.targetMonthly, "RON/lună") { store.targetMonthly = it.toFloat() }
    }

    private fun showThresholds() {
        content.removeAllViews()
        sectionTitle("Setări praguri", "Criteriile tale minime. ProfitRide le folosește ca să coloreze oferta.")
        addValueCard("Prag NET / km", "Câștigul minim dorit după costuri pentru fiecare km total", store.minRonKm, "RON/km") { store.minRonKm = it.toFloat() }
        addValueCard("Prag NET / oră", "Venitul minim dorit după costuri raportat la timpul total", store.minRonHour, "RON/oră") { store.minRonHour = it.toFloat() }
        addValueCard("Profit minim / cursă", "Profitul minim estimat pe care vrei să îl obții", store.minProfit, "RON") { store.minProfit = it.toFloat() }
        addValueCard("Ride Score minim", "Scor combinat între NET/km, NET/oră și profit", store.minScore, "/5") { store.minScore = it.toFloat() }
        infoCard("Cum funcționează scorul", "Scorul nu acceptă și nu refuză nimic. Este doar un indicator vizual calculat din criteriile tale.")
    }

    private fun showVoice() {
        content.removeAllViews()
        sectionTitle("Setări voce", "Vocea este scurtă și spune doar informația de care ai nevoie.")
        addSwitchCard("Spune doar suma ofertei", "Exemplu: „9 lei și 82 bani”. Nu citește adrese, kilometri sau alte detalii.", store.voiceOnlyAmount, true) { store.voiceOnlyAmount = it }
        addSwitchCard("Voce activă", "Anunță automat suma o singură dată pentru fiecare ofertă nouă.", store.voiceEnabled, true) { store.voiceEnabled = it }
        addValueCard("Volum voce", "0 = minim, 1 = maxim", store.voiceVolume, "") { store.voiceVolume = it.toFloat().coerceIn(0f, 1f) }
        addValueCard("Viteză vorbire", "1,00 = normal", store.speechRate, "x") { store.speechRate = it.toFloat().coerceIn(0.5f, 1.8f) }
        content.addView(actionCard("TEST VOCE", "Testează anunțul vocal folosind suma 9,82 lei.", "Testează") {
            var engine: android.speech.tts.TextToSpeech? = null
            engine = android.speech.tts.TextToSpeech(this) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    engine?.language = Locale("ro", "RO")
                    engine?.speak("9 lei și 82 bani", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "test")
                }
            }
        })
    }

    private fun showInfo() {
        content.removeAllViews()
        sectionTitle("Informații", "Despre aplicație, ghid rapid și suport.")
        infoCard("Despre aplicație", "ProfitRide citește oferta afișată în aplicația de șofer, calculează automat costurile și afișează NET/km, NET/oră, profit și scor. Nu acceptă și nu refuză curse în locul tău.")
        infoCard("Ghid rapid", "1. Activează accesibilitatea.\n2. Pornește analiza.\n3. Intră în Bolt/Uber.\n4. Mută cardul unde nu te încurcă.\n5. La accept cardul dispare; în Waze rămâne ascuns; la următoarea ofertă reapare.")
        infoCard("Versiune", "ProfitRide 3.0.0")
        infoCard("Confidențialitate", "OCR-ul și calculele sunt procesate local pe telefon în această versiune. Nu există cont sau abonament implementat încă.")
        val creator = card().apply {
            addView(label("Creat de Plesia Razvan", 13f, Color.parseColor("#7E909B"), true).apply { gravity = Gravity.CENTER })
        }
        content.addView(creator, lpTop())
    }

    private fun startAnalysis() {
        if (!Settings.canDrawOverlays(this)) {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(i)
            Toast.makeText(this, "Activează «Afișare peste alte aplicații», apoi revino și apasă din nou START.", Toast.LENGTH_LONG).show()
            return
        }
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mgr.createScreenCaptureIntent())
    }

    private fun sectionTitle(title: String, subtitle: String) {
        content.addView(label(title, 25f, Color.WHITE, true))
        content.addView(label(subtitle, 14f, Color.parseColor("#A9B7C0")).apply { setPadding(0,dp(2),0,dp(10)) })
    }

    private fun addSwitchCard(title: String, desc: String, checked: Boolean, enabled: Boolean, onChanged: (Boolean) -> Unit) {
        val c = card()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 17f, Color.WHITE, true))
        texts.addView(label(desc, 12.5f, Color.parseColor("#A9B7C0")))
        val sw = SwitchCompat(this).apply {
            isChecked = checked; isEnabled = enabled
            buttonTintList = null
            setOnCheckedChangeListener { _, b -> onChanged(b) }
        }
        row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(sw, LinearLayout.LayoutParams(dp(60), dp(48)))
        c.addView(row)
        content.addView(c, lpTop())
    }

    private fun addValueCard(title: String, desc: String, value: Float, unit: String, save: (Double) -> Unit) {
        val c = card()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(label(title, 17f, Color.WHITE, true))
        texts.addView(label(desc, 12.5f, Color.parseColor("#A9B7C0")))
        val valueButton = button(formatValue(value, unit), "#142630", "#22E77A").apply {
            textSize = 13f
            setOnClickListener { editNumber(title, value.toDouble(), unit) { v -> save(v); showCostsOrThresholds(title) } }
        }
        row.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(valueButton, LinearLayout.LayoutParams(dp(122), dp(52)))
        c.addView(row)
        content.addView(c, lpTop())
    }

    private fun showCostsOrThresholds(title: String) {
        when {
            title.contains("Prag") || title.contains("Profit minim") || title.contains("Score") -> showThresholds()
            title.contains("voce", ignoreCase = true) || title.contains("Viteză vorbire") -> showVoice()
            else -> showCosts()
        }
    }

    private fun editNumber(title: String, current: Double, unit: String, save: (Double) -> Unit) {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(String.format(Locale.US, "%.2f", current))
            setSelectAllOnFocus(true)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(unit)
            .setView(input)
            .setNegativeButton("Anulează", null)
            .setPositiveButton("Salvează") { _, _ ->
                val v = input.text.toString().replace(',', '.').toDoubleOrNull()
                if (v != null && v >= 0) save(v)
            }.show()
    }

    private fun addFuelTypeCard() {
        val c = card()
        c.addView(label("Combustibil / Energie", 17f, Color.WHITE, true))
        c.addView(label("Alege tipul folosit de mașină", 12.5f, Color.parseColor("#A9B7C0")).apply { setPadding(0,dp(3),0,dp(10)) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Benzină", "Diesel", "GPL", "Electric").forEach { type ->
            val active = store.fuelType == type
            val b = button(type, if (active) "#22E77A" else "#142630", if (active) "#04120A" else "#FFFFFF").apply {
                setOnClickListener { store.fuelType = type; showCosts() }
            }
            row.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f).apply { marginEnd = dp(5) })
        }
        c.addView(row)
        content.addView(c, lpTop())
    }

    private fun actionCard(title: String, desc: String, buttonText: String, action: () -> Unit): View {
        val c = card()
        c.addView(label(title, 18f, Color.WHITE, true))
        c.addView(label(desc, 12.5f, Color.parseColor("#A9B7C0")).apply { setPadding(0,dp(3),0,dp(10)) })
        c.addView(button(buttonText, "#22E77A", "#04120A").apply { setOnClickListener { action() } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)))
        return c
    }

    private fun infoCard(title: String, desc: String) {
        val c = card()
        c.addView(label(title, 17f, Color.WHITE, true))
        c.addView(label(desc, 13f, Color.parseColor("#A9B7C0")).apply { setPadding(0,dp(5),0,0) })
        content.addView(c, lpTop())
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = rounded("#0F1B23", "#28404C", 18f, 1)
    }

    private fun label(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); typeface = roundedTypeface
        if (bold) setTypeface(roundedTypeface, Typeface.BOLD)
    }

    private fun button(t: String, bg: String, fg: String) = Button(this).apply {
        text = t; setTextColor(Color.parseColor(fg)); textSize = 13f; typeface = roundedTypeface
        isAllCaps = false; background = rounded(bg, bg, 14f, 0); stateListAnimator = null
    }

    private fun rounded(fill: String, stroke: String, radius: Float, strokeWidth: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(radius.toInt()).toFloat()
        setColor(Color.parseColor(fill))
        if (strokeWidth > 0) setStroke(dp(strokeWidth), Color.parseColor(stroke))
    }

    private fun formatValue(v: Float, unit: String): String {
        val digits = if (v >= 100) 0 else 2
        return String.format(Locale.US, "%.${digits}f %s", v, unit).replace('.', ',')
    }

    private fun lpTop() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
}
