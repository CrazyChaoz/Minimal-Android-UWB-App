package at.jku.ins.uwb_default_values

import androidx.core.uwb.*
import com.google.common.primitives.Shorts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


// The coroutineScope responsible for handling uwb ranging.
// This will be initialized when startRanging is called.
@OptIn(DelicateCoroutinesApi::class)
fun startRanging(
    otherSideLocalAddress: Short,
    uwbChannel: UwbComplexChannel,
    uwbSessionScope: UwbClientSessionScope,
    uwbRangingResultCallback: (it: RangingResult.RangingResultPosition) -> Unit
) {
    val partnerAddress = UwbAddress(Shorts.toByteArray(otherSideLocalAddress))

    // Create the ranging parameters.
    val partnerParameters = RangingParameters(
        uwbConfigType = RangingParameters.UWB_CONFIG_ID_1,
        sessionId = 12345,
        sessionKeyInfo = null,
        complexChannel = uwbChannel,
        peerDevices = listOf(UwbDevice(partnerAddress)),
        updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
    )

    val sessionFlow = uwbSessionScope.prepareSession(partnerParameters)

    // Start a coroutine scope that initiates ranging.
    GlobalScope.launch {
        println("Launched Coroutine")
        sessionFlow.collect {
            when (it) {
                is RangingResult.RangingResultPosition -> {
                    uwbRangingResultCallback.invoke(it)
                }
                is RangingResult.RangingResultPeerDisconnected -> {
                    println("CONNECTION LOST")
                }
            }
        }
    }
}