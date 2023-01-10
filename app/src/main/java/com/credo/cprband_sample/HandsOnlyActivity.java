package com.credo.cprband_sample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HandsOnlyActivity extends Activity {

    private BluetoothDevice testBLEDevice;
    private BluetoothAdapter mBluetoothAdapter;
    Button scan, start, connect, ready;
    RadioButton sec30, sec60, sec90;
    TextView time, correct_count, total_count, status;
    private boolean start_check = true;
    private long StartTime_L = 0L;
    private boolean mScanning;
    private ArrayList<Float> bluetoothtime_list01 = new ArrayList<>();
    private BLEServiceHands bleServiceHands;
    private CountDownTimer countDownTimer;
    private int correct_count_int = 0;
    private int total_count_int = 0;
    private int time_int = 0;

    private static final int COUNT_DOWN_INTERVAL = 1000;
    private int MILLISIEVERT = 0;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cpr_hands);

        scan = findViewById(R.id.scan_btn);
        start = findViewById(R.id.start_btn);
        connect = findViewById(R.id.connect_btn);
        ready = findViewById(R.id.ready_btn);

        sec30 = findViewById(R.id.sec30_radio);
        sec60 = findViewById(R.id.sec60_radio);
        sec90 = findViewById(R.id.sec90_radio);

        time = findViewById(R.id.time_tv);
        correct_count = findViewById(R.id.correctcount_tv);
        total_count = findViewById(R.id.totalcount_tv);
        status = findViewById(R.id.status_tv);

        Intent bleServiceIntent = new Intent(this, BLEServiceHands.class);
        bindService(bleServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        permissionCheck();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        scan.setOnClickListener(v ->{
            scanLeDevice(true);
        });

        connect.setOnClickListener(v ->{
            scanLeDevice(false);
            Log.e("testBLEDevice1", testBLEDevice.toString());
            bleServiceHands.connectgatt(testBLEDevice);
            status.setText("connecting...");
            //bleServiceHands.writeCharacteristic("f1");
        });

        ready.setOnClickListener(v ->{
            bleServiceHands.writeCharacteristic(bleServiceHands.CMD_READY);
            status.setText("ready");
            start.setEnabled(true);
        });

        start.setOnClickListener(v ->{
            if(start_check){
                start_check = false;
                start.setText("Stop");
                if(sec30.isChecked()){
                    MILLISIEVERT = 31000;
                    count = 30;
                }else if(sec60.isChecked()){
                    MILLISIEVERT = 61000;
                    count = 60;
                }else if(sec90.isChecked()){
                    MILLISIEVERT = 91000;
                    count = 90;
                }
                time_int = count;
                countDownTimer = new CountDownTimer(MILLISIEVERT, COUNT_DOWN_INTERVAL) {

                    @Override
                    public void onTick(long millisUntilFinished) {
                        time.setText(String.valueOf(count));
                        count--;
                    }

                    @Override
                    public void onFinish() {
                        time.setText("0");
                        bleServiceHands.writeCharacteristic("f1");
                        start_check = true;
                        start.setText("Start");
                        status.setText("Finished");
                        countDownTimer.cancel();
                        Intent intent = new Intent(HandsOnlyActivity.this, ReportActivity.class);
                        intent.putExtra("correct_count", correct_count_int);
                        intent.putExtra("total_count", total_count_int);
                        intent.putExtra("time", time_int);
                        startActivity(intent);
                        correct_count_int = 0;
                        total_count_int = 0;
                        time_int = 0;
                    }
                };
                bleServiceHands.writeCharacteristic("f3");
                countDownTimer.start();
                StartTime_L = System.currentTimeMillis();
                status.setText("Running");
            }else{
                bleServiceHands.writeCharacteristic("f1");
                start_check = true;
                start.setText("Start");
                countDownTimer.cancel();
                status.setText("Stopped");
            }
        });
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            status.setText("bluetooth connected");
            if(bleServiceHands.ACTION_CONNECTED.equals(action)) {
                Log.e("testBLEDevice2", testBLEDevice.toString());
                ready.setEnabled(true);
                status.setText("ready");
            }
            if(bleServiceHands.ACTION_DEPTH_DATA_AVAILABLE.equals(action)){
                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat timeformat = new SimpleDateFormat("hh:mm:ss");
                String pushtime = timeformat.format(date);
                String value = intent.getStringExtra(bleServiceHands.EXTRA_DATA);
                total_count_int++;
                total_count.setText(String.valueOf(total_count_int));
                if(30 < Integer.parseInt(value) && Integer.parseInt(value) < 60){
                    correct_count_int++;
                    correct_count.setText(String.valueOf(correct_count_int));
                }
            }
            if(bleServiceHands.ACTION_ANGLE_DATA_AVAILABLE.equals(action)){
                Log.d("battery",intent.getStringExtra(bleServiceHands.EXTRA_DATA));
                String value = intent.getStringExtra(bleServiceHands.EXTRA_DATA);
            }
        }
    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bleServiceHands = ((BLEServiceHands.LocalBinder) service).getService();
            StartTime_L = System.currentTimeMillis();
            if (!bleServiceHands.initialize()) {
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bleServiceHands = null;
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        Intent sender = new Intent(HandsOnlyActivity.this, BLEServiceHands.class);
        sender.setAction(BLEServiceHands.ACTION_SCANPAUSE);
        startService(sender);
    }

    @Override
    protected void onResume(){
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable){
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scan_filter = new ScanFilter.Builder()
                .build();
        filters.add( scan_filter );
        ScanSettings settings= new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_POWER)
                .build();
        if(enable) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan((ScanCallback) mLeScanCallback);
                }
            }, 10000);

            mScanning = true;
            bluetoothLeScanner.startScan(mLeScanCallback);
            status.setText("Scanning...");
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan((ScanCallback) mLeScanCallback);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEServiceHands.ACTION_DEPTH_DATA_AVAILABLE);
        intentFilter.addAction(BLEServiceHands.ACTION_ANGLE_DATA_AVAILABLE);
        return intentFilter;
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result){
            Log.d("ScanResult", String.valueOf(result.getDevice().getName()));
            if(String.valueOf(result.getDevice().getName()).contains("CPR-BAND")){
                testBLEDevice = result.getDevice();
                Log.e("testBLEDevice", String.valueOf(testBLEDevice));
                connect.setEnabled(true);
                status.setText("Found Device! Press connect button");
            }
        }

        @Override
        public void onScanFailed(int errorCode){
            status.setText("Scan Failed");
            Log.e("ScanError", String.valueOf(errorCode));
        }
    };

    private void permissionCheck() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    },
                    1);
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    1);
        }
    }

    public void countDownTimer(){
        countDownTimer = new CountDownTimer(MILLISIEVERT, COUNT_DOWN_INTERVAL) {
            public void onTick(long millisUntilFinished) {

            }
            public void onFinish() {
            }
        };
    }
}