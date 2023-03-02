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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getValuesButton = findViewById<Button>(R.id.get_values_button)
        val communicateButton = findViewById<Button>(R.id.communicate_button)
        val isControllerSwitch = findViewById<Switch>(R.id.is_controller)
        val addressInputField = findViewById<EditText>(R.id.address_input)
        val preambleInputField = findViewById<EditText>(R.id.preamble_input)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.UWB_RANGING
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.UWB_RANGING), 123)

        isControllerSwitch.setOnClickListener {
            preambleInputField.isEnabled = !isControllerSwitch.isChecked
        }

        getValuesButton.setOnClickListener {
            if (isControllerSwitch.isChecked) {
                /**
                 * CONTROLLER / SERVER
                 */
                val scope = CoroutineScope(Dispatchers.Main)
                val context = this
                scope.launch {
                    val uwbManager = UwbManager.createInstance(context)
                    val controllerSessionScope = uwbManager.controllerSessionScope()

                    AlertDialog.Builder(context).setTitle("CONTROLLER / SERVER").setMessage(
                        "Your Address is: ${Shorts.fromByteArray(controllerSessionScope.localAddress.address)}\n\n" +
                                "uwbComplexChannel channel is: ${controllerSessionScope.uwbComplexChannel.channel}\n\n" +
                                "uwbComplexChannel preambleIndex is: ${controllerSessionScope.uwbComplexChannel.preambleIndex}"
                    ).setNeutralButton("OK", { dialog, which -> })
                        .create()
                        .show()

                    communicateButton.setOnClickListener {
                        try {
                            val otherSideLocalAddress =
                                Integer.parseInt(addressInputField.text.toString()).toShort()
                            println("Other side address should be: $otherSideLocalAddress")

                            val scope2 = CoroutineScope(Dispatchers.Main)
                            scope2.launch {
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
                val scope = CoroutineScope(Dispatchers.Main)
                val context = this
                scope.launch {
                    val uwbManager = UwbManager.createInstance(context)
                    // Initiate a session that will be valid for a single ranging session.
                    val controleeSessionScope = uwbManager.controleeSessionScope()

                    AlertDialog.Builder(context).setTitle("CONTROLLEE / CLIENT")
                        .setMessage(
                            "Your Address is: ${Shorts.fromByteArray(controleeSessionScope.localAddress.address)}" +
                                    "\n\nYour Device supports Distance: " + controleeSessionScope.rangingCapabilities.isDistanceSupported +
                                    "\n\nYour Device supports Azimuth: " + controleeSessionScope.rangingCapabilities.isAzimuthalAngleSupported +
                                    "\n\nYour Device supports Elevation: " + controleeSessionScope.rangingCapabilities.isElevationAngleSupported
                        )
                        .setNeutralButton("OK", { dialog, which -> }).create().show()



                    communicateButton.setOnClickListener {
                        try {
                            val otherSideLocalAddress =
                                Integer.parseInt(addressInputField.text.toString()).toShort()
                            println("Other side address should be: $otherSideLocalAddress")

                            val channelPreamble =
                                Integer.parseInt(preambleInputField.text.toString())
                            println("channel preamble should be: $channelPreamble")

                            val scope2 = CoroutineScope(Dispatchers.Main)
                            scope2.launch {
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
    suspend fun startRanging(
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
        CoroutineScope(Dispatchers.Main.immediate).launch {
            sessionFlow.collect {
                when (it) {
                    is RangingResult.RangingResultPosition -> {

                        val distance_display = findViewById<TextView>(R.id.distance_display)
                        val elevation_display = findViewById<TextView>(R.id.elevation_display)
                        val azimuth_display = findViewById<TextView>(R.id.azimuth_display)

                        distance_display.text=((distance_display.text.toString().toFloat()+ it.position.distance?.value!!)/2).toString()
                        elevation_display.text=((elevation_display.text.toString().toFloat()+ it.position.elevation?.value!!)/2).toString()
                        azimuth_display.text=((azimuth_display.text.toString().toFloat()+ it.position.azimuth?.value!!)/2).toString()

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