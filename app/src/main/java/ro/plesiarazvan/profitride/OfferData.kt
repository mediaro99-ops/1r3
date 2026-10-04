package ro.plesiarazvan.profitride

data class OfferData(
    val amount: Double,
    val pickupMin: Double,
    val pickupKm: Double,
    val rideMin: Double,
    val rideKm: Double,
) {
    val totalMin get() = pickupMin + rideMin
    val totalKm get() = pickupKm + rideKm
}

data class OfferMetrics(
    val netPerKm: Double,
    val netPerHour: Double,
    val totalCost: Double,
    val profit: Double,
    val score: Double,
    val worthIt: Boolean
)
