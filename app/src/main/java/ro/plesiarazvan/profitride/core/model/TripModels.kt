package ro.plesiarazvan.profitride.core.model

data class TripOffer(
    val platform: String,
    val fare: Double,
    val pickupDistanceKm: Double,
    val pickupDurationMin: Int,
    val tripDistanceKm: Double,
    val tripDurationMin: Int,
    val paymentType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RecommendationLevel {
    ACCEPTA, ACCEPTABIL, SLABA, RESPINGE
}

data class DriverProfile(
    val fuelType: String,
    val fuelPrice: Double,
    val fuelConsumptionPer100: Double,
    val maintenancePerKm: Double,
    val rentOrRateMonthly: Double,
    val insuranceMonthly: Double,
    val phoneMonthly: Double,
    val otherMonthly: Double,
    val fixedAllocationMode: String,
    val monthlyKm: Double,
    val hoursPerDay: Double,
    val daysPerWeek: Double,
    val minProfitPerTrip: Double,
    val minProfitPerKm: Double,
    val minProfitPerHour: Double,
    val minGrossPerKm: Double,
    val maxPickupKm: Double,
    val maxPickupMinutes: Int
)

data class TripAnalysis(
    val grossFare: Double,
    val totalDistanceKm: Double,
    val totalDurationMin: Int,
    val fuelCost: Double,
    val vehicleCost: Double,
    val rentOrRateCost: Double,
    val otherFixedCost: Double,
    val totalCost: Double,
    val netProfit: Double,
    val grossPerKm: Double,
    val grossPerHour: Double,
    val profitPerKm: Double,
    val profitPerHour: Double,
    val recommendation: RecommendationLevel,
    val recommendationReason: String,
    val contextualTip: String
)
