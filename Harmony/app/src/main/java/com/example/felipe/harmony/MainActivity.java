package com.example.felipe.harmony;




import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BTDevicesDialogFragment.NoticeDialogListener {

    //*************************************** BT Variables *****************************************
    protected static final int REQUEST_ENABLE_BT = 0;
    ArrayAdapter<String> BTArrayAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayList<BluetoothDevice> BTDevicesToConnect;
    BluetoothAdapter mBluetoothAdapter;
    public final String NAME = "Sounds";
    public final UUID MY_UUID = UUID.fromString("3f4e0c20-f5d2-11e5-9ce9-5e5517507c66");
    protected static final int SUCCESS_CONNECT = 0;
    BTServer mBTServer;
    BTClient mBTClient;
    public final MyHandler mHandler = new MyHandler(MainActivity.this);
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

                    mBTServer = new BTServer(MainActivity.this);
                    mBTServer.start();
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
        checkBT();
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
            Toast.makeText(getApplicationContext(), "You must turn the Bluetooth on to continue", Toast.LENGTH_SHORT).show();
        }
    }


    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }


    @Override
    public void onClickBTDevices(android.support.v4.app.DialogFragment dialog, int chosenDevice) {
        //BTServer mBTServer = new BTServer(mBluetoothAdapter);
        //mBTServer.start();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        BluetoothDevice selectedDevice = BTDevicesToConnect.get(chosenDevice);
        mBTClient = new BTClient(selectedDevice, MainActivity.this,mHandler);
        mBTClient.start();
        Toast.makeText(getApplicationContext(), selectedDevice.getName(), Toast.LENGTH_SHORT).show();
    }


    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case SUCCESS_CONNECT:
                        Toast.makeText(activity.getApplicationContext(),"FUCKING CONNECTED" , Toast.LENGTH_SHORT).show();
                        Log.d("BT CONNECTION","ALGUNA MIERDA");
                        break;
                }
            }
        }

    }
}
