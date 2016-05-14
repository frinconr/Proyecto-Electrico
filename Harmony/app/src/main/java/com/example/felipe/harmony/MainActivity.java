package com.example.felipe.harmony;



import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BTDevicesDialogFragment.NoticeDialogListener {

    public static final String TAG = "MainActivity";

    //*************************************** BT Variables *****************************************
    protected static final int REQUEST_ENABLE_BT = 0;
    private ArrayAdapter<String> BTArrayAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ArrayList<BluetoothDevice> BTDevicesToConnect;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mChatService;
    private BluetoothDevice selectedDevice;
    private String mConnectedDeviceName;
    //**********************************************************************************************


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
//************************************* Server Button **********************************************
        Button server_button = (Button) findViewById(R.id.smartphone_button);
        server_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (mBluetoothAdapter.isEnabled()) {
                    ensureDiscoverable();
                    //Intent myIntent = new Intent(MainActivity.this, Server_activity.class);
                    //MainActivity.this.startActivity(myIntent);
                } else {
                    Toast.makeText(getApplicationContext(), "You must turn the Bluetooth on to continue", Toast.LENGTH_SHORT).show();
                    checkBT();
                }
            }
        });


//************************************* Client Button **********************************************

        Button client_button = (Button) findViewById(R.id.tablet_button);
        client_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (mBluetoothAdapter.isEnabled()) {
                    findBTDevices();
                    FragmentManager manager = getSupportFragmentManager();
                    BTDevicesDialogFragment mBTDevicesDialog = new BTDevicesDialogFragment();
                    mBTDevicesDialog.setListAdapter(BTArrayAdapter);
                    mBTDevicesDialog.show(manager, "Bluetooth Devices");

                } else {
                    Toast.makeText(getApplicationContext(), "You must turn the Bluetooth on to continue", Toast.LENGTH_SHORT).show();
                    checkBT();
                }
            }

        });
//**************************************************************************************************

    }


    private void init() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        BTDevicesToConnect = new ArrayList<>();
        mChatService = null;
        selectedDevice = null;
        mConnectedDeviceName = null;
        checkBT();
        if(mChatService==null && mBluetoothAdapter.isEnabled()){
            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(getApplicationContext(), mHandler);
        }
    }


    private void checkBT() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available on your device", Toast.LENGTH_SHORT).show();
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void findBTDevices() {

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        } else {
            BTArrayAdapter.clear();
            BTDevicesToConnect.clear();

            mBluetoothAdapter.startDiscovery();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    BTDevicesToConnect.add(device);
                    BTArrayAdapter.add(device.getName() + " (PAIRED)" + "\n" + device.getAddress());
                    BTArrayAdapter.notifyDataSetChanged();
                }
            }
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);

        }
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void connectDevice(BluetoothDevice device, boolean secure) {
        // Get the device MAC address
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }



    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean allReadyFound = false;

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                for (int i = 0; i < BTDevicesToConnect.size(); i++) {
                    if (device == BTDevicesToConnect.get(i)) {
                        allReadyFound = true;
                        break;
                    }
                }

                if (!allReadyFound) {
                    BTDevicesToConnect.add(device);
                }

                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                BTArrayAdapter.notifyDataSetChanged();
            }
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(),R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "BT not enabled");
        }else{
            Toast.makeText(getApplicationContext(),"Bluetooth Enabled", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "BT not enabled");
        }
    }


    public void onDestroy() {
        unregisterReceiver(mReceiver);
        if (mChatService != null) {
            mChatService.stop();
        }
        super.onDestroy();
    }


    @Override
    public void onClickBTDevices(android.support.v4.app.DialogFragment dialog, int chosenDevice) {
        //BTServer mBTServer = new BTServer(mBluetoothAdapter);
        //mBTServer.start();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        selectedDevice = BTDevicesToConnect.get(chosenDevice);
        connectDevice(selectedDevice, false);
        //Toast.makeText(getApplicationContext(), selectedDevice.getName(), Toast.LENGTH_SHORT).show();
    }
/*
    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }*/

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };


}
