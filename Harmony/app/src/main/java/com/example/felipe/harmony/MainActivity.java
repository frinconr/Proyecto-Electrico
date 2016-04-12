package com.example.felipe.harmony;



import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity implements BTDevicesDialogFragment.NoticeDialogListener{

    //*************************************** BT Variables *****************************************
    protected static final int REQUEST_ENABLE_BT = 0;
    ListView BT_paired_devices_list;
    ArrayAdapter<String> BTArrayAdapter;
    Set<BluetoothDevice> pairedDevices;
    BluetoothAdapter mBluetoothAdapter;
    private int BTDeviceChosen;
    //**********************************************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        Button server_button = (Button) findViewById(R.id.smartphone_button);
        server_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (mBluetoothAdapter.isEnabled()) {
                    BTServer mBTServer = new BTServer(mBluetoothAdapter);
                    mBTServer.start();
                    Intent myIntent = new Intent(MainActivity.this, Server_activity.class);
                    MainActivity.this.startActivity(myIntent);
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

                    Toast.makeText(getApplicationContext(), Integer.toString(BTDeviceChosen), Toast.LENGTH_SHORT).show();


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
        BTArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        pairedDevices = mBluetoothAdapter.getBondedDevices();
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

        if (mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        else{
            BTArrayAdapter.clear();
            mBluetoothAdapter.startDiscovery();

            if(pairedDevices.size()>0){
                for(BluetoothDevice device: pairedDevices){
                    BTArrayAdapter.add(device.getName() + " (PAIRED)" + "\n" + device.getAddress());
                    BTArrayAdapter.notifyDataSetChanged();
                }
            }
            IntentFilter filter  = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);

        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
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


    public void onDestroy(){
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }


    @Override
    public void onClickBTDevices(android.support.v4.app.DialogFragment dialog, int chosenDevice) {
        Toast.makeText(getApplicationContext(),Integer.toString(chosenDevice), Toast.LENGTH_SHORT).show();
    }
}

