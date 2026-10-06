package ro.plesiarazvan.profitride.core.calculation

import ro.plesiarazvan.profitride.core.model.*
import kotlin.math.max

object TripCalculator {

    fun calculate(offer: TripOffer, profile: DriverProfile): TripAnalysis {
        val totalKm = offer.pickupDistanceKm + offer.tripDistanceKm
        val totalMin = offer.pickupDurationMin + offer.tripDurationMin

        val grossPerKm = if (totalKm > 0) offer.fare / totalKm else 0.0
        val grossPerHour = if (totalMin > 0) offer.fare / (totalMin / 60.0) else 0.0

        val fuelCost =
            totalKm * (profile.fuelConsumptionPer100 / 100.0) * profile.fuelPrice

        val vehicleCost = totalKm * profile.maintenancePerKm

        val monthlyHours =
            max(1.0, profile.hoursPerDay * profile.daysPerWeek * 4.33)

        val monthlyMinutes = monthlyHours * 60.0
        val safeMonthlyKm = max(1.0, profile.monthlyKm)

        val rentOrRateCost: Double
        val otherFixedCost: Double

        val otherMonthly =
            profile.insuranceMonthly +
            profile.phoneMonthly +
            profile.otherMonthly

        if (profile.fixedAllocationMode.equals("Per oră", ignoreCase = true)) {
            rentOrRateCost =
                profile.rentOrRateMonthly / monthlyMinutes * totalMin
            otherFixedCost =
                otherMonthly / monthlyMinutes * totalMin
        } else {
            rentOrRateCost =
                profile.rentOrRateMonthly / safeMonthlyKm * totalKm
            otherFixedCost =
                otherMonthly / safeMonthlyKm * totalKm
        }

        val totalCost =
            fuelCost + vehicleCost + rentOrRateCost + otherFixedCost

        val netProfit = offer.fare - totalCost

        val profitPerKm =
            if (totalKm > 0) netProfit / totalKm else 0.0

        val profitPerHour =
            if (totalMin > 0) netProfit / (totalMin / 60.0) else 0.0

        val evaluation = evaluate(
            offer = offer,
            profile = profile,
            netProfit = netProfit,
            profitPerKm = profitPerKm,
            profitPerHour = profitPerHour,
            grossPerKm = grossPerKm,
            totalCost = totalCost
        )

        return TripAnalysis(
            grossFare = offer.fare,
            totalDistanceKm = totalKm,
            totalDurationMin = totalMin,
            fuelCost = fuelCost,
            vehicleCost = vehicleCost,
            rentOrRateCost = rentOrRateCost,
            otherFixedCost = otherFixedCost,
            totalCost = totalCost,
            netProfit = netProfit,
            grossPerKm = grossPerKm,
            grossPerHour = grossPerHour,
            profitPerKm = profitPerKm,
            profitPerHour = profitPerHour,
            recommendation = evaluation.first,
            recommendationReason = evaluation.second,
            contextualTip = contextualTip(
                offer,
                profile,
                netProfit,
                profitPerKm,
                profitPerHour,
                grossPerKm,
                totalCost
            )
        )
    }

    private fun evaluate(
        offer: TripOffer,
        profile: DriverProfile,
        netProfit: Double,
        profitPerKm: Double,
        profitPerHour: Double,
        grossPerKm: Double,
        totalCost: Double
    ): Pair<RecommendationLevel, String> {

        if (netProfit <= 0.0 || totalCost >= offer.fare * 0.95) {
            return RecommendationLevel.RESPINGE to
                "Costurile consumă aproape toată cursa."
        }

        val checks = listOf(
            netProfit / max(0.01, profile.minProfitPerTrip),
            profitPerKm / max(0.01, profile.minProfitPerKm),
            profitPerHour / max(0.01, profile.minProfitPerHour),
            grossPerKm / max(0.01, profile.minGrossPerKm),
            profile.maxPickupKm / max(0.01, offer.pickupDistanceKm),
            profile.maxPickupMinutes.toDouble() / max(1.0, offer.pickupDurationMin.toDouble())
        )

        val minRatio = checks.minOrNull() ?: 0.0

        val reason = when {
            offer.pickupDistanceKm > profile.maxPickupKm ->
                "Prea mulți km până la client."
            offer.pickupDurationMin > profile.maxPickupMinutes ->
                "Timpul până la client depășește limita ta."
            profitPerHour < profile.minProfitPerHour ->
                "Profit/oră sub limita ta."
            profitPerKm < profile.minProfitPerKm ->
                "Profit mic raportat la distanță."
            grossPerKm < profile.minGrossPerKm ->
                "Plata/km brută este sub pragul tău."
            else ->
                "Profit bun pentru timpul necesar."
        }

        return when {
            minRatio >= 1.0 ->
                RecommendationLevel.ACCEPTA to reason
            minRatio >= 0.80 ->
                RecommendationLevel.ACCEPTABIL to reason
            minRatio >= 0.45 ->
                RecommendationLevel.SLABA to reason
            else ->
                RecommendationLevel.RESPINGE to reason
        }
    }

    private fun contextualTip(
        offer: TripOffer,
        profile: DriverProfile,
        netProfit: Double,
        profitPerKm: Double,
        profitPerHour: Double,
        grossPerKm: Double,
        totalCost: Double
    ): String {
        return when {
            offer.pickupDistanceKm > profile.maxPickupKm * 1.25 ->
                "Pickup mare pentru valoarea cursei."
            offer.pickupDistanceKm > profile.maxPickupKm &&
                offer.tripDistanceKm >= offer.pickupDistanceKm * 2 ->
                "Direcția compensează parțial pickup-ul."
            netProfit <= profile.minProfitPerTrip * 0.5 ->
                "Profit mic după costurile mașinii."
            totalCost >= offer.fare * 0.70 ->
                "Costurile consumă mult din încasare."
            profitPerHour >= profile.minProfitPerHour * 1.25 &&
                profitPerKm >= profile.minProfitPerKm * 1.20 ->
                "Profit bun raportat la timp."
            grossPerKm < profile.minGrossPerKm ->
                "Cursa este bună dacă direcția te avantajează."
            else ->
                "Oferta este aproape de pragurile tale."
        }
    }
}
