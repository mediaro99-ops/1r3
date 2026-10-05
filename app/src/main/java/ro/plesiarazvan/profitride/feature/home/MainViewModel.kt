package ro.plesiarazvan.profitride.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.plesiarazvan.profitride.core.storage.SettingsRepository

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val repo = SettingsRepository(app)
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ro.plesiarazvan.profitride.core.storage.AppSettings())

    fun setBool(key: String, value: Boolean) = viewModelScope.launch { repo.updateBoolean(key, value) }
    fun setDouble(key: String, value: Double) = viewModelScope.launch { repo.updateDouble(key, value) }
    fun setString(key: String, value: String) = viewModelScope.launch { repo.updateString(key, value) }
    fun setVoice(volume: Float, rate: Float) = viewModelScope.launch { repo.updateVoice(volume, rate) }
}
