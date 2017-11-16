package com.iii.libtool;

import android.content.Context;
import android.hardware.Sensor;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by brain on 2017/1/16.
 */

public class ReadSettings {
    private String TAG = "ReadSettings";
    Context context;
    SegmentMotion seg = null;
    SegmentMotion seg_data = null;
    SensorDataHandler handler = null;

    public String file_list;
    //public String setting_file;
    public String device_type;
    public String device_name;
    public String action_list_file;
    //public String feature_file;
    //public String model_file;
    public int sensor_num;
    public String[] sensor_type_str;
    public int[] sensor_type_id;
    public int[] sensor_columns;
    public int[] cavy_sensor_index; // 記錄cavy sensor index
    public int frame_rate;
    public int max_data_size;
    public int data_column;
    public int low_tolerant;
    public int low_threshold;
    public int high_threshold;
    public int split_depend;

    public boolean emptyAppeared = false;
    public int emptyColumnNum = 0;

    // action list file
    public int action_list_num = 0;
    public int action_group_num = 0;
    public String[] action_list;
    public String[] action_group;
    public int[] action_list_id;
    public int[] action_group_id;
    public int[] action_group_id_map;

    public String motionListName;

    public String errMsg;
    
    public String[] columnDescription;
    public int[] columnDim;

    public String[] FileList = new String[4];

    String[] default_sensor_type_str = {"Accelerometer", "Linear Accelerometer", "Gyroscope", "Magnetometer", "Heart Rate Sensor"};
    String[] default_sensor_type_cavy_str = {"Quaternion", "Euler Angle", "Accelerometer", "Linear Accelerometer", "Velocity"};
    int[] default_sensor_type_id = {Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_HEART_RATE};
    int[] default_sensor_type_cavy_id = {100, 101, Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION, 102};
    
    String UTF8_BOM = "\uFEFF";

    static final int MODEL_FILE = 0;
    static final int FEATURE_FILE = 1;
    static final int MOTION_FILE = 2;
    static final int SETTING_FILE = 3;

    public static final String ANDROID_WEAR = "AndroidWear";
    public static final String CAVY_BAND = "CavyBand";

    public ReadSettings(Context c, String file){
        context = c;
        file_list = file;
        readSettings();
        String t = getFile(ReadSettings.MOTION_FILE);
        motionListName = t.substring(0, t.length() - 4);
    }

    public String getFile(int index){
        return FileList[index];
    }

    public static String toUtf8(String str) {
        try {
            return new String(str.getBytes("UTF-8"), "UTF-8");
        }
        catch(Exception e){
            return null;
        }
    }

    public boolean readSettings(){
        boolean res = getFileList();
        if(res == false)
            return false;
        res = readSettingsFile();
        if(res == false)
            return false;
        res = readActionListFile();
        return res;
    }

    public String getErrMsg(){
        return errMsg;
    }

    public boolean getFileList(){
        InputStream in;
        BufferedReader reader;
        try {
            in = context.getAssets().open(file_list);
            reader = new BufferedReader(new InputStreamReader(in));
            FileList[MODEL_FILE] = reader.readLine();
            FileList[FEATURE_FILE] = reader.readLine();
            FileList[MOTION_FILE] = reader.readLine();
            FileList[SETTING_FILE] = reader.readLine();
            reader.close();
        }
        catch(IOException e){
            errMsg = "讀取列表檔錯誤" + e.getMessage();
            return false;
        }
        return true;
    }

    public int getSensorTypeID(String s){
        for(int i = 0; i < default_sensor_type_str.length; i++)
            if(s.equals(default_sensor_type_str[i]))
                return default_sensor_type_id[i];
        return -1;
    }

    public int getCavySensorTypeID(String s){
        for(int i = 0; i < default_sensor_type_cavy_str.length; i++)
            if(s.equals(default_sensor_type_cavy_str[i]))
                return default_sensor_type_cavy_id[i];
        return -1;
    }

