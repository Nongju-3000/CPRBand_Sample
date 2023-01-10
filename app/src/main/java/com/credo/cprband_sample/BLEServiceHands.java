package com.credo.cprband_sample;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

public class BLEServiceHands extends Service {

    public final static String ACTION_DEPTH_DATA_AVAILABLE = "com.credo.bluetooth.le.ACTION_DEPTH_DATA_AVAILABLE";
    public final static String ACTION_ANGLE_DATA_AVAILABLE = "com.credo.bluetooth.le.ACTION_ANGLE_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.credo.bluetooth.le.EXTRA_DATA";

    public final static String ACTION_SCANPAUSE = "ACTION_SCANPAUSE";
    public final static String ACTION_CONNECTED = "ACTION_CONNECTED";
    public final static String CMD_READY = "f1";
    public final static String CMD_PRACTICE_MODE = "f3";

    private static final UUID SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final UUID DEPTH_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final UUID ANGLE_UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final UUID BATTERY_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb");
    private static final UUID WRITE_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt bluetoothGatt;
    private final IBinder mBinder = new LocalBinder();
    private final static String TAG = BLEServiceHands.class.getSimpleName();

    public class LocalBinder extends Binder {

        public BLEServiceHands getService() {
            return BLEServiceHands.this;
        }

    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        List<BluetoothGattCharacteristic> chars = new ArrayList<>();
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.d("BLE Gatt connected", "1");
                bluetoothGatt.discoverServices();
            }
        }
        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("GattStatus", String.valueOf(status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService mBluetoothGattService = bluetoothGatt.getService(SERVICE_UUID);
                chars.add(mBluetoothGattService.getCharacteristic(ANGLE_UUID));
                chars.add(mBluetoothGattService.getCharacteristic(DEPTH_UUID));
                subscribeToCharacteristics(gatt);
                if (mBluetoothGattService != null) {
                    Log.i(TAG, "Service characteristic UUID found: " + mBluetoothGattService.getUuid().toString());
                } else {
                    Log.i(TAG, "Service characteristic not found for UUID: " + SERVICE_UUID);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt,characteristic);
            Log.d("characterUUid", String.valueOf(characteristic.getUuid()));
            if(characteristic.getUuid().equals(ANGLE_UUID)){
                String receiver = HexString.bytesToHex(characteristic.getValue());
                int data = Integer.parseInt(receiver, 16);
                Log.d("angle_value", String.valueOf(data));
                receiver = (data + 200.0f) / 10.0f + " ℃";
                final Intent intent = new Intent(ACTION_ANGLE_DATA_AVAILABLE);
                intent.putExtra(EXTRA_DATA, String.valueOf(data));
                sendBroadcast(intent);
            }
            if(characteristic.getUuid().equals(DEPTH_UUID)){
                String receiver = HexString.bytesToHex(characteristic.getValue());
                int data = Integer.parseInt(receiver, 16);
                Log.d("depth_value", String.valueOf(data));
                receiver = String.valueOf(data);
                final Intent intent = new Intent(ACTION_DEPTH_DATA_AVAILABLE);
                intent.putExtra(EXTRA_DATA, String.valueOf(data));
                sendBroadcast(intent);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d("value", String.valueOf(characteristic.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            chars.remove(0);
            subscribeToCharacteristics(gatt);
        }

        @SuppressLint("MissingPermission")
        private void subscribeToCharacteristics(BluetoothGatt gatt) {
            if(chars.size() == 0) return;
            BluetoothGattCharacteristic characteristic = chars.get(0);
            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if(descriptor != null) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }
    };

    @SuppressLint("MissingPermission")
    public void connectgatt(BluetoothDevice device){
        Log.d("connectdevice", String.valueOf(device.getName()));
        bluetoothGatt = device.connectGatt(getApplicationContext(), false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    public void writeCharacteristic(String data){
        byte[] sender = HexString.hexToBytes(data);
        BluetoothGattService mBluetoothGattService = bluetoothGatt.getService(SERVICE_UUID);
        BluetoothGattCharacteristic mCH = mBluetoothGattService.getCharacteristic(WRITE_UUID);
        Log.d("sender", String.valueOf(sender));
        mCH.setValue(sender);
        bluetoothGatt.writeCharacteristic(mCH);
    }
}
