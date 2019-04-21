package com.example.mbientlab.showertunes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
//import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.Temperature;

//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;

import bolts.Continuation;


import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Debug;
public class MainActivity extends Activity implements ServiceConnection {

    private static final String TAG = "MainActivity";
    private final String META_ADDR = "F7:02:E6:49:04:AF";
    private final String SPEAKER_ADDR = "00:58:02:A8:02:44";

    private ImageView Metawear;
    private ImageView BluetoothSpeaker;
    private ImageView AlbumArt;
    private TextView MusicText;

    private MediaPlayer mediaPlayer;
    private BtleService.LocalBinder serviceBinder;
    private BluetoothAdapter mBluetoothAdapter;
    private MetaWearBoard board;
    private Accelerometer accelerometer; //TODO: Get rid of
    private Temperature temperature;

    private boolean btActive;
    private boolean btSpeakerConnect;
    private boolean metaConnect;

    private Logging logging;
    private Debug debug;

    /** onCreate
     * Set up objects for components of app. Set alpha of the metawear icon and speaker icon at low opacity.
     * Get blueTooth adapter
     * At startup, check if Bluetooth is on on the android phone. Display appropriate message.
     * If status of Bluetooth changes, use bluetoothReceiver to listen and change text as appropriate
     * Have Filter for listening for bluetooth speaker
     * Create ArrayList for pairedDevices and look for specific Bluetooth speaker via its MAC address
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Bind UI elements
        Metawear = (ImageView) findViewById(R.id.Metawear);
        BluetoothSpeaker = (ImageView) findViewById(R.id.BluetoothSpeaker);
        AlbumArt = (ImageView) findViewById(R.id.AlbumArt);
        MusicText = (TextView) findViewById(R.id.MusicText);

        // Bind media player
        mediaPlayer = mediaPlayer.create(getApplicationContext(), R.raw.song); // TODO: Black Betty by Ram Jam

        // Set opacity for both speaker and metawear icons to low
        BluetoothSpeaker.setAlpha(0.1f);
        Metawear.setAlpha(0.1f);

        // get bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Relevant when first starting the application
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d("BluetoothAdapter", "A bluetooth adapter isn't available.");
            btActive = false;
        } else if (!bluetoothAdapter.isEnabled()) {
            Log.d("BluetoothAdapter", "Bluetooth is not currently enabled");
            MusicText.setText("Bluetooth Disabled\nPlease turn on");
            btActive = false;
        } else {
            Log.d("BluetoothAdapter", "Bluetooth enabled. Continuing...");
            MusicText.setText("Connecting Devices...");
            btActive = true;

            // Check if a headset is connected
            btSpeakerConnect = bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
        }

        // Below is code for checking state changes to Bluetooth (enable/disable)
        IntentFilter changeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, changeFilter);

        // Have filter for connected bluetooth devices and receiver to help look for MAC address of desired speaker
        IntentFilter pairedBlueFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        pairedBlueFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(deviceReceiver, pairedBlueFilter);

        // Metawear API
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);

        // Now that everything is ready, check if everything is active
        checkDependencies();
    }

    /** onDestroy
     * Unbind services, unregister receivers, and release the mediaPlayer
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(deviceReceiver);
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    /** onServiceConnected
     * Binding for metawear api, and call retrieveBoard to get board and startup more metawear tasks/functions
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        Log.i("metawear", serviceBinder.toString());

         try {
            retrieveBoard(META_ADDR);
         }
         catch (Exception ex){
             Log.d("ShowerTunes", "Failed to retieveboard: " + ex.getMessage());
         }
    }

    /**
     * onServiceDisconnected: Disconnect devices. This in tutorials was always kinda left blank so I'm sticking with that.
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        //Disconnect device
    }

    /**
     * Broadcast receiver for changes made to Bluetooth on the android phone
     */
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        // Create a BroadcastReceiver for ACTION_FOUND. Relevant if change of Bluetooth status.
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        MusicText.setText("Bluetooth Disabled\nPlease turn on");
                        btActive = false;
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "bluetoothReceiver: STATE TURNING OFF");
                        btActive = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "bluetoothReceiver: STATE ON");;
                        MusicText.setText("Connecting Devices...");
                        btActive = true;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "bluetoothReceiver: STATE TURNING ON");
                        btActive = false;
                        break;
                }

                checkDependencies();
            }
        }
    };

    /**
     * Broadcast receiver looking for any newly added bluetooth devices. When Bluetooth speaker is turned on, this will
     * Listen for it.
     */
    private final BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            btSpeakerConnect = false;

            Log.d("BluetoothDevice", action);

            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                // Get BluetoothDevice object from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BluetoothDevice", device.toString());
                btSpeakerConnect = true;
            }
            else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                // Get BluetoothDevice object from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BluetoothDevice", device.toString());
                btSpeakerConnect = false;
            }

            checkDependencies();
        }
    };

    /**
     * checkDependencies checks if we have all our requirements set. If we do, we call upon toggleMusic
     */
    private void checkDependencies() {
        //Check Bluetooth
//        if (!btSpeakerConnect || !btActive )
//        {
//            BluetoothSpeaker.setAlpha(0.1f);
//        }
//        else
//        {
//            BluetoothSpeaker.setAlpha(1.0f);
//        }
        // Check Metawear
        if (!metaConnect) {
            Metawear.setAlpha(0.1f);
        }
        else {
            Metawear.setAlpha(1.0f);
        }

        // Function to check btSpeaker and metawear are connected
        // If both are connected, play music.
        if (btActive && btSpeakerConnect && metaConnect) {
            Log.d("Play","Yep");
            toggleMusic(true);
        }
        else {
            Log.d("Play","Nope");
            toggleMusic(false);
        }
    }

    /**
     * toggleMusic called when Bluetooth speaker connected and (when written correctly)  metawear detects
     * above required threshold in temperature. Makes edit in music text and image, and starts song. Will stop
     * song when parameters are not met. 
     */
    private void toggleMusic(boolean setToggle) {

        if (setToggle) {
            MusicText.setText("Regina Spektor - \"Eet\" ");
            AlbumArt.setImageResource(R.drawable.image);
            mediaPlayer.start();
            Log.d("Play start", "Yeeeeeeettttthhhhh");
        }
        else {
            // mediaPlayer.stop();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        }
    }

    /** retrieveBoard
     *  Get board information, do the binding, get temperature data. Will add more comments when it works.
     */
    public void retrieveBoard(String META_ADDR) {
        // btManager manages bluetooth connection
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        // gets remote device
        Log.i("bluetooth", "Getting remote device: " + btManager.getAdapter().getRemoteDevice(META_ADDR).toString());
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(META_ADDR);

        // Log.i("MetawearBoard", "remoteDevice is " + remoteDevice.toString());
        String LOG_TAG = "callbackAccelerometer";

        // create a new Metawear board object for the Bluetooth device
        board = serviceBinder.getMetaWearBoard(remoteDevice);

        // This section of text is for testing with accelerometer sensor
        board.connectAsync().onSuccessTask(task -> {
            accelerometer = board.getModule(Accelerometer.class);
            //temperature = board.getModule(Temperature.class);

            accelerometer.configure()
                    .odr(50f)
                    .commit();
            return accelerometer.acceleration().addRouteAsync(source ->
                    source.map(Function1.RSS).lowpass((byte) 4).filter(ThresholdOutput.BINARY, 0.5f)
                            .multicast()
                            .to().filter(Comparison.EQ, -1).log((data, env) -> Log.i(LOG_TAG, data.formattedTimestamp() + ": Entered Free Fall"))
                            .to().filter(Comparison.EQ, 1).log((data, env) -> Log.i(LOG_TAG, data.formattedTimestamp() + ": Left Free Fall"))
                            .end());
        }).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                Log.e(LOG_TAG, board.isConnected() ? "Error setting up route" : "Error connecting", task.getError());
            } else {
                Log.i(LOG_TAG, "Connected");
                metaConnect = true;
                debug = board.getModule(Debug.class);
                logging= board.getModule(Logging.class);
                checkDependencies();
            }
            return null;
        });
//The other one
//        if(board.getModel()!=null){
//
//            Log.i("MetawearBoard", "board is " + String.valueOf(board.getModel()));
//        }
//        else{
//            Log.i("MetawearBoard", "board is null");
//        }
//
//        //Log.i ("MetawearBoard", String.valueOf(board.isConnected()));
//        //Log.i("MetawearBoard", "in Retrieve Board after getModel() call");
//
//        // Establishes a BLE connection to MetaWear board
//        board.connectAsync().onSuccessTask(task -> {
//            @Override
//            public Void then(Task <Void> task) throws Exception {
//                Log.i("ShowerTunes", task.toString());
//                Log.i("ShowerTunes", "Connected to " + META_ADDR);
//
//                logging = board.getModule(Logging.class);
//
//                // create a reference to the temperature sensor on the board
//                final Temperature temp = board.getModule(Temperature.class);
//
//                final Temperature.Sensor tempSensor = temp.findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];
//
//                board.getModule(BarometerBosch.class).start();
//                temp.findSensors(Temperature.SensorType.BOSCH_ENV)[0].read();
//
//                tempSensor.addRouteAsync(new RouteBuilder() {
//                    @Override
//                    public void configure(RouteComponent source) {
//                        source.stream(new Subscriber() {
//                            @Override
//                            public void apply(Data data, Object ... env) {
//                                Log.i("MainActivity", "Temperature (C) = " + data.value(Float.class));
//                            }
//                        });
//                    }
//                }).continueWith(new Continuation<Route, Void>() {
//                    @Override
//                    public Void then(Task<Route> task) throws Exception {
//                        tempSensor.read();
//                        return null;
//                    }
//                });
//                return null;
//            }
//        });
    }

}