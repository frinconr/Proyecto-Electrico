package com.example.felipe.harmony3;


import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
@SuppressLint("HandlerLeak")

public class PhoneActivity extends AppCompatActivity{


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
     * Char array representing button state: 1-> pressed 0->released
     */
    private int[] mPointersState= {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};

    /**
     * Notes buttons
     */

    /*
    private Button first_button;
    private Button second_button;
    private Button third_button;
    private Button fourth_button;
    private Button fifth_button;
    private Button sixth_button;
    private Button seventh_button;
    private Button eighth_button;
    private Button nineth_button;
    private Button tenth_button;
*/

    RelativeLayout mRelativeLayout;
    LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone2);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available on your device", Toast.LENGTH_SHORT).show();
            finish();
        }

        mRelativeLayout = (RelativeLayout) findViewById(R.id.PhoneRelativeLayout);
        mLinearLayout =  (LinearLayout) findViewById(R.id.PhoneLinearLayout);



/*
        // Initialize the notes buttons
        first_button    = (Button) findViewById(R.id.first_button);
        second_button   = (Button) findViewById(R.id.second_button);
        third_button    = (Button) findViewById(R.id.third_button);
        fourth_button   = (Button) findViewById(R.id.fourth_button);
        fifth_button    = (Button) findViewById(R.id.fifth_button);
        sixth_button    = (Button) findViewById(R.id.sixth_button);
        seventh_button  = (Button) findViewById(R.id.seventh_button);
        eighth_button   = (Button) findViewById(R.id.eighth_button);
        nineth_button   = (Button) findViewById(R.id.nineth_button);
        tenth_button    = (Button) findViewById(R.id.tenth_button);
*/
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
        getMenuInflater().inflate(R.menu.phone_menu, menu);

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
                Intent serverIntent = new Intent(PhoneActivity.this, DeviceListActivity.class);
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
                    //Log.d(TAG, writeMessage);
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


