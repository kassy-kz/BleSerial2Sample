package orz.kassy.bleserial2sample;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

/**
 * BLESerial（浅草ギ研製BLE通信モジュール）との接続と通信を行うサービス。
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<String> mBluetoothDeviceAddressList;
    private ArrayList<BluetoothGatt> mBluetoothGattList;
    private BluetoothGattCallback mCallback;

    public static String UUID_BLESERIAL_SERVICE =   "bd011f22-7d3c-0db6-e441-55873d44ef40";
    public static String UUID_BLESERIAL_RX =   "2a750d7d-bd9a-928f-b744-7d5a70cef1f9";
    public static String UUID_BLESERIAL_TX =   "0503b819-c75b-ba9b-3641-6a7f338dd9bd";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public final static String ACTION_GATT_CONNECTED =
            "com.robotsfx.bleserialtest.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.robotsfx.bleserialtest.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.robotsfx.bleserialtest.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.robotsfx.bleserialtest.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.robotsfx.bleserialtest.EXTRA_DATA";

    // －－－以降４つはBind関連－－－
    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // BLEデバイスとの接続を終了する時には、必ずBluetoothGatt.close()する。
        if (mBluetoothGattList != null) {
            for (BluetoothGatt gatt : mBluetoothGattList) {
                Log.i(TAG, "gatt disconnect close ********************************************************");
                gatt.disconnect();
                gatt.close();
                gatt = null;
            }
        }
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();


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

    /**
     * 指定したCharacteristicをread.
     * 結果は非同期でBluetoothGattCallback#onCharacteristicReadで報告される
     *
     * @param ：読み込むキャラクタリスティックを指定.
     */
//    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
//            Log.w(TAG, "BluetoothAdapter not initialized");
//            return;
//        }
//        mBluetoothGatt.readCharacteristic(characteristic);
//    }
//
}
