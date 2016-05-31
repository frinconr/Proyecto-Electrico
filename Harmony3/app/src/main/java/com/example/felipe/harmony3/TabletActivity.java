package com.example.felipe.harmony3;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class TabletActivity extends AppCompatActivity {

    private static final String TAG = "TabletActivity";



    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;


    /**
     * Char array representing button state: 1-> pressed 0->released
     */
    private char[] mPhoneButtonStatus= {'0','0','0','0','0','0','0','0','0','0'};


    /**
     * Status from the phone activity button
     */
    private boolean IsInhalePlaying = false;

    /**
     * Status from the phone activity button
     */
    private boolean IsExhalePlaying = false;


    /**
     * Status from the phone activity button
     */
    private boolean IsPlayingAnyButton = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablet);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available on your device", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }


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
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tablet_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.devices_list:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(TabletActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.Discover_device:
                ensureDiscoverable();
                return true;
        }
        return false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                        connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                    Toast.makeText(getApplicationContext(), "Bluetooth Enabled",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getApplicationContext(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

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
                            setStatus(getString(R.string.title_connected_to)+ " " + mConnectedDeviceName);
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    UpdatePhoneStatus(readMessage);
                    //Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
                   // mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
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



    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     *
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the notes buttons
        Button Inhale_button = (Button) findViewById(R.id.inhale_button);

        if (Inhale_button != null) {
            Inhale_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if(event.getAction()==MotionEvent.ACTION_UP){

                        IsInhalePlaying = false;
                        Log.d(TAG, "NADA");

                    }else if(event.getAction()==MotionEvent.ACTION_DOWN){

                        if(IsPlayingAnyButton){
                            IsInhalePlaying = true;
                            Log.d(TAG, "SONANDO");
                            new Thread(new MusicTask(true)).start();

                        }
                    }else if(event.getAction()==MotionEvent.ACTION_MOVE){
                        //Toast.makeText(getApplicationContext(), "Moving", Toast.LENGTH_SHORT).show();

                    }

                    return false;
                }
            });

            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(this, mHandler);
        }

        Button Exhale_button = (Button) findViewById(R.id.exhale_button);

        if (Exhale_button != null) {
            Exhale_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if(event.getAction()==MotionEvent.ACTION_UP){

                        IsExhalePlaying = false;
                        Log.d(TAG, "NADA");

                    }else if(event.getAction()==MotionEvent.ACTION_DOWN){

                        if(IsPlayingAnyButton){
                            IsExhalePlaying = true;
                            new Thread(new MusicTask(false)).start();
                            Log.d(TAG, "SONANDO");
                        }
                    }else if(event.getAction()==MotionEvent.ACTION_MOVE){
                        Log.d(TAG, "Moviendose");
                        //Toast.makeText(getApplicationContext(), "Moving", Toast.LENGTH_SHORT).show();

                    }

                    return false;
                }
            });
        }

    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {

        final ActionBar actionBar = getSupportActionBar();

        if (null == actionBar) {
            return;
        }

        actionBar.setSubtitle(resId);
    }
    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {

        final ActionBar actionBar = getSupportActionBar();


        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }


    private void UpdatePhoneStatus(String state){

        if(state.equals("0000000000")){
            //IsInhalePlaying = false;
            //IsExhalePlaying = false;
            IsPlayingAnyButton = false;
        } else {
            IsPlayingAnyButton = true;
        }
        mPhoneButtonStatus = state.toCharArray();

    }


    class MusicTask implements Runnable{

        private boolean InhaleOrExhale;

        public MusicTask(boolean mInhaleOrExhale){
            this.InhaleOrExhale  = mInhaleOrExhale;
        }

    @Override
        public void run(){
            int SamplingRate = 44100;
            int buffer_size = AudioTrack.getMinBufferSize(SamplingRate,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SamplingRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size, AudioTrack.MODE_STREAM);
            short samples_button1[] = new short[buffer_size];
            short samples_button2[] = new short[buffer_size];
            short samples_button3[] = new short[buffer_size];
            short samples_button4[] = new short[buffer_size];
            short samples_button5[] = new short[buffer_size];
            short samples_button6[] = new short[buffer_size];
            short samples_button7[] = new short[buffer_size];
            short samples_button8[] = new short[buffer_size];
            short samples_button9[] = new short[buffer_size];
            short samples_button10[] = new short[buffer_size];
            short ResultWave[] = new short[buffer_size];

            int amp = 10000;
            double twopi = 8.*Math.atan(1.);

            double Frequency_button1;
            double Frequency_button2;
            double Frequency_button3;
            double Frequency_button4;
            double Frequency_button5;
            double Frequency_button6;
            double Frequency_button7;
            double Frequency_button8;
            double Frequency_button9;
            double Frequency_button10;

            double phase_button1 = 0.0;
            double phase_button2 = 0.0;
            double phase_button3 = 0.0;
            double phase_button4 = 0.0;
            double phase_button5 = 0.0;
            double phase_button6 = 0.0;
            double phase_button7 = 0.0;
            double phase_button8 = 0.0;
            double phase_button9 = 0.0;
            double phase_button10 = 0.0;

            // start audio
            mAudioTrack.play();

            // synthesis loop
            while(InhaleOrExhale? IsInhalePlaying:IsExhalePlaying){

                for(int i=0; i < buffer_size; i++){

                    //************************ FIRST BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[0]=='1'){
                        Frequency_button1 = InhaleOrExhale? Notes.D1*Math.pow(2,3):Notes.C1*Math.pow(2,3);
                        samples_button1[i] = (short) (amp*Math.sin(phase_button1));
                        phase_button1 += twopi*(Frequency_button1)/SamplingRate;
                    }else{samples_button1[i] = 0;}

                    //************************ SECOND BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[1]=='1'){
                        Frequency_button2 = InhaleOrExhale? Notes.G1*Math.pow(2,3):Notes.E1*Math.pow(2,3);
                        samples_button2[i] = (short) (amp*Math.sin(phase_button2));
                        phase_button2 += twopi*(Frequency_button2)/SamplingRate;
                    }else{samples_button2[i] = 0;}

                    //************************ THIRD BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[2]=='1'){
                        Frequency_button3 = InhaleOrExhale? Notes.B1*Math.pow(2,3):Notes.G1*Math.pow(2,3);
                        samples_button3[i] = (short) (amp*Math.sin(phase_button3));
                        phase_button3 += twopi*(Frequency_button3)/SamplingRate;
                    }else{samples_button3[i] = 0;}

                    //************************ FOURTH BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[3]=='1'){
                        Frequency_button4 = InhaleOrExhale? Notes.D1*Math.pow(2,4):Notes.C1*Math.pow(2,4);
                        samples_button4[i] = (short) (amp*Math.sin(phase_button4));
                        phase_button4 += twopi*(Frequency_button4)/SamplingRate;
                    }else{samples_button4[i] = 0;}


                    //************************ FIFTH BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[4]=='1'){
                        Frequency_button5 = InhaleOrExhale? Notes.F1*Math.pow(2,4):Notes.E1*Math.pow(2,4);
                        samples_button5[i] = (short) (amp*Math.sin(phase_button5));
                        phase_button5 += twopi*(Frequency_button5)/SamplingRate;
                    }else{samples_button5[i] = 0;}


                    //************************ SIXTH BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[5]=='1'){
                        Frequency_button6 = InhaleOrExhale? Notes.A1*Math.pow(2,3):Notes.F1*Math.pow(2,3);
                        samples_button6[i] = (short) (amp*Math.sin(phase_button6));
                        phase_button6 += twopi*(Frequency_button6)/SamplingRate;
                    }else{samples_button6[i] = 0;}


                    //************************ SEVENTH BUTTON SOUNDS *******************************
                    if(mPhoneButtonStatus[6]=='1'){
                        Frequency_button7 = InhaleOrExhale? Notes.B1*Math.pow(2,4):Notes.C1*Math.pow(2,5);
                        samples_button7[i] = (short) (amp*Math.sin(phase_button7));
                        phase_button7 += twopi*(Frequency_button7)/SamplingRate;
                    }else{samples_button7[i] = 0;}

                    //************************ EIGHTH BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[7]=='1'){
                        Frequency_button8 = InhaleOrExhale? Notes.D1*Math.pow(2,5):Notes.E1*Math.pow(2,5);
                        samples_button8[i] = (short) (amp*Math.sin(phase_button8));
                        phase_button8 += twopi*(Frequency_button8)/SamplingRate;
                    }else{samples_button8[i] = 0;}

                    //************************ NINETH BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[8]=='1'){
                        Frequency_button9 = InhaleOrExhale? Notes.F1*Math.pow(2,4):Notes.G1*Math.pow(2,5);
                        samples_button9[i] = (short) (amp*Math.sin(phase_button9));
                        phase_button9 += twopi*(Frequency_button9)/SamplingRate;
                    }else{samples_button9[i] = 0;}

                    //************************ TENTH BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[9]=='1'){
                        Frequency_button10 = InhaleOrExhale? Notes.A1*Math.pow(2,5):Notes.C1*Math.pow(2,6);
                        samples_button10[i] = (short) (amp*Math.sin(phase_button10));
                        phase_button10 += twopi*(Frequency_button10)/SamplingRate;
                    }else{samples_button10[i] = 0;}

                    ResultWave[i] = (short) (samples_button1[i]
                                            +samples_button2[i]
                                            +samples_button3[i]
                                            +samples_button4[i]
                                            +samples_button5[i]
                                            +samples_button6[i]
                                            +samples_button7[i]
                                            +samples_button8[i]
                                            +samples_button9[i]
                                            +samples_button10[i]);
                }


                mAudioTrack.write(ResultWave, 0, buffer_size);
            }
            mAudioTrack.stop();
            mAudioTrack.release();

        }
    }

}
