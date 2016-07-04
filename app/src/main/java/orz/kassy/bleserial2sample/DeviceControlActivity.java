package orz.kassy.bleserial2sample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.UUID;

public class DeviceControlActivity extends AppCompatActivity implements OnClickListener {
    public static String UUID_BLESERIAL_SERVICE =   "bd011f22-7d3c-0db6-e441-55873d44ef40";
    public static String UUID_BLESERIAL_RX =   "2a750d7d-bd9a-928f-b744-7d5a70cef1f9";
    public static String UUID_BLESERIAL_TX =   "0503b819-c75b-ba9b-3641-6a7f338dd9bd";


    private final static String TAG = "DeviceControlActivity";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private Button ledOnButton;
    private Button ledOffButton;
    private TextView resDataText;
    private boolean mFlag = false;
    private boolean bool = false;

    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> mBluetoothDeviceAddressList;
    private ArrayList<BluetoothGatt> mBluetoothGattList;
    private BluetoothGattCallback mCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_com);

        //　BTデバイス名とアドレス引継ぎ
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //ボタン
        ledOnButton = (Button) findViewById(R.id.led_on_button);
        ledOnButton.setOnClickListener(this);
        ledOffButton = (Button) findViewById(R.id.led_off_button);
        ledOffButton.setOnClickListener(this);

        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
        Button btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(this);

        //センサー値表示
        resDataText = (TextView) findViewById(R.id.res_text);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!initialize(mGattCallback)) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        // BTアダプタへの参照の初期化が成功したら、接続動作を開始
        connect(mDeviceAddress);

//        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        disconnect();
        super.onPause();
        finish();
    }

    /**
     * タイトルを押すと前画面へ戻る
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private static int convByteToInt(byte b) {
        int i = (int) b;
        i &= 0x000000FF;
        return i;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnConnect:
                Log.i(TAG, "connect **************************************");
                connect(mDeviceAddress);

                break;

            case R.id.btnDisconnect:
                Log.i(TAG, "disconnect **************************************");
                disconnect();

                break;

            default:
                if (mGatt != null) {
                    byte[] data;
                    if (bool) {
                        Log.i(TAG, "led on ");
                        data = new byte[]{(byte) 0x01};
                        bool = false;
                    } else {
                        Log.i(TAG, "led off ");
                        data = new byte[]{(byte) 0x00};
                        bool = true;
                    }

                    BluetoothGattService gattService = mGatt.getService(UUID.fromString(UUID_BLESERIAL_SERVICE));
                    BluetoothGattCharacteristic targetCharacteristic = gattService.getCharacteristic(UUID.fromString(UUID_BLESERIAL_TX));

                    if (targetCharacteristic != null) {
                        targetCharacteristic.setValue(data);
                        mGatt.writeCharacteristic(targetCharacteristic);
                    } else {
                        Log.i(TAG, "can't write !!!!!!");
                    }

                }
                break;
        }
    }

    private BluetoothGatt mGatt = null;
    /**
     * GATTイベントコールバック
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        // 接続状態変化イベント
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            Log.i(TAG, "onConnectionStateChange gatt addr : " + gatt.getDevice().getAddress() + ", " + newState);

            // Gattに接続した
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mGatt = gatt;
                gatt.discoverServices();
            }

            // GATT切断した
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mGatt = null;
            }
        }

        // サービス一覧取得終了イベント
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onServiceDiscovered gatt addr : " + gatt.getDevice().getAddress());
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    //RXにNotificateをセット
                    BluetoothGattService myService = gatt.getService(UUID.fromString(UUID_BLESERIAL_SERVICE));
                    BluetoothGattCharacteristic characteristic = myService.getCharacteristic(UUID.fromString(UUID_BLESERIAL_RX));
                    gatt.setCharacteristicNotification(characteristic, true);
                } else {
                    Log.w(TAG, "onServicesDiscovered received: " + status);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // CharacteristicReadイベント
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final byte[] data = characteristic.getValue();
                Log.i(TAG, "read data * " + data);
            }
        }

        // 受信イベント
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (UUID.fromString(UUID_BLESERIAL_RX).equals(characteristic.getUuid())) {
                final byte[] data = characteristic.getValue();
                Log.i(TAG, "change data * " + data[0]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "change value" + data[0]);
                        resDataText.setText("value : " + data[0]);
                    }
                });
            }
        }
    };


    /**
     * BluetoothAdapter 初期化.
     * @return true：初期化成功
     */
    public boolean initialize(BluetoothGattCallback callback) {

        mBluetoothGattList = new ArrayList<BluetoothGatt>();
        mBluetoothDeviceAddressList = new ArrayList<String>();

        mCallback = callback;

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }


    /**
     * GATT server（BLESerial） と接続
     * 結果は非同期で、BluetoothGattCallback（冒頭部分）で報告される。
     * @param address ：Device Address
     * @return true：接続成功
     */
    public boolean connect(final String address) {
        Log.e(TAG, "connect method called ********************************************");

        // 前回接続したデバイスだった場合は再接続
//        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
//            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                return true;
//            } else {
//                return false;
//            }
//        }

        // 接続
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // デバイスと直接通信するので、autoConnectパラメータ（２個目の引数）はfalse
        Log.i(TAG, "Trying to create a new connection.");
        mBluetoothGattList.add(device.connectGatt(this, false, mCallback));
        mBluetoothDeviceAddressList.add(address);

        return true;
    }

    /**
     * 切断
     * 結果は非同期でBluetoothGattCallbackで報告される
     */
    public void disconnect() {
        if (mBluetoothGattList == null) {
            return;
        }
        for (BluetoothGatt gatt : mBluetoothGattList) {
            Log.i(TAG, "gatt disconnect close b ********************************************************");
            gatt.disconnect();
            gatt.close();
        }
    }
}
