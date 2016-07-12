package com.example.felipe.harmony3;

import android.content.Context;
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
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.ToggleButton;


public class TabletActivity extends AppCompatActivity {

    /**
     * Tag for Log
     */
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
     * Inhale thread's runnable for audio synthesis
     */
    private MusicTask InhaleRunnable;

    /**
     * Exhale thread's runnable for audio synthesis
     */
    private MusicTask ExhaleRunnable;


    /**
     * Boolean to activate square wave synthesis
     */
    private boolean SquareEffect = false;

    /**
     * Boolean to activate sawtooth wave synthesis
     */
    private boolean SawtoothEffect = false;

    /**
     * Boolean to activate triangle wave synthesis
     */
    private boolean TriangleEffect = false;

    /**
     * Finger Y coordinate at Inhale button
     */
    private float InhaleYPosition;

    /**
     * Normalized from 0-1 finger Y coordinate at inhale button
     */
    private float InhaleYOffset;

    /**
     * Finger X coordinate at Inhale button
     */
    private float InhaleXPosition;

    /**
     * Normalized from 0-1 finger X coordinate at inhale button
     */
    private float InhaleXOffset;

    /**
     * Finger Y coordinate at Exhale button
     */
    private float ExhaleYPosition;
    /**
     * Normalized from 0-1 finger Y coordinate at exhale button
     */
    private float ExhaleYOffset;

    /**
     * Finger X coordinate at Exhale button
     */
    private float ExhaleXPosition;

    /**
     * Normalized from 0-1 finger X coordinate at exhale button
     */
    private float ExhaleXOffset;

    /**
     * AudioManager object to control volume
     */
    AudioManager audioManager;


