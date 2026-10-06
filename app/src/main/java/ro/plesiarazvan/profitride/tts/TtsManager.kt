package ro.plesiarazvan.profitride.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) {
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) tts.language = Locale("ro", "RO")
        }
    }

    fun speakAmount(amount: Double, volume: Float = 0.7f, rate: Float = 1.0f) {
        if (!ready) return
        tts.setSpeechRate(rate)
        val lei = amount.toInt()
        val bani = (((amount - lei) * 100.0) + 0.5).toInt().coerceAtLeast(0)
        val text = if (bani > 0) "$lei lei și $bani" else "$lei lei"
        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "profitride_amount")
    }

    fun shutdown() {
        if (::tts.isInitialized) tts.shutdown()
    }
}
