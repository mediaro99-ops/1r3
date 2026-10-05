package ro.plesiarazvan.profitride.rideprovider.uber
import ro.plesiarazvan.profitride.core.model.RideOffer
import ro.plesiarazvan.profitride.rideprovider.common.RideProvider

class UberRideProvider : RideProvider {
    override val id = "uber"
    override fun normalize(rawText: String): RideOffer? = null // integrarea live OCR se conectează în etapa provider.
}
