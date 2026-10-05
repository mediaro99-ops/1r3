package ro.plesiarazvan.profitride.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ro.plesiarazvan.profitride.MainActivity
import ro.plesiarazvan.profitride.R

class MonitoringService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel("monitor","ProfitRide monitor",NotificationManager.IMPORTANCE_LOW))
        }
        val pending = PendingIntent.getActivity(this,0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        startForeground(2101, NotificationCompat.Builder(this,"monitor")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ProfitRide este activ")
            .setContentText("Așteaptă următoarea ofertă.")
            .setContentIntent(pending)
            .setOngoing(true)
            .build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
