package com.example.felipe.audiotest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;


public class BTServer extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private static final String NAME = "Sounds";
    private static final UUID MY_UUID = UUID.fromString("3f4e0c20-f5d2-11e5-9ce9-5e5517507c66");


    public BTServer(BluetoothAdapter mBluetoothAdapter) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {
            Log.d("RFCOMM", "Cannot create BlueTooth Socket:" + e.toString());
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.d("BT CONNECTION", "Couldn't accept connection");
                break;
            }
            Log.d("BT CONNECTION", "Trying to connect");
            // If a connection was accepted
            if (socket != null) {
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.d("BT SOCKET", "Couldn't close connection");
                }
                break;
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.d("BT SOCKET", "Couldn't close connection");
        }
    }
}