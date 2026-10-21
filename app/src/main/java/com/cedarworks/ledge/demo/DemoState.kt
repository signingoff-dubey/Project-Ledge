package com.cedarworks.ledge.demo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

@Stable
class DemoState(
    wifi: Boolean = true,
    bluetooth: Boolean = true,
    airplane: Boolean = false,
    flashlight: Boolean = false,
    inCall: Boolean = false,
    muted: Boolean = false,
    playing: Boolean = true,
    repeating: Boolean = false,
    focus: Boolean = false,
    listening: Boolean = false,
    favorite: Boolean = false,
) {
    var wifi by mutableStateOf(wifi)
    var bluetooth by mutableStateOf(bluetooth)
    var airplane by mutableStateOf(airplane)
    var flashlight by mutableStateOf(flashlight)
    var inCall by mutableStateOf(inCall)
    var muted by mutableStateOf(muted)
    var playing by mutableStateOf(playing)
    var repeating by mutableStateOf(repeating)
    var focus by mutableStateOf(focus)
    var listening by mutableStateOf(listening)
    var favorite by mutableStateOf(favorite)

    fun updateWifi(enabled: Boolean) {
        wifi = enabled
        if (enabled) airplane = false
    }

    fun updateBluetooth(enabled: Boolean) {
        bluetooth = enabled
        if (enabled) airplane = false
    }

    fun updateAirplane(enabled: Boolean) {
        airplane = enabled
        if (enabled) {
            wifi = false
            bluetooth = false
        }
    }

    companion object {
        val Saver = listSaver<DemoState, Boolean>(
            save = { state ->
                listOf(
                    state.wifi,
                    state.bluetooth,
                    state.airplane,
                    state.flashlight,
                    state.inCall,
                    state.muted,
                    state.playing,
                    state.repeating,
                    state.focus,
                    state.listening,
                    state.favorite,
                )
            },
            restore = { values ->
                DemoState(
                    wifi = values[0],
                    bluetooth = values[1],
                    airplane = values[2],
                    flashlight = values[3],
                    inCall = values[4],
                    muted = values[5],
                    playing = values[6],
                    repeating = values[7],
                    focus = values[8],
                    listening = values[9],
                    favorite = values[10],
                )
            },
        )
    }
}
