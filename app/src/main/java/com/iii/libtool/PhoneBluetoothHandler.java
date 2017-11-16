package com.iii.libtool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.util.*;

//import static com.google.android.gms.internal.zzhu.runOnUiThread;


/**
 * Created by brain on 2016/12/5.
 */

public class PhoneBluetoothHandler {
    // for BT variable
    private String TAG = "PhoneBtHandler";
    private PhoneBluetoothChatService mChatService = null;
    private BluetoothAdapter mBluetoothAdapter;
    //private boolean btConnected = false;
    private boolean enableBluetooth = false;

    // for btHandler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_CONNECTION_FAILED = 6;
    public static final int MESSAGE_DISCONNECTED = 7;

    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    public static final String READ = "read";

    private Context context;

    private BTEventListener connected_listener = null;
    private BTEventListener no_connected_listener = null;
    private BTEventListener get_result_listener = null;
    private ExceptionEventListener exception_listener = null;
    private SettingListener setting_listener = null;
    //private boolean isWearDevice;
    private DataHandler handler;
    int btState = 0;
    Timer timer01;
    boolean timer_running = false;
    int no_data_count = 0;
    int connect_delay = 0;
    int connectDelay = 5;
    int timeout = 4;
    int frame_rate = 25;
    List<String> msgPool;
    int msg_num;
    int msg_delay = 0;
    int msgDelay = 10;
    String device_name = "";
    int[] sensor_type = null;

    public PhoneBluetoothHandler(Context c, DataHandler h){
        context = c;
        //isWearDevice = isWear;
        handler = h;
        msgPool = new ArrayList<String>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setConnectedListener(BTEventListener toAdd) {
        connected_listener = toAdd;
    }

    public void setNoConnectionListener(BTEventListener toAdd) {
        no_connected_listener = toAdd;
    }

    public void setGetResultListener(BTEventListener toAdd) {
        get_result_listener = toAdd;
    }

    public void setExceptionEventListener(ExceptionEventListener toAdd) {
        exception_listener = toAdd;
    }

    public void setSettingListener(SettingListener toAdd) {
        setting_listener = toAdd;
    }

    /*public boolean isConnected(){
        return btConnected;
    }*/

    public void setConnectDelay(int delay){
        connectDelay = delay;
    }

    public int getConnectDelay(){
        return connectDelay;
    }

    public void setTimeout(int t){
        timeout = t;
    }

    public int getTimeout(){
        return timeout;
    }

    public void setFrameRate(int frame){
        frame_rate = frame;
        setMsgPool("frame=" + frame_rate + "\n");
    }

    public int getFrameRate(){
        return frame_rate;
    }

    public String getDeviceName(){
        return device_name;
    }

    public void requestDeviceName(){
        setMsgPool("devicename");
    }
    
    public void setSensorType(int[] s){
        sensor_type = s;
        setMsgPool(getSensorTypeStr());
    }

    public int[] getSensorType(){
        return sensor_type;
    }

    public String getSensorTypeStr(){
        if(sensor_type == null)
            return null;
        String str = "column=";
        for(int i = 0; i < sensor_type.length; i++) {
            str += sensor_type[i];
            if (i < sensor_type.length - 1)
                str += ',';
            else
                str += '\n';
        }
        return str;
    }

    public void setMsgPool(String msg){
        int i = 0;
        if(msg == "" || msg == null)
            return;
        if(msgPool.size() > 0) {
            // do not add the same message
            for (i = 0; i < msgPool.size(); i++)
                if (msgPool.get(i).equals(msg))
                    break;
        }
        if(i == msgPool.size())
            msgPool.add(msg);
    }

    public void sendMsgPool(){
        if(msgPool.size() == 0)
            return;
        write(msgPool.get(0));
    }

    public void ackMsgPool(String response){
        if(msgPool.size() == 0)
            return;
        if(msgPool.get(0).contains(response) || response.contains(msgPool.get(0)))
            msgPool.remove(0);
    }

    public int getMsgNum(){
        return msgPool.size();
    }

    public void setDataHandler(DataHandler h){
        handler = h;
    }

    private void startService(){
        if(enableBluetooth == false)
            return;
        /*if (isWearDevice) {
            if(connect_delay % connectDelay == 0) {
                connectDevice(false);
            }
            connect_delay++;
        } else {*/
        mChatService.start();
        //手機才有此功能
        if(mChatService.getBTState() == false){
            //請開啟藍牙功能
            //runThread();
            if(exception_listener != null)
                exception_listener.onEvent("請開啟藍牙功能");
        }
//}
    }

    public void start() {
        enableBluetooth = true;
        //btConnected = false;
        connect_delay = 0;
        if (mChatService == null) {
            mChatService = new PhoneBluetoothChatService(context, btHandler);
            //手機才有此功能
            if(mChatService == null) {
                //Toast.makeText(context, "請開啟藍牙並重開APP", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (mChatService.getState() == PhoneBluetoothChatService.STATE_NONE) {
            startService();
            if(timer_running == false) {
                timer_running = true;
                Log.i(TAG, "Into timer schedule...timer_running=" + Boolean.toString(timer_running));
                timer01 = new Timer();
                timer01.schedule(task, 0, 1000);
            }
        }
    }

    public void stop(){
        if(mChatService.getState() == PhoneBluetoothChatService.STATE_CONNECTED) {
            if (mChatService != null)
                mChatService.stop();
            //btConnected = false;
        }
        enableBluetooth = false;
    }

    public boolean write(String msg){
        if(mChatService.getState() == PhoneBluetoothChatService.STATE_CONNECTED) {
            Log.i(TAG, "string=" + msg);
            mChatService.write(msg.getBytes());
            return true;
        }
        return false;
    }

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            /*if(isWearDevice == true){
                if(mChatService.getState() == BluetoothChatService.STATE_NONE) {
                    Log.i(TAG, "Into TimerTask...");
                    startService();
                }
            }
            else {*/
            no_data_count++;
            Log.i(TAG, "Into TimerTask...no_data_count=" + Integer.toString(no_data_count));
            if (no_data_count > timeout){
                startService();
            }
            //}
        }
    };

