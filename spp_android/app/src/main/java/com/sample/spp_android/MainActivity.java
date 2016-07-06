package com.sample.spp_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static int REQUEST_ENABLE_BLUETOOTH = 123;

    private BluetoothAdapter bluetoothAdapter;

    private ArrayAdapter<String> pairedDeviceAdapter, nonPairedDeviceAdapter;

    private BluetoothServerThread serverThread;
    private BluetoothClientThread clientThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = null;
        serverThread = null;
        clientThread = null;
        // 端末のBluetoothをONにする
        if (!getBluetoothAdapter()) {
            // Bluetooth非対応の端末のとき
            showErrorDialog("Bluetooth非対応の為、アプリを終了します");
        }
        checkEnableBluetooth();

        // 接続履歴のあるデバイスを取得
        setPairedDeviceList();

        // ボタンのクリックリスナーをセット
        findViewById(R.id.main_button).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serverThread != null && serverThread.isAlive())
            return;
        if (bluetoothAdapter != null) {
            //サーバースレッド起動、クライアントのからの要求待ちを開始
            BluetoothServerSocket tmpServSock = null;
            try{
                //自デバイスのBluetoothサーバーソケットの取得
                tmpServSock = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            }catch(IOException e) {
                e.printStackTrace();
            }
            if (tmpServSock == null)
                return;
            serverThread = new BluetoothServerThread(this, new Handler(),tmpServSock);
            serverThread.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isReceiverRegistered)
            unregisterReceiver(deviceFoundReceiver);

        // 検索中の場合は検出をキャンセルする
        if(bluetoothAdapter != null && bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        // サーバースレッドを終了する
        if (serverThread != null) {
            serverThread.cancel();
        }
        if (clientThread != null) {
            clientThread.cancel();
        }
        try {
            if (serverThread != null) {
                serverThread.join();
            }
            if (clientThread != null) {
                clientThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // デバイスを検出時に受け取るブロードキャストレシーバを定義
    private boolean isReceiverRegistered = false;
    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver(){
        //検出されたデバイスからのブロードキャストを受ける
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            String dName = null;
            BluetoothDevice foundDevice;
            ListView nonpairedList = (ListView)findViewById(R.id.nonPairedDeviceList);

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // スキャン開始
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // デバイスが検出された
                foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if((dName = foundDevice.getName()) != null){
                    if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                        //接続したことのないデバイスのみアダプタに詰める
                        nonPairedDeviceAdapter.add(dName + "\n" + foundDevice.getAddress());
                    }
                }
                nonpairedList.setAdapter(nonPairedDeviceAdapter);
            } else if (BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
                // 名前が検出された
                foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                dName = foundDevice.getName();
                if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
                    // 接続したことのないデバイスのみアダプタに詰める
                    nonPairedDeviceAdapter.add(dName + "\n" + foundDevice.getAddress());
                }
                nonpairedList.setAdapter(nonPairedDeviceAdapter);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // スキャン終了
            }
        }
    };

    /**
     * BluetoothAdapterを取得する
     * @return 端末がBluetoothに対応しているかどうかを返す
     */
    private boolean getBluetoothAdapter() {
        //BluetoothAdapter取得
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return !bluetoothAdapter.equals(null);
    }

    /**
     * ユーザーがBluetooth設定をONにしているか確認する
     * もしONになっていない場合はONにするように促す
     */
    private void checkEnableBluetooth() {
        if(!bluetoothAdapter.isEnabled()){
            //OFFだった場合、ONにすることを促すダイアログを表示する画面に遷移
            Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(btOn, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    /**
     * 接続履歴のあるデバイスのリストビューを初期化
     * BluetoothAdapterから接続履歴のあるデバイスを取得して、リストビューにセットする
     */
    private void setPairedDeviceList() {
        //接続履歴のあるデバイスを取得
        pairedDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        //BluetoothAdapterから、接続履歴のあるデバイスの情報を取得
        Set pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            //接続履歴のあるデバイスが存在する
            for(Object object : pairedDevices){
                //接続履歴のあるデバイスの情報を順に取得してアダプタに詰める
                //getName()・・・デバイス名取得メソッド
                //getAddress()・・・デバイスのMACアドレス取得メソッド
                if (object.getClass() == BluetoothDevice.class) {
                    BluetoothDevice device = (BluetoothDevice)object;
                    pairedDeviceAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            ListView deviceList = (ListView)findViewById(R.id.pairedDeviceList);
            if (deviceList != null) {
                deviceList.setAdapter(pairedDeviceAdapter);
                deviceList.setOnItemClickListener(this);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int ResultCode, Intent date){
        //ダイアログ画面から結果を受けた後の処理を記述
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && ResultCode != RESULT_OK) {
            // BluetoothをONにしてもらえなかった場合
            showErrorDialog("Bluetoothが利用できなかった為、アプリを終了します");
        } else if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            // サーバーの起動
            if (serverThread != null && serverThread.isAlive())
                return;
            BluetoothServerSocket tmpServSock = null;
            try{
                //自デバイスのBluetoothサーバーソケットの取得
                tmpServSock = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            }catch(IOException e) {
                e.printStackTrace();
            }
            if (tmpServSock == null)
                return;
            serverThread = new BluetoothServerThread(this, new Handler(),tmpServSock);
            serverThread.start();
        }
    }

    /**
     * エラーが発生した時のAlertDialogを表示する
     * ダイアログが閉じるとActivityを終了させる
     * @param message 表示するメッセージ
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        finish();
                    }
                })
                .show();
    }

    /**
     * 新規デバイス検出ボタンが押された時の処理
     * まず、自分のデバイスを検出可の状態にするようにユーザーに促す
     * 次に、BroadcastReceiverを登録し、デバイスの検出を開始する
     * @param v
     */
    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.main_button)
            return;

        // 自デバイスの検出を有効にする
        Intent discoverableOn = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableOn.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableOn);

        // 接続履歴のないデバイスのリストビューを可視状態にする
        findViewById(R.id.nonPairedListTitle).setVisibility(View.VISIBLE);
        findViewById(R.id.nonPairedDeviceList).setVisibility(View.VISIBLE);

        ((ListView)findViewById(R.id.nonPairedDeviceList)).setOnItemClickListener(this);

        // インテントフィルターとBroadcastReceiverの登録
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(deviceFoundReceiver, filter);
        isReceiverRegistered = true;

        nonPairedDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        // 接続可能なデバイスを検出
        if(bluetoothAdapter.isDiscovering()){
            // 検索中の場合は検出をキャンセルする
            bluetoothAdapter.cancelDiscovery();
        }
        // デバイスを検索する
        // 一定時間の間検出を行う
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // リストビューからアイテムが選択された時（接続するデバイスが選択された時）、
        // クライエントスレッドを開始する
        if (clientThread != null && clientThread.isAlive()) {
            return;
        }

        // 通信相手のアドレスを取得
        String address = getAddress(parent, position);
        if (!bluetoothAdapter.checkBluetoothAddress(address))
            return;

        // クライエントサーバー起動
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        String sendMessage = (((EditText)findViewById(R.id.main_sendMessage)).getText()).toString();
        if (sendMessage.equals(""))
            sendMessage = "empty";
        clientThread = new BluetoothClientThread(this, new Handler(), sendMessage, device, bluetoothAdapter);
        clientThread.start();
    }

    /**
     * リストビューから接続する端末のアドレスを取得する
     * @param parent リストビューにセットしたアダプター
     * @param position 選択されたアイテムの場所
     * @return アドレス。失敗するとNULL
     */
    private String getAddress(AdapterView<?> parent, int position) {
        String address = (String)parent.getItemAtPosition(position);
        try {
            MessageFormat format = new MessageFormat("{0}\n{1}");
            Object[] result = format.parse(address);
            address = (String)result[1];
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }

        return address;
    }

    /**
     * 送信するメッセージをEditTextから取得
     * @return EditTextの中身
     */
    public String getSendMessage() {
        return (((EditText)findViewById(R.id.main_sendMessage)).getText()).toString();
    }
}
