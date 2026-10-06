package ro.plesiarazvan.profitride.rideprovider.common
import ro.plesiarazvan.profitride.core.model.RideOffer

interface RideProvider {
    val id: String
    fun normalize(rawText: String): RideOffer?
}
