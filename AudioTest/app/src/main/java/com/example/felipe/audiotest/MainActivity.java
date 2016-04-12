package com.example.felipe.audiotest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {


    //*************************************** Audio Variables **************************************

    Thread t;       // hold the audio processing thread

    int sr = 44100; // Sampling rate
    boolean isRunnig = false; // switch the audio on and off

    SeekBar fslider;
    double sliderval;

    SeekBar second_fslider;
    double second_sliderval;

    //*************************************** BT Variables *****************************************

    ListView BT_paired_devices_list;
    ArrayAdapter<String> BTArrayAdapter;
    Set<BluetoothDevice> pairedDevices;
    BluetoothAdapter mBluetoothAdapter;
    //*************************************** BT Servidor *****************************************
    BTServer mBTserver;

    //**********************************************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // *********************************** Slider **********************************************
        // Point the slider to the widget
        fslider = (SeekBar) findViewById(R.id.frequency);
        // create a listener for the slide bar

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) sliderval = progress / (double)seekBar.getMax();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        fslider.setOnSeekBarChangeListener(listener);

        //******************************************************************************************


        // New thread for synt the audio

        t = new Thread(){
            public void run(){
                setPriority(Thread.MAX_PRIORITY); // Set Thread priority

                int buffsize = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
                //AudioTrack object
                AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,sr,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,buffsize,AudioTrack.MODE_STREAM);

                // Synthesis

                short samples[] = new short[buffsize];
                short scd_samples[] = new short[buffsize];
                short result[] = new short[buffsize];
                int amp = 10000;
                double twopi = 8.*Math.atan(1.);
                double fr = 440.f ;
                double scd_fr = 261.6f;
                double ph = 0.0;
                double ph_2 = 0.0;

                audioTrack.play(); // starts audio


                //synthesis loop

                Log.d("LOOP","Si hay tread");


                while(isRunnig){

                    for (int i=0; i<buffsize; i++){
                        fr = 440 + 440*sliderval;
                        scd_fr = 261.6 + 261.6*second_sliderval;
                        samples[i] = (short) (amp*Math.sin(ph));
                        scd_samples[i] = (short) (amp*Math.sin(ph_2));
                        result[i] = (short) (samples[i] + scd_samples[i]);
                        ph += twopi*fr/sr;
                        ph_2 += twopi*scd_fr/sr;
                    }

                    audioTrack.write(result,0,buffsize);
                }

                audioTrack.stop();
                audioTrack.release();


            }
        };

        // *********************************** Play/Stop Button ************************************

        Button play_stop_button = (Button) findViewById(R.id.Play_stop);
        play_stop_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                isRunnig = !isRunnig;
                Log.d("PLAY",Boolean.toString(isRunnig));
            }
        });

        //t.start();



        // *****************************************************************************************


        // *********************************** Slider **********************************************
        // Point the slider to the widget
        second_fslider = (SeekBar) findViewById(R.id.second_freq);
        // create a listener for the slide bar

        SeekBar.OnSeekBarChangeListener second_listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) second_sliderval = progress / (double)seekBar.getMax();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        second_fslider.setOnSeekBarChangeListener(second_listener);

        //******************************************************************************************



        // *********************************** Bluetooth *******************************************


        // *********************************** Check Enable ****************************************

        int REQUEST_ENABLE_BT = 3;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null){
            // Bluetooth not available
            Log.d("BLUETOOTH","Bluetooth not available");
        }
        else if (!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Log.d("BLUETOOTH", Integer.toString(REQUEST_ENABLE_BT));
            }


        // ************************* Discovering devices & Paired Devices **************************

        BT_paired_devices_list = (ListView) findViewById(R.id.Bluetooth_listview);
        BTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        BT_paired_devices_list.setAdapter(BTArrayAdapter);
        pairedDevices = mBluetoothAdapter.getBondedDevices();


        Button findBTdevices = (Button) findViewById(R.id.findBTDevices);
        findBTdevices.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (mBluetoothAdapter.isDiscovering()){

                    mBluetoothAdapter.cancelDiscovery();
                }
                else{
                    BTArrayAdapter.clear();
                    mBluetoothAdapter.startDiscovery();
                    IntentFilter filter  = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mReceiver, filter);

                }
            }
        });

        // *****************************************************************************************

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver(){

        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                Log.d("BT LIsT","si entra");

                if(pairedDevices.size()>0){
                    for(BluetoothDevice device: pairedDevices){
                        BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    }
                }

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(BTArrayAdapter.getCount()!=0) {
                    for (int i = 0; i < BTArrayAdapter.getCount(); i++) {
                        if(!BTArrayAdapter.getItem(i).equals(device.getName() + "\n" + device.getAddress())){
                            BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                            BTArrayAdapter.notifyDataSetChanged();
                        }
                    }
                }

            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "You must turn the Bluetooth on to continue", Toast.LENGTH_SHORT).show();
            //finish();
        }

    }



    public void onDestroy(){

        super.onDestroy();
        isRunnig = false;
        mBTserver.cancel();

        try{
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        t = null;

        unregisterReceiver(mReceiver);
    }

}
