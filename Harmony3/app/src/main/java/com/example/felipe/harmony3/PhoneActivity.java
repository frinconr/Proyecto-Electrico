package com.example.felipe.harmony3;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class PhoneActivity extends AppCompatActivity{


    /**
     * Tag for Log
     */
    private static final String TAG = "PhoneActivity";


    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE= 1;
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
    private char[] mButtonsState= {'0','0','0','0','0','0','0','0','0','0'};

    /**
     * Int array that stores which number of button an specific finger is touching on screen:
     * -1->Inactive pointer {0-9} -> Number of button
     */
    private int[] mPointersState= {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};

    //Layouts of user interface
    RelativeLayout mRelativeLayout;
    LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone2);

        //Obtain BluetoothAdapter for managing all Bluetooth actions
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if devices has Bluetooth available
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available on your device", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initialize layouts with xml definitions
        mRelativeLayout = (RelativeLayout) findViewById(R.id.PhoneRelativeLayout);
        mLinearLayout =  (LinearLayout) findViewById(R.id.PhoneLinearLayout);
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
        // Inflate the menu; adds items to the action bar.
        getMenuInflater().inflate(R.menu.phone_menu, menu);
        return true;
    }

    /**
     * This method is called whenever an item in the options menu is selected
     * @param item Menu item id defined on xml description
     * @return Returns true if action was processed or false in case not
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar click actions.
        int id = item.getItemId();
        switch (id) {
            case R.id.devices_list:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(PhoneActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.Discover_device:
                // Discover device for other to scan it.
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
                            setStatus(getString(R.string.title_connected_to)+" "+mConnectedDeviceName);
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
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
    }


    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();

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

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();


        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check stablished connection
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the message bytes and tell the BluetoothChatService to write
        byte[] send = message.getBytes();
        mChatService.write(send);

    }

    /**
     * Converts the Char array mButtonState to send button state via Bluetooth
     */
    private void sendUpdatedState() {
        String state = new String(mButtonsState);
        Log.d(TAG, state);
        sendMessage(state);
    }
    /**
    *Called when a touch screen motion event occurs.
    */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        //Get the type of action that invoke the method
        int action = event.getActionMasked();
        //Get the pointer index
        int pointerIndex = MotionEventCompat.getActionIndex(event);
        //Get the unique pointer ID
        int pointerId = event.getPointerId(pointerIndex);
        // Number of button the pointer is touching
        int ButtonIndex;

        switch(action){
            // ACTION_DOWN is the event that happens when the first finger touches the screen
            case MotionEvent.ACTION_DOWN:
                //With the pointer coordinates we found the number of button thats pressing
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                //In case the finger is touching some button, we save that number in mPointersState array
                //using the pointer's ID as index. If not, a -1 is stored in case the pointer moves around.
                //Button state is activated and message is sent.

                mPointersState[pointerId]=ButtonIndex;
                if(ButtonIndex!=-1) {
                    mButtonsState[ButtonIndex]='1';
                    sendUpdatedState();
                }else{
                    return false;
                }
                //Log.d("DACTION:", "Button" + String.valueOf(ButtonIndex) +"Pressed by:"+String.valueOf(pointerIndex)+","+String.valueOf(pointerId));
                break;

            //ACTION_UP occurs when the first pointer stops touching the screen. This will deactivate the pointer and the effect of the button.
            // The pointer number is reset to -1 in mPointerState array.
            case MotionEvent.ACTION_UP:
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                if(mPointersState[pointerId]!=-1) {
                    mButtonsState[mPointersState[pointerId]] = '0';
                    mPointersState[pointerId] = -1;
                    sendUpdatedState();
                }
               break;

            // ACTION_POINTER_DOWN occurs when more than one finger are touching the screen. The instructions are
            // the same as ACTION_DOWN.
            case MotionEvent.ACTION_POINTER_DOWN:
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                mPointersState[pointerId]=ButtonIndex;
                if(ButtonIndex!=-1) {
                    mButtonsState[ButtonIndex]='1';
                    sendUpdatedState();
                }
                break;
            // ACTION_POINTER_UP occurs when a pointer different than the first one releases the pressure form the button . The instructions are
            // the same as ACTION_UP.
            case MotionEvent.ACTION_POINTER_UP:
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                if(mPointersState[pointerId]!=-1) {
                    mButtonsState[mPointersState[pointerId]]='0';
                    mPointersState[pointerId] = -1;
                    sendUpdatedState();
                }
                break;

            // ACTION_MOVE occurs when any of the pointers touching the screen starts moving around.
            // in this case we check if the the pointer was touching a button so that we deactivate
            // the old one and activate the new one. The new pointer state is changed on mPointersState.

            case MotionEvent.ACTION_MOVE:

                for(int i = 0; i < event.getPointerCount();i++) {

                    ButtonIndex = FindButtonPressed((int) event.getX(i), (int) event.getY(i));
                    pointerId = event.getPointerId(i);
                    // In case the pointer has left the button it was touching.
                    if (ButtonIndex != -1 & ButtonIndex != mPointersState[pointerId]) {
                        mButtonsState[mPointersState[pointerId]] = '0';
                        mPointersState[pointerId] = ButtonIndex;
                        mButtonsState[ButtonIndex] = '1';
                        sendUpdatedState();
                    // In case some finger had touched the screen outside any button and then moved
                    // to activate one.
                    }else if(ButtonIndex != -1 & mPointersState[pointerId]==-1){
                        mPointersState[pointerId] = ButtonIndex;
                        mButtonsState[ButtonIndex] = '1';
                        sendUpdatedState();
                    }
                }
                break;

        }

        return super.onTouchEvent(event);
    }

    /**
     * This function takes a pointer's coordinates and seeks which button it is touching.
     *
     * @param XCoor X coordinate on screen
     * @param YCoord Y coordinate on screen
     * @return Returns an integer between 0 and 9 indicating the number of button or -1 if isn't touching any
     */


    private int FindButtonPressed(int XCoor, int YCoord){

        //Check all item in xml definition finding buttons
        for(int i =0; i< mLinearLayout.getChildCount(); i++)
        {

            ViewGroup LinearLayChild = (ViewGroup) mLinearLayout.getChildAt(i);

            for (int j =0; j< LinearLayChild.getChildCount(); j++) {

                View view = LinearLayChild.getChildAt(j);
                int[] ViewLocation = new int[2];
                view.getLocationOnScreen(ViewLocation);
                //Get the button coordinates
                Rect outRect = new Rect(ViewLocation[0], ViewLocation[1], ViewLocation[0] + view.getWidth(), ViewLocation[1] + view.getHeight());
                // Compares the pointer's coordinates with the ones of the button
               if (outRect.contains(XCoor, YCoord)) {
                    return GetButtonNumber(getResources().getResourceEntryName(view.getId()));
                }
            }
        }
        return -1;
    }

    /**
     * Converts the id of a button in a number to process
     * @param Id Id taken when finding button coordinates
     * @return Integer between 0-9 indicating number of button
     */

    private int GetButtonNumber(String Id){

        switch(Id){
            case "first_button":    return 0;
            case "second_button":   return 1;
            case "third_button":    return 2;
            case "fourth_button":   return 3;
            case "fifth_button":    return 4;
            case "sixth_button":    return 5;
            case "seventh_button":  return 6;
            case "eighth_button":   return 7;
            case "nineth_button":   return 8;
            case "tenth_button":    return 9;
            default: return -1;
        }
    }
}
