package com.iii.libtool;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by brain on 2016/12/6.
 */

public class SensorDataHandler implements DataHandler {
    private String TAG = "SensorDataHandler";
    private float[] one_groups_data;// = new float[data_groups_num * data_dimension];
    private SegmentMotion seg = null;

    static final int bufferSize = 1000;

    private String[] line = new String[bufferSize];
    private int lineCount = 0;

    public static final int SPLIT_METHOD_AUTO = 1;
    public static final int SPLIT_METHOD_MANUAL = 2;
    
    static final int AVG_FRAME_RATE_RESET = 100;
    
    static final int DEFAULT_LABEL = 1;
        
    private SensorEventListener in_motion_listener = null;
    private SensorEventListener non_motion_listener = null;
    private SensorEventListener action_listener = null;
    private SensorEventListener next_action_listener = null;
    private SensorEventListener complete_data_collection_listener = null;
    private ExceptionEventListener exception_listener = null;
    private BufferOutOfBoundListener buffer_out_of_bound_listener = null;

    boolean useSplit = false;
    int splitMethod;
    boolean start = false;
    Calendar calendar;
    String filename = null;
    boolean saveFile = false;
    Context context;
    FileWriter output;
    BufferedWriter bwStart;
    int label = -1;
    String absolutePath = null;

    int[] dataColumns;
    String[] dataColumnNames;
    int data_num = 0;
    int dataCount = 0;
    long startTime = -1;
    float avgDataRate = 0.0f;

    String errMsg = "";
    boolean recordData = false;
    private int motionNum = 0;
    boolean labelInOrder = false;
    int currLabel = -1;
    int numPerLabel = 3;
    int actionNum = -1;

    boolean isPause = false;
    boolean modifyAndPause = false;

    public SensorDataHandler(Context c, SegmentMotion s, int[] col_num, String[] col_str, boolean modify_and_pause){
        calendar = Calendar.getInstance();
        context = c;
        seg = s;
        dataColumns = col_num;
        dataColumnNames = col_str;
        modifyAndPause = modify_and_pause;

        if(dataColumns != null && dataColumns.length > 0) {
            for (int i = 0; i < dataColumns.length; i++)
                data_num += dataColumns[i];
		}

        one_groups_data = new float[seg.getMaxDataLine() * seg.getDataColumn()];
    }
    
    public void setSegmentationParameters(int max_line, int data_column, int stop_count, float value, float peak, int[] rms_col){
        seg.initSplit(max_line, data_column, stop_count, value, peak, rms_col);
    }

    public void setInMotionListener(SensorEventListener toAdd) {
        in_motion_listener = toAdd;
    }
    public void setNonMotionListener(SensorEventListener toAdd) {
        non_motion_listener = toAdd;
    }
    public void setActionMotionListener(SensorEventListener toAdd) {
        action_listener = toAdd;
    }
    public void setNextActionListener(SensorEventListener toAdd) {
        next_action_listener = toAdd;
    }
    public void setCompleteDataCollectionListener(SensorEventListener toAdd) {
        complete_data_collection_listener = toAdd;
    }
    public void setExceptionEventListener(ExceptionEventListener toAdd) {
        exception_listener = toAdd;
    }
    public void setBufferOutOfBoundListener(BufferOutOfBoundListener toAdd) {
        buffer_out_of_bound_listener = toAdd;
    }    

    public int getDataColumnNum(int col){
        return dataColumns[col];
	}

    public String getDataColumnName(int col){
        return dataColumnNames[col];
    }

    public void setUseSplit(boolean use){
        useSplit = use;
        //if(useSplit == true)
        //    label = 1;
    }

    public boolean getUseSplit(){
        return useSplit;
    }

    public void setLabel(int lbl){
        label = lbl;
    }

    public int getLabel(){
        return label;
    }

    public void setSplitMethod(int m){
        splitMethod = m;
    }

    public int getSplitMethod(){
        return splitMethod;
    }

    public float getAvgDataRate(){
        return avgDataRate;
    }

    public void resetDataCount(){
        dataCount = 0;
        startTime = -1;
    }

    public void setLabelInOrder(boolean order){
        labelInOrder = order;
        //if(labelInOrder == true)
        //    label = 1;
    }

    public boolean getLabelInOrder(){
        return labelInOrder;
    }

    public void setNumPerLabel(int num){
        numPerLabel = num;
    }

    public boolean getSaveFileFlag(){
        return saveFile;
    }

    public int getMotionNum(){ return motionNum; }

    public String getLastErrMsg(){
        String msg = errMsg;
        errMsg = "";
        return msg;
    }

