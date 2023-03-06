package at.jku.ins.uwb_default_values

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.uwb.*
import com.google.common.primitives.Shorts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
    private val uwbManager = UwbManager.createInstance(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getValuesButton = findViewById<Button>(R.id.get_values_button)
        val communicateButton = findViewById<Button>(R.id.communicate_button)
        val isControllerSwitch = findViewById<Switch>(R.id.is_controller)
        val addressInputField = findViewById<EditText>(R.id.address_input)
        val preambleInputField = findViewById<EditText>(R.id.preamble_input)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.UWB_RANGING
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.UWB_RANGING), 123)

        isControllerSwitch.setOnClickListener {
            preambleInputField.isEnabled = !isControllerSwitch.isChecked
            coroutineScope.cancel()
        }

        getValuesButton.setOnClickListener {
            if (isControllerSwitch.isChecked) {
                /**
                 * CONTROLLER / SERVER
                 */
                val context = this
                coroutineScope.launch {
                    val controllerSessionScope = uwbManager.controllerSessionScope()

                    AlertDialog.Builder(context).setTitle("CONTROLLER / SERVER").setMessage(
                        "Your Address is: ${Shorts.fromByteArray(controllerSessionScope.localAddress.address)}\n\n" +
                                "uwbComplexChannel channel is: ${controllerSessionScope.uwbComplexChannel.channel}\n\n" +
                                "uwbComplexChannel preambleIndex is: ${controllerSessionScope.uwbComplexChannel.preambleIndex}"
                    ).setNeutralButton("OK") { _, _ -> }
                        .create()
                        .show()

                    communicateButton.setOnClickListener {
                        try {
                            val otherSideLocalAddress =
                                Integer.parseInt(addressInputField.text.toString()).toShort()
                            println("Other side address should be: $otherSideLocalAddress")

                            coroutineScope.launch {
                                startRanging(
                                    otherSideLocalAddress,
                                    controllerSessionScope.uwbComplexChannel,
                                    controllerSessionScope
                                )
                            }
                        } catch (_: NumberFormatException) {

                        }
                    }
                }
            } else {
                /**
                 * CONTROLLEE / CLIENT
                 */
                val context = this
                coroutineScope.launch {
                    // Initiate a session that will be valid for a single ranging session.
                    val controleeSessionScope = uwbManager.controleeSessionScope()

                    AlertDialog.Builder(context).setTitle("CONTROLLEE / CLIENT")
                        .setMessage(
                            "Your Address is: ${Shorts.fromByteArray(controleeSessionScope.localAddress.address)}" +
                                    "\n\nYour Device supports Distance: " + controleeSessionScope.rangingCapabilities.isDistanceSupported +
                                    "\n\nYour Device supports Azimuth: " + controleeSessionScope.rangingCapabilities.isAzimuthalAngleSupported +
                                    "\n\nYour Device supports Elevation: " + controleeSessionScope.rangingCapabilities.isElevationAngleSupported
                        )
                        .setNeutralButton("OK") { _, _ -> }.create().show()



                    communicateButton.setOnClickListener {
                        try {
                            val otherSideLocalAddress =
                                Integer.parseInt(addressInputField.text.toString()).toShort()
                            println("Other side address should be: $otherSideLocalAddress")

                            val channelPreamble =
                                Integer.parseInt(preambleInputField.text.toString())
                            println("channel preamble should be: $channelPreamble")

                            coroutineScope.launch {
                                startRanging(
                                    otherSideLocalAddress,
                                    UwbComplexChannel(9, channelPreamble),
                                    controleeSessionScope
                                )
                            }
                        } catch (_: NumberFormatException) {

                        }
                    }
                }
            }
        }
    }

    // The coroutineScope responsible for handling uwb ranging.
    // This will be initialized when startRanging is called.
    private suspend fun startRanging(
        otherSideLocalAddress: Short,
        channel: UwbComplexChannel,
        sessionScope: UwbClientSessionScope
    ) {
        val partnerAddress = UwbAddress(Shorts.toByteArray(otherSideLocalAddress))

        // Create the ranging parameters.
        val partnerParameters = RangingParameters(
            uwbConfigType = RangingParameters.UWB_CONFIG_ID_1,
            sessionId = 12345,
            sessionKeyInfo = null,
            complexChannel = channel,
            peerDevices = listOf(UwbDevice(partnerAddress)),
            updateRateType = RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
        )

        val sessionFlow = sessionScope.prepareSession(partnerParameters)

        // Start a coroutine scope that initiates ranging.
        coroutineScope.launch {
            sessionFlow.collect {
                when (it) {
                    is RangingResult.RangingResultPosition -> {

                        val distanceDisplay = findViewById<TextView>(R.id.distance_display)
                        val elevationDisplay = findViewById<TextView>(R.id.elevation_display)
                        val azimuthDisplay = findViewById<TextView>(R.id.azimuth_display)

                        distanceDisplay.text = ((distanceDisplay.text.toString()
                            .toFloat() + it.position.distance?.value!!) / 2).toString()
                        elevationDisplay.text = ((elevationDisplay.text.toString()
                            .toFloat() + it.position.elevation?.value!!) / 2).toString()
                        azimuthDisplay.text = ((azimuthDisplay.text.toString()
                            .toFloat() + it.position.azimuth?.value!!) / 2).toString()

                        println("distance")
                        println(it.position.distance?.value)
                        println("azimuth")
                        println(it.position.azimuth?.value)
                        println("elevation")
                        println(it.position.elevation?.value)
                    }
                    is RangingResult.RangingResultPeerDisconnected -> {
                        println("CONNECTION LOST")
                    }
                }
            }
        }
    }
}