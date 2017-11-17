package com.iii.libtool;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.awmotiondemo15.R;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.R.attr.path;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;


/**
 * Created by brain on 2016/12/2.
 */
// Storage Permissions


/**
 * Checks if the app has permission to write to device storage
 * <p>
 * If the app does not has permission then the user will be prompted to grant permissions
 *
 * @param activity
 */

public class MotionRecognitionHandler implements DataHandler {
    private String TAG = "RecognitionHandler";

    private MotionSDK motionSDK;
    private Context context;
    //private int data_groups_num = 200;
    //private int data_dimension = 20;
    private float[] one_groups_data;// = new float[data_groups_num * data_dimension];
    String theSensorData = "";

    private SegmentMotion seg = null;
    private ReadSettings read = null;
    private int ModelType;
    //private SegmentMotion seg = new SegmentMotion(data_groups_num, data_dimension, 3, 0.4f, 1.8f);
    private MotionEventListener in_motion_listener = null;
    private MotionEventListener non_motion_listener = null;
    private MotionEventListener action_listener = null;

    private int MotionID = -1;
    private int Length = 0;


    //2017/11/16


    int dataCount = 0;
    long startTime = -1;
    float avgDataRate = 0.0f;

    int groupTest = 0;

    static final int AVG_FRAME_RATE_RESET = 100;

    public static final int SINGLE_TEST = 0;
    public static final int GROUP_TEST = 1;

    public MotionRecognitionHandler(Context c, ReadSettings r) {
        read = r;
        //ModelType = model_type;
        motionSDK = new MotionSDK();
        context = c;
        initialize(c);
    }

    public boolean getState() {
        return seg.isInMotion();
    }

    public void setInMotionListener(MotionEventListener toAdd) {
        in_motion_listener = toAdd;
    }

    public void setNonMotionListener(MotionEventListener toAdd) {
        non_motion_listener = toAdd;
    }

    public void setActionMotionListener(MotionEventListener toAdd) {
        action_listener = toAdd;
    }

    public float getAvgDataRate() {
        return avgDataRate;
    }

    public void setGroupTest(int g) {
        groupTest = g;
    }

    public int getGroupTest() {
        return groupTest;
    }

    public SegmentMotion getSegmentMotion() {
        return seg;
    }

    public boolean isInMotion() {
        return seg.isInMotion();
    }

    public int getMotionID() {
        return MotionID;
    }

    public int getMotionGroupID() {
        return read.action_group_id[MotionID];
    }

    public int getMotionLength() {
        int len = Length;
        Length = 0;
        return len;
    }

    public String getMotionName() {
        Log.i(TAG, "read and TTTTKKKKKHHHHHHHHHHHHH MotionRecognitionHand 159");
        int id = getMotionID();
        if (id < 0)
            return "";
            //else if(id == 0)
            //    return "非動作";
            //else if(id > read.action_list_num)
            //    return "動作ID超過範圍";
        else {
            String result;
            String act;
            int i, j;
            if (groupTest == GROUP_TEST && read.action_group_num > 0) {
                for (i = 0; i < read.action_list_id.length && read.action_list_id[i] != id; i++) ;
                if (i < read.action_list_id.length) {
                    int t = read.action_group_id_map[i];
                    for (j = 0; j < read.action_group_id.length && read.action_group_id[j] != t; j++)
                        ;
                    if (j < read.action_group_id.length) {
                        result = read.action_group[j] + "(" + read.action_group_id_map[i] + ")";
                    } else {
                        return "動作群組ID=" + t + " (非動作)";
                    }
                } else {
                    return "動作ID=" + id + " (非動作)";
                }
            } else {
                for (i = 0; i < read.action_list_id.length && read.action_list_id[i] != id; i++) ;
                if (i < read.action_list_id.length) {
                    int t = read.action_list[i].indexOf("(");
                    if (t >= 0)
                        act = read.action_list[i].substring(0, t);
                    else
                        act = read.action_list[i];
                    result = act + "(" + read.action_list_id[i] + ")";
                } else {
                    return "動作ID=" + id + " (非動作)";
                }
            }
            return result;
        }
    }