    public String getAbsolutePath(){
        return absolutePath;
    }

    public void setStart(){
        start = true;
    }

    public void setEnd(){
        start = false;
        motionNum++;
        if (recordData && action_listener != null)
            action_listener.onEvent(null, getLabel());
        if(labelInOrder && motionNum % numPerLabel == 0) {
            if(getLabel() + 1 > actionNum) {
                stopRecording();
                if (complete_data_collection_listener != null)
                    complete_data_collection_listener.onEvent(null, getLabel());
            }
            else {
                setLabel(getLabel() + 1);
                if (next_action_listener != null)
                    next_action_listener.onEvent(null, getLabel());
            }
        }
    }

    public boolean openFile(String f, boolean append){
        try {
            File outfile = new File(context.getExternalFilesDir(null), filename);
            Log.i("TAG", outfile.getAbsolutePath());
            output = new FileWriter(outfile.getAbsolutePath(), append);
            bwStart = new BufferedWriter(output);
            saveFile = true;
            absolutePath = outfile.getAbsolutePath();
        }catch (Exception e) {
            e.printStackTrace();
            errMsg = "檔案開啟失敗:" + e.getMessage().toString();
            saveFile = false;
            if(exception_listener != null)
                exception_listener.onEvent(errMsg);
        }
        return saveFile;
    }

