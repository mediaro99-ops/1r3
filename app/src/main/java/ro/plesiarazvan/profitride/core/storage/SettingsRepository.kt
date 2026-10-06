package ro.plesiarazvan.profitride.core.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "profitride_settings")

data class AppSettings(
    val boltEnabled: Boolean = true,
    val uberEnabled: Boolean = true,
    val autoStart: Boolean = false,
    val hideInWaze: Boolean = true,
    val resetOnReject: Boolean = true,
    val pauseOnAccept: Boolean = true,
    val reappearAfterTrip: Boolean = true,
    val vibration: Boolean = true,
    val overlayMode: String = "Automat",
    val fuelType: String = "GPL",
    val fuelPrice: Double = 4.66,
    val fuelConsumption: Double = 10.0,
    val monthlyFixedCosts: Double = 800.0,
    val insuranceCosts: Double = 300.0,
    val phoneCosts: Double = 50.0,
    val otherCosts: Double = 100.0,
    val maintenancePerKm: Double = 0.0,
    val hoursPerDay: Double = 8.0,
    val daysPerWeek: Double = 6.0,
    val monthlyTarget: Double = 5000.0,
    val minRonKm: Double = 2.0,
    val minRonHour: Double = 50.0,
    val minProfit: Double = 5.0,
    val rideScoreEnabled: Boolean = false,
    val minRideScore: Double = 4.7,
    val voiceEnabled: Boolean = true,
    val voiceVolume: Float = 0.7f,
    val voiceRate: Float = 1.0f,
    val speakOnlyGood: Boolean = false,
    val onboardingDone: Boolean = false,
    val serviceEnabled: Boolean = false
)

