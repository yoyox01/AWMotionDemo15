package com.awmotiondemo15;

import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.iii.libtool.BTEventListener;
import com.iii.libtool.MotionEventListener;
import com.iii.libtool.MotionRecognitionHandler;
import com.iii.libtool.PhoneBluetoothHandler;
import com.iii.libtool.ReadSettings;
import com.iii.libtool.SensorDataHandler;
import com.iii.libtool.SettingListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    // ---------------需加入新Project的部分-----------------
    private String TAG = "MainActivity";
    /*
    //2017/11/16
    Thread connectInternet=new Thread(new Runnable() {
        @Override
        public void run() {
            try {

                BufferedReader br = null;
                String response = null;
                //String theSensorData = "51531,865456,487321,4894616,47469,5165,464654,568,8797,";
                StringBuffer output = new StringBuffer();
                String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String savePath = fullPath + File.separator + "Android/data/com.awmotiondemo15/files/Log1"+".txt";
                Log.i("Write File:", savePath + "");
                br = new BufferedReader(new FileReader(savePath));
                String line = "";
                String theSensorData = "";
                for (int i=0;i<200;i++) {
                    line = br.readLine();
                    theSensorData += line;
                }

                String uri = Uri.parse("http://140.123.97.110:1337/brushTeeth/uploads/index.php")
                        .buildUpon()
                        .appendQueryParameter("data", theSensorData)
                        .build().toString();
                URL url = new URL(uri);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream     = urlConnection.getInputStream();
                BufferedReader bufferedReader  = new BufferedReader( new InputStreamReader(inputStream) );
                String tempStr;
                StringBuffer stringBuffer = new StringBuffer();

                while( ( tempStr = bufferedReader.readLine() ) != null ) {
                    stringBuffer.append( tempStr );
                }

                bufferedReader.close();
                inputStream.close();
                urlConnection.disconnect();
                Log.d("拜託",stringBuffer.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

*/


    PhoneBluetoothHandler bluetoothHandler = null;
    MotionRecognitionHandler handler = null;
    SensorDataHandler data_handler = null;
    public ReadSettings read = null;
    static final String FILE_LIST = "FileList.txt";

    TextView tv_connect_text, TextView_Status, tv_act, TextView_Error_Log;
    TextView TextView_Motion_Num;
    TextView TextView_res;

    private RadioGroup Radio_Test;

    private RadioButton Radio_Single, Radio_Group;

    private Button Button_Start_Record;
    private Button Button_Stop_Record;
    private Button Button_Correct;
    private Button Button_Wrong;
    private Button Button_Error;

    private Spinner spinner;
    private String[] list = {""};
    private ArrayAdapter<String> listAdapter;

    private String currentListName;

    int correct = 0;
    int wrong = 0;
    int error = 0;
    int num = 0;
    int curr_motion_id = -1;
    boolean start_record = false;
    int sync_num = 0;
    // -----------------------------------------------------
    // 被動式寫法
    private void runThread(final String msg){
        runOnUiThread (new Thread(new Runnable() {
            public void run() {
                if(msg == "Connected"){
                    tv_connect_text.setTextColor(0xff0000ff);
                    tv_connect_text.setText("已連線..." + bluetoothHandler.getDeviceName());
                    Toast.makeText(MainActivity.this, "接受連線已成功", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "已連線");
                }
                else if(msg == "NoConnection"){
                    tv_connect_text.setTextColor(0xffff0000);
                    tv_connect_text.setText("等待連線...");
                    Button_Start_Record.setEnabled(false);
                    Log.d(TAG, "等待連線");
                }
                else if(msg == "Moving"){
                    TextView_Status.setTextColor(0xff00ff00);
                    String str = String.format("%.1f", handler.getAvgDataRate());
                    TextView_Status.setText("資料傳輸中(" + str + ")...動作中");
                }
                else if(msg == "Motion"){
                    int motion_id = handler.getMotionID();
                    int len = handler.getMotionLength();
                    String motion_name = handler.getMotionName();
                    if(start_record == false || Button_Correct.isEnabled() == false) {
                        tv_act.setTextColor(0xff0000ff);
                        tv_act.setText("長度=" + len + ",  " + motion_name);
                    }
                    if(start_record && Button_Correct.isEnabled() == false) {
                        curr_motion_id = motion_id;
                        spinner.setSelection(0);
                        Button_Correct.setEnabled(true);
                        Button_Wrong.setEnabled(true);
                        Button_Error.setEnabled(true);
                    }
                }
                else if(msg == "Sync"){
                    String s;
                    if(sync_num > 0){
                        s = "與裝置同步中...剩" + sync_num + "個項目";
                        TextView_Error_Log.setText(s);
                        Log.d(TAG, s);
                    }
                    if(sync_num == 0) {
                        s = "同步完成!";
                        TextView_Error_Log.setText(s);
                        Log.d(TAG, s);
                        if(Button_Stop_Record.isEnabled() == false)
                            Button_Start_Record.setEnabled(true);
                    }
                }
                else if(msg == "GetData"){
                    Log.d(TAG, msg);
                    TextView_Status.setTextColor(0xff00ff00);
                    String str = String.format("%.1f", handler.getAvgDataRate());
                    TextView_Status.setText("資料傳輸中(" + str + ")");
                }
                //TextView_Status.setTextColor(0xff00ff00);
                //String str = String.format("%.1f", handler.getAvgDataRate());
                //TextView_Status.setText("資料傳輸中(" + str + ")...");
                //TextView_Status.setText(s);
                //tv_connect_text.setTextColor(0xff0000ff);
                //tv_connect_text.setText("已連線..." + bluetoothHandler.getDeviceName());
            }
        }));
    }

    public void aboutApp(View view) {
        // 顯示訊息框，指定三個參數
        // Context：通常指定為「this」
        // String或int：設定顯示在訊息框裡面的訊息或文字資源
        // int：設定訊息框停留在畫面的時間
        //connectInternet.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // ---------------需加入新Project的部分-----------------
        if (shouldAskPermissions()) {
            askPermissions();
        }
        //connectInternet.start();




        tv_connect_text = (TextView) findViewById(R.id.textView);
        tv_connect_text.setTextColor(0xffff0000);
        tv_connect_text.setText("等待連線...");
        tv_connect_text.setVisibility(View.VISIBLE);

        TextView_Status = (TextView) findViewById(R.id.textView2);
        TextView_Status.setVisibility(View.VISIBLE);

        tv_act = (TextView) findViewById(R.id.textView3);
        tv_act.setVisibility(View.VISIBLE);

        TextView_Error_Log = (TextView) findViewById(R.id.textView4);
        TextView_Error_Log.setVisibility(View.VISIBLE);

        TextView_Motion_Num = (TextView) findViewById(R.id.textView5);
        TextView_res = (TextView) findViewById(R.id.textView6);

        Radio_Test = (RadioGroup) findViewById(R.id.radioGroup);
        Radio_Single = (RadioButton) findViewById(R.id.radioButton);
        Radio_Group = (RadioButton) findViewById(R.id.radioButton2);

        Button_Start_Record = (Button) findViewById(R.id.button);
        Button_Stop_Record = (Button) findViewById(R.id.button2);
        Button_Correct = (Button) findViewById(R.id.button3);
        Button_Wrong = (Button) findViewById(R.id.button4);
        Button_Error = (Button) findViewById(R.id.button5);

        spinner = (Spinner) findViewById(R.id.spinner);

        Button_Start_Record.setEnabled(false);
        Button_Stop_Record.setEnabled(false);
        Button_Correct.setEnabled(false);
        Button_Wrong.setEnabled(false);
        Button_Error.setEnabled(false);
        Radio_Test.check(R.id.radioButton);

        // Android Wear
        read = new ReadSettings(this, FILE_LIST);
        currentListName = read.getMotionListName(); // 取得動作列表名稱
        Log.d(TAG, "currentListName=" + currentListName);
        handler = new MotionRecognitionHandler(this, read);
        handler.setGroupTest(MotionRecognitionHandler.SINGLE_TEST); // 設定實測方式: SINGLE_TEST: 單動作測試, GROUP_TEST: 群組動作測試
        // getSensorDataHandler(Context c, boolean modify_and_pause, int label, boolean use_split, int split_method):
        // modify_and_pause: 動作後暫停並修改標記,  label: 預設標記, use_split: 使用動作切割
        // split_method: 切割方法 (use_split為true時才有效)
        //                          自動切割: SensorDataHandler.SPLIT_METHOD_AUTO  = 1
        //                          開始與結束點切割: SensorDataHandler.SPLIT_METHOD_MANUAL  = 2
        data_handler = read.getSensorDataHandler(this, true, 1, true, 0);
        Log.d(TAG, "GroupTest=" + handler.getGroupTest());
        for(int i = 0; i < read.action_list_num; i++)
            Log.d(TAG, (i) + "." + read.action_list[i] + "\t\t" + read.action_list_id[i]);
        for(int i = 0; i < read.action_group_num; i++)
            Log.d(TAG, (i) + "." + read.action_group[i] + "(" + read.action_group_id[i] + ")");

        // 測試資料檔辨識測試
        //handler.dataHandlerTest(輸入放在Assets中的測試檔名稱);
        bluetoothHandler = new PhoneBluetoothHandler(this, handler);
        bluetoothHandler.setFrameRate(read.frame_rate); // 設定手錶frame rate
        bluetoothHandler.setSensorType(read.sensor_type_id); // 設定感測器類型
        Log.d(TAG, "FrameRate=" + bluetoothHandler.getFrameRate());
        Log.d(TAG, "SensorType=" + Arrays.toString(bluetoothHandler.getSensorType()));
        // -----------------------------------------------------
        // ------------------被動式呼叫寫法(主動式/被動式擇一加入即可)------------------------
// 定義函式內容
        bluetoothHandler.setConnectedListener(new BTEventListener() {
            @Override
            public void onEvent(String msg) {
                runThread("Connected");
            }
        });
        bluetoothHandler.setNoConnectionListener(new BTEventListener() {
            @Override
            public void onEvent(String msg) {
                runThread("NoConnection");
            }
        });
        bluetoothHandler.setGetResultListener(new BTEventListener() {
            @Override
            public void onEvent(String msg) {
                runThread("GetData");
                data_handler.DealWithData(msg);
            }
        });
        bluetoothHandler.setSettingListener(new SettingListener() {
            @Override
            public void onEvent(int val) {
                sync_num = val;
                runThread("Sync");
            }
        });
        handler.setInMotionListener(new MotionEventListener() {
            @Override
            public void onEvent(float[] data, int motion_id) {
                runThread("Moving");
            }
        });
        handler.setNonMotionListener(new MotionEventListener() {
            @Override
            public void onEvent(float[] data, int motion_id) {
                //tv_status.setText("無動作");
            }
        });
        handler.setActionMotionListener(new MotionEventListener() {
            @Override
            public void onEvent(float[] data, int motion_id) {
                runThread("Motion");
            }
        });

        Button_Start_Record.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(MainActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);


                input.setText(data_handler.getDefaultFileName(currentListName + "_test")); // handler處理部分
                input.setLayoutParams(lp);

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle("為此資料存檔命名");
                dialog.setView(input);

                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // do nothing
                    }
                });

                dialog.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //handler處理部分
                        final String default_file_name = input.getText().toString();
                        boolean ret = data_handler.startSaveFile(input.getText().toString(), spinner.getCount());
                        if(ret) {
                            correct = 0;
                            wrong = 0;
                            error = 0;
                            num = 0;
                            TextView_Motion_Num.setText("正確=" + correct + "/" + num + ", 誤判=" + wrong + "/" + num  + ", 誤觸=" + error + "/" + num);
                            String str = String.format("正確率=%.2f%%", correct * 100.0f / num);
                            TextView_res.setText(str);
                            Button_Start_Record.setEnabled(false);
                            Button_Stop_Record.setEnabled(true);
                            Radio_Single.setEnabled(false);
                            Radio_Group.setEnabled(false);
                            start_record = true;
                        }
                    }
                });
                dialog.show();
            }
        });

        Button_Stop_Record.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button_Stop_Record.setEnabled(false);
                Button_Start_Record.setEnabled(true);
                Button_Correct.setEnabled(false);
                Button_Wrong.setEnabled(false);
                Button_Error.setEnabled(false);
                Radio_Single.setEnabled(true);
                Radio_Group.setEnabled(true);
                start_record = false;
            }
        });

        Button_Correct.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button_Correct.setEnabled(false);
                Button_Wrong.setEnabled(false);
                Button_Error.setEnabled(false);
                correct++;
                num++;
                TextView_Motion_Num.setText("正確=" + correct + "/" + num + ", 誤判=" + wrong + "/" + num  + ", 誤觸=" + error + "/" + num);
                String str = String.format("正確率=%.2f%%", correct * 100.0f / num);
                TextView_res.setText(str);
                data_handler.modifyLastMotionData(curr_motion_id);
                data_handler.writeFile();
            }
        });

        Button_Wrong.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                int idx = spinner.getSelectedItemPosition();
                if(idx == 0) {
                    Toast.makeText(MainActivity.this, "請先選擇正確答案", Toast.LENGTH_SHORT).show();
                    return;
                }
                Button_Correct.setEnabled(false);
                Button_Wrong.setEnabled(false);
                Button_Error.setEnabled(false);
                wrong++;
                num++;

                TextView_Motion_Num.setText("正確=" + correct + "/" + num + ", 誤判=" + wrong + "/" + num  + ", 誤觸=" + error + "/" + num);
                String str = String.format("正確率=%.2f%%", correct * 100.0f / num);
                TextView_res.setText(str);
                data_handler.modifyLastMotionData(idx);
                data_handler.writeFile();
            }
        });

        Button_Error.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Button_Correct.setEnabled(false);
                Button_Wrong.setEnabled(false);
                Button_Error.setEnabled(false);
                error++;
                num++;

                TextView_Motion_Num.setText("正確=" + correct + "/" + num + ", 誤判=" + wrong + "/" + num  + ", 誤觸=" + error + "/" + num);
                String str = String.format("正確率=%.2f%%", correct * 100.0f / num);
                TextView_res.setText(str);
                data_handler.modifyLastMotionData(0);
                data_handler.writeFile();
            }
        });

        list = (String[])new String[read.action_list.length+1];
        System.arraycopy(read.action_list, 0, list, 1, read.action_list.length);
        list[0] = "請選擇正確動作";
        listAdapter = new ArrayAdapter<String>(this, R.layout.simple_spinner_item, list);
        spinner.setAdapter(listAdapter);
        //spinner.setClickable(false);
        spinner.setSelection(0);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                // handler處理部分
                if(position > 0)
                    data_handler.setLabel(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });
        Radio_Test.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                View radioButton = Radio_Test.findViewById(checkedId);
                int idx = Radio_Test.indexOfChild(radioButton);
                // handler處理部分
                handler.setGroupTest(idx);
            }
        });
// ---------------------------------------------------------------

    }
    // ---------------需加入新Project的部分-----------------
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothHandler.stop();
    }

    @Override
    public void onStart() {
        super.onStart();
        bluetoothHandler.start();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    // -----------------------------------------------------
    //permission
    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }


}
