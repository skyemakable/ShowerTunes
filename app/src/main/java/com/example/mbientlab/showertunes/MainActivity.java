package com.example.mbientlab.showertunes;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.module.HumidityBme280;

import bolts.Continuation;
import bolts.Task;


public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private final String MAC_ADDR = "F7:02:E6:49:04:AF";

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

        getApplicationContext().bindService(new Intent(this, BtleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // typecase the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        Log.i("Metawear", "Service Connected");

        // mac addr here for metawear device
        retrieveBoard();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    // Look for metawear connection and get humidity level
    // Lines 144 - 156 of freefall
    private void findMeta() {

        // boolean metaFound = false;
        // if (!metaFound) {
        //     // transparent the image, check again?
        //     Metawear.setAlpha(0.5f);
        // } else {
        //     // solid the image.
        //     Metawear.setAlpha(1.0f);
        //     // look if we connected to Bluetooth speaker
        //     boolean blueFound = findBlue();
        //     // find humidity level
        //     float humidity_level = 60;
        //     if (humidity_level > 50) {
        //         playMusic();
        //     }
        // }

    }

    // Play music when meta is found, bluetooth speaker found, and when humidity is high enough
    private void playMusic() {
        // play that funky music.
    }

    public void retrieveBoard() {
        // btManager manages bluetooth connection
        final BluetoothManager btManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // gets remote device
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(MAC_ADDR);

        // create a new Metawear board object for the Bluetooth device
        // board = serviceBinder.getMetaWearBoard(remoteDevice);
        // board.connectAsync().onSuccessTask(new Continuation<Void, Task<Void>>() {
        //     @Override
        //     public Task<Void> then(Task <Void> task) throws Exception {
        //         Log.i("ShowerTunes", "Connected to " + MAC_ADDR);

        //         logging = board.getModule(Logging.class);

        //         // create a reference to the humidity sensor on the board
        //         final HumidityBme280 humiditySensor = board.getModule(HumidityBme280.class);

        //         // https://mbientlab.com/androiddocs/latest/data_route.html?highlight=addrouteasync
        //         humiditySensor.value().addRouteAsync(new RouteBuilder() {
        //             @Override
        //             public void configure(RouteComponent source) {
        //                 source.stream(new Subscriber() {
        //                     @Override
        //                     public void apply(Data data, Object ... env) {
        //                         Log.i("ShowerTunes", "Humidity = " + data.value(Float.class));
        //                     }
        //                 });
        //             }
        //         }).continueWith(new Continuation<Route, Void>() {
        //             @Override
        //             public Void then(Task<Route> task) throws Exception {
        //                 humiditySensor.value().read();
        //                 return null;
        //             }
        //         });
        //     }
        // });
    }

    // Look for Bluetooth connection
    private boolean findBlue() {
        // boolean blueFound = false;

        // // look for bluetooth speaker
        // if (!blueFound) {
        //     // make transparent
        //     BluetoothSpeaker.setAlpha(0.5f);
        //     // repeat looking for it?
        // }
        // // found bluetooth speaker
        // else {
        //     BluetoothSpeaker.setAlpha(1.0f);
        //     blueFound = true;
        // }

        // return blueFound;
        return false;
    }
}