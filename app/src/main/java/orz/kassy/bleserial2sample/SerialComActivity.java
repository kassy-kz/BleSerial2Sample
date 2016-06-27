package orz.kassy.bleserial2sample;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

/**
 * BLESerialとの接続、データ送信、受信データ表示を行う
 * GATTサーバ = BLE機器 = BLESerial
 * <p/>
 * Backボタンで終了＆前画面へ戻る
 * <p/>
 * 送信は、EditText内文字をByte[]に変換して送信
 * <p/>
 * 受信は、データを１バイトづつ表示（符号付8bit値）
 * BLESerialのデータペイロードが２０バイト／パケットなので、２０バイトを
 * 超えるデータを送ると、２０バイトづつ上書きして表示（最後のパケットしか見れない）
 *
 * @author T.Ishii
 */
public class SerialComActivity extends AppCompatActivity implements OnClickListener {
    private final static String TAG = "SerialComActivity";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private Button ledOnButton;
    private Button ledOffButton;
    private TextView resDataText;
    private boolean mFlag = false;
    private boolean bool = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_com);

        //　BTデバイス名とアドレス引継ぎ
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //　サービス接続（BluetoothLeService）
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

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
//        final boolean result = mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    protected void onPause() {
        unbindService(mServiceConnection);
        mBluetoothLeService.disconnect();
        mBluetoothLeService = null;
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

    //　サービスコネクション（BluetoothLeService間）
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "service connected *********************************");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize(mGattCallback)) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // BTアダプタへの参照の初期化が成功したら、接続動作を開始
            mBluetoothLeService.connect(mDeviceAddress);
            Log.e(TAG, "bluetoothleservice connect");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "service disconnected *********************************");
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;
        }
    };

    ///////////////////////////////////////////////
    //	byteを符号無しintへ変換
    ////////////////////////////////////////////////
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
                mBluetoothLeService.connect(mDeviceAddress);

                break;

            case R.id.btnDisconnect:
                Log.i(TAG, "disconnect **************************************");
                mBluetoothLeService.disconnect();

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

                    BluetoothGattService gattService = mGatt.getService(UUID.fromString(BluetoothLeService.UUID_BLESERIAL_SERVICE));
                    BluetoothGattCharacteristic targetCharacteristic = gattService.getCharacteristic(UUID.fromString(BluetoothLeService.UUID_BLESERIAL_TX));

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
                    BluetoothGattService myService = gatt.getService(UUID.fromString(BluetoothLeService.UUID_BLESERIAL_SERVICE));
                    BluetoothGattCharacteristic characteristic = myService.getCharacteristic(UUID.fromString(BluetoothLeService.UUID_BLESERIAL_RX));
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
            if (UUID.fromString(BluetoothLeService.UUID_BLESERIAL_RX).equals(characteristic.getUuid())) {
                final byte[] data = characteristic.getValue();
                Log.i(TAG, "change data * " + data[0]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resDataText.setText("value : " + data[0]);
                    }
                });
            }
        }
    };
}
