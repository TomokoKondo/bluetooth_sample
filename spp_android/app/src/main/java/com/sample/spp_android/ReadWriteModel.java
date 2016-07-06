package com.sample.spp_android;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by kondoutomoko on 2016/07/06.
 */
public class ReadWriteModel {
    //ソケットに対するI/O処理

    //コンストラクタの定義
    public ReadWriteModel(){
    }

    /**
     * 書き込んでから読み込む
     * @param socket 読み書きするBluetoothソケット
     * @param sendMessage 送信するメッセージ
     * @return 失敗時：NULL、成功時：受信したメッセージ
     */
    public static String writeRead(BluetoothSocket socket, String sendMessage){
        InputStream in;
        OutputStream out;
        try {
            //接続済みソケットからI/Oストリームをそれぞれ取得
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        }

        // 書き込み
        try {
            //Outputストリームへのデータ書き込み
            out.write(sendMessage.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // 読み込み
        String receivedMessage = null;
        byte[] buf = new byte[1024];
        int tmpBuf = 0;
        while(true){
            try {
                tmpBuf = in.read(buf);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            if(tmpBuf!=0){
                try {
                    receivedMessage = new String(buf, "UTF-8");
                    break;
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return receivedMessage;
    }

    /**
     * 読み込んでから書き込む
     * @param socket 読み書きするBluetoothソケット
     * @param sendMessage 送信するメッセージ
     * @return 失敗時：NULL、成功時：受信したメッセージ
     */
    public static String readWrite(BluetoothSocket socket, String sendMessage){
        // I/Oストリームを取得
        InputStream in;
        OutputStream out;
        try {
            //接続済みソケットからI/Oストリームをそれぞれ取得
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return null;
        }

        // 読み込み
        byte[] buf = new byte[1024];
        int tmpBuf = 0;
        String receivedMessage = null;
        while(true){
            try {
                tmpBuf = in.read(buf);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
            if(tmpBuf!=0){
                try {
                    receivedMessage = new String(buf, "UTF-8");
                    break;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        // 書き込み
        try {
            //Outputストリームへのデータ書き込み
            out.write(sendMessage.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return receivedMessage;
    }
}
