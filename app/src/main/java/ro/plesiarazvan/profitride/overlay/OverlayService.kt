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
        const val ACTION_SHOW_DEMO = "ro.plesiarazvan.profitride.SHOW_DEMO"
        const val ACTION_HIDE = "ro.plesiarazvan.profitride.HIDE_OVERLAY"
        const val ACTION_SHOW = "ro.plesiarazvan.profitride.SHOW_OVERLAY"
        const val ACTION_STOP = "ro.plesiarazvan.profitride.STOP_OVERLAY"
    }

    private lateinit var wm: WindowManager
    private var view: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var compact = false
    private val prefs by lazy { getSharedPreferences("overlay_pos", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
        startForeground(2201, NotificationCompat.Builder(this, "profitride")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ProfitRide este activ")
            .setContentText("Așteaptă următoarea ofertă.")
            .setOngoing(true).build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_SHOW_DEMO -> showDemo()
            ACTION_HIDE -> hide()
            ACTION_SHOW -> if (view != null) view?.visibility = View.VISIBLE
            ACTION_STOP -> { remove(); stopSelf() }
        }
        return START_STICKY
    }

    private fun showDemo() {
        if (!Settings.canDrawOverlays(this)) return
        remove()
        compact = false
        val density = resources.displayMetrics.density
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14*density).toInt(), (12*density).toInt(), (14*density).toInt(), (12*density).toInt())
            background = roundedBg("#0A1117", "#35F58A", 20f)
        }

        fun tv(text: String, size: Float, color: String="#FFFFFF", bold:Boolean=false): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.parseColor(color))
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = tv("R", 22f, "#35F58A", true)
        val title = tv("  ProfitRide", 16f, "#FFFFFF", true)
        val spacer = Space(this).apply { layoutParams = LinearLayout.LayoutParams(0,1,1f) }
        val minimize = Button(this).apply {
            text = "↙"
            textSize = 12f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#14232C"))
            setOnClickListener { toggleCompact() }
        }
        header.addView(logo); header.addView(title); header.addView(spacer); header.addView(minimize)
        box.addView(header)

        val amount = tv("9,82 lei", 32f, "#35F58A", true).apply { gravity = Gravity.CENTER_HORIZONTAL }
        box.addView(amount)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fun infoBox(a:String,b:String,c:String): LinearLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((8*density).toInt(), (6*density).toInt(), (8*density).toInt(), (6*density).toInt())
            background = roundedBg("#102434", "#2A95E8", 12f)
            addView(tv(a,16f,"#FFFFFF",true).apply{gravity=Gravity.CENTER})
            addView(tv(b,15f,"#FFFFFF",true).apply{gravity=Gravity.CENTER})
            addView(tv(c,11f,"#C8D8E6").apply{gravity=Gravity.CENTER})
        }
        row.addView(infoBox("6 min","2.8 km","până la client"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1f).apply{marginEnd=(4*density).toInt()})
        row.addView(infoBox("7 min","3.5 km","cursă"), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT,1f).apply{marginStart=(4*density).toInt()})
        box.addView(row)
        box.addView(tv("Total: 13 min • 6.3 km",14f,"#FFFFFF",true).apply{gravity=Gravity.CENTER; setPadding(0,(8*density).toInt(),0,(6*density).toInt())})

        val metrics = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        fun metric(v:String,l:String,green:Boolean=false)=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER
            setPadding((8*density).toInt(),(5*density).toInt(),(8*density).toInt(),(5*density).toInt())
            background = roundedBg(if(green)"#0D5A35" else "#15212A", if(green)"#35F58A" else "#273A45", 10f)
            addView(tv(v,16f,if(green)"#35F58A" else "#FFFFFF",true).apply{gravity=Gravity.CENTER})
            addView(tv(l,10f,"#FFFFFF").apply{gravity=Gravity.CENTER})
        }
        metrics.addView(metric("1.56","lei/km"), LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        metrics.addView(metric("45","lei/oră"), LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f).apply{setMargins((4*density).toInt(),0,(4*density).toInt(),0)})
        metrics.addView(metric("5.86","lei profit",true), LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
        box.addView(metrics)

        box.addView(tv("🔊  Voce: 9,82 lei",12f,"#DCE7EC").apply{setPadding(0,(8*density).toInt(),0,0)})

        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            (330*density).toInt(), WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", 20)
            y = prefs.getInt("y", 120)
        }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        box.setOnTouchListener { _, e ->
            when(e.action) {
                MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; startX=params!!.x; startY=params!!.y; true }
                MotionEvent.ACTION_MOVE -> {
                    params!!.x = startX + (e.rawX-downX).roundToInt()
                    params!!.y = startY + (e.rawY-downY).roundToInt()
                    wm.updateViewLayout(box, params!!); true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit().putInt("x",params!!.x).putInt("y",params!!.y).apply(); true
                }
                else -> false
            }
        }
        view = box
        wm.addView(box, params)
    }

    private fun toggleCompact() {
        compact = !compact
        val v = view as? LinearLayout ?: return
        if (compact) {
            while (v.childCount > 2) v.removeViewAt(2)
            params?.width = (220*resources.displayMetrics.density).toInt()
            wm.updateViewLayout(v, params!!)
        } else showDemo()
    }

    private fun roundedBg(fill:String, stroke:String, radius:Float) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = radius * resources.displayMetrics.density
            setColor(Color.parseColor(fill))
            setStroke((1.5f*resources.displayMetrics.density).toInt(), Color.parseColor(stroke))
        }

    private fun hide() { view?.visibility = View.GONE }
    private fun remove() { view?.let { try { wm.removeView(it) } catch (_:Exception){} }; view=null }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("profitride","ProfitRide",NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onDestroy() { remove(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
