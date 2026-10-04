package ro.plesiarazvan.profitride

import android.content.Context

class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("profitride_settings", Context.MODE_PRIVATE)

    var carPayment: Float
        get() = p.getFloat("carPayment", 0f)
        set(v) = p.edit().putFloat("carPayment", v).apply()
    var insuranceTaxes: Float
        get() = p.getFloat("insuranceTaxes", 0f)
        set(v) = p.edit().putFloat("insuranceTaxes", v).apply()
    var phoneData: Float
        get() = p.getFloat("phoneData", 0f)
        set(v) = p.edit().putFloat("phoneData", v).apply()
    var otherFixed: Float
        get() = p.getFloat("otherFixed", 0f)
        set(v) = p.edit().putFloat("otherFixed", v).apply()
    val fixedTotal: Float
        get() = carPayment + insuranceTaxes + phoneData + otherFixed
    var fuelType: String
        get() = p.getString("fuelType", "GPL") ?: "GPL"
        set(v) = p.edit().putString("fuelType", v).apply()
    var fuelPrice: Float
        get() = p.getFloat("fuelPrice", 4.66f)
        set(v) = p.edit().putFloat("fuelPrice", v).apply()
    var consumption: Float
        get() = p.getFloat("consumption", 10f)
        set(v) = p.edit().putFloat("consumption", v).apply()
    var maintenancePerKm: Float
        get() = p.getFloat("maintenancePerKm", 0.15f)
        set(v) = p.edit().putFloat("maintenancePerKm", v).apply()
    var weeklyHours: Float
        get() = p.getFloat("weeklyHours", 40f)
        set(v) = p.edit().putFloat("weeklyHours", v).apply()
    var weeklyKm: Float
        get() = p.getFloat("weeklyKm", 800f)
        set(v) = p.edit().putFloat("weeklyKm", v).apply()
    var targetMonthly: Float
        get() = p.getFloat("targetMonthly", 5000f)
        set(v) = p.edit().putFloat("targetMonthly", v).apply()
    var minRonKm: Float
        get() = p.getFloat("minRonKm", 2f)
        set(v) = p.edit().putFloat("minRonKm", v).apply()
    var minRonHour: Float
        get() = p.getFloat("minRonHour", 50f)
        set(v) = p.edit().putFloat("minRonHour", v).apply()
    var minProfit: Float
        get() = p.getFloat("minProfit", 5f)
        set(v) = p.edit().putFloat("minProfit", v).apply()
    var minScore: Float
        get() = p.getFloat("minScore", 4.7f)
        set(v) = p.edit().putFloat("minScore", v).apply()
    var speechRate: Float
        get() = p.getFloat("speechRate", 1.0f)
        set(v) = p.edit().putFloat("speechRate", v).apply()
    var voiceVolume: Float
        get() = p.getFloat("voiceVolume", 0.8f)
        set(v) = p.edit().putFloat("voiceVolume", v).apply()
    var voiceEnabled: Boolean
        get() = p.getBoolean("voiceEnabled", true)
        set(v) = p.edit().putBoolean("voiceEnabled", v).apply()
    var voiceOnlyAmount: Boolean
        get() = p.getBoolean("voiceOnlyAmount", true)
        set(v) = p.edit().putBoolean("voiceOnlyAmount", v).apply()
    var hideInWaze: Boolean
        get() = p.getBoolean("hideInWaze", true)
        set(v) = p.edit().putBoolean("hideInWaze", v).apply()
    var resetOnReject: Boolean
        get() = p.getBoolean("resetOnReject", true)
        set(v) = p.edit().putBoolean("resetOnReject", v).apply()
    var pauseOnAccept: Boolean
        get() = p.getBoolean("pauseOnAccept", true)
        set(v) = p.edit().putBoolean("pauseOnAccept", v).apply()
    var reappearOnDriverApp: Boolean
        get() = p.getBoolean("reappearOnDriverApp", true)
        set(v) = p.edit().putBoolean("reappearOnDriverApp", v).apply()
    var vibrateOnOffer: Boolean
        get() = p.getBoolean("vibrateOnOffer", true)
        set(v) = p.edit().putBoolean("vibrateOnOffer", v).apply()
    var overlayX: Int
        get() = p.getInt("overlayX", 20)
        set(v) = p.edit().putInt("overlayX", v).apply()
    var overlayY: Int
        get() = p.getInt("overlayY", 120)
        set(v) = p.edit().putInt("overlayY", v).apply()
}
