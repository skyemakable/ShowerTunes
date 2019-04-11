package com.example.mbientlab.showertunes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import bolts.Continuation;
import bolts.Task;


public class MainActivity extends Activity implements ServiceConnection {

    private static final String TAG = "MainActivity";
    private final String MAC_ADDR = "F7:02:E6:49:04:AF";
    private final String Blu_MAC = "00:58:02:A8:02:44";

    BluetoothAdapter mBluetoothAdapter;

    private ImageView Metawear;
    private ImageView BluetoothSpeaker;
    private ImageView AlbumArt;
    private TextView MusicText;
    private MetaWearBoard board;
    private Logging logging;
    private BtleService.LocalBinder serviceBinder;

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
                        MusicText.setText("Bluetooth Disabled. Please turn on");
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MusicText = (TextView) findViewById(R.id.MusicText);
        Metawear = (ImageView) findViewById(R.id.Metawear);
        BluetoothSpeaker = (ImageView) findViewById(R.id.BluetoothSpeaker);
        AlbumArt = (ImageView) findViewById(R.id.AlbumArt);

        // get bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Relevant when first starting the application
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d("BluetoothAdapter", "A bluetooth adapter isn't available.");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d("BluetoothAdapter", "Bluetooth is not currently enabled");
                MusicText.setText("Bluetooth Disabled. Please turn on");
            }
            else {
                Log.d("BluetoothAdapter", "Bluetooth enabled. Continuing...");
                MusicText.setText("Connecting Devices...");
            }
        }
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, filter);

        /*if (findBlue()) {
            Log.d("BluetoothSpeaker", "Found the bluetooth speaker");
        }
        else {
            Log.d("BluetoothSpeaker", "Fudge");
        }
*/
        getApplicationContext().bindService(new Intent(this, BtleService.class),
            this, Context.BIND_AUTO_CREATE);


    }
/*
    public boolean findBlue() {
        boolean found = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        List<String> s = new ArrayList<String>();
        for (BluetoothDevice bt: pairedDevices) {
            if (bt.getAddress().equals(Blu_MAC)) {
                found = true;
            }
        }
        return found;
    }*/



    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
        unregisterReceiver(mBroadcastReceiver1);
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

    // Play music when meta is found, bluetooth speaker found, and when temperature is high enough
    private void playMusic(String path, String fileName) {
        // play that funky music.
        MediaPlayer mp = new MediaPlayer();

        try {
            mp.setDataSource(path + File.separator + fileName);
            mp.prepare();
            mp.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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