    /**
     * Called when the activity is starting. Initialize the BluetoothAdapter and set AudioManager
     * configuration to control de volume
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     *                           then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     *                           Note: Otherwise it is null.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablet2);
        //Obtain BluetoothAdapter for managing all Bluetooth actions
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if devices has Bluetooth available
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available on your device", Toast.LENGTH_SHORT).show();
            finish();
        }
        //Initialize AudioManager object
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        // Get the maximum volume of the device
        int MaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // Get current volume of the device
        int CurrrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        // Initialize Seekbar to control volume in the application
        SeekBar VolumenBar = (SeekBar)findViewById(R.id.VolumeBar);
        //Set maximum volume as the maximum value of the seekbar
        VolumenBar.setMax(MaxVolume);
        //Initialize seekbar at device current volume
        VolumenBar.setProgress(CurrrentVolume);

        // Change the streaming volume as the volume seekbar is modified by the user.
        VolumenBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
            }
        });
    }

    /**
     * If BT is not on, request that it be enabled.
     * setupChat() will then be called during onActivityResult.
     */
    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (mChatService == null) {
            setupChat();
        }
    }

    /**
     * Release resources from BluetoothChatService
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    /**
     * Covers the case in which BT was not enabled during onStart(), so we were paused to enable it.
     * onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mChatService != null) {
            // STATE_NONE indicates that BluetoothChatService hasn't been started
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu
     * @param menu The options menu
     * @return Returns true for the menu to be displayed
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tablet_menu, menu);

        return true;
    }

    /**
     * This method is called whenever an item in the options menu is selected
     * @param item Menu item id defined on xml description
     * @return Returns true if action was processed or false in case not
     */
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

    /**
     *Called when the users responds to the Bluetooth enabled request or when a device id selected
     *on DeviceListActivity.
     *@param requestCode
     *          Code with which the activity was started
     *@param resultCode
     *          Resulted code returned form activity
     *@param data
     *          Any additional data from it
     */
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
                        // Bluetooth connection has change state. Change header subtitle for notify user.
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to)+ " " + mConnectedDeviceName);
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
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //Transform string to char array
                    UpdatePhoneStatus(readMessage);
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
     * Set up the UI and background operations for chat. Also define the functions of inhale and
     * exhale buttons and all the effects toggle buttons.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the inhale button
        final Button Inhale_button = (Button) findViewById(R.id.inhale_button);

        if (Inhale_button != null) {
            Inhale_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if(event.getAction()==MotionEvent.ACTION_UP){
                        // Stops audio streaming and resets all variables related to finger position
                        InhaleRunnable.Stop();
                        InhaleYPosition = 0;
                        InhaleXPosition = 0;
                        InhaleYOffset = 0;
                        InhaleXOffset = 0;
                    }else if(event.getAction()==MotionEvent.ACTION_DOWN){
                        // Save the pointer coordinates at first touch and reset the offsets
                        InhaleYPosition = event.getY();
                        InhaleXPosition = event.getRawX();
                        InhaleYOffset = 0;
                        InhaleXOffset = 0;
                        //for debugging the alpha and beta calculations at inhale button
                        Log.d("Boton de Inhalar", "Alpha = " + String.valueOf(InhaleYOffset)+" Beta = " + String.valueOf(InhaleXOffset) );
                        // Initialize synthesis task to be assigned to the inhale thread for execution
                        InhaleRunnable = new MusicTask(true);
                        Thread InhaleThread = new Thread(InhaleRunnable);
                        InhaleThread.start();

                    }else if(event.getAction()==MotionEvent.ACTION_MOVE){
                        // Some finger is moving so calculate the Y offset from initial position
                        InhaleYOffset = InhaleYPosition-event.getY();
                        // Is moving up if the offset is positive, else is moving down through the screen
                        if(InhaleYOffset>0){
                            InhaleYOffset = InhaleYOffset/(InhaleYPosition-Inhale_button.getTop());
                        }
                        if(InhaleYOffset<0){
                            InhaleYOffset = InhaleYOffset/(Inhale_button.getBottom()-InhaleYPosition);

                        }
                        // Calculate the X offset from initial position
                        InhaleXOffset = event.getRawX()-InhaleXPosition;
                        // Is moving to the right if the offset is positive, else is moving left through the screen
                        if(InhaleXOffset>0){
                            InhaleXOffset = InhaleXOffset/(Inhale_button.getRight()-InhaleXPosition);
                        }
                        if(InhaleXOffset<0) {
                            InhaleXOffset = InhaleXOffset / (InhaleXPosition - Inhale_button.getLeft());
                        }
                        //for debugging the alpha and beta calculations at inhale button
                        Log.d("Boton de Inhalar", "Alpha = " + String.valueOf(InhaleYOffset)+" Beta = " + String.valueOf(InhaleXOffset) );


                    }

                    return false;
                }
            });


        }
        // Initialize the exhale button
        final Button Exhale_button = (Button) findViewById(R.id.exhale_button);

        if (Exhale_button != null) {
            Exhale_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if(event.getAction()==MotionEvent.ACTION_UP){

                        // Stops audio streaming and resets all variables related to finger position
                        ExhaleRunnable.Stop();
                        ExhaleYPosition = 0;
                        ExhaleXPosition = 0;
                        ExhaleYOffset = 0;
                        ExhaleXOffset = 0;

                    }
                    if(event.getAction()==MotionEvent.ACTION_DOWN){
                        // Save the pointer coordinates at first touch and reset the offsets
                        ExhaleYPosition = event.getY();
                        ExhaleXPosition = event.getRawX();
                        ExhaleYOffset = 0;
                        ExhaleXOffset = 0;
                        //for debugging the alpha and beta calculations at exhale button
                        Log.d("Boton de Exhalar", "Alpha = " + String.valueOf(ExhaleYOffset)+" Beta = " + String.valueOf(ExhaleXOffset) );
                        // Initialize synthesis task to be assigned to the inhale thread for execution
                        ExhaleRunnable = new MusicTask(false);
                        Thread ExhaleThread = new Thread(ExhaleRunnable);
                        ExhaleThread.start();

                    }

                    if(event.getAction()==MotionEvent.ACTION_MOVE){
                        // Some finger is moving so calculate the Y offset from initial position
                        ExhaleYOffset = ExhaleYPosition-event.getY();
                        // Is moving up if the offset is positive, else is moving down through the screen
                        if(ExhaleYOffset>0){
                            ExhaleYOffset = ExhaleYOffset/(ExhaleYPosition-Exhale_button.getTop());
                        }
                        if(ExhaleYOffset<0){
                            ExhaleYOffset = ExhaleYOffset/(Exhale_button.getBottom()-ExhaleYPosition);

                        }
                        // Calculate the X offset from initial position
                        ExhaleXOffset = event.getRawX()-ExhaleXPosition;
                        // Is moving to the right if the offset is positive, else is moving left through the screen
                        if(ExhaleXOffset>0){
                            ExhaleXOffset = ExhaleXOffset/(Exhale_button.getRight()-ExhaleXPosition);
                        }
                        if(ExhaleXOffset<0) {
                            ExhaleXOffset = ExhaleXOffset / (ExhaleXPosition - Exhale_button.getLeft());
                        }
                        //for debugging the alpha and beta calculations at exhale button
                        Log.d("Boton de Exhalar", "Alpha = " + String.valueOf(ExhaleYOffset)+" Beta = " + String.valueOf(ExhaleXOffset) );

                    }
                    return false;
                }
            });
        }

        // Initialize all toogle buttons for sound effect selection
        final ToggleButton SquareToggleButton = (ToggleButton) findViewById(R.id.SquareButton);
        final ToggleButton SawToggleButton = (ToggleButton) findViewById(R.id.SawtoothButton);
        final ToggleButton TriangleButton = (ToggleButton) findViewById(R.id.TriangleButton);

        // Each button sets to true it's corresponding boolean and sets the other ones to false
        // so that only one can be selected at the time.

        if (SquareToggleButton != null) {
            SquareToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SquareEffect = isChecked;
                    if(isChecked){
                        SawToggleButton.setChecked(false);
                        TriangleButton.setChecked(false);
                    }
                }
            });
        }


        if (SawToggleButton != null) {
            SawToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SawtoothEffect = isChecked;
                    if(isChecked){
                        SquareToggleButton.setChecked(false);
                        TriangleButton.setChecked(false);

                    }
                }
            });
        }


        if (TriangleButton != null) {
            TriangleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    TriangleEffect = isChecked;
                    if(isChecked){
                        SawToggleButton.setChecked(false);
                        SquareToggleButton.setChecked(false);
                    }
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

    /**
     * Converts the incoming state string into a char array and save it in the local variable
     * @param state State String received from the phone buttons status
     */
    private void UpdatePhoneStatus(String state){

        mPhoneButtonStatus = state.toCharArray();

    }

    /**
     * Class dedicated to the synthesis of the audio. Initializes the corresponding variables for
     * inhale or exhale streaming. Manages all actions of AudioTrack object and generates the
     * selected wave to create and be played.
     */

    class MusicTask implements Runnable{

        /**
         * Boolean to know id define variables for inhale or exhale audio playback.
         * True -> Inhale playback
         * False -> Exhale playback
         */
        private boolean InhaleOrExhale;

        /**
         * Boolean to know when to stop synthesis task. It is modified if the user stops touching
         * the instrument buttons.
         */
        private volatile boolean Stop = false;

        /**
         * Constructor. Prepares a new runnable task.
         *
         * @param mInhaleOrExhale Boolean to know if exhaling or inhaling
         */
        public MusicTask(boolean mInhaleOrExhale){
            this.InhaleOrExhale  = mInhaleOrExhale;
        }

        /**
         * Stop function sets the Stop boolean to true when user stops selecting the inhale or exhale button
         */
        public void Stop(){
            Stop = true;
        }

        /**
         * run method contains all the synthesis procedure. All variables and loops to create the
         * desired wave.
         */

        @Override
        public void run(){

            // Synthesis sampling rate. Double of audible bandwidth
            int SamplingRate = 44100;
            // Get the minimum size of playback buffer
            int buffer_size = AudioTrack.getMinBufferSize(SamplingRate,AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            // Initialize AudioTrack object with streaming mode, sampling rate, mono format,16bit encoding and previous minimum buffer size calculated
            AudioTrack mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SamplingRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size, AudioTrack.MODE_STREAM);

            // Array of shorts that will store a part of the generated wave to be played the size of the buffer.
            // There are 10 for each possible button on smartphone
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

            // The ResultWave is the normalized sum of each note produced. Is the one send to playback buffer.
            short ResultWave[] = new short[buffer_size];

            // Amplitud of all waves generated
            int Amplitud = 10000;
            // 2*pi constant
            double twopi = 8.*Math.atan(1.);

            // Define all the possible frequencies possible for the 10 notes at the smartphone depending if
            // exhaling or inhaling.
            double Frequency_button1 = InhaleOrExhale? Notes.D1*Math.pow(2,3):Notes.C1*Math.pow(2,3);
            double Frequency_button2 = InhaleOrExhale? Notes.G1*Math.pow(2,3):Notes.E1*Math.pow(2,3);
            double Frequency_button3 = InhaleOrExhale? Notes.B1*Math.pow(2,3):Notes.G1*Math.pow(2,3);
            double Frequency_button4 = InhaleOrExhale? Notes.D1*Math.pow(2,4):Notes.C1*Math.pow(2,4);
            double Frequency_button5 = InhaleOrExhale? Notes.F1*Math.pow(2,4):Notes.E1*Math.pow(2,4);
            double Frequency_button6 = InhaleOrExhale? Notes.A1*Math.pow(2,4):Notes.G1*Math.pow(2,4);
            double Frequency_button7 = InhaleOrExhale? Notes.B1*Math.pow(2,5):Notes.C1*Math.pow(2,5);
            double Frequency_button8 = InhaleOrExhale? Notes.D1*Math.pow(2,5):Notes.E1*Math.pow(2,5);
            double Frequency_button9 = InhaleOrExhale? Notes.F1*Math.pow(2,5):Notes.G1*Math.pow(2,5);
            double Frequency_button10 = InhaleOrExhale? Notes.A1*Math.pow(2,6):Notes.C1*Math.pow(2,6);

            // Auto Vibrato frequency constant of 10Hz
            float AutoVibFreq = 10;

            // All the 10 phases of the automatic vibrato.
            float AutoPhase_b1 = 0;
            float AutoPhase_b2 = 0;
            float AutoPhase_b3 = 0;
            float AutoPhase_b4 = 0;
            float AutoPhase_b5 = 0;
            float AutoPhase_b6 = 0;
            float AutoPhase_b7 = 0;
            float AutoPhase_b8 = 0;
            float AutoPhase_b9 = 0;
            float AutoPhase_b10 = 0;

            // Phases to evaluate each part of the notes wave generated to be playback
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


            // Initialize alpha and beta constants
            double alpha = 0.0;
            double beta = 0.0;


            // Start audio
            mAudioTrack.play();

            // Synthesis loop
            while(!Stop){
                // Assign Alpha and Beta depending of motion in inhale or exhale buttons
                alpha    = InhaleOrExhale? InhaleYOffset:ExhaleYOffset;
                beta     = InhaleOrExhale? InhaleXOffset:ExhaleXOffset;

                // Create a chunk the size of buffer for each note selected
                for(int i=0; i < buffer_size; i++){

                    // Here are all the possible buttons, each one has:
                    // All effects possible: sine, sawtooth, square or triangle wave.
                    // Automatic vibrato generation using alpha and beta parameters.
                    // Saving of wave phase to generate a continuous wave each tie the loop ends.
                    // If a note isn't selected, the array saves zeros.

                    //************************ FIRST BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[0]=='1'){

                        if(SquareEffect) {
                            samples_button1[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button1)));

                        }else if(SawtoothEffect){
                            samples_button1[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button1/2))));
                        }else if(TriangleEffect){
                            samples_button1[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button1)));
                        }
                        else{
                            samples_button1[i] = (short) (Amplitud * Math.sin(phase_button1));
                        }


                        phase_button1 += (float) twopi*(Frequency_button1+(alpha*Frequency_button1*Math.cos(AutoPhase_b1)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b1 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b1 = 0;
                        }
                    }else{samples_button1[i] = 0;}

                    //************************ SECOND BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[1]=='1'){

                        if(SquareEffect) {
                            samples_button2[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button2)));

                        }else if(SawtoothEffect){
                            samples_button2[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button2/2))));
                        }else if(TriangleEffect){
                            samples_button2[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button2)));
                        }
                        else{
                            samples_button2[i] = (short) (Amplitud * Math.sin(phase_button2));
                        }
                        phase_button2 += (float) twopi*(Frequency_button2+(alpha*Frequency_button2*Math.cos(AutoPhase_b2)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b2 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b2 = 0;
                        }
                    }else{samples_button2[i] = 0;}

                    //************************ THIRD BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[2]=='1'){


                        if(SquareEffect) {
                            samples_button3[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button3)));

                        }else if(SawtoothEffect){
                            samples_button3[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button3/2))));
                        }else if(TriangleEffect){
                            samples_button3[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button3)));
                        }
                        else{
                            samples_button3[i] = (short) (Amplitud * Math.sin(phase_button3));
                        }
                        phase_button3 += (float) twopi*(Frequency_button3+(alpha*Frequency_button3*Math.cos(AutoPhase_b3)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b3 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b3 = 0;
                        }
                    }else{samples_button3[i] = 0;}

                    //************************ FOURTH BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[3]=='1'){

                        if(SquareEffect) {
                            samples_button4[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button4)));

                        }else if(SawtoothEffect){
                            samples_button4[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button4/2))));
                        }else if(TriangleEffect){
                            samples_button4[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button4)));
                        }
                        else{
                            samples_button4[i] = (short) (Amplitud * Math.sin(phase_button4));
                        }
                        phase_button4 += (float) twopi*(Frequency_button4+(alpha*Frequency_button4*Math.cos(AutoPhase_b4)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b4 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b4 = 0;
                        }
                    }else{samples_button4[i] = 0;}


                    //************************ FIFTH BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[4]=='1'){

                        if(SquareEffect) {
                            samples_button5[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button5)));

                        }else if(SawtoothEffect){
                            samples_button5[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button5/2))));
                        }else if(TriangleEffect){
                            samples_button5[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button5)));
                        }
                        else{
                            samples_button5[i] = (short) (Amplitud * Math.sin(phase_button5));
                        }
                        phase_button5 += (float) twopi*(Frequency_button5+(alpha*Frequency_button5*Math.cos(AutoPhase_b5)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b5 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b5 = 0;
                        }
                    }else{samples_button5[i] = 0;}


                    //************************ SIXTH BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[5]=='1'){

                        if(SquareEffect) {
                            samples_button6[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button6)));

                        }else if(SawtoothEffect){
                            samples_button6[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button6/2))));
                        }else if(TriangleEffect){
                            samples_button6[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button6)));
                        }
                        else{
                            samples_button6[i] = (short) (Amplitud * Math.sin(phase_button6));
                        }
                        phase_button6 += (float) twopi*(Frequency_button6+(alpha*Frequency_button6*Math.cos(AutoPhase_b6)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b6 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b6 = 0;
                        }
                    }else{samples_button6[i] = 0;}


                    //************************ SEVENTH BUTTON SOUNDS *******************************
                    if(mPhoneButtonStatus[6]=='1'){

                        if(SquareEffect) {
                            samples_button7[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button7)));

                        }else if(SawtoothEffect){
                            samples_button7[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button7/2))));
                        }else if(TriangleEffect){
                            samples_button7[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button7)));
                        }
                        else{
                            samples_button7[i] = (short) (Amplitud * Math.sin(phase_button7));
                        }
                        phase_button7 += (float) twopi*(Frequency_button7+(alpha*Frequency_button7*Math.cos(AutoPhase_b7)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b7 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b7 = 0;
                        }
                    }else{samples_button7[i] = 0;}

                    //************************ EIGHTH BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[7]=='1'){

                        if(SquareEffect) {
                            samples_button8[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button8)));

                        }else if(SawtoothEffect){
                            samples_button8[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button8/2))));
                        }else if(TriangleEffect){
                            samples_button8[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button8)));
                        }
                        else{
                            samples_button8[i] = (short) (Amplitud * Math.sin(phase_button8));
                        }
                        phase_button8 += (float) twopi*(Frequency_button8+(alpha*Frequency_button8*Math.cos(AutoPhase_b8)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b8 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b8 = 0;
                        }
                    }else{samples_button8[i] = 0;}

                    //************************ NINETH BUTTON SOUNDS ********************************
                    if(mPhoneButtonStatus[8]=='1'){

                        if(SquareEffect) {
                            samples_button9[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button9)));
                        }else if(SawtoothEffect){
                            samples_button9[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button9/2))));
                        }else if(TriangleEffect){
                            samples_button9[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button9)));
                        }
                        else{
                            samples_button9[i] = (short) (Amplitud * Math.sin(phase_button9));
                        }
                        phase_button9 += (float) twopi*(Frequency_button9+(alpha*Frequency_button9*Math.cos(AutoPhase_b9)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b9 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b9 = 0;
                        }
                    }else{samples_button9[i] = 0;}

                    //************************ TENTH BUTTON SOUNDS *********************************
                    if(mPhoneButtonStatus[9]=='1'){

                        if(SquareEffect) {
                            samples_button10[i] = (short) (Amplitud * Math.signum(Math.sin(phase_button10)));

                        }else if(SawtoothEffect){
                            samples_button10[i] = (short) (-4*Amplitud/twopi * Math.atan(1/(Math.tan(phase_button10/2))));
                        }else if(TriangleEffect){
                            samples_button10[i] = (short) (4*Amplitud/twopi * Math.asin(Math.sin(phase_button10)));
                        }
                        else{
                            samples_button10[i] = (short) (Amplitud * Math.sin(phase_button10));
                        }
                        phase_button10 += (float) twopi*(Frequency_button10+(alpha*Frequency_button10*Math.cos(AutoPhase_b10)/12))/SamplingRate;
                        if(Math.abs(beta)>0.25) {
                            AutoPhase_b10 += twopi * beta * AutoVibFreq / SamplingRate;
                        }else{
                            AutoPhase_b10 = 0;
                        }
                    }else{samples_button10[i] = 0;}

                    // Calculate the resulting wave by adding all the notes and normalize the amplitude
                    // depending on how many notes are selected.

                    ResultWave[i] = (short) ((samples_button1[i]
                            + samples_button2[i]
                            + samples_button3[i]
                            + samples_button4[i]
                            + samples_button5[i]
                            + samples_button6[i]
                            + samples_button7[i]
                            + samples_button8[i]
                            + samples_button9[i]
                            + samples_button10[i])/NotesPlaying());
                }
                // Send the result wave to playback
                mAudioTrack.write(ResultWave, 0, buffer_size);
            }
            // Stops AudioTrack streaming
            mAudioTrack.stop();
            // Release AudioTrack resources
            mAudioTrack.release();

        }
    }

    /**
     * Calculates with activity's global variable mPhoneButtonStatus how many notes are selected on
     * the smartphone.
     * @return An integer between 1 and 10 representing the amount of notes currently being played
     */
    private int NotesPlaying(){
        int counter = 0;
        for(int i=0; i < mPhoneButtonStatus.length; i++){
            if(mPhoneButtonStatus[i]=='1'){counter++;}
        }
        if(counter==0){return 1;}
        return counter;
    }

}
