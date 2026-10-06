package ro.plesiarazvan.profitride.overlay

enum class OverlayMode {
    WAITING,
    OFFER_READY
}

enum class OverlayDisplayMode {
    MINIMIZED,
    COMPACT,
    EXPANDED
}

data class OverlayUiState(
    val mode: OverlayMode = OverlayMode.WAITING,
    val platform: String = "",
    val paymentType: String = "",
    val fare: Double = 0.0,
    val pickupMinutes: Int = 0,
    val pickupKm: Double = 0.0,
    val tripMinutes: Int = 0,
    val tripKm: Double = 0.0,
    val grossPerKm: Double = 0.0,
    val grossPerHour: Double = 0.0,
    val profitPerKm: Double = 0.0,
    val profitPerHour: Double = 0.0,
    val fuelCost: Double = 0.0,
    val maintenanceCost: Double = 0.0,
    val rentRateCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val recommendation: String = "",
    val tip: String = ""
) {
    val totalMinutes: Int
        get() = pickupMinutes + tripMinutes

    val totalDistanceKm: Double
        get() = pickupKm + tripKm
}
