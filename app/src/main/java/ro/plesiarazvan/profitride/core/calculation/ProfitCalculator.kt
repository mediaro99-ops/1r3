package ro.plesiarazvan.profitride.core.calculation

import ro.plesiarazvan.profitride.core.model.ProfitResult
import ro.plesiarazvan.profitride.core.model.RideOffer
import kotlin.math.max

object ProfitCalculator {
    fun calculate(
        offer: RideOffer,
        fuelPrice: Double,
        fuelConsumptionPer100: Double,
        monthlyFixedCosts: Double,
        workingHoursPerDay: Double,
        workingDaysPerWeek: Double
    ): ProfitResult {
        val totalKm = offer.pickupDistanceKm + offer.tripDistanceKm
        val totalMinutes = offer.pickupTimeMinutes + offer.tripTimeMinutes
        val ronPerKm = if (totalKm > 0) offer.fare / totalKm else 0.0
        val ronPerHour = if (totalMinutes > 0) offer.fare / (totalMinutes / 60.0) else 0.0
        val fuelCost = totalKm * (fuelConsumptionPer100 / 100.0) * fuelPrice

        val monthlyWorkHours = max(1.0, workingHoursPerDay * workingDaysPerWeek * 4.33)
        val fixedPerMinute = monthlyFixedCosts / (monthlyWorkHours * 60.0)
        val proportionalFixedCosts = fixedPerMinute * totalMinutes
        val profit = offer.fare - fuelCost - proportionalFixedCosts

        return ProfitResult(
            totalKm = totalKm,
            totalMinutes = totalMinutes,
            ronPerKm = ronPerKm,
            ronPerHour = ronPerHour,
            fuelCost = fuelCost,
            proportionalFixedCosts = proportionalFixedCosts,
            profit = profit
        )
    }
}