    public boolean readSettingsFile(){
        int i, j, k;
        InputStream in;
        BufferedReader reader;
        emptyAppeared = false;
        emptyColumnNum = 0;
        try {
            in = context.getAssets().open(FileList[SETTING_FILE]);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            line = reader.readLine();
            device_type = line.substring(line.indexOf("=")+1);
            line = reader.readLine();
            device_name = line.substring(line.indexOf("\"")+1, line.length() - 1);
            line = reader.readLine();
            action_list_file = line.substring(line.indexOf("=")+1);
            line = reader.readLine();
            sensor_num = Integer.parseInt(line.substring(line.indexOf("=")+1));
            line = reader.readLine();
            sensor_type_str = line.substring(line.indexOf("=")+1).split(",");
            if(sensor_num != sensor_type_str.length) {
                throw new Exception("SensorNum與SensorTypeName長度不一致");
            }
            sensor_type_id = new int[sensor_num];
            for(i = 0; i < sensor_num; i++){
                sensor_type_id[i] = getSensorTypeID(sensor_type_str[i]);
            }
            line = reader.readLine();
            String[] col_str = line.substring(line.indexOf("=")+1).split(",");
            columnDescription = line.substring(line.indexOf("=")+1).split(",");
            line = reader.readLine();
            String[] col = line.substring(line.indexOf("=")+1).split(",");
            if(col.length != col_str.length) {
                throw new Exception("ColumnDim與ColumnDescription長度不一致");
            }
            
            columnDim = new int[col.length];
            for(i = 0; i < col.length; i++)
                columnDim[i] = Integer.parseInt(col[i]);
            
            sensor_columns = new int[sensor_num];
            cavy_sensor_index = new int[sensor_num];
            for(i = 0, j = 0, k = 0; i < col.length; i++) {
                if(col_str[i].equals("Empty")){
                    emptyAppeared = true;
                    emptyColumnNum = Integer.parseInt(col[i]);
                    if(emptyColumnNum < 0){
                        throw new Exception("Empty長度不合法");
                    }
                    k += Integer.parseInt(col[i]);
                    continue;
                }
                if(col_str[i].equals("Timestamp") == false && col_str[i].equals("Label") == false) {
                    if(emptyAppeared == true){
                        throw new Exception("Empty後只可接Label");
                    }
                    sensor_columns[j] = Integer.parseInt(col[i]);
                    if(sensor_columns[j] < 0){
                        throw new Exception("欄位" + col_str[i] + "=" + col[i] + "不合法");
                    }
                    if(device_type.equals("CavyBand"))
                        cavy_sensor_index[j] = k;
                    else
                        cavy_sensor_index[j] = 0;
                    j++;
                }
                k += Integer.parseInt(col[i]);
            }
            if(j != sensor_num) {
                throw new Exception("ColumnDim中之Sensor數量與SensorNum不一致");
            }
            line = reader.readLine();
            frame_rate = Integer.parseInt(line.substring(line.indexOf("=")+1));
            line = reader.readLine();
            String[] s = line.substring(line.indexOf("=")+1).split(",");
            max_data_size = Integer.parseInt(s[0]);
            data_column = Integer.parseInt(s[1]);
            low_tolerant = Integer.parseInt(s[2]);
            low_threshold = Integer.parseInt(s[3]);
            high_threshold = Integer.parseInt(s[4]);
            split_depend = Integer.parseInt(s[5]);
            reader.close();
        }
        catch(Exception e) {
            errMsg = "讀取設定檔(" + FileList[SETTING_FILE] + ")錯誤: " + e.getMessage();
            Log.d(TAG, errMsg);
            return false;
        }
        return true;
    }

    public boolean readActionListFile(){
        InputStream in;
        BufferedReader reader;
        try {
            in = context.getAssets().open(action_list_file);
            reader = new BufferedReader(new InputStreamReader(in));
            int i = 0;
            String line;
            String[] items = new String[2];
            String[] act_list = new String[1000];
            int[] act_id = new int[1000];
            action_list_num = 0;
            while((line = reader.readLine()) != null){
                if(line.length() > 0){
                    if(line.startsWith(UTF8_BOM))
                        line = line.replace(UTF8_BOM,"");
                    items = line.split("[\t]+");
                    if(items.length != 2)
                        break;
                    //act_list[action_list_num] = new String(items[0].getBytes("big5"),"big5");
                    act_list[action_list_num] = items[0];
                    act_id[action_list_num++] = Integer.parseInt(items[1]);
                }
                else
                    break;
            }
            action_list = new String[action_list_num];
            action_list[0] = "";
            action_list_id = new int[action_list_num];
            action_list_id[0] = 0;
            action_group_id_map = new int[action_list_num];
            for(i = 0; i < action_list_num; i++)
                action_group_id_map[i] = -1;
            for(i = 0; i < action_list_num; i++) {
                action_list[i] = act_list[i];
                action_list_id[i] = act_id[i];
                int s = action_list[i].indexOf("(");
                if(s != -1){
                    int t = action_list[i].indexOf(")");
                    int id = Integer.parseInt(action_list[i].substring(s+1, t));
                    if(id >= 0)
                        action_group_id_map[i] = id;
                }
            }
            action_group_num = 0;
            while((line = reader.readLine()) != null){
                if(line.length() > 0){
                    items = line.split("[\t]+");
                    if(items.length != 2)
                        break;
                    //act_list[action_list_num] = new String(items[0].getBytes("big5"),"big5");
                    act_list[action_group_num] = items[0];
                    act_id[action_group_num++] = Integer.parseInt(items[1]);
                }
                else
                    break;
            }
            if(action_group_num > 0) {
                action_group = new String[action_group_num];
                action_group[0] = "";
                action_group_id = new int[action_group_num];
                for (i = 0; i < action_group_num; i++)
                    action_group_id[i] = -1;
                for (i = 0; i < action_group_num; i++) {
                    action_group[i] = act_list[i];
                    action_group_id[i] = act_id[i];
                }
            }
            reader.close();
        }
        catch(IOException e){
            return false;
        }
        return true;
    }