class SettingsRepository(private val context: Context) {
    private object K {
        val bolt = booleanPreferencesKey("bolt")
        val uber = booleanPreferencesKey("uber")
        val auto = booleanPreferencesKey("auto")
        val waze = booleanPreferencesKey("waze")
        val reset = booleanPreferencesKey("reset")
        val pause = booleanPreferencesKey("pause")
        val reappear = booleanPreferencesKey("reappear")
        val vib = booleanPreferencesKey("vib")
        val mode = stringPreferencesKey("mode")
        val fuelType = stringPreferencesKey("fuelType")
        val fuelPrice = doublePreferencesKey("fuelPrice")
        val fuelConsumption = doublePreferencesKey("fuelConsumption")
        val monthly = doublePreferencesKey("monthly")
        val insurance = doublePreferencesKey("insurance")
        val phone = doublePreferencesKey("phone")
        val other = doublePreferencesKey("other")
        val maint = doublePreferencesKey("maint")
        val hpd = doublePreferencesKey("hpd")
        val dpw = doublePreferencesKey("dpw")
        val target = doublePreferencesKey("target")
        val km = doublePreferencesKey("km")
        val hour = doublePreferencesKey("hour")
        val profit = doublePreferencesKey("profit")
        val scoreEnabled = booleanPreferencesKey("scoreEnabled")
        val score = doublePreferencesKey("score")
        val voice = booleanPreferencesKey("voice")
        val volume = floatPreferencesKey("volume")
        val rate = floatPreferencesKey("rate")
        val onlyGood = booleanPreferencesKey("onlyGood")
        val onboarding = booleanPreferencesKey("onboarding")
        val serviceEnabled = booleanPreferencesKey("serviceEnabled")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            boltEnabled = p[K.bolt] ?: true,
            uberEnabled = p[K.uber] ?: true,
            autoStart = p[K.auto] ?: false,
            hideInWaze = p[K.waze] ?: true,
            resetOnReject = p[K.reset] ?: true,
            pauseOnAccept = p[K.pause] ?: true,
            reappearAfterTrip = p[K.reappear] ?: true,
            vibration = p[K.vib] ?: true,
            overlayMode = p[K.mode] ?: "Automat",
            fuelType = p[K.fuelType] ?: "GPL",
            fuelPrice = p[K.fuelPrice] ?: 4.66,
            fuelConsumption = p[K.fuelConsumption] ?: 10.0,
            monthlyFixedCosts = p[K.monthly] ?: 800.0,
            insuranceCosts = p[K.insurance] ?: 300.0,
            phoneCosts = p[K.phone] ?: 50.0,
            otherCosts = p[K.other] ?: 100.0,
            maintenancePerKm = p[K.maint] ?: 0.0,
            hoursPerDay = p[K.hpd] ?: 8.0,
            daysPerWeek = p[K.dpw] ?: 6.0,
            monthlyTarget = p[K.target] ?: 5000.0,
            minRonKm = p[K.km] ?: 2.0,
            minRonHour = p[K.hour] ?: 50.0,
            minProfit = p[K.profit] ?: 5.0,
            rideScoreEnabled = p[K.scoreEnabled] ?: false,
            minRideScore = p[K.score] ?: 4.7,
            voiceEnabled = p[K.voice] ?: true,
            voiceVolume = p[K.volume] ?: 0.7f,
            voiceRate = p[K.rate] ?: 1.0f,
            speakOnlyGood = p[K.onlyGood] ?: false,
            onboardingDone = p[K.onboarding] ?: false,
            serviceEnabled = p[K.serviceEnabled] ?: false
        )
    }

    fun syncRuntime(s: AppSettings) {
        context.getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("bolt", s.boltEnabled)
            .putBoolean("uber", s.uberEnabled)
            .putBoolean("auto", s.autoStart)
            .putBoolean("waze", s.hideInWaze)
            .putBoolean("reset", s.resetOnReject)
            .putBoolean("pause", s.pauseOnAccept)
            .putBoolean("reappear", s.reappearAfterTrip)
            .putBoolean("vib", s.vibration)
            .putBoolean("scoreEnabled", s.rideScoreEnabled)
            .putBoolean("voice", s.voiceEnabled)
            .putBoolean("onlyGood", s.speakOnlyGood)
            .putBoolean("serviceEnabled", s.serviceEnabled)
            .putString("mode", s.overlayMode)
            .putString("fuelType", s.fuelType)
            .putLong("fuelPrice", java.lang.Double.doubleToRawLongBits(s.fuelPrice))
            .putLong("fuelConsumption", java.lang.Double.doubleToRawLongBits(s.fuelConsumption))
            .putLong("monthly", java.lang.Double.doubleToRawLongBits(s.monthlyFixedCosts))
            .putLong("insurance", java.lang.Double.doubleToRawLongBits(s.insuranceCosts))
            .putLong("phone", java.lang.Double.doubleToRawLongBits(s.phoneCosts))
            .putLong("other", java.lang.Double.doubleToRawLongBits(s.otherCosts))
            .putLong("maint", java.lang.Double.doubleToRawLongBits(s.maintenancePerKm))
            .putLong("hpd", java.lang.Double.doubleToRawLongBits(s.hoursPerDay))
            .putLong("dpw", java.lang.Double.doubleToRawLongBits(s.daysPerWeek))
            .putLong("target", java.lang.Double.doubleToRawLongBits(s.monthlyTarget))
            .putLong("km", java.lang.Double.doubleToRawLongBits(s.minRonKm))
            .putLong("hour", java.lang.Double.doubleToRawLongBits(s.minRonHour))
            .putLong("profit", java.lang.Double.doubleToRawLongBits(s.minProfit))
            .putLong("score", java.lang.Double.doubleToRawLongBits(s.minRideScore))
            .putFloat("volume", s.voiceVolume)
            .putFloat("rate", s.voiceRate)
            .apply()
    }

    suspend fun updateBoolean(key: String, value: Boolean) {
        context.dataStore.edit { p ->
            when (key) {
                "bolt" -> p[K.bolt] = value
                "uber" -> p[K.uber] = value
                "auto" -> p[K.auto] = value
                "waze" -> p[K.waze] = value
                "reset" -> p[K.reset] = value
                "pause" -> p[K.pause] = value
                "reappear" -> p[K.reappear] = value
                "vib" -> p[K.vib] = value
                "scoreEnabled" -> p[K.scoreEnabled] = value
                "voice" -> p[K.voice] = value
                "onlyGood" -> p[K.onlyGood] = value
                "onboarding" -> p[K.onboarding] = value
                "serviceEnabled" -> p[K.serviceEnabled] = value
            }
        }

        // Mirror settings needed by Android services/boot before Compose/DataStore UI is active.
        if (key in setOf("auto", "waze", "bolt", "uber", "reappear", "serviceEnabled")) {
            context.getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(key, value)
                .apply()
        }
    }

    suspend fun updateDouble(key: String, value: Double) {
        context.dataStore.edit { p ->
            when (key) {
                "fuelPrice" -> p[K.fuelPrice] = value
                "fuelConsumption" -> p[K.fuelConsumption] = value
                "monthly" -> p[K.monthly] = value
                "insurance" -> p[K.insurance] = value
                "phone" -> p[K.phone] = value
                "other" -> p[K.other] = value
                "maint" -> p[K.maint] = value
                "hpd" -> p[K.hpd] = value
                "dpw" -> p[K.dpw] = value
                "target" -> p[K.target] = value
                "km" -> p[K.km] = value
                "hour" -> p[K.hour] = value
                "profit" -> p[K.profit] = value
                "score" -> p[K.score] = value
            }
        }
        context.getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
            .edit().putLong(key, java.lang.Double.doubleToRawLongBits(value)).apply()
    }

    suspend fun updateString(key: String, value: String) {
        context.dataStore.edit { p ->
            when (key) {
                "mode" -> p[K.mode] = value
                "fuelType" -> p[K.fuelType] = value
            }
        }
        context.getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    suspend fun updateVoice(volume: Float, rate: Float) {
        context.dataStore.edit { p ->
            p[K.volume] = volume
            p[K.rate] = rate
        }
        context.getSharedPreferences("runtime_settings", Context.MODE_PRIVATE)
            .edit()
            .putFloat("volume", volume)
            .putFloat("rate", rate)
            .apply()
    }
}
