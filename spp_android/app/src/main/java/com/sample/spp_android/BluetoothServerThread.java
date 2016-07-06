package com.sample.spp_android;

import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by kondoutomoko on 2016/07/06.
 */
public class BluetoothServerThread extends Thread {
    private boolean isRunning;
    //サーバー側の処理
    //UUID：Bluetoothプロファイル毎に決められた値
    private BluetoothServerSocket servSock;
    private Context mContext;
    private Handler mHandler;
    //UUIDの生成
    // SSPのUUIDを作成
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //コンストラクタの定義
    public BluetoothServerThread(Context context, Handler handler, BluetoothServerSocket sock){
        //各種初期化
        isRunning = false;
        mContext = context;
        mHandler = handler;
        servSock = sock;
    }

    public void run(){
        if (servSock == null) {
            return;
        }
        isRunning = true;
        BluetoothSocket receivedSocket;
        while(isRunning){
            receivedSocket = null;
            try{
                //クライアント側からの接続要求待ち。ソケットが返される。
                // ５秒間待つ
                receivedSocket = servSock.accept(5000);
            }catch(IOException e){
                continue;
            }

            if(receivedSocket != null){
                //ソケットを受け取れていた(接続完了時)の処理
                String sendMessage = ((MainActivity)mContext).getSendMessage();
                if (sendMessage.equals(""))
                    sendMessage = "empty";
                final String rcvMessage = ReadWriteModel.readWrite(receivedSocket, sendMessage);
                if (rcvMessage != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "received : " + rcvMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                try {
                    receivedSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        try {
            servSock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        try {
            isRunning = false;
            if (servSock != null)
                servSock.close();
        } catch (IOException e) { }
    }
}
