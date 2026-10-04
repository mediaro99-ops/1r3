package ro.plesiarazvan.profitride

object BoltOfferParser {
    private val amountRegex = Regex("""(?i)(\d{1,4}[,.]\d{1,2}|\d{1,4})\s*(lei|ron)""")
    private val minKmRegex = Regex("""(?i)(\d{1,3})\s*min\s*[•·\-]?\s*(\d{1,3}(?:[,.]\d{1,2})?)\s*km""")

    fun parse(text: String): OfferData? {
        val normalized = text.replace("\n", " ").replace("  ", " ")
        val amounts = amountRegex.findAll(normalized).mapNotNull {
            it.groupValues[1].replace(',', '.').toDoubleOrNull()
        }.filter { it > 0.5 && it < 2000 }.toList()
        val pairs = minKmRegex.findAll(normalized).mapNotNull {
            val m = it.groupValues[1].toDoubleOrNull()
            val km = it.groupValues[2].replace(',', '.').toDoubleOrNull()
            if (m != null && km != null) m to km else null
        }.toList()

        if (amounts.isEmpty() || pairs.size < 2) return null
        val amount = amounts.first()
        return OfferData(amount, pairs[0].first, pairs[0].second, pairs[1].first, pairs[1].second)
    }
}
