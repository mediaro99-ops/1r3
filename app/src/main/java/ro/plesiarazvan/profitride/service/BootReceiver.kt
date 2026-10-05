package ro.plesiarazvan.profitride.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val p = context.getSharedPreferences("boot", Context.MODE_PRIVATE)
        if (p.getBoolean("auto", false)) {
            ContextCompat.startForegroundService(context, Intent(context, MonitoringService::class.java))
        }
    }
}