    public void sentDataGet(final String mData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    String uri = Uri.parse("http://140.123.175.101:8888/")
                            .buildUpon()
                            .appendQueryParameter("data", mData)
                            .build().toString();
                    URL url = new URL(uri);

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = urlConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String tempStr;
                    StringBuffer stringBuffer = new StringBuffer();

                    while ((tempStr = bufferedReader.readLine()) != null) {
                        stringBuffer.append(tempStr);
                    }

                    bufferedReader.close();
                    inputStream.close();
                    urlConnection.disconnect();
                    Log.d("fuck", stringBuffer.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sentDataPost(final String mData) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String uri = Uri.parse("http://140.123.175.101:8888/")
                            .buildUpon()
                            .build().toString();

                    URL url = new URL(uri);

                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Connection", "Keep-Alive");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    BufferedOutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

                    writer.write("data=" + URLEncoder.encode(mData, "utf-8"));
                    writer.flush();
                    writer.close();

                    InputStream inputStream = urlConnection.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String tempStr;
                    StringBuffer stringBuffer = new StringBuffer();

                    while ((tempStr = bufferedReader.readLine()) != null) {
                        stringBuffer.append(tempStr);
                    }

                    bufferedReader.close();
                    inputStream.close();
                    urlConnection.disconnect();
                    Log.d("fuck", stringBuffer.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // HEARRRRRRRRRRRRRRRRRRRRRR~!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public int dataHandler(float[] data) {

        int id = -1;
        MotionID = id;
        seg.splitRealTime(data);
        //IMPORTANT


        //float amount=100.00f;

        if (seg.isInMotion()) {
            if (in_motion_listener != null)
                in_motion_listener.onEvent(data, -1);
        } else {
            if (non_motion_listener != null)
                non_motion_listener.onEvent(data, -1);
        }

        boolean ret = seg.isAnAction();
        Log.i(TAG, Boolean.toString(ret));
        if (seg.isAnAction()) {
            int c = seg.getMotionData(one_groups_data);
            Length = c;
            if (c == 0)
                c = c - 1 + 1; // debug中斷點使用
            else {
                Log.i(TAG, "[DealWithData] strValue = " + Length);

                //java.util.Arrays.deepToString(one_groups_data);

                FileOutputStream fop = null;
                File file;
                //String content = message;

                try {

                    File sdcard = Environment.getExternalStorageDirectory();
                    //Log.i(TAG,"HERE:", sdcard );
                    file = new File(sdcard, "Android/data/com.awmotiondemo15/files/Log1.txt"); //輸出檔案位置
                    Log.i("Write File:", file + "");

                    //fop = context.openFileOutput("Log1.txt", 3);
                    fop = new FileOutputStream(file);
                    //FileOutputStream fop = openFileOutput(file,Context.MODE_WORLD_WRITEABLE);

                    if (!file.exists()) { // 如果檔案不存在，建立檔案
                        file.createNewFile();
                    }
                    Log.i(TAG, "GOOOOOOOOOOOOO");
                    //byte[] contentInBytes = S_one_groups_data.getBytes();// 取的字串內容bytes


                    System.out.println(one_groups_data);

                    String[] S_one_groups_data = new String[one_groups_data.length];

                    for (int i = 0; i < one_groups_data.length; i++) {
                        theSensorData = theSensorData + String.valueOf(one_groups_data[i]) + ",";
                        S_one_groups_data[i] = String.valueOf(one_groups_data[i]);
                        //byte[] contentInBytes = S_one_groups_data[i].getBytes();
                        fop.write(S_one_groups_data[i].getBytes());
                        fop.write(',');
                        if ((i > 0) && ((i + 1) % 14 == 0))
                            fop.write('\n');
                    }
                    //fop.write(contentInBytes); //輸出

                    fop.flush();
                    fop.close();
                    sentDataGet("我媽寶" + Math.random());

                } catch (IOException e) {
                    Log.i("Write E:", e + "");
                    e.printStackTrace();
                } finally {
                    try {
                        if (fop != null) {
                            fop.close();
                        }
                    } catch (IOException e) {
                        Log.i("Write IOException", e + "");
                        e.printStackTrace();
                    }
                }

                id = motionSDK.predictActivity(one_groups_data, c);
                MotionID = id;
                if (action_listener != null)
                    action_listener.onEvent(data, id);
                seg.startMotion();
            }
        }
        return id;
    }

    public void moveFile(Context c, String file) {
        File f1 = new File(c.getExternalFilesDir(null), file);
        if (f1.exists()) {
            boolean deleted = f1.delete();
        }
        InputStream in;
        BufferedReader reader;
        FileWriter output;
        BufferedWriter bwStart;
        String line;
        try {
            in = c.getAssets().open(file);
            reader = new BufferedReader(new InputStreamReader(in));
            File outfile = new File(c.getExternalFilesDir(null), file);
            Log.i(TAG, outfile.getAbsolutePath());
            output = new FileWriter(outfile.getAbsolutePath(), true);
            bwStart = new BufferedWriter(output);
            while ((line = reader.readLine()) != null) {
                bwStart.write(line);
                bwStart.newLine();
            }
            bwStart.close();    //關檔
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initialize(Context c) {
        // SegmentMotion settings
        seg = new SegmentMotion(read.max_data_size, read.data_column, read.low_tolerant, read.low_threshold / 100.0f, read.high_threshold / 100.0f, read.getRMSColumnByMode(read.split_depend));
        one_groups_data = new float[seg.getMaxDataLine() * seg.getDataColumn()];

        // initialize feature file
        moveFile(c, read.getFile(ReadSettings.FEATURE_FILE));
        File f1 = new File(c.getExternalFilesDir(null), read.getFile(ReadSettings.FEATURE_FILE));
        String feature_file = f1.getAbsolutePath();
        motionSDK.setFeatureFile(feature_file);

        // initialize model file
        if (checkOpenCV3Model(c, read.getFile(ReadSettings.MODEL_FILE))) {
            transformOpenCV3Model(c, read.getFile(ReadSettings.MODEL_FILE));
        } else {
            moveFile(c, read.getFile(ReadSettings.MODEL_FILE));
        }
        File f2 = new File(c.getExternalFilesDir(null), read.getFile(ReadSettings.MODEL_FILE));
        String s2 = f2.getAbsolutePath();
        int r = motionSDK.init(s2, 2, seg.getMaxDataLine(), seg.getDataColumn() + read.getEmptyNum());
    }

    public boolean checkOpenCV3Model(Context c, String file) {
        InputStream in;
        BufferedReader reader;
        String line;
        try {
            in = c.getAssets().open(file);
            reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                if (line.equals("   format: 3")) {
                    reader.close();    //關檔
                    in.close();
                    return true;
                }
            }
            reader.close();    //關檔
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void transformOpenCV3Model(Context c, String file) {
        int row = 0;
        File f1 = new File(c.getExternalFilesDir(null), file);
        if (f1.exists()) {
            boolean deleted = f1.delete();
        }
        InputStream in;
        BufferedReader reader;
        FileWriter output;
        BufferedWriter bwStart;
        String line;
        try {
            in = c.getAssets().open(file);
            reader = new BufferedReader(new InputStreamReader(in));
            File outfile = new File(c.getExternalFilesDir(null), file);
            Log.i(TAG, outfile.getAbsolutePath());
            output = new FileWriter(outfile.getAbsolutePath(), true);
            bwStart = new BufferedWriter(output);
            while ((line = reader.readLine()) != null) {
                if (line.equals("   format: 3")) {
                } else if (line.startsWith("   svmType:")) {
                    bwStart.write("   svm_type:" + line.substring(12));
                    bwStart.newLine();
                } else if (line.startsWith("   var_count:")) {
                    bwStart.write("   var_all:" + line.substring(13));
                    bwStart.newLine();
                    bwStart.write("   var_count:" + line.substring(13));
                    bwStart.newLine();
                } else if (line.startsWith("      rows:")) {
                    row = Integer.parseInt(line.substring(12));
                } else if (line.startsWith("      cols:")) {
                    bwStart.write("      rows:" + line.substring(12));
                    bwStart.newLine();
                    bwStart.write("      cols:" + Integer.toString(row));
                    bwStart.newLine();
                } else {
                    bwStart.write(line);
                    bwStart.newLine();
                }
            }
            bwStart.close();    //關檔
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int DealWithData(String strValue) {
        int n = read.getEmptyNum();
        float[] data = new float[seg.getDataColumn() + n];
        float sub_time;
        //Log.d(TAG, "[DealWithData] strValue = " +strValue);
        sub_time = 0.0f;
        strValue = Float.valueOf(sub_time).toString() + "," + strValue;
        Calendar c_end = Calendar.getInstance();
        long c_sub = c_end.getTimeInMillis();
        dataCount = (dataCount + 1) % AVG_FRAME_RATE_RESET;
        if (dataCount == 0)
            startTime = c_sub;
        else
            avgDataRate = dataCount * 1000.0f / (c_sub - startTime);
        boolean Data_Correctness = true;

        try {
            int i;
            String[] Receiving_Data = strValue.split(",");
            for (i = 0; i < Receiving_Data.length; i++)
                data[i] = Float.parseFloat(Receiving_Data[i]);
            if (n > 0) {
                for (; n > 0; n--)
                    data[i++] = 0.0f;
            }
        } catch (Exception e1) {
            Data_Correctness = false;
        }
        if (Data_Correctness) {
            int ret = dataHandler(data);
            return ret;
        } else
            return -2;
    }

    public void dataHandlerTest(String asset_datafile_for_test) {
        TestDataHandler handler = new TestDataHandler(context, motionSDK, read.max_data_size, read.data_column * 20);
        int lbl_idx = read.findColumnIndex("Label");

        // 測試方式 (任選其一)
        handler.readAndTest(asset_datafile_for_test, lbl_idx); // 資料切割 + 動作辨識測試
        //handler.readAndSingleTest(asset_datafile_for_test, lbl_idx); // 連續動作辨識測試 (CNN)
    }
}
