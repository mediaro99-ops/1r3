package ro.plesiarazvan.profitride

object AppState {
    @Volatile var overlayService: CaptureOverlayService? = null
    @Volatile var pausedForRide: Boolean = false
    @Volatile var foregroundPackage: String = ""
}
