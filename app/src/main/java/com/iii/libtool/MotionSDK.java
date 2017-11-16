package com.iii.libtool;

import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by brain on 2017/3/3.
 */

public class MotionSDK {
    static {
        //Log.d(TAG, "MotionSDK LLLLLLLLLLLOOOOOOOOOOOODDDDDDDDDDDDD");
        System.loadLibrary("JNItest");
        System.loadLibrary("opencv_java");
    }
    public native int setFeatureFile(String file);

    public native int init(String A, int B, int C, int D);

    public native int predictActivity(float[] predict_data, int count);
}