/*
    //************************************ FIRST BUTTON ********************************************

        if (first_button != null) {
            first_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[0]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[0]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    //************************************ SECOND BUTTON ********************************************
        if (second_button != null) {
            second_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[1]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[1]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }
    //************************************ THIRD BUTTON ********************************************
        if (third_button != null) {
            third_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[2]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[2]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }
    //************************************ THIRD BUTTON ********************************************
        if (fourth_button != null) {
            fourth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[3]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[3]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }
    //************************************ FOURTH BUTTON ********************************************
        if (fourth_button != null) {
            fourth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[3]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[3]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    //************************************ FIFTH BUTTON ********************************************
        if (fifth_button != null) {
            fifth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[4]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[4]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }
    //************************************ SIXTH BUTTON ********************************************
        if (sixth_button != null) {
            sixth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[5]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[5]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    //************************************ SEVENTH BUTTON ********************************************
        if (seventh_button != null) {
            seventh_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[6] = '0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[6] = '1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    //************************************ EIGHTH BUTTON ********************************************
        if (eighth_button != null) {
            eighth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[7]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[7]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    //************************************ NINETH BUTTON ********************************************
        if (nineth_button != null) {
            nineth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[8]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[8]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    //************************************ TENTH BUTTON ********************************************
        if (tenth_button != null) {
            tenth_button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        mButtonsState[9]='0';
                        sendUpdatedState();

                    } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        mButtonsState[9]='1';
                        sendUpdatedState();
                    }

                    return false;
                }
            });
        }

    */

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
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the message bytes and tell the BluetoothChatService to write
        byte[] send = message.getBytes();
        mChatService.write(send);

    }

    /**
     * Sends the state of the note buttons each time they change
     */
    private void sendUpdatedState() {
        String state = new String(mButtonsState);
        Log.d(TAG, state);
        sendMessage(state);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getActionMasked();
        int pointerIndex = MotionEventCompat.getActionIndex(event);
        int pointerId = event.getPointerId(pointerIndex);
        int ButtonIndex;

        switch(action){
            // ACTION_DOWN es el evento que se activa cuando el primer dedo oprime la pantalla
            case MotionEvent.ACTION_DOWN:
                // Con las coordenadas del puntero se encuentra el numero del boton que esta presionando
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                // En caso de que si este presionando alguno se guarda el numero de boton en mPointersState
                // con el ID del puntero como indice.Sino, se guarda un -1 por si el dedo se mueve despues.
                // Se cambia el estado del boton a activo y se envia el mensaje de bluetooth.
                mPointersState[pointerId]=ButtonIndex;
                if(ButtonIndex!=-1) {
                    mButtonsState[ButtonIndex]='1';
                    sendUpdatedState();
                }else{
                    return false;
                }
                //Log.d("DACTION:", "Button" + String.valueOf(ButtonIndex) +"Pressed by:"+String.valueOf(pointerIndex)+","+String.valueOf(pointerId));
                break;
            // El evento ACTION_UP ocurre cuando el primer dedo deja de tocar la pantalla. En este caso se cambia el estado del numero de boton
            // que se habia guardado en el arreglo mPointerState y se resetea el puntero colocando un -1.
            case MotionEvent.ACTION_UP:
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                if(mPointersState[pointerId]!=-1) {
                    mButtonsState[mPointersState[pointerId]] = '0';
                    mPointersState[pointerId] = -1;
                    sendUpdatedState();
                }
                //Log.d("DACTION:", "Button" + String.valueOf(ButtonIndex) +"Released by:"+String.valueOf(pointerIndex)+","+String.valueOf(pointerId));
               break;
            // El evento ACTION_POINTER_DOWN ocurre cuando mas de un dedo presiona la pantalla. Se realizan las mismas
            // acciones que en el ACTION_DOWN.
            case MotionEvent.ACTION_POINTER_DOWN:
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                mPointersState[pointerId]=ButtonIndex;
                if(ButtonIndex!=-1) {
                    mButtonsState[ButtonIndex]='1';
                    sendUpdatedState();
                }
                //Log.d("DACTION:", "Button" + String.valueOf(ButtonIndex) +"Pressed by:"+String.valueOf(pointerIndex)+","+String.valueOf(pointerId));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                ButtonIndex = FindButtonPressed((int)event.getX(pointerIndex), (int)event.getY(pointerIndex));
                if(mPointersState[pointerId]!=-1) {
                    mButtonsState[mPointersState[pointerId]]='0';
                    mPointersState[pointerId] = -1;
                    sendUpdatedState();
                }
                //Log.d("DACTION:", "Button" + String.valueOf(ButtonIndex) +"Released by:"+String.valueOf(pointerIndex)+","+String.valueOf(pointerId));
                break;

            // El evento ACTION_MOVE ocurre cuando alguno de los dedos que toca la pantalla, se mueve.
            // En este caso hay que revisar si el puntero salio de los limites del boton que estaba tocando.
            // Si se sale de los limites, se inhabilita el boton pasado en mButtonState y ase activa el actual.
            // Se guarada el nuevo estado del puntero en mPointerState.

            case MotionEvent.ACTION_MOVE:

                for(int i = 0; i < event.getPointerCount();i++) {

                    ButtonIndex = FindButtonPressed((int) event.getX(i), (int) event.getY(i));
                    pointerId = event.getPointerId(i);
                    // En caso de que el dedo se haya desplazado de su posicion actual
                    if (ButtonIndex != -1 & ButtonIndex != mPointersState[pointerId]) {
                        mButtonsState[mPointersState[pointerId]] = '0';
                        mPointersState[pointerId] = ButtonIndex;
                        mButtonsState[ButtonIndex] = '1';
                        sendUpdatedState();
                    // En caso de que el dedo no haya tocado ningun boton, pero se movio despues.
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




    private int FindButtonPressed(int XCoor, int YCoord){
        for(int i =0; i< mLinearLayout.getChildCount(); i++)
        {

            ViewGroup LinearLayChild = (ViewGroup) mLinearLayout.getChildAt(i);

            for (int j =0; j< LinearLayChild.getChildCount(); j++) {

                View view = LinearLayChild.getChildAt(j);
                int[] ViewLocation = new int[2];
                view.getLocationOnScreen(ViewLocation);
                Rect outRect = new Rect(ViewLocation[0], ViewLocation[1], ViewLocation[0] + view.getWidth(), ViewLocation[1] + view.getHeight());

               if (outRect.contains(XCoor, YCoord)) {
                    return GetButtonNumber(getResources().getResourceEntryName(view.getId()));
                }
            }
        }
        return -1;
    }

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
            default: return 0;
        }
    }


}