    public int[] getSensorColumnArray(){
        int[] col = new int[sensor_type_str.length];
        for(int i = 0; i < sensor_type_str.length; i++){
            col[i] = sensor_columns[i];
        }
        return col;
    }

    public String[] getSensorTypeNameArray(){
        String[] str = new String[sensor_type_str.length];
        for(int i = 0; i < sensor_type_str.length; i++){
            str[i] = sensor_type_str[i];
        }
        return str;
    }

    /*public int[] getCavySensorIndex(){
        int i, j = 0, idx = 1; // 跳過timestamp;
        int[] sensor_idx = new int[sensor_type_str.length];
        for(i = 0; i < sensor_type_str.length; i++){
            sensor_idx[i] = idx;
            idx += sensor_columns[i];
        }
        return sensor_idx;
    }*/

    public int[] getCavySensorIndex() {
        return cavy_sensor_index;
    }

    public int getEmptyNum(){
        return emptyColumnNum;
    }

    public int computeDataDimension(){
        int i, dim = 0;
        if(sensor_type_str == null)
            return 0;
        for(i = 0; i < sensor_type_str.length; i++){
            dim += sensor_columns[i];
        }
        return dim + 1; // add 1 for timestamp
    }

    @Nullable
    public int[] getRMSColumnByMode(int mode){
        int i, j, dim = 0;
        int[] rms_col;
        String[] sensor_map = {"", "Accelerometer", "Quaternion"};
        if(mode <= 0 || mode > 2) // currently only two modes
            return null;
        for(i = 0; i < sensor_type_str.length; i++){
            if(sensor_type_str[i].equals(sensor_map[mode])) {
                rms_col = new int[sensor_columns[i]];
                for(j = 0; j < sensor_columns[i]; j++)
                    rms_col[j] = dim + j + 1; // +1 for timestamp
                return rms_col;
            }
            dim += sensor_columns[i];
        }
        return null;
    }

    public SegmentMotion getSegmentMotion(){
        if(seg_data == null)
            seg_data = new SegmentMotion(max_data_size, computeDataDimension(), low_tolerant, low_threshold / 100.0f, high_threshold / 100.0f,
                    getRMSColumnByMode(split_depend));
        return seg_data;
    }

    public SensorDataHandler getSensorDataHandler(Context c, boolean modify_and_pause, int label, boolean use_split, int split_method){
        if(handler == null)
            handler = new SensorDataHandler(c, getSegmentMotion(), sensor_columns, sensor_type_str, modify_and_pause); 
        else{
            handler.setSegmentationParameters(max_data_size, computeDataDimension(), low_tolerant, low_threshold / 100.0f, high_threshold / 100.0f,
                    getRMSColumnByMode(split_depend));
        }
        handler.setLabel(label); // 將標記設成預設值
        handler.setUseSplit(use_split); // 使用動作切割
        handler.setSplitMethod(split_method); // 設定自動切割動作

        return handler;
    }

    public String getMotionListName(){
        return motionListName;
    }

    public int findColumnIndex(String col_name){
        int idx = 0;
        for(int i = 0; i < columnDescription.length; i++) {
            if (columnDescription[i].equals(col_name))
                return idx;
            idx += columnDim[i];
        }
        return -1;
    }
}
