package pl.programowanie_robotow.arduinobt;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private final String DEVICE_NAME="MSlave"; // Target device name
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton, stopButton;
    ImageButton[] movementBtns = new ImageButton[5];
    boolean deviceConnected = false;

    private static String[] PERMISSIONS_BLUETOOTH = {
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
    };

    private Vibrator vib;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, PERMISSIONS_BLUETOOTH,1);

        vib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.disable();

        // make sure bt connection is closed then connect again
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.enable();
            }
        },1000);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(restartBluetooth, filter);

        startButton = findViewById(R.id.buttonStart);
        stopButton = findViewById(R.id.buttonStop);

        movementBtns[0] = findViewById(R.id.topButton);
        movementBtns[1] = findViewById(R.id.leftButton);
        movementBtns[2] = findViewById(R.id.rightButton);
        movementBtns[3] = findViewById(R.id.bottomButton);
        movementBtns[4] = findViewById(R.id.stopButton);

        setupButtons();

        setUiEnabled(false);
        startButton.setEnabled(false);
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        for (ImageButton movementBtn : movementBtns) movementBtn.setEnabled(bool);
    }

    @SuppressLint("MissingPermission")
    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();

        if (bluetoothAdapter == null) {
            showInfo("Device doesn't Support Bluetooth");
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            showInfo("Enable Bluetooth!");
            return false;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty())
            showInfo("Please Pair the Device first");
        else {
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator.getName().equals(DEVICE_NAME)) {
                    device = iterator;
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    @SuppressLint("MissingPermission")
    public boolean BTconnect() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
        if (connected) {
            showInfo("Connected to " + DEVICE_NAME + "!");
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return connected;
    }

    void setupButtons() {
        movementBtns[0].setOnClickListener(view -> onClickSend("t")); // top
        movementBtns[1].setOnClickListener(view -> onClickSend("l")); // left
        movementBtns[2].setOnClickListener(view -> onClickSend("r")); // right
        movementBtns[3].setOnClickListener(view -> onClickSend("b")); // bottom
        movementBtns[4].setOnClickListener(view -> onClickSend("s")); // stop
    }

    public void onClickSend(String data) {
        try {
            outputStream.write(data.getBytes());
            vib.vibrate(VibrationEffect.createOneShot(125,70));
            Log.d("Sent:", data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClickStart(View view) {
        if (!BTinit()) return;
        if (!BTconnect()) return;

        setUiEnabled(true);
        deviceConnected = true;
    }

    @SuppressLint("MissingPermission")
    public void onClickStop(View view) throws IOException {
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected = false;
    }

    void showInfo(String msg) {
        Snackbar.make(stopButton, msg, Snackbar.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver restartBluetooth = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_ON:
                    startButton.setEnabled(true);
                    break;
            }
        }
        }
    };
}