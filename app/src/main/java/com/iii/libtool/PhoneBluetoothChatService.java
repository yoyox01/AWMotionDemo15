/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iii.libtool;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class PhoneBluetoothChatService {
    // Debugging
    private static final String TAG = "PhoneBtChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;

    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private boolean btState = false;
    //private boolean isWearDevice;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public PhoneBluetoothChatService(Context context, Handler handler/*, boolean isWear*/) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
        //isWearDevice = isWear;
    }

    public boolean getBTState(){
        return btState;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "[PBCS]_setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();

    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "[PBCS]_start() -ChatService start()");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            if(mSecureAcceptThread.getBTState()) {
                mSecureAcceptThread.start();
                btState = true;
            }
            else
                btState = false;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            if(mInsecureAcceptThread.getBTState()) {
                mInsecureAcceptThread.start();
                btState = true;
            }
            else
                btState = false;
        }
        /*if(isWearDevice == true){
            Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    Log.e("Mac Addressess","are:  "+mAdapter.getRemoteDevice(device.getAddress()));
                }
            }
        }*/

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {

        Log.d(TAG, "[PBCS]_connect(device, secure) -Into BT Service connect...");
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "[PBCS]_connected(socket,device,type) -Socket Type: " +socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity

        Message msg = mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneBluetoothHandler.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "[PBCS]_stop()");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_CONNECTION_FAILED);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneBluetoothHandler.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity

        Message msg = mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_DISCONNECTED);
        Bundle bundle = new Bundle();
        bundle.putString(PhoneBluetoothHandler.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_NONE);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        //private final BluetoothServerSocket mmServerSocket;
        private BluetoothServerSocket mmServerSocket;
        private String mSocketType;
        private boolean mSecure;
        private boolean btState = false;

        public AcceptThread(boolean secure) {
            mSecure = secure;
            btState = initServerSocket(mSecure);
        }

        public boolean getBTState(){
            return btState;
        }

        public boolean initServerSocket(boolean secure){
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure":"Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    Log.d(TAG, "[PBCS]_AcceptThread() -if(secure) Socket Type: " + mSocketType + " "+MY_UUID_SECURE);
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    Log.d(TAG, "[PBCS]_AcceptThread() -if(insecure) Socket Type: " + mSocketType + " "+MY_UUID_INSECURE);
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "[PBCS]_AcceptThread() -Socket Type: " + mSocketType + "listen() failed");
                Log.e(TAG, "IOException = "+ e);
            }
            mmServerSocket = tmp;
            if(mmServerSocket == null)
                return false;
            else
                return true;
        }

        public void run() {
            if (D) Log.d(TAG, "[PBCS]_AcceptThread() -run() -SocketType: " +mSocketType+ " BEGIN mAcceptThread" +this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            while(mmServerSocket == null)
                initServerSocket(mSecure);

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    Log.d(TAG, "[PBCS]_AcceptThread() -run() -while (mState != STATE_CONNECTED)");
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "[PBCS]_AcceptThread() -run() -Socket Type: " + mSocketType + " accept() failed");
                    Log.e(TAG, "IOException = "+ e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (PhoneBluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "[PBCS]_AcceptThread() Could not close unwanted socket");
                                    Log.e(TAG, "IOException = "+ e);
                                }
                                break;
                        }
                    }
                }
            }
            if (D) Log.d(TAG, "[PBCS]_AcceptThread() -END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "[PBCS]_cancel() -Socket Type " + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "[PBCS]_cancel() -close() -Socket Type " + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "[PBCS]_ConnectThread(device, secure) -Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "[PBCS]_ConnectThread(device, secure) run() -BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "[PBCS]_ConnectThread(device, secure) run() -unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (PhoneBluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "[PBCS]_canel() -close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "[PBCS] _ConnectedThread(socket, socketType) -create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "[PBCS]_ConnectedThread(socket, socketType) -temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "[PBCS]_ConnectedThread(socket, socketType) -run() -BEGIN mConnectedThread");
            int bufferSize = 1024, n = 0;
            byte[] buffer = new byte[bufferSize];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    if (mmInStream.available() > 0) {
                        // get buffer size n
                        mmInStream.read(buffer,0,4);
                        n = byteArrayToInt(buffer);

                        if(n > 0 && n < 10000) { // filter unreasonable length
                            mmInStream.read(buffer, 0, n);
                            String Receiving_Data = new String(buffer, 0, n);
                            //Log.d(TAG, "n=" +n+ " data=" +Receiving_Data);
                            Message msg;
                            Bundle bundle = new Bundle();

                            msg = mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_READ);
                            bundle.putString(PhoneBluetoothHandler.READ, Receiving_Data);
                            msg.setData(bundle);
                            mHandler.sendMessage(msg);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "[PBCS]_ConnectedThread(socket, socketType) -IOException: disconnected");
                    //Log.e(TAG, e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            //mmOutStream.write(buffer);
            DataOutputStream dOut = new DataOutputStream(mmOutStream);

            try {
                Log.e(TAG, "[PBCS]_write() -Before dOut.write");
                Log.e(TAG, "[PBCS]_write() -buffer length = "+buffer.length);
                dOut.writeInt(buffer.length); // write length of the message
                dOut.write(buffer); // write the message
                // Share the sent message back to the UI Activity
            	mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_WRITE, buffer.length, -1, buffer).sendToTarget();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                mHandler.obtainMessage(PhoneBluetoothHandler.MESSAGE_DISCONNECTED, buffer.length, -1, buffer)
                        .sendToTarget();
                mState = STATE_NONE;
            }

            
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "[PBCS]_cancel() -close() -of connect socket failed", e);
            }
        }
    }

}