    public String getDefaultFileName(String f)
    {
        calendar = Calendar.getInstance();
        String Time_Name = String.format("%d%02d%02d_%02d%02d", calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE));
        return f + "_" + Time_Name + ".txt";
    }

    public boolean startSaveFile(String f, int num){
        if(num > 0) {
            actionNum = num;
            motionNum = 0;
            if(labelInOrder)
                setLabel(1);
            if (f != null) {
                stopRecording();
                filename = f;
                openFile(filename, true);
                closeFile();
                //Toast.makeText(context, "開始存檔", Toast.LENGTH_SHORT).show();
            }
        }
        recordData = saveFile;
        return saveFile;
    }

    public void stopRecording()
    {
        recordData = false;

        if(filename != null) {
            if(lineCount > 0)
                writeFile();
            closeFile();
            filename = null;
            //Toast.makeText(context, "動作收集完成:" , Toast.LENGTH_SHORT).show();
        }

    }

    public void pauseRecording(){
        isPause = true;
    }

    public void resumeRecording(){
        isPause = false;
    }

    public void modifyLastMotionData(int label){
        int i, j, k = -1, lbl = 0;
        boolean is_motion = false;
        for(i = lineCount - 1; i >= 0; i--){
            //String s = line[i].substring(line[i].indexOf(";")+1, line[i].length() - 1);
            String s = line[i].substring(line[i].lastIndexOf(",")+1, line[i].length() - 1);
            j = Integer.parseInt(s);
            if(is_motion == true && j == 0)
                break;
            else if(j > 0) {
                if(k == -1)
                    k = i;
                is_motion = true;
                lbl = j;
            }
        }
        if (lbl > 0) {
            for (i++; i <= k; i++) {
                //String prefix = line[i].substring(0, line[i].indexOf(";") + 1);
                String prefix = line[i].substring(0, line[i].lastIndexOf(",") + 1);
                line[i] = prefix + label + "\n";
            }
        }
    }

    public boolean writeFile(){
        try {
            openFile(filename, true);
            for (int i = 0; i < lineCount; i++) {
                bwStart.write(line[i]);
            }
            closeFile();
            //recordData = saveFile;
            saveFile = recordData;
            lineCount = 0;
            resumeRecording();
        } catch (Exception e1) {
            e1.printStackTrace();
            errMsg = "檔案寫入失敗:" + e1.getMessage().toString();
            if(exception_listener != null)
                exception_listener.onEvent(errMsg);
            recordData = false;
            saveFile = false;
        }
        return recordData;
    }

    public boolean writeSingleTxtData(String strValue) {
        boolean ret = true;
        if(isPause)
            return true;
        if(lineCount > bufferSize)
            return false;
        line[lineCount++] = strValue;
        if(lineCount > bufferSize - 50) {
            if(modifyAndPause)
                pauseRecording();
            else {
                if(buffer_out_of_bound_listener != null)
                    buffer_out_of_bound_listener.onEvent();
                writeFile();
            }
        }
        return ret;
    }

    public void closeFile()
    {
        try {
            bwStart.close();
            output.close();
        } catch (Exception e1) {
            e1.printStackTrace();
            errMsg = "檔案關閉失敗:" + e1.getMessage().toString();
            if(exception_listener != null)
                exception_listener.onEvent(errMsg);
        }
    }

    public void dataHandler(float[] data){
        //Log.d(TAG, "[DealWithData] strValue = " + data.length);
        int id = -1;
        int lbl = 0;
        int c = 0;
        if(useSplit == false) {
            String str = "";
            for(int i = 0; i < data.length-1; i++)
                str += Float.toString(data[i]) + ",";
            //str += Float.toString(data.length-1) + ";" + Integer.toString(getLabel()) + "\n";
            str += Float.toString(data[data.length-1]) + "," + Integer.toString(getLabel()) + "\n";
            if(saveFile && recordData) {
                writeSingleTxtData(str);
            }
        }
        else if(splitMethod == SPLIT_METHOD_MANUAL) {
            String str = "";
            for(int i = 0; i < data.length - 1; i++)
                str += Float.toString(data[i]) + ",";
            str += Float.toString(data[data.length - 1]);
            /*if(start == true)
                str += ";" + Integer.toString(getLabel()) + "\n";
            else
                str += ";0\n";*/
            if(start == true)
                str += "," + Integer.toString(getLabel()) + "\n";
            else
                str += ",0\n";
            if(saveFile && recordData) {
                writeSingleTxtData(str);
            }
        }
        else if(splitMethod == SPLIT_METHOD_AUTO){
            seg.splitRealTime(data);
            if (seg.isInMotion()) {
                if (recordData && in_motion_listener != null)
                    in_motion_listener.onEvent(data, -1);
            } else {
                //c = seg.getMotionData(one_groups_data);
                c = seg.getMotionCount();
                if (seg.isAnAction()) {
                    if (c == 0)
                        c = c - 1 + 1;
                    else {
                        lbl = getLabel();
                        motionNum++;
                        if (recordData && action_listener != null)
                            action_listener.onEvent(data, lbl);
                        if(labelInOrder && motionNum % numPerLabel == 0) {
                            if(getLabel() + 1 > actionNum) {
                                stopRecording();
                                if (complete_data_collection_listener != null)
                                    complete_data_collection_listener.onEvent(null, getLabel());
                            }
                            else {
                                setLabel(getLabel() + 1);
                                if (next_action_listener != null)
                                    next_action_listener.onEvent(null, getLabel());
                            }
                        }
                    }
                }
                else{
                    lbl = 0;
                }
                String str;
                /*for(int i = 0; i < c; i++) {
                    str = seg.getStrOneMotionData() + ";" + Integer.toString(lbl) + "\n";
                    if(saveFile && recordData) {
                        writeSingleTxtData(str);
                    }
                }*/
                for(int i = 0; i < c; i++) {
                    str = seg.getStrOneMotionData() + "," + Integer.toString(lbl) + "\n";
                    if(saveFile && recordData) {
                        writeSingleTxtData(str);
                    }
                }
                str = "";
                for(int i = 0; i < data.length - 1; i++) {
                    str += Float.toString(data[i]) + ",";
                }
                //str += Float.toString(data[data.length - 1]) + ";0\n";
                str += Float.toString(data[data.length - 1]) + ",0\n";
                if(saveFile && recordData) {
                    writeSingleTxtData(str);
                }
                if (recordData && non_motion_listener != null)
                    non_motion_listener.onEvent(data, -1);
				if (seg.isAnAction() && c > 0){
                    seg.startMotion();
                }
            }
        }
    }

    public int DealWithData(String strValue) {
        float[] data = new float[seg.getDataColumn()];
        float sub_time;
        //Log.d(TAG, "[DealWithData] strValue = " +strValue);
        Calendar c_end = Calendar.getInstance();
        long c_sub = c_end.getTimeInMillis();
        long d = calendar.getTimeInMillis();
        sub_time = (c_sub - d) / 1000.0f;
        strValue = Float.valueOf(sub_time).toString() + "," + strValue;
        dataCount = (dataCount + 1) % AVG_FRAME_RATE_RESET;
        if(dataCount == 0 || startTime == -1)
            startTime = c_sub;
        else
            avgDataRate = dataCount * 1000.0f / (c_sub - startTime);
        boolean Data_Correctness = true;

        try {
            int i;
            String[] Receiving_Data = strValue.split(",");
            for(i = 0; i < data_num; i++){
                data[i] = Float.parseFloat(Receiving_Data[i]);
            }
        } catch (Exception e1) {
            Data_Correctness = false;
        }
        if (Data_Correctness) {
            dataHandler(data);
            return 1;
        }
        else
            return -1;
    }
}
