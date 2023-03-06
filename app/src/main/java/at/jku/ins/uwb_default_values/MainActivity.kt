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
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbManager
import com.google.common.primitives.Shorts
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val uwbManager = UwbManager.createInstance(this)

    @OptIn(DelicateCoroutinesApi::class)
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


        val context = this

        isControllerSwitch.setOnClickListener {
            preambleInputField.isEnabled = !isControllerSwitch.isChecked
            try {
                GlobalScope.cancel()
                println("Cancelled a job")
            } catch (e: IllegalStateException) {
                println(e)
            }
        }

        CoroutineScope(Dispatchers.Main).launch{
            val controllerSessionScope = uwbManager.controllerSessionScope()
            val controlleeSessionScope = uwbManager.controleeSessionScope()

            getValuesButton.setOnClickListener {
                if (isControllerSwitch.isChecked) {
                    AlertDialog
                        .Builder(context)
                        .setTitle("CONTROLLER / SERVER")
                        .setMessage(
                            "Your Address is: ${Shorts.fromByteArray(controllerSessionScope.localAddress.address)}\n\n" +
                                    "uwbComplexChannel channel is: ${controllerSessionScope.uwbComplexChannel.channel}\n\n" +
                                    "uwbComplexChannel preambleIndex is: ${controllerSessionScope.uwbComplexChannel.preambleIndex}"
                        )
                        .setNeutralButton("OK") { _, _ -> }
                        .create()
                        .show()
                } else {
                    AlertDialog
                        .Builder(context)
                        .setTitle("CONTROLLEE / CLIENT")
                        .setMessage(
                            "Your Address is: ${Shorts.fromByteArray(controlleeSessionScope.localAddress.address)}" +
                                    "\n\nYour Device supports Distance: " + controlleeSessionScope.rangingCapabilities.isDistanceSupported +
                                    "\n\nYour Device supports Azimuth: " + controlleeSessionScope.rangingCapabilities.isAzimuthalAngleSupported +
                                    "\n\nYour Device supports Elevation: " + controlleeSessionScope.rangingCapabilities.isElevationAngleSupported
                        )
                        .setNeutralButton("OK") { _, _ -> }
                        .create()
                        .show()
                }
            }

            communicateButton.setOnClickListener {
                if (isControllerSwitch.isChecked) {
                    /**
                     * CONTROLLER / SERVER
                     */
                    try {
                        val otherSideLocalAddress =
                            Integer.parseInt(addressInputField.text.toString()).toShort()
                        println("Other side address should be: $otherSideLocalAddress")

                        startRanging(
                            otherSideLocalAddress,
                            controllerSessionScope.uwbComplexChannel,
                            controllerSessionScope
                        ) {
                            val distanceDisplay =
                                findViewById<TextView>(R.id.distance_display)
                            val elevationDisplay =
                                findViewById<TextView>(R.id.elevation_display)
                            val azimuthDisplay =
                                findViewById<TextView>(R.id.azimuth_display)

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
                    } catch (e: NumberFormatException) {
                        println("Caught Exception")
                        println(e)
                    }
                } else {
                    /**
                     * CONTROLLEE / CLIENT
                     */
                    try {
                        val otherSideLocalAddress =
                            Integer.parseInt(addressInputField.text.toString()).toShort()
                        println("Other side address should be: $otherSideLocalAddress")

                        val channelPreamble =
                            Integer.parseInt(preambleInputField.text.toString())
                        println("channel preamble should be: $channelPreamble")

                        startRanging(
                            otherSideLocalAddress,
                            UwbComplexChannel(9, channelPreamble),
                            controlleeSessionScope
                        ) {
                            val distanceDisplay =
                                findViewById<TextView>(R.id.distance_display)
                            val elevationDisplay =
                                findViewById<TextView>(R.id.elevation_display)
                            val azimuthDisplay =
                                findViewById<TextView>(R.id.azimuth_display)

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

                    } catch (e: NumberFormatException) {
                        println("Caught Exception")
                        println(e)
                    }
                }
            }
        }
    }


}
