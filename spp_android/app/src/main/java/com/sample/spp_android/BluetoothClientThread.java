package com.sample.spp_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by kondoutomoko on 2016/07/06.
 */
public class BluetoothClientThread extends Thread {
    //クライアント側の処理
    private final BluetoothSocket clientSocket;
    private Context mContext;
    private Handler mHandler;
    //UUIDの生成
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static BluetoothAdapter myClientAdapter;
    public String mSendMessage;

    //コンストラクタ定義
    public BluetoothClientThread(Context context, Handler handler, String sendMessage , BluetoothDevice device, BluetoothAdapter btAdapter){
        //各種初期化
        mContext = context;
        mHandler = handler;
        BluetoothSocket tmpSock = null;
        myClientAdapter = btAdapter;
        mSendMessage = sendMessage;

        try{
            //自デバイスのBluetoothクライアントソケットの取得
            tmpSock = device.createRfcommSocketToServiceRecord(TECHBOOSTER_BTSAMPLE_UUID);
        }catch(IOException e){
            e.printStackTrace();
        }
        clientSocket = tmpSock;
    }

    public void run(){
        //接続要求を出す前に、検索処理を中断する。
        if(myClientAdapter.isDiscovering()){
            myClientAdapter.cancelDiscovery();
        }

        try{
            //サーバー側に接続要求
            clientSocket.connect();
        }catch(IOException e){
            try {
                clientSocket.close();
            } catch (IOException closeException) {
                e.printStackTrace();
            }
            return;
        }

        //接続完了時の処理
        final String rcvMessage = ReadWriteModel.writeRead(clientSocket, mSendMessage);
        if (rcvMessage != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "received : " + rcvMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
