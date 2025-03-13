package com.example.smart_pillbox;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class BluetoothManager {
    private static BluetoothManager instance;
    private BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream inputStream;
    private boolean isConnecting = false;
    private static final int MAX_RETRY_COUNT = 3;
    private int retryCount = 0;
    private boolean keepConnectionAlive = true;
    private Context context;
    private ConnectionCallback connectionCallback;
    private static final String TAG = "BluetoothManager";

    public interface ConnectionCallback {
        void onConnected();
        void onConnectionFailed(String error);
    }

    public void setConnectionCallback(ConnectionCallback callback) {
        this.connectionCallback = callback;
    }
    private BluetoothManager(Context context) {
        this.context = context.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static synchronized BluetoothManager getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothManager(context);
        }
        return instance;
    }

    public void connect(String address) {
        Log.d("BluetoothManager", "connect called with address: " + address);
        if (isConnected() || isConnecting) {
            Log.d("BluetoothManager", "Already connected or connecting. Skipping connection attempt.");
            return;
        }

        if (!hasRequiredPermissions()) {
            Log.e("BluetoothManager", "Bluetooth permissions are not granted");
            if (connectionCallback != null) {
                connectionCallback.onConnectionFailed("Bluetooth permissions are not granted");
            }
            return;
        }

        isConnecting = true;
        retryCount = 0;
        keepConnectionAlive = true;
        connectWithRetry(address);
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
        }
    }


    private void connectWithRetry(String address) {
        new Thread(() -> {
            int retryCount = 0;
            while (keepConnectionAlive && retryCount < MAX_RETRY_COUNT) {
                try {
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    inputStream = bluetoothSocket.getInputStream();
                    isConnecting = false;
                    if (connectionCallback != null) {
                        connectionCallback.onConnected();
                    }
                    keepAlive();
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "Connection attempt failed: " + e.getMessage());
                    retryCount++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Sleep interrupted", ie);
                    }
                }
            }
            isConnecting = false;
            if (connectionCallback != null) {
                connectionCallback.onConnectionFailed("Connection failed after " + MAX_RETRY_COUNT + " attempts");
            }
        }).start();
    }

    private void keepAlive() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            while (keepConnectionAlive) {
                try {
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer);
                    }
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    Log.e("BluetoothManager", "Error in keepAlive: " + e.getMessage());
                    break;
                }
            }
        }).start();
    }
    public void disconnect() {
        keepConnectionAlive = false;
        if (bluetoothSocket != null) {
            try {
                inputStream.close();
                bluetoothSocket.close();
                bluetoothSocket = null;
                inputStream = null;
                Log.d("BluetoothManager", "Bluetooth disconnected successfully");
            } catch (IOException e) {
                Log.e("BluetoothManager", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return bluetoothSocket != null && bluetoothSocket.isConnected();
    }

    public InputStream getInputStream() {
        return inputStream;
    }
}
