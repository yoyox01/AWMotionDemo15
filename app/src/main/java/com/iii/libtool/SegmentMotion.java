package com.iii.libtool;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import static java.lang.Math.sqrt;

/**
 * Created by brain on 2016/11/22.
 */

public class SegmentMotion {
    // states of the algorithm
    public int STATE_START = 0;  // start
    public int STATE_STATIC = 1;  // no action
    public int STATE_MOTION	= 2;  // motion action
    public int STATE_WAIT = 3; // detected an action
    
    // peak flag
    public int PEAK_NULL = 0;  // not yet detected a peak
    public int PEAK_SET = 1;  // a peak detected (higher than peak_threshold in current region)
    
    // output values
    public int DETECT_NONE = 0;
    public int DETECT_A_MOTION = 1;
    public int DETECT_START_MOTION =	2;
    //public int DETECT_NON_MOTION	= 3;
    public int OUT_OF_BOUND = 4;

    // Global variables:
    public float avg_distance = 0.0f;
    public float value_threshold;
    public float peak_threshold;
    public int peak_start, peak_end;
    public int state;
    public int peak_state;
    public int complete_state;
    public int count = 0;
    public int max_data_line;
    public int vector_num;
    public int stop_low_count;
    public float v1[]; // values of the last line
    private String motion_data[];
    public float q1[] = {0.0f, 0.0f, 0.0f, 0.0f};
    public float q_score;
    public int low_count;
    public Queue<String> data_queue;
	public int[] rms_column;


    //-----------------------------------------------------------
    // functions:

    public SegmentMotion(int max_line, int data_column, int stop_count, float value, float peak, int[] rms_col){
        initSplit(max_line, data_column, stop_count, value, peak, rms_col);
    }

    public int initSplit(int max_line, int data_column, int stop_count, float value, float peak, int[] rms_col)
    {
        int i;
        rms_column = rms_col;
        max_data_line = max_line;
        motion_data = new String[max_data_line];
        vector_num = data_column;
        stop_low_count = stop_count;
        value_threshold = value;
        peak_threshold = peak;
        peak_start = 0;
        peak_end = 0;
        v1 = new float[vector_num];
        for (i = 0; i < vector_num; i++)
            v1[i] = 0.0f;
        data_queue = new LinkedList<String>();

        startMotion();
        return 1;
    }

    public float RMS_Value(float[] v)
    {
        float rms = 0.0f;
        if(rms_column == null || rms_column.length == 0)
            return 0.0f;
        else {
            for (int i = 0; i < rms_column.length; i++)
                rms += (v[rms_column[i]] - v1[rms_column[i]]) * (v[rms_column[i]] - v1[rms_column[i]]);
            rms = (float) sqrt(rms);
            return rms;
        }
    }

    public int splitRealTime(float[] v)
    {
        int is_complete_action = DETECT_NONE;
        int i, j;
        String s;
        q_score = -1.0f;

        if(state == STATE_START){
            for (i = 0; i < vector_num; i++)
                v1[i] = v[i];
            state = STATE_STATIC;
            complete_state = DETECT_NONE;
            return DETECT_NONE;
        }
        else if (count > max_data_line - 10) {
            startMotion();
            return OUT_OF_BOUND;
        }
        else{
            q_score = RMS_Value(v);
            if (q_score == 0.0f) { // ignore sudden zero value in the data
                return -1;
            }
            for (i = 0; i < vector_num; i++)
                v1[i] = v[i];
            if (q_score > value_threshold) { // start a region if the filter function value "score" higher than value_threshold
                if (state == STATE_STATIC) {
                    is_complete_action = DETECT_START_MOTION;
                }
                s = "";
                for(i = 0; i < v.length-1; i++)
                    s += Float.toString(v[i])+",";
                s += Float.toString(v[i]);
                data_queue.offer(s);
                count++;
                state = STATE_MOTION;
                if (q_score > peak_threshold) // accept the region if the filter function value "score" higher than value_threshold
                    peak_state = PEAK_SET;
            }
            if (q_score <= value_threshold) { // no action or close a region (output data & reset states)
                if (state == STATE_MOTION) { // output data
                    low_count++;
                    if (low_count >= stop_low_count) {
                        if (peak_state == PEAK_SET) {
                            is_complete_action = DETECT_A_MOTION;
                            state = STATE_WAIT;
                        } else {
                            startMotion();
                        }
                    }
                    else {
                        s = "";
                        for(i = 0; i < v.length-1; i++)
                            s += Float.toString(v[i])+",";
                        s += Float.toString(v[i]);
                        data_queue.offer(s);
                        count++;
                    }
                }
            }
            complete_state = is_complete_action;
            return is_complete_action;
        }

    }

    public float getRMSValue(){
        return q_score;
    }

    public int getMaxDataLine(){
        return max_data_line;
    }

    public int getDataColumn(){
        return vector_num;
    }

    public int getMotionCount() {
        return data_queue.size();
    }

    public boolean isInMotion()
    {
        return (state == STATE_MOTION);
    }

    public boolean isAnAction() {
        boolean i = (complete_state == DETECT_A_MOTION);
        //Log.i("TAG", "action=" + Boolean.toString(i) + ", count=" + count);
        return (complete_state == DETECT_A_MOTION);
    }

    public String getStrOneMotionData(){
        return data_queue.poll();
    }

    public float[] getOneMotionData(){
        if(data_queue.isEmpty())
            return null;
        else {
            String[] s = data_queue.poll().split(",");
            int i;
            float [] v = new float[vector_num];
            for(i = 0; i < s.length && i < vector_num; i++){
                v[i] = Float.parseFloat(s[i]);
            }
            return v;
        }
    }

    public int getMotionData(float[] data){
        if(data_queue.isEmpty())
            return 0;
        else {
            float[] tmp;
            int i, j;
            int c = data_queue.size();
            for(i = 0; i < c; i++){
                tmp = getOneMotionData();
                for(j = 0; j < vector_num; j++)
                    data[i * vector_num + j] = tmp[j];
            }
            return c;
        }
    }


    public void startMotion()
    {
        count = 0;
        low_count = 0;
        state = STATE_START;
        peak_state = PEAK_NULL;
        complete_state = DETECT_NONE;
        data_queue.clear();
    }

}
