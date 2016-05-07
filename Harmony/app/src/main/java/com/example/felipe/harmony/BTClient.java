package com.example.felipe.harmony;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;

public class BTClient extends Thread {
    private final BluetoothSocket mmSocket;
    Handler handler;
    final MainActivity mMain;

    public BTClient(BluetoothDevice device, MainActivity Main, Handler mHandler) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        handler = mHandler;
        mMain = Main;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(mMain.MY_UUID);
        } catch (IOException e) {
            Log.d("BTClient", "Cannot create BlueTooth Socket ");

        }
        mmSocket = tmp;
    }

    public void run(BluetoothAdapter mBluetoothAdapter) {
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            Log.d("BTClient", "Trying to connect as a client");
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.d("BTClient", "Unable to connect");
            }
        }

        // Do work to manage the connection (in a separate thread)
        handler.obtainMessage(mMain.SUCCESS_CONNECT, mmSocket).sendToTarget();

    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.d("BTClient", "Unable to close socket");
        }
    }
}

