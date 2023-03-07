package at.jku.ins.uwb_default_values;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.uwb.RangingParameters;
import androidx.core.uwb.RangingResult;
import androidx.core.uwb.UwbAddress;
import androidx.core.uwb.UwbClientSessionScope;
import androidx.core.uwb.UwbComplexChannel;
import androidx.core.uwb.UwbControleeSessionScope;
import androidx.core.uwb.UwbControllerSessionScope;
import androidx.core.uwb.UwbDevice;
import androidx.core.uwb.UwbManager;
import androidx.core.uwb.rxjava3.UwbClientSessionScopeRx;
import androidx.core.uwb.rxjava3.UwbManagerRx;

import com.google.common.primitives.Shorts;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.UWB_RANGING) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.UWB_RANGING}, 123);
        }

        UwbManager uwbManager = UwbManager.createInstance(this);
        AtomicReference<Disposable> rangingResultObservable = new AtomicReference<>(null);
        AtomicInteger lastControlee = new AtomicInteger(0);

        Button getValuesButton = findViewById(R.id.get_values_button);
        Button communicateButton = findViewById(R.id.communicate_button);
        Switch isControllerSwitch = findViewById(R.id.is_controller);
        EditText addressInputField = findViewById(R.id.address_input);
        EditText preambleInputField = findViewById(R.id.preamble_input);
        TextView distanceDisplay = findViewById(R.id.distance_display);
        TextView elevationDisplay = findViewById(R.id.elevation_display);
        TextView azimuthDisplay = findViewById(R.id.azimuth_display);


        new Thread(() -> {
            AtomicReference<UwbClientSessionScope> currentUwbSessionScope = new AtomicReference<>(UwbManagerRx.controleeSessionScopeSingle(uwbManager).blockingGet());

            isControllerSwitch.setOnClickListener(v -> {
                preambleInputField.setEnabled(!isControllerSwitch.isChecked());

                if (rangingResultObservable.get() != null) {
                    rangingResultObservable.get().dispose();
                    rangingResultObservable.set(null);
                }

                if (isControllerSwitch.isChecked()) {
                    /**
                     * CONTROLLER / SERVER
                     */
                    currentUwbSessionScope.set(UwbManagerRx.controllerSessionScopeSingle(uwbManager).blockingGet());
                } else {
                    /**
                     * CONTROLLEE / CLIENT
                     */
                    currentUwbSessionScope.set(UwbManagerRx.controleeSessionScopeSingle(uwbManager).blockingGet());
                }
            });


            getValuesButton.setOnClickListener((view) -> {
                if (isControllerSwitch.isChecked()) {
                    /**
                     * CONTROLLER / SERVER
                     */
                    UwbControllerSessionScope controllerSessionScope = (UwbControllerSessionScope) currentUwbSessionScope.get();
                    new AlertDialog.Builder(view.getContext()).setTitle("CONTROLLER / SERVER").setMessage("Your Address is: " + Shorts.fromByteArray(controllerSessionScope.getLocalAddress().getAddress()) + "\nuwbComplexChannel channel is: " + controllerSessionScope.getUwbComplexChannel().getChannel() + "\nuwbComplexChannel preambleIndex is: " + controllerSessionScope.getUwbComplexChannel().getPreambleIndex()).setNeutralButton("OK", (a, b) -> {
                    }).create().show();
                } else {
                    /**
                     * CONTROLLEE / CLIENT
                     */
                    UwbControleeSessionScope controleeSessionScope = (UwbControleeSessionScope) currentUwbSessionScope.get();
                    new AlertDialog.Builder(view.getContext()).setTitle("CONTROLLEE / CLIENT").setMessage("Your Address is: " + Shorts.fromByteArray(controleeSessionScope.getLocalAddress().getAddress()) + "\nYour Device supports Distance: " + controleeSessionScope.getRangingCapabilities().isDistanceSupported() + "\nYour Device supports Azimuth: " + controleeSessionScope.getRangingCapabilities().isAzimuthalAngleSupported() + "\nYour Device supports Elevation: " + controleeSessionScope.getRangingCapabilities().isElevationAngleSupported()).setNeutralButton("OK", (a, b) -> {
                    }).create().show();
                }
            });
            communicateButton.setOnClickListener((view -> {
                try {
                    int otherSideLocalAddress = Integer.parseInt(addressInputField.getText().toString());
                    UwbAddress partnerAddress = new UwbAddress(Shorts.toByteArray((short) otherSideLocalAddress));
                    UwbComplexChannel uwbComplexChannel = null;

                    if (isControllerSwitch.isChecked()) {
                        /**
                         * CONTROLLER / SERVER
                         */
                        uwbComplexChannel = ((UwbControllerSessionScope)currentUwbSessionScope.get()).getUwbComplexChannel();
                        if(rangingResultObservable.get()!=null){
                            if(lastControlee.intValue()!=otherSideLocalAddress){
                                //Here you would additional Controlees, if that were possible

                                //controllerSessionScope.addControlee(partnerAddress);
                                //return;
                            }
                        }
                    } else {
                        /**
                         * CONTROLLEE / CLIENT
                         */
                        int channelPreamble = Integer.parseInt(preambleInputField.getText().toString());
                        uwbComplexChannel = new UwbComplexChannel(9, channelPreamble);
                    }

                    RangingParameters partnerParameters = new RangingParameters(RangingParameters.UWB_CONFIG_ID_1, 12345, null, uwbComplexChannel, Collections.singletonList(new UwbDevice(partnerAddress)), RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC);
                    rangingResultObservable.set(UwbClientSessionScopeRx.rangingResultsObservable(currentUwbSessionScope.get(), partnerParameters).subscribe(rangingResult -> {
                                if (rangingResult instanceof RangingResult.RangingResultPosition) {
                                    RangingResult.RangingResultPosition rangingResultPosition = (RangingResult.RangingResultPosition) rangingResult;
                                    if (rangingResultPosition.getPosition().getDistance() != null) {
                                        distanceDisplay.setText(String.valueOf((Float.parseFloat(distanceDisplay.getText().toString()) + rangingResultPosition.getPosition().getDistance().getValue()) / 2));
                                        System.out.println("Distance: " + rangingResultPosition.getPosition().getDistance().getValue());
                                    }
                                    if (rangingResultPosition.getPosition().getAzimuth() != null) {
                                        azimuthDisplay.setText(String.valueOf((Float.parseFloat(azimuthDisplay.getText().toString()) + rangingResultPosition.getPosition().getAzimuth().getValue()) / 2));
                                        System.out.println("Azimuth: " + rangingResultPosition.getPosition().getAzimuth().getValue());
                                    }
                                    if (rangingResultPosition.getPosition().getElevation() != null) {
                                        elevationDisplay.setText(String.valueOf((Float.parseFloat(elevationDisplay.getText().toString()) + rangingResultPosition.getPosition().getElevation().getValue()) / 2));
                                        System.out.println("Elevation: " + rangingResultPosition.getPosition().getElevation().getValue());
                                    }
                                } else {
                                    System.out.println("CONNECTION LOST");
                                }
                            }, // onNext
                            System.out::println, // onError
                            () -> {
                                System.out.println("Completed the observing of RangingResults");
                            } //onCompleted
                    ));

                } catch (NumberFormatException e) {
                    System.out.println("Caught Exception: " + e);
                }
            }));
        }).start();
    }
}