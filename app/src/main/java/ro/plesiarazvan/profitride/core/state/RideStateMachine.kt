package ro.plesiarazvan.profitride.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RideState {
    APP_STOPPED, IDLE, WAITING_FOR_OFFER, OFFER_DETECTED, ANALYZING,
    GOOD_OFFER, BAD_OFFER, OFFER_VISIBLE, OFFER_ACCEPTED, PAUSED,
    NAVIGATION_WAZE, TRIP_ACTIVE, RETURNING_TO_PROVIDER, TRIP_FINISHED,
    OFFER_REJECTED, RESETTING, ERROR
}

class RideStateMachine {
    private val _state = MutableStateFlow(RideState.APP_STOPPED)
    val state: StateFlow<RideState> = _state

    fun set(newState: RideState) { _state.value = newState }
}
