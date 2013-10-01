package br.metodista.controle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
� * Esta classe faz todo o trabalho para a cria��o e gest�o da.s conex�es Bluetooth
� * com outros dispositivos. Tem uma Thread que aguarda
� * Conex�es de entrada, uma Thread para conex�o com um dispositivo, e uma
� * Thread para a realiza��o de transmiss�es de dados quando conectado.
� */
public class BluetoothSerialService {
    // Declara��es de v�riaveis.
    private static final String TAG = "BluetoothReadService";
    private static final boolean D = true;

	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    
    // Estados das conex�es.
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    //Prepara a sess�o BluetoothChat.
    public BluetoothSerialService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    //Define os estados da conex�o.
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(Main_Controle_Remoto.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    // M�todo que retorna o estado da conex�o.
    public synchronized int getState() {
        return mState;
    }

    /*Gest�o das Thread*/
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancela a tentativa de conex�o.
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancela a conex�o que est� em execu��o.
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    //Inicia a Thread para conex�o com dispositivo.
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    //Thread que gerencia a conex�o estabelecida.
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Inicia Thread.
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Devolve o nome do dispositivo conectado para a classe principal.
        Message msg = mHandler.obtainMessage(Main_Controle_Remoto.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Main_Controle_Remoto.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    //Interrompe Thread ativas.
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");


        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }
    
    //M�todo que mostra quando a tentativa de conex�o falhou.
    private void connectionFailed() {
        setState(STATE_NONE);
        Message msg = mHandler.obtainMessage(Main_Controle_Remoto.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Main_Controle_Remoto.TOAST, "N�o foi poss�vel conectar com o dispositivo");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    //M�todo que mostra quando a conex�o atual foi perdida.
    private void connectionLost() {
        setState(STATE_NONE);
        Message msg = mHandler.obtainMessage(Main_Controle_Remoto.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Main_Controle_Remoto.TOAST, "A conex�o foi encerrada");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    //Essa Thread executa quandop tenta-se fazer uma conex�o com um dispositivo.
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
            try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                return;
            }

            synchronized (BluetoothSerialService.this) {
                mConnectThread = null;
            }

            // Inicia a thread de conex�o
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    //Esta thread  � executado durante uma conex�o com um dispositivo remoto.
    //Lida com todas as transmiss�es de entrada e sa�da.
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(Main_Controle_Remoto.MESSAGE_WRITE, buffer.length, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
