package ro.plesiarazvan.profitride.rideprovider.bolt
import ro.plesiarazvan.profitride.core.model.RideOffer
import ro.plesiarazvan.profitride.rideprovider.common.RideProvider

class BoltRideProvider : RideProvider {
    override val id = "bolt"
    override fun normalize(rawText: String): RideOffer? = null // integrarea live OCR se conectează în etapa provider.
}
