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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MusicText = (TextView) findViewById(R.id.MusicText);
        Metawear = (ImageView) findViewById(R.id.Metawear);
        BluetoothSpeaker = (ImageView) findViewById(R.id.BluetoothSpeaker);
        AlbumArt = (ImageView) findViewById(R.id.AlbumArt);
        mediaPlayer = mediaPlayer.create(getApplicationContext(), R.raw.song); // TODO: Black Betty by Ram Jam

        BluetoothSpeaker.setAlpha(0.1f);
        Metawear.setAlpha(0.1f);


        // get bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice btSpeaker = null;


        // Establish connection to the proxy
        // bluetoothAdapter.getProfileProxy(this, profileListener, BluetoothProfile.A2DP);

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
        //Below is code for checking for changing in Bluetooth (enable/disable)
        IntentFilter changeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, changeFilter);

        IntentFilter pairedBlueFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        pairedBlueFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        pairedBlueFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        pairedBlueFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        pairedBlueFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        pairedBlueFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(deviceReceiver, pairedBlueFilter);

        // Get arraylist of paired Bluetooth devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
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
        // if not found
        else {
            // MusicText.setText("Bluetooth Speaker not found. Turn on and pair speaker");
        }

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
        unregisterReceiver(mBroadcastReceiver1);
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
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

    // Create a BroadcastReceiver for ACTION_FOUND. Relevant if change of Bluetooth status.
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
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
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");;
                        MusicText.setText("Connecting Devices...");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                // Get BluetoothDevice object from the intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("BluetoothDevice", device.toString());
                BluetoothSpeaker.setAlpha(1.0f);
                playMusic();
//                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                    Log.d("BluetoothDevice", "Device found");
//                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
//                    Log.d("BluetoothDevice", "Device now connected");
//                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                    Log.d("BluetoothDevice", "Device searching");
//                } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
//                    Log.d("BluetoothDevice", "Device about to disconnect");
//                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
//                    Log.d("BluetoothDevice", "Device disconnected");
//                }
            }
        }
    };

    // Play music when meta is found, bluetooth speaker found, and when temperature is high enough
    private void playMusic() {
        // play that funky music.
        MusicText.setText("Regina Spektor - \"Eet\" ");
        AlbumArt.setImageResource(R.drawable.image);
        mediaPlayer.start();

    }

    public void retrieveBoard() {

        // btManager manages bluetooth connection
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // gets remote device
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MAC_ADDR);
        Log.i("Remote device", "remoteDevice is " + remoteDevice.toString());

        // create a new Metawear board object for the Bluetooth device
        board = serviceBinder.getMetaWearBoard(remoteDevice);
        Log.i("MetawearBoard", "board is " + board.getModel());
        Log.i ("MetawearBoard", String.valueOf(board.isConnected()));

        // Establishes a BLE connection to MetaWear board
        board.connectAsync().onSuccess(new Continuation<Void, Void>() {
            @Override
            public Void then(Task <Void> task) throws Exception {
                Log.i("ShowerTunes", "Connected to " + MAC_ADDR);

                logging = board.getModule(Logging.class);

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
        });
    }

    // Make device discoverable? https://stackoverflow.com/questions/46841534/pair-a-bluetooth-device-in-android-studio
    private void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.i("Log", "Discoverable");
    }
}