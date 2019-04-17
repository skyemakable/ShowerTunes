package com.example.mbientlab.showertunes;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;


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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
//import java.util.UUID;

import bolts.Continuation;
import bolts.Task;

import com.mbientlab.metawear.module.Accelerometer;
public class MainActivity extends Activity implements ServiceConnection {

    private static final String TAG = "MainActivity";
    private final String MAC_ADDR = "F7:02:E6:49:04:AF";
    private final String Blu_MAC = "00:58:02:A8:02:44";
    MediaPlayer mediaPlayer;

    BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> pairedDevices = new ArrayList<String>();

    //private static final java.util.UUID myUUID = UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB");

    private ImageView Metawear;
    private ImageView BluetoothSpeaker;
    private ImageView AlbumArt;
    private TextView MusicText;
    private MetaWearBoard board;
    private Logging logging;
    private BtleService.LocalBinder serviceBinder;
    private Accelerometer accelerometer;

    /* onCreate
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
        MusicText = (TextView) findViewById(R.id.MusicText);
        Metawear = (ImageView) findViewById(R.id.Metawear);
        BluetoothSpeaker = (ImageView) findViewById(R.id.BluetoothSpeaker);
        AlbumArt = (ImageView) findViewById(R.id.AlbumArt);
        mediaPlayer = mediaPlayer.create(getApplicationContext(), R.raw.song); // TODO: Black Betty by Ram Jam

        // Set opacity for both speaker and metawear icons to low
        BluetoothSpeaker.setAlpha(0.1f);
        Metawear.setAlpha(0.1f);

        // get bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice btSpeaker = null;

        // Relevant when first starting the application
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d("BluetoothAdapter", "A bluetooth adapter isn't available.");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d("BluetoothAdapter", "Bluetooth is not currently enabled");
                MusicText.setText("Bluetooth Disabled\nPlease turn on");
            } else {
                Log.d("BluetoothAdapter", "Bluetooth enabled. Continuing...");
                MusicText.setText("Connecting Devices...");
            }
        }
        // Below is code for checking for changing in Bluetooth (enable/disable)
        IntentFilter changeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, changeFilter);

        // Have filter for connected bluetooth devices and receiver to help look for MAC address of desired speaker
        IntentFilter pairedBlueFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        pairedBlueFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(deviceReceiver, pairedBlueFilter);

        // Get arraylist of paired Bluetooth devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // Boolean if the speaker we are looking at is found
        boolean found = false;

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice d : pairedDevices) {
                String deviceName = d.getName();
                String macAddress = d.getAddress();
                Log.i("BluetoothDevices", "paired device: " + deviceName + " at " + macAddress);
                if (macAddress.equals(Blu_MAC)) {
                    // Log.i("BluetoothSpeaker", "Found bluetooth speaker with mac Address: " + macAddress);
                    found = true;
                    btSpeaker = d;
                    break;
                }
            }
        }
        if (found) {
            Log.i("BluetoothDevices", "Found speaker: " + btSpeaker.getAddress());
            //playMusic();
        }
        // Metawear API
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    /* onDestroy
    * Unbind services, unregister receivers, and release the mediaPlayer
    */

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(deviceReceiver);
        mediaPlayer.release();
        mediaPlayer = null;
    }
    /* onServiceConnected
    * Binding for metawear api, and call retrieveBoard to get board and startup more metawear tasks/functions
    */

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        Log.i("metawear", serviceBinder.toString());
        Log.i("Metawear", "Service Connected");

        // mac addr here for metawear device
        try {
            retrieveBoard();
        }
        catch (Exception ex){
            Log.d("ShowerTunes", "Failed to retieveboard" + ex.getMessage());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        //Disconnect device
    }

    /*
       Broadcast receiver for changes made to Bluetooth on the android phone
    */

    // Create a BroadcastReceiver for ACTION_FOUND. Relevant if change of Bluetooth status.
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        MusicText.setText("Bluetooth Disabled\nPlease turn on");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "bluetoothReceiver: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "bluetoothReceiver: STATE ON");;
                        MusicText.setText("Connecting Devices...");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "bluetoothReceiver: STATE TURNING ON");
                        break;
                }
            }
        }
    };
    /*
    * Broadcast receiver looking for any newly added bluetooth devices. When Bluetooth speaker is turned on, this will
    * Listen for it.
    */

    private final BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                // Get BluetoothDevice object from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BluetoothDevice", device.toString());
                BluetoothSpeaker.setAlpha(1.0f);
                // TODO: call metawear api to get values
                playMusic();
            }
        }
    };

    /*
    * playMusic called when Bluetooth speaker connected and (when written correctly)  metawear detects
    * above required threshold in temperature. Makes edit in music text and image, and starts song.
     */
    private void playMusic() {
        // play that funky music.
        MusicText.setText("Regina Spektor - \"Eet\" ");
        AlbumArt.setImageResource(R.drawable.image);
        mediaPlayer.start();

    }
    /* retrieveBoard
    *  Get board information, do the binding, get temperature data. Will add more comments when it works.
     */
    public void retrieveBoard() {

        // btManager manages bluetooth connection
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // gets remote device
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MAC_ADDR);
        Log.i("Remote device", "remoteDevice is " + remoteDevice.toString());

        // create a new Metawear board object for the Bluetooth device
        board = serviceBinder.getMetaWearBoard(remoteDevice);
        Log.i("MetawearBoard", "in Retrieve Board: before getModel() call");
        Log.i("MetawearBoard", String.valueOf(board.isConnected()));
        board.connectAsync().onSuccessTask(task -> {
            String LOG_TAG = "callback-for-accelerometer";
            accelerometer = board.getModule(Accelerometer.class);
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
                debug = board.getModule(Debug.class);
                logging= board.getModule(Logging.class);
            }

            return null;
        });
        /*if(board.getModel()!=null){

            Log.i("MetawearBoard", "board is " + String.valueOf(board.getModel()));
        }
        else{
            Log.i("MetawearBoard", "board is null");
        }*/

        //Log.i ("MetawearBoard", String.valueOf(board.isConnected()));
        //Log.i("MetawearBoard", "in Retrieve Board after getModel() call");

        // Establishes a BLE connection to MetaWear board
        /*board.connectAsync().onSuccessTask(new Continuation<Void, Void>() {
            @Override
            public Void then(Task <Void> task) throws Exception {
                Log.i("showerTunes", task.toString());
                Log.i("ShowerTunes", "Connected to " + MAC_ADDR);

                /*logging = board.getModule(Logging.class);

                // create a reference to the temperature sensor on the board
                final Temperature temp = board.getModule(Temperature.class);

                final Temperature.Sensor tempSensor = temp.findSensors(Temperature.SensorType.PRESET_THERMISTOR)[0];

                board.getModule(BarometerBosch.class).start();
                temp.findSensors(Temperature.SensorType.BOSCH_ENV)[0].read();

                tempSensor.addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                Log.i("MainActivity", "Temperature (C) = " + data.value(Float.class));
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        tempSensor.read();
                        return null;
                    }
                });
                return null;
            }
        }); */
    }

}