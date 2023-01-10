package com.credo.cprband_sample;

import android.util.Log;

import com.polidea.rxandroidble2.BuildConfig;

public class Print {

    public static void d(String TAG, String mes) {
        print(0, TAG, mes);
    }

    public static void v(String TAG, String mes) {
        print(1, TAG, mes);
    }

    public static void i(String TAG, String mes) {
        print(2, TAG, mes);
    }

    public static void e(String TAG, String mes) {
        print(3, TAG, mes);
    }

    public static void w(String TAG, String mes) {
        print(4, TAG, mes);
    }


    // 0: d, v : 1, i : 2, 3 : e, 4: w
    private static void print(int level, String TAG, String mes) {
        if(!BuildConfig.DEBUG) return;
        switch (level) {
            case 0:
                Log.d(TAG, mes);
                break;
            case 1:
                Log.v(TAG, mes);
                break;
            case 2:
                Log.i(TAG, mes);
                break;
            case 3:
                Log.e(TAG, mes);
                break;
            case 4:
                Log.w(TAG, mes);
                break;
        }
    }
}
