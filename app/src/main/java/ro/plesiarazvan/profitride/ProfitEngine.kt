package ro.plesiarazvan.profitride

import android.content.Context
import kotlin.math.max
import kotlin.math.min

object ProfitEngine {
    fun calculate(context: Context, offer: OfferData): OfferMetrics {
        val s = SettingsStore(context)
        val fuelPerKm = (s.consumption / 100.0) * s.fuelPrice
        val variablePerKm = fuelPerKm + s.maintenancePerKm
        val monthlyKm = max(1.0, s.weeklyKm * 4.345)
        val fixedPerKm = s.fixedTotal / monthlyKm
        val totalCostPerKm = variablePerKm + fixedPerKm
        val totalCost = totalCostPerKm * offer.totalKm
        val profit = offer.amount - totalCost
        val netPerKm = if (offer.totalKm > 0) profit / offer.totalKm else 0.0
        val netPerHour = if (offer.totalMin > 0) profit / offer.totalMin * 60.0 else 0.0

        val kmScore = min(5.0, max(0.0, netPerKm / max(0.1, s.minRonKm.toDouble()) * 4.0))
        val hourScore = min(5.0, max(0.0, netPerHour / max(1.0, s.minRonHour.toDouble()) * 4.0))
        val profitScore = min(5.0, max(0.0, profit / max(1.0, s.minProfit.toDouble()) * 4.0))
        val score = (kmScore * 0.4 + hourScore * 0.35 + profitScore * 0.25)

        val worth = netPerKm >= s.minRonKm && netPerHour >= s.minRonHour && profit >= s.minProfit && score >= s.minScore
        return OfferMetrics(netPerKm, netPerHour, totalCost, profit, score, worth)
    }
}
