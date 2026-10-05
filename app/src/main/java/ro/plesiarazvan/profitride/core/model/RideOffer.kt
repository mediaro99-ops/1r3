package ro.plesiarazvan.profitride.core.model

enum class Provider { BOLT, UBER }

data class RideOffer(
    val id: String,
    val provider: Provider,
    val fare: Double,
    val pickupDistanceKm: Double,
    val pickupTimeMinutes: Int,
    val tripDistanceKm: Double,
    val tripTimeMinutes: Int,
    val rating: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProfitResult(
    val totalKm: Double,
    val totalMinutes: Int,
    val ronPerKm: Double,
    val ronPerHour: Double,
    val fuelCost: Double,
    val proportionalFixedCosts: Double,
    val profit: Double
)

data class UserThresholds(
    val minRonKm: Double = 2.0,
    val minRonHour: Double = 50.0,
    val minProfit: Double = 5.0,
    val rideScoreEnabled: Boolean = false,
    val minRideScore: Double = 4.7
)

enum class Evaluation { GOOD, BAD, NEUTRAL }