    // The Handler that gets information back from the PhoneBluetoothChatService
    private final Handler btHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case MESSAGE_STATE_CHANGE:

                    switch (msg.arg1) {
                        case PhoneBluetoothChatService.STATE_CONNECTED: {
                            /*if(isWearDevice){
                                Toast.makeText(context, "連線成功", Toast.LENGTH_SHORT).show();
                            }
                            else {*/
                            //Toast.makeText(context, "接受連線已成功", Toast.LENGTH_SHORT).show();
                            setMsgPool("frame=" + frame_rate + "\n");
                            setMsgPool(getSensorTypeStr());
                            requestDeviceName();
                            //}
                            if(connected_listener != null)
                                connected_listener.onEvent(null);
                            //btConnected = true;
                            break;
                        }
                        case PhoneBluetoothChatService.STATE_CONNECTING:
                            break;

                        case PhoneBluetoothChatService.STATE_LISTEN:
                            //btConnected = false;
                            if(no_connected_listener != null)
                                no_connected_listener.onEvent(null);
                            break;

                        case PhoneBluetoothChatService.STATE_NONE:
                            startService();
                            if(no_connected_listener != null)
                                no_connected_listener.onEvent(null);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    break;
                case MESSAGE_READ:
                    String Receiving_Data = msg.getData().getString(READ);
                    if (Receiving_Data != null){
                        /*if(isWearDevice){
                            wearMsgHandler(Receiving_Data);
                        }
                        else{*/
                            /*String str;
                            if(!Receiving_Data.startsWith("0"))
                                str = "1";*/
                        if(Receiving_Data.startsWith("devicename=")){
                            device_name = Receiving_Data.substring(11, Receiving_Data.length() - 1);
                        }
                        int msg_num = msgPool.size();
                        if(msg_num > 0){
                        	if(setting_listener != null){
                            	setting_listener.onEvent(msgPool.size());
                            }
                        }
                        ackMsgPool(Receiving_Data);
                        if(msg_num > 0 && msgPool.size() == 0){ // Only show once
                        	if(setting_listener != null){
                            	setting_listener.onEvent(msgPool.size());
                            }
                        }
                        if(msgPool.size() > 0){
                            if(msg_delay % msgDelay == 0)
                                sendMsgPool();
                            msg_delay++;
                        }
                    }
                    no_data_count = 0;
                    if(get_result_listener != null) {
                        get_result_listener.onEvent(Receiving_Data);
                    }
                    if(handler != null){
                        handler.DealWithData(Receiving_Data);
                    }
                    //}
                    break;
                case MESSAGE_CONNECTION_FAILED:
                    Log.i(TAG, "Connect to device");
                    startService();
                    break;

                case MESSAGE_DISCONNECTED:
                    startService();
                    break;
                case MESSAGE_DEVICE_NAME:
                    break;
                case MESSAGE_TOAST:
                    break;

            }
        }
    };
}
