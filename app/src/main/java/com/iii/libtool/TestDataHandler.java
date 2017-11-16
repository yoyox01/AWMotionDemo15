package com.iii.libtool;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by brain on 2017/1/4.
 */

public class TestDataHandler {
    String TAG = "TestDataHandler";

	MotionSDK motionSDK;
    Context context;
    int maxMotionNum = 0;
    int motionStrLineLen = 0;
    public TestDataHandler(Context c, MotionSDK sdk, int max_motion_num, int motion_str_line_len){
        Log.d(TAG, "read and TTTTTTTTEEEEEEEEEEESSSSSSSSTT");
        context = c;
        motionSDK = sdk;
        maxMotionNum = max_motion_num;
        motionStrLineLen = motion_str_line_len;
    }

    // NOOOOOOOOOO UUUUUSSSSSEEEEEEE
	// 資料切割 + 動作辨識測試
    public void readAndTest(String filename, int label_index){
        InputStream in;
        BufferedReader reader;
        String line;
        //Log.d(TAG, "read and TTTTTTTTEEEEEEEEEEESSSSSSSSTT");
        int lineNo = 0;
        try{
            in = context.getAssets().open(filename);
            reader = new BufferedReader(new InputStreamReader(in));
            float[][] data = new float[motionStrLineLen][maxMotionNum];

            while ((line = reader.readLine()) != null) {
                String[] value = line.split(",");
                for(int i = 0; i < value.length; i++)
                    data[lineNo][i] = Float.parseFloat(value[i]);
                lineNo++;
                if(lineNo > 1 && data[lineNo - 1][label_index] != data[lineNo - 2][label_index]){
                    testSelData(data, 0, lineNo - 2, data[lineNo - 2][label_index]);
                    for(int i = 0; i < data[0].length; i++)
                        data[0][i] = data[lineNo - 1][i];
                    lineNo = 1;
                }
            }
            testSelData(data, 0, lineNo - 1, data[lineNo - 1][label_index]);
            reader.close();
            in.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            e.getStackTrace();
        }

    }

	// 辨識一段動作資料
    void testSelData(float[][] data, int start_index, int end_index, float label){
        Log.d(TAG, "read and TTTTKKKKKHHHHHHHHHHHHH" );
        float id;
        Log.d(TAG, "test motion len = " + Integer.toString(end_index - start_index + 1) + ", label = " + label);
        for(int i = start_index; i <= end_index; i++) {
            id = motionSDK.predictActivity(data[i], 0);
            Log.d(TAG, "read and TTTTKKKKKHHHHHHHHHHHHH" );
        }

    }

	// 連續動作辨識測試 (CNN)
    public void readAndSingleTest(String filename, int label_index){
        Log.d(TAG, "read and TTTTKKKKKHHHHHHHHHHHHH" );
        InputStream in;
        BufferedReader reader;
        String line;
        int lineNo = 0;
        try{
            in = context.getAssets().open(filename);
            reader = new BufferedReader(new InputStreamReader(in));
            float[] data = new float[maxMotionNum * motionStrLineLen];
            while ((line = reader.readLine()) != null) {
                String[] value = line.split(",");
                for(int i = 0; i < value.length; i++)
                    data[i] = Float.parseFloat(value[i]);
                int id = motionSDK.predictActivity(data, 0);
            }
            reader.close();
            in.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
            e.getStackTrace();
        }
    }
}
