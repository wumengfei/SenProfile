package com.example.vsense;

import android.Manifest;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.provider.SyncStateContract.Helpers.update;

/**
 * Created by thinkpad on 2016/8/5.
 */


public class CarFragment extends Fragment implements SensorEventListener {

    LocationManager lm;
    SensorManager sm;
    TextView tv;
    Button bu;
    ImageView iv;


    //蓝牙相关
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int DEFAULT_ADVERTISE_INTERVAL = 500;
    private static final int INTERVAL_BETWEEN_ONANDOFF = 300;
    private static final ParcelUuid SAMPLE_UUID =
            ParcelUuid.fromString("0000FE00-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBluetoothAdapter;
    // helper objects for BLE advertising, derived from mBluetoothAdapter above
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseSettings.Builder mBleAdvertiseSettingsBuilder;
    private AdvertiseData.Builder mBleAdvertiseDataBuilder;
    // helper objects for BLE scanning, derived from mBluetoothAdapter above
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanSettings.Builder mBleScanSettingsBuilder;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0x123) {
                //do something here
                //Toast.makeText(MainActivity.this, "timer", Toast.LENGTH_SHORT).show();
                startAdvertising();
                try {
                    Thread.sleep(INTERVAL_BETWEEN_ONANDOFF);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopAdvertising();
                //start advertising
                //adStatus.setText("正在广播......");
            }
        }
    };;
    private Timer timer;
    private TimerTask timerTask;
    private boolean isActive = true;

    private byte[] serviceData;
    private String VEHICLES = "0";

    private static final String TAG = "SampleBLE";
    private long startTime;
    private long endTime;

    int[] images = new int[]{
            R.drawable.car1, R.drawable.car2,
            R.drawable.car3, R.drawable.car4,
            R.drawable.car5, R.drawable.car6,
    };

    private int ShowFlag = 0;
    final static int No_Bump = 0;
    final static int One_Bump = 1;
    final static int Waiting_for_Bump = 2;
    final static float s = (float) 0.03;
    final static float h = (float) 0.05;
    float z = (float) 1.0;
    final static float t = (float) 1.0;
    final static float T_NEXT_DELAY = (float) 2.0;

    float ACC_OF_BRAKE = (float) -2.0;
    float EMERGENCY = (float) -5.0;


    public float ALL_OF_BUMP = 0;
    public float BAD_TURN_1 = 0;
    public float BAD_TURN_2 = 0;
    public float BAD_TURN = 0;
    public int BAD_ACC = 0;
    public float accelerationFromGps;
    public float speedLatter = 0;
    public float speedFormer;
    public float speed;        //get the velocity from GPS
    public float velocity = 0;       //compute the velocity from acceleration
    public float velocity1 = 0;
    public float velo = 0;
    public float velo1 = 0;
    public float avervelocity;
    public float avervelocity1;
    public float v;            //get the angular acceleration from gyroscope
    public float v1;
    public double accelerator;  //acceleration from accelerator
    public double accelerator1;
    public double averalinear;//acceleration in the geo-frame
    public double averalinear1;
    public double averacc;
    public double averacc1;
    public double hpaveracc;
    public double hpaveracc1;
    public double accnoise = 0;
    public double accnoise1;

    public int flag = 0;
    public int FLAG_OF_BRAKE = 0;
    public float MAX_OF_BRAKE = 0;
    //设置采样频率
    public float intervalT;
    public float times = 0;
    public float turnLeftTimes = 0;
    public float turnRightTimes = 0;
    public float turnBackTimes = 0;
    public float ltorTimes = 0;
    public float rtolTimes = 0;
    public float brakeTimes = 0;
    public float emergencyBrakeTimes = 0;
    public float curvyRoadTimes = 0;
    public float shift = 0;
    public static int start = 0;
    public static int begin, begin2 = 0;
    public static int end, end2 = 0;
    public float max;
    public float max1;
    public float max2;
    public float T_BUMP, T_BUMP2, T_dwell = 0;
    public int state = No_Bump;
    public int start_of_2nd_bump = 0;
    public float distance;
    public float ang;
    public float tdistance;
    public float tang;
    public float KalmanFilterSpeed;
    public double ori;
    public float ang_degree;
    public float[] vs = new float[200];
    public float[] speeds = new float[200];
    //private float accelerators[]=new float[100];
    public float[] velocitys = new float[200];
    public float[] velos = new float[200];
    public float[] accelerometerValues = new float[3];//get three-dimensional values from accelerometer从加速计获取3D值
    public float[] magneticFieldValues = new float[3];//get three-dimensional values from gyroscope从磁场计获取3D值
    public float[] orientation = new float[3];//get the azimuth from values above 从加速计和磁场计计算得来的方位角

    public String mode;
    public static String filename = "vsense";

    DataSet dataset = new DataSet();
    MovingAverage moveAverage;
    MovingAverage moveAveragedir;
    MovingAverage moveAverageacc;
    private long driveTime;

    private ImageButton Ibt_back2;
    private Message resultMessage;
    private TextView tv_alert;


    //	public int middle=0;
    //返回值是角度
    public float angle_calculate(float[] vs, int b, int e) {
        float aaa = 0;
        float aaa1 = 0;
        if (e >= b) {
            for (int n = b; n <= e; n++) {
                aaa = (float) (aaa + vs[n] * 0.05 * 57.29578);
            }
        } else if (e < b) {
            for (int n = b; n < 200; n++) {
                aaa = (float) (aaa + vs[n] * 0.05 * 57.29578);
            }
            for (int n = 0; n <= e; n++) {
                aaa = (float) (aaa + vs[n] * 0.05 * 57.29578);
            }
        }

        DecimalFormat df2 = new DecimalFormat("#.00");
        aaa1 = Float.valueOf(df2.format(aaa));

        return aaa1;
    }


    //need to be finished
    private void writeSDcard(String str) {
        try {
            Log.d("SDcard", "exists");
            // if the SDcard exists 判断是否存在SD卡
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                // get the directory of the SDcard 获取SD卡的目录
                File sdDire = Environment.getExternalStorageDirectory();
                FileOutputStream outFileStream = new FileOutputStream(
                        sdDire.getCanonicalPath() + "/" + filename + ".txt", true);
                outFileStream.write(str.getBytes());
                outFileStream.close();
                Log.d("SDcard", "input");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public float getdistance(float ve[], float vv[], int b, int e) {
        float d = 0;
        float d1 = 0;
        float a = 0;

        if (e >= b) {
            for (int i = b; i < e; i++) {
                a = a + vv[i] * (float) 0.05;     //angle=angle before * time (100ms/0.1s)角度等于原始角度加陀螺仪角速度乘以0.1秒时间
                d = d + ve[i] * (float) 0.05 * (float) Math.sin(a);  //horizontal displacement 水平距离等于速度乘以0.1秒时间乘以sin值
            }
        } else if (e < b) {
            for (int i = b; i < 200; i++) {
                a = a + vv[i] * (float) 0.05;     //angle=angle before * time (100ms/0.1s)角度等于原始角度加陀螺仪角速度乘以0.1秒时间
                d = d + ve[i] * (float) 0.05 * (float) Math.sin(a);  //horizontal displacement 水平距离等于速度乘以0.1秒时间乘以sin值
            }
            for (int i = 0; i <= e; i++) {
                a = a + vv[i] * (float) 0.05;     //angle=angle before * time (100ms/0.1s)角度等于原始角度加陀螺仪角速度乘以0.1秒时间
                d = d + ve[i] * (float) 0.05 * (float) Math.sin(a);  //horizontal displacement 水平距离等于速度乘以0.1秒时间乘以sin值
            }
        }

        DecimalFormat df = new DecimalFormat("#.00");
        d1 = Float.valueOf(df.format(d));

        return d1;
    }

    /*Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            //计算gps加速度的定时器
            if (msg.what == 0x110) {
                accelerationFromGps = speedLatter - speedFormer;
            }else if(msg.what == 0x234){
                switch ((String)msg.obj){
                    case "左转":
                        tv.setText("左转");
                        iv.setImageResource(images[1]);
                        VEHICLES = "1";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "右转":
                        tv.setText("右转");
                        iv.setImageResource(images[2]);
                        VEHICLES = "2";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "调头":
                        tv.setText("调头");
                        iv.setImageResource(images[3]);
                        VEHICLES = "3";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "向左单排变道":
                        tv.setText("向左单排变道");
                        iv.setImageResource(images[4]);
                        VEHICLES = "4";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "向左多排变道":
                        tv.setText("向左多排变道");
                        iv.setImageResource(images[4]);
                        VEHICLES = "4";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "向右单排变道":
                        tv.setText("向右单排变道");
                        iv.setImageResource(images[5]);
                        VEHICLES = "5";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "向右多排变道":
                        tv.setText("向右多排变道");
                        iv.setImageResource(images[5]);
                        VEHICLES = "5";
                        ShowFlag = 0;
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "刹车":
                        tv.setText("刹车");
                        iv.setImageResource(images[0]);
                        VEHICLES = "6";
                        startAdvertising();
                        stopAdvertising();
                        break;
                    case "急刹车":
                        tv.setText("急刹车");
                        iv.setImageResource(images[0]);
                        VEHICLES = "7";
                        startAdvertising();
                        stopAdvertising();
                        break;
                    default:
                        if (ShowFlag > 3){
                            tv.setText("直线行驶");
                            iv.setImageResource(images[0]);
                            VEHICLES = "0";
                            ShowFlag = 0;
                            startAdvertising();
                            stopAdvertising();
                        }
                        ShowFlag++;
                        break;
                }
            }

        }
    };*/
    Handler handler = new Handler() {

        public void handleMessage(Message msg) {
            //计算gps加速度的定时器
            if (msg.what == 0x110) {
                accelerationFromGps = speedLatter - speedFormer;
            }else if(msg.what == 0x234){
                switch ((String)msg.obj){
                    case "左转":
                        tv.setText("左转");
                        iv.setImageResource(images[1]);
                        VEHICLES = "1";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "右转":
                        tv.setText("右转");
                        iv.setImageResource(images[2]);
                        VEHICLES = "2";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "调头":
                        tv.setText("调头");
                        iv.setImageResource(images[3]);
                        VEHICLES = "3";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "向左单排变道":
                        tv.setText("向左单排变道");
                        iv.setImageResource(images[4]);
                        VEHICLES = "4";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "向左多排变道":
                        tv.setText("向左多排变道");
                        iv.setImageResource(images[4]);
                        VEHICLES = "4";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "向右单排变道":
                        tv.setText("向右单排变道");
                        iv.setImageResource(images[5]);
                        VEHICLES = "5";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "向右多排变道":
                        tv.setText("向右多排变道");
                        iv.setImageResource(images[5]);
                        VEHICLES = "5";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "刹车":
                        tv.setText("刹车");
                        iv.setImageResource(images[0]);
                        VEHICLES = "6";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    case "急刹车":
                        tv.setText("急刹车");
                        iv.setImageResource(images[0]);
                        VEHICLES = "7";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                    default:
                        tv.setText("直线行驶");
                        iv.setImageResource(images[0]);
                        VEHICLES = "0";
                        startAdvertising();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        stopAdvertising();
                        break;
                }
            }

        }
    };

    private void dataProcess() {
        times++;                //全局变量，统计log记得次数
        moveAverage.pushValue(v);
        v = moveAverage.getValue();
        vs[start] = v;
        //if(velocity==0){velocity=accelerator;}
        //if(velocitys[99]!=0){velocitys[99]=(float) 0.0;}
//        		if((start-1)<=0){
//        			middle=(start-1)+100;
//        			}else{
//        				middle=start-1;
//        				}

        avervelocity += averalinear * 0.05;
//        		velocity+=averacc*0.05;
//        		velo+=accelerator*0.05;
        DecimalFormat df = new DecimalFormat("#.00");
        avervelocity1 = Float.valueOf(df.format(avervelocity));

//        		velocity1 = Float.valueOf(df.format(velocity));
//        		velo1 = Float.valueOf(df.format(velo));
        velocitys[start] = avervelocity;
//        		velos[start]=velo1;
        KalmanFilterSpeed = algorithmKalmanFilter();
        speeds[start] = speed;
        //speeds[start]=KalmanFilterSpeed;
        start = (start + 1) % 200;   //start++;if start exceed 100, than begin from 0 但是防止start超过100，过了100后从0开始循环


        tdistance = Math.abs(getdistance(speeds, vs, (start + 199) % 200, start));//改成start
        tang = angle_calculate(vs, (start + 199) % 200, start);//改成start
        ori = calculateOrientation();


        distance = Math.abs(getdistance(speeds, vs, begin, start));
        ang = angle_calculate(vs, begin, start);


        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");
        String str = sdf.format(new Date());
        String message = str;

        writeSDcard(message + "\t" + accelerator1 + "\t" + averacc1 + "\t" + averalinear1 + "\t" + accelerationFromGps + "\t" + avervelocity1 + "\t"
                + speed + "\t" + KalmanFilterSpeed + "\t"
                + v1 + "\t" + moveAverage.getValue() + "\t" + ang + "\t"
                + ori + "\t" + distance + "\t" + tang + "\t" + tdistance + "\t" + "\n");
        //判断刹车
        if (averalinear1 <= ACC_OF_BRAKE) {
            FLAG_OF_BRAKE++;
            if (averalinear1 < MAX_OF_BRAKE) {
                MAX_OF_BRAKE = (float) averalinear1;
            }
        } else if (averalinear1 > ACC_OF_BRAKE) {
            if (FLAG_OF_BRAKE > 2) {
                resultMessage = new Message();
                resultMessage.what = 0x234;
                resultMessage.obj = "刹车";

                handler.sendMessage(resultMessage);
//                tv.setText("刹车");
                brakeTimes++;
                if (MAX_OF_BRAKE < EMERGENCY) {
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "急刹车";

                    handler.sendMessage(resultMessage);
//                    tv.setText("急刹车");
                    emergencyBrakeTimes++;
                }
            }
            MAX_OF_BRAKE = 0;
            FLAG_OF_BRAKE = 0;
        }


        if (state == No_Bump && Math.abs(v) > s) {     //陀螺仪读数大于s，进入一个凸点状态
            state = One_Bump;

            begin = start;
            max = v;

        } else if (state == One_Bump && Math.abs(v) > s) {      //此时为一个凸点状态
            if (Math.abs(v) > z) {
                BAD_TURN_1 = 1;
            }

            T_BUMP = T_BUMP + (float) 0.05;                 //第一个凸点的停留时间
            if (Math.abs(v) > Math.abs(max)) {
                max = v;
            }                               //计算测量过程中的最大偏航率
        } else if (state == One_Bump && Math.abs(v) <= s) {
            end = start;


            if (Math.abs(max) > h && T_BUMP > t) {

                ALL_OF_BUMP++;
                BAD_TURN += BAD_TURN_1;

                if (angle_calculate(vs, begin, end) <= -80 && angle_calculate(vs, begin, end2) >= -135) {
                    //Toast.makeText(MainActivity.this, "Turn Right finished", Toast.LENGTH_SHORT).show();
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "右转";

                    handler.sendMessage(resultMessage);
                    turnRightTimes++;
                    state=No_Bump;
                    writeSDcard(message + "\t" + "右转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                } else if (angle_calculate(vs, begin, end) <= 135 && angle_calculate(vs, begin, end2) >= 80) {
                    //Toast.makeText(MainActivity.this, "Turn Left finished", Toast.LENGTH_SHORT).show();
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "左转";

                    handler.sendMessage(resultMessage);
                    turnLeftTimes++;
                    writeSDcard(message + "\t" + "左转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                }
                else if (Math.abs(angle_calculate(vs, begin, end)) > 135) {
                    //Toast.makeText(MainActivity.this, "Turn Back", Toast.LENGTH_SHORT).show();
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "掉头";

                    handler.sendMessage(resultMessage);
                    turnBackTimes++;
                    state=No_Bump;
                    writeSDcard(message + "\t" + "掉头" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                }
/*
						if (angle_calculate(vs, begin, end) < -60 && angle_calculate(vs, begin, end) > -135) {
							//Toast.makeText(MainActivity.this, "Turn Right", Toast.LENGTH_SHORT).show();
							tv.setText("Turn Right");
							iv.setImageResource(images[2]);
							//turnRightTimes++;
							writeSDcard(message + "\t" + "Turn Right" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end) + "\n");
						} else if (angle_calculate(vs, begin, end) < 135 && angle_calculate(vs, begin, end) > 60) {
							//Toast.makeText(MainActivity.this, "Turn Left", Toast.LENGTH_SHORT).show();
							tv.setText("Turn Left");
							iv.setImageResource(images[1]);
							//turnLeftTimes++;
							writeSDcard(message + "\t" + "Turn Left" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end) + "\n");
						}//此时判定检测到的凸点为有效凸点，进入等待凸点状态
*/
                max1 = max;
                state = Waiting_for_Bump;
                max = 0;
            } else {                                     //如不符合三个条件，则判定刚才检测到的凸点无效，算法返回无凸点状态
                T_BUMP = 0;
                state = No_Bump;
                max = 0;
                BAD_TURN_1 = 0;
//                iv.setImageResource(images[0]);
//                tv.setText("直线行驶");
                resultMessage = new Message();
                resultMessage.what = 0x234;
                resultMessage.obj = "直线行驶";
                handler.sendMessage(resultMessage);
            }

        } else if (state == Waiting_for_Bump) {              //如果此时进入了等待凸点状态
            if (Math.abs(v) <= s && start_of_2nd_bump == 0) {
                T_dwell = T_dwell + (float) 0.05;                //计算上一个凸点第二个s值与下一个凸点第一个s值之间的时间
            }

            //如果两个s之间时间间隔超过2秒 则不会打出finished
            if (T_dwell < T_NEXT_DELAY && Math.abs(v) > s) {   //如果两个凸点之间的时间间隔小于陀螺仪读数的最大停留时间，且陀螺仪读数大于s，进入第二个凸点状态

                T_BUMP2 = T_BUMP2 + (float) 0.05;            //第二个凸点的停留时间
                start_of_2nd_bump = 1;                   //开启第二个凸点状态
                if (Math.abs(v) > z) {
                    BAD_TURN_2 = 1;
                }
                if (Math.abs(v) > Math.abs(max)) {          //计算第二个凸点状态中的最大偏航率
                    max = v;
                }

            } else if (Math.abs(v) <= s && start_of_2nd_bump == 1) {    //算法已经进入第二个凸点状态，且已经结束 未必是凸点
                end2 = start;

                //有效的凸点
                if (Math.abs(max) > h && T_BUMP2 > t) {
                    //5月4日 删除了距离的下限 只要第二个凸点结束就会提示变道

                    ALL_OF_BUMP++;
                    BAD_TURN += BAD_TURN_2;

                    if (/*Math.abs(getdistance(speeds, vs, begin, end2)) <= 4 && */(max * max1 < 0)/* && Math.abs(getdistance(speeds, vs, begin, end2)) > 1.5*/) {    //&&(max*max1<0)
                        //Toast.makeText(MainActivity.this, "Lane Change", Toast.LENGTH_SHORT).show();
                        if (max1 > 0) {

                            resultMessage = new Message();
                            resultMessage.what = 0x234;
                            if(Math.abs(getdistance(speeds, vs, begin, end2)) <= 4){
                                resultMessage.obj = "向左单排变道";

                            }
                            else if(Math.abs(getdistance(speeds, vs, begin, end2)) > 4){
                                resultMessage.obj = "向左多排变道";

                            }
                            handler.sendMessage(resultMessage);
//                            tv.setText("向左变道");
//                            iv.setImageResource(images[4]);
                            rtolTimes++;
                            writeSDcard(message + "\t" + "Change to a Left Lane" + "\t" + angle_calculate(vs, begin, end2)+"\t"+ 0.05*(end-begin) + "\t" +
                                    getdistance(speeds, vs, begin, end2)+"\t" + "\n");
                        } else if (max1 < 0) {
                            resultMessage = new Message();
                            resultMessage.what = 0x234;
                            if(Math.abs(getdistance(speeds, vs, begin, end2)) <= 4){
                                resultMessage.obj = "向右单排变道";

                            }
                            else if(Math.abs(getdistance(speeds, vs, begin, end2)) > 4){
                                resultMessage.obj = "向右多排变道";

                            }
                            handler.sendMessage(resultMessage);
//                            tv.setText("向右变道");
//                            iv.setImage3 begin, end2)+"\n");
                        }
                    }
                    //两个反向凸点区分变道、在弯曲的道路上
                    else if (max * max1 > 0) {
                        if (angle_calculate(vs, begin, end2) <= -60 && angle_calculate(vs, begin, end2) >= -135) {
                            //Toast.makeText(MainActivity.this, "Turn Right finished", Toast.LENGTH_SHORT).show();
                            resultMessage = new Message();
                            resultMessage.what = 0x234;
                            resultMessage.obj = "右转";
                            handler.sendMessage(resultMessage);
//                            tv.setText("右转");
                            turnRightTimes++;
//                            iv.setImageResource(images[2]);
                            writeSDcard(message + "\t" + "右转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                        } else if (angle_calculate(vs, begin, end2) <= 135 && angle_calculate(vs, begin, end2) >= 60) {
                            //Toast.makeText(MainActivity.this, "Turn Left finished", Toast.LENGTH_SHORT).show();
                            resultMessage = new Message();
                            resultMessage.what = 0x234;
                            resultMessage.obj = "左转";
                            handler.sendMessage(resultMessage);
//                            tv.setText("左转");
                            turnLeftTimes++;
//                            iv.setImageResource(images[1]);
                            writeSDcard(message + "\t" + "左转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                        }/* else if (Math.abs(angle_calculate(vs, begin, end)) > 135) {
                            //Toast.makeText(MainActivity.this, "Turn Back", Toast.LENGTH_SHORT).show();
                            resultMessage = new Message();
                            resultMessage.what = 0x234;
                            resultMessage.obj = "调头";
                            handler.sendMessage(resultMessage);
//                            tv.setText("调头");
//                            iv.setImageResource(images[3]);
                            turnBackTimes++;
                            writeSDcard(message + "\t" + "调头" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end) + "\t" + getdistance(speeds, vs, begin, end) + "\n");
                        }*/
                    }
//        						if(Math.abs(getdistance(speeds,vs,begin,end2))>4){
//        							//Toast.makeText(MainActivity. this, "Curvy Road",Toast.LENGTH_SHORT).show();
//        							tv.setText("Curvy Road"+"\t"+angle_calculate(vs, begin, end2)+"\t"+getdistance(speeds,vs,begin,end2));
//        							curvyRoadTimes++;
//        							writeSDcard(message+"\t"+"Curvy Road"+"\t"+angle_calculate(vs, begin, end2)+"\t"+
//        									getdistance(speeds,vs,begin,end2)+"\n");
//        						}
                }
                //无效的凸点
                else {
                    if (angle_calculate(vs, begin, end) <= -60 && angle_calculate(vs, begin, end2) >= -135) {
                        //Toast.makeText(MainActivity.this, "Turn Right finished", Toast.LENGTH_SHORT).show();
                        resultMessage = new Message();
                        resultMessage.what = 0x234;
                        resultMessage.obj = "右转";
                        handler.sendMessage(resultMessage);
//                        tv.setText("右转");
                        turnRightTimes++;
//                        iv.setImageResource(images[2]);
                        writeSDcard(message + "\t" + "右转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                    } else if (angle_calculate(vs, begin, end) <= 135 && angle_calculate(vs, begin, end2) >= 60) {
                        //Toast.makeText(MainActivity.this, "Turn Left finished", Toast.LENGTH_SHORT).show();
                        resultMessage = new Message();
                        resultMessage.what = 0x234;
                        resultMessage.obj = "左转";
                        handler.sendMessage(resultMessage);
//                        tv.setText("左转");
                        turnLeftTimes++;
//                        iv.setImageResource(images[1]);
                        writeSDcard(message + "\t" + "左转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\n");
                    } /*else if (Math.abs(angle_calculate(vs, begin, end)) > 135) {
                        //Toast.makeText(MainActivity.this, "Turn Back", Toast.LENGTH_SHORT).show();
                        resultMessage = new Message();
                        resultMessage.what = 0x234;
                        resultMessage.obj = "调头";
                        handler.sendMessage(resultMessage);
//                        tv.setText("调头");
//                        iv.setImageResource(images[3]);
                        turnBackTimes++;
                        writeSDcard(message + "\t" + "调头" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end) + "\t" + getdistance(speeds, vs, begin, end) + "\n");
                    }*/ else {
                        resultMessage = new Message();
                        resultMessage.what = 0x234;
                        resultMessage.obj = "直线行驶";
                        handler.sendMessage(resultMessage);
//                        iv.setImageResource(images[0]);
//                        tv.setText("直线行驶");
                    }
                }

                T_BUMP = 0;
                T_BUMP2 = 0;
                state = No_Bump;
                max = 0;
                max1 = 0;
                T_dwell = 0;
                start_of_2nd_bump = 0;
                //恢复到原始状态
                BAD_TURN_1 = 0;
                BAD_TURN_2 = 0;

            } else if (T_dwell >= T_NEXT_DELAY) {
                end2 = start;

                //此时判定为只有一个凸点
                if (angle_calculate(vs, begin, end) < -60 && angle_calculate(vs, begin, end) >= -135) {
                    //Toast.makeText(MainActivity.this, "Turn Right finished", Toast.LENGTH_SHORT).show();
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "右转";
                    handler.sendMessage(resultMessage);
//                    tv.setText("右转");
                    turnRightTimes++;
//                    iv.setImageResource(images[2]);
                    writeSDcard(message + "\t" + "右转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\t" + "One_bump" + "\n");

                } else if (angle_calculate(vs, begin, end) <= 135 && angle_calculate(vs, begin, end) > 60) {
                    //Toast.makeText(MainActivity.this, "Turn Left finished", Toast.LENGTH_SHORT).show();
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "左转";
                    handler.sendMessage(resultMessage);
//                    tv.setText("左转");
                    turnLeftTimes++;
//                    iv.setImageResource(images[1]);
                    writeSDcard(message + "\t" + "左转" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end)+"\t"+ 0.05*(end-begin) + "\t" + "One_bump" + "\n");

                } /*else if (Math.abs(angle_calculate(vs, begin, end)) > 135) {
                    //Toast.makeText(MainActivity.this, "Turn Back", Toast.LENGTH_SHORT).show();
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "调头";
                    handler.sendMessage(resultMessage);
//                    tv.setText("调头");
//                    iv.setImageResource(images[3]);
                    writeSDcard(message + "\t" + "调头" + "\t" + v1 + "\t" + moveAverage.getValue() + "\t" + angle_calculate(vs, begin, end) + "\t" + getdistance(speeds, vs, begin, end) + "\t" + "One_bump" + "\n");
                    //此时汽车调头，应伴随转向灯信号
                } */else {
                    resultMessage = new Message();
                    resultMessage.what = 0x234;
                    resultMessage.obj = "直线行驶";
                    handler.sendMessage(resultMessage);
//                    iv.setImageResource(images[0]);
//                    tv.setText("直线行驶");
                }

                T_BUMP = 0;
                T_BUMP2 = 0;
                state = No_Bump;
                T_dwell = 0;
                max = 0;
                max1 = 0;
                start_of_2nd_bump = 0;
                BAD_TURN_1 = 0;
                BAD_TURN_2 = 0;

            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startTime = System.currentTimeMillis();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_main_fragment1,container,false);

        tv = (TextView) v.findViewById(R.id.tv);
        tv_alert = (TextView) v.findViewById(R.id.alert);
        bu = (Button) v.findViewById(R.id.button_end);
        Ibt_back2 = (ImageButton) v.findViewById(R.id.back2);
        sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        //开启广播和扫描

        //startTimer();
        startScanning();
        //蓝牙部分
       /* if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }*/



        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // check for peripheral mode support
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            assert (mBluetoothLeAdvertiser != null);
        }


        Intent intent = getActivity().getIntent();
        Bundle bd = intent.getExtras();
        ACC_OF_BRAKE = bd.getFloat("min");
        EMERGENCY = bd.getFloat("max");
        z = bd.getFloat("z");
        mode = bd.getString("mode");

        // 10 可自定义，代表平均“窗口”大小。。小刘博要改成偶数？
        moveAverage = new MovingAverage(15);
        moveAveragedir = new MovingAverage(5);
        moveAverageacc = new MovingAverage(3);

        iv = (ImageView) v.findViewById(R.id.image);

        iv.setImageResource(images[0]);
        tv.setText("直线行驶");

        final Timer a = new Timer();
        a.schedule(new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
//                handler.sendEmptyMessage(0x111);
                dataProcess();
            }

        }, 0, 50);
        //此两个timer线程是否并行？http://www.cnblogs.com/chenssy/p/3788407.html
        final Timer b = new Timer();
        b.schedule(new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                handler.sendEmptyMessage(0x110);
            }

        }, 0, 1000);

        Ibt_back2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent1 = new Intent(getActivity(), SelectActivity.class);
                startActivity(intent1);
            }
        });

        bu.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                endTime = System.currentTimeMillis();
                driveTime = endTime -startTime;
                Intent intent = new Intent(getActivity(), Ending.class);
                Float percent = (float) (emergencyBrakeTimes / brakeTimes);
                Bundle bundle = new Bundle();
                bundle.putFloat("percentage", percent);
                bundle.putFloat("left", turnLeftTimes);
                bundle.putFloat("right", turnRightTimes);
                bundle.putFloat("back", turnBackTimes);
                bundle.putFloat("ltor", ltorTimes);
                bundle.putFloat("rtol", rtolTimes);
                bundle.putFloat("brake", brakeTimes);
                bundle.putFloat("eBrake", emergencyBrakeTimes);
                bundle.putString("mode", mode);
                bundle.putLong("driveTime", driveTime);

                intent.putExtras(bundle);
                startActivity(intent);
                a.cancel();
                b.cancel();
                getActivity().finish();
            }
        });

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
           return null;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.2f, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                update(location);
                speedFormer = speedLatter;
                speedLatter = speed;

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                update(lm.getLastKnownLocation(provider));
            }


            @Override
            public void onProviderDisabled(String provider) {
                update(null);
            }

        });

        if (loc == null) {
            loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        update(loc);
        return v;
    }

    public void update(Location loc) {
        if (loc != null) {
            speed = (float) loc.getSpeed();//单位：m/s

        }
    }

    public void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            getActivity().finish();
            return;
        }

        // instantiate BLE advertising helper objects
        if (mBluetoothLeAdvertiser != null) {
            mBleAdvertiseSettingsBuilder = new AdvertiseSettings.Builder()
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                            //.setTimeout(DEFAULT_ADVERTISE_TIMEOUT)
                    .setConnectable(false);
            mBleAdvertiseDataBuilder = new AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .setIncludeTxPowerLevel(true);
        }

        // instantiate BLE scanner helper objects
        if (mBluetoothLeScanner != null) {
            mBleScanSettingsBuilder = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        }
        sm.registerListener(this,sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        DecimalFormat df = new DecimalFormat("#,##0.000");
        String message = new String();
        float[] values=event.values;
        double save = 0;
        int type=event.sensor.getType();
        switch(type){
            case Sensor.TYPE_ACCELEROMETER:
                accelerometerValues = values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticFieldValues = values;
                values[0] = (float)Math.toDegrees(values[0]);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                //z轴线性加速度
                accelerator = values[2];    //z-axis value of accelerometer 加速度计z轴读数

                //滑动滤波
                moveAverageacc.pushValue((float)accelerator);
                averacc=moveAverageacc.getValue();
                float csc=(float) Math.sin(calculateOrientation());
                if(csc==0){
                    csc=(float) 0.1;
                }
                averalinear = averacc/csc;//acceleration in horizontal 水平方向的加速度
                if(Math.abs(averalinear)>=10){
                    averalinear=save;
                }else{
                    save=averalinear;
                }

                DecimalFormat df1 = new DecimalFormat("#.00");

                accelerator1 = Double.valueOf(df1.format(accelerator));
                averacc1 = Double.valueOf(df1.format(averacc));
                hpaveracc1 = Double.valueOf(df1.format(hpaveracc));
                averalinear1 = Double.valueOf(df1.format(averalinear));
                break;
            case Sensor.TYPE_GYROSCOPE:
                v=(float) Math.sqrt(values[1]*values[1]+values[2]*values[2]);
                if((values[1]+values[2])<0){
                    v=0-v;
                }

                DecimalFormat df2 = new DecimalFormat("#.00");
                v1 = Float.valueOf(df2.format(v));

                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                float X = values[0];
                float Y = values[1];
                float Z = values[2];

                message += df.format(X) + "  ";
                message += df.format(Y) + "  ";
                message += df.format(Z) +  "\n";


                break;
        }
        }



    public double calculateOrientation() {
        float R[] = new float[9];
        double averori;
        double ori1;
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(R, orientation);
        moveAveragedir.pushValue(orientation[1]);
        averori = moveAveragedir.getValue();
        DecimalFormat df = new DecimalFormat("#.00");
        ori1 = Double.valueOf(df.format(averori));
        return ori1;
//		tv4.setText("the orientation now is: "+orientation[0]);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }


    public float algorithmKalmanFilter() {
        float x_10;
        float p_10;
        float K;
        float x_11 = 0;
        float p_11;
        float estimatedVelocity = 0;
        x_10 = (float) (dataset.getX_00() + 0.05 * averalinear1);
        p_10 = dataset.getP_00() + dataset.getQ();
        K = p_10 / (p_10 + dataset.getR());
        x_11 = x_10 + K * (speed - x_10);
        dataset.setX_00(x_11);
        p_11 = p_10 - K * p_10;
        dataset.setP_00(p_11);
        //dataset.estimatedVelocity = x_11;
        DecimalFormat df = new DecimalFormat("#.00");
        estimatedVelocity = Float.valueOf(df.format(x_11));
        dataset.kAll = K;
        //Log.v("x11",""+x_11);
        return estimatedVelocity;
    }

    private void startAdvertising() {
        Log.d("startad", "startAdvertising");
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(getActivity(), R.string.ble_peripheral_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        /*数字的话17位刚刚不会报错
        * 构建驾驶行为信息广播包
				*/
        serviceData = VEHICLES.getBytes();


        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
        final AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(SAMPLE_UUID)
                .addServiceData(SAMPLE_UUID, serviceData)
                .build();

        Log.d(TAG, "Starting advertising with settings:" + advertiseSettings + " and data:" + advertiseData);

        // the default settings already put a time limit of 10 seconds, so there's no need to schedule
        // a task to stop it
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mBleAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            Log.d(TAG, "Stop advertising.");
            mBluetoothLeAdvertiser.stopAdvertising(mBleAdvertiseCallback);
        }
    }

    private final ScanCallback mBleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.w("batch", "more devices");
            for (ScanResult result : results) {
                processScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String description;
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    description = "SCAN_FAILED_ALREADY_STARTED";
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    description = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    description = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    description = "SCAN_FAILED_INTERNAL_ERROR";
                    break;
                default:
                    description = "Unknown error code " + errorCode;
                    break;
            }
            Log.e(TAG, "onScanFailed: " + description);
        }
    };

    private void startScanning() {
        assert (mBluetoothLeScanner != null);
        assert (mBleScanSettingsBuilder != null);

        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBleScanSettingsBuilder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

        // add a filter to only scan for advertisers with the given service UUID
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(SAMPLE_UUID).build()
        );

        ScanSettings bleScanSettings = mBleScanSettingsBuilder.build();

        Log.d(TAG, "Starting scanning with settings:" + bleScanSettings + " and filters:" + bleScanFilters);

        // tell the BLE controller to initiate scan
        mBluetoothLeScanner.startScan(bleScanFilters, bleScanSettings, mBleScanCallback);

    }

    private void stopScanning() {
        if (mBluetoothLeScanner != null) {
            Log.d(TAG, "Stop scanning.");
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            Toast.makeText(getActivity(), "扫描已关闭", Toast.LENGTH_SHORT).show();
        }
    }

    private void processScanResult(ScanResult result) {
        Log.d(TAG, "processScanResult: " + result);

        BluetoothDevice device = result.getDevice();

        Log.d(TAG, "Device name: " + device.getName());
        Log.d(TAG, "Device address: " + device.getAddress());
        Log.d(TAG, "Device service UUIDs: " + device.getUuids());

        ScanRecord record = result.getScanRecord();
        String gpsInfo = new String(record.getServiceData(SAMPLE_UUID), Charset.forName("UTF-8"));
        String otherInfo = new String() ;
        switch (gpsInfo){
            case "1" :
                otherInfo = "附近有车辆左转";
                tv_alert.setText(otherInfo);
                break;
            case "2" :
                otherInfo = "附近有车辆右转";
                tv_alert.setText(otherInfo);
                break;
            case "3" :
                otherInfo = "附近有车辆掉头";
                tv_alert.setText(otherInfo);
                break;
            case "4" :
                otherInfo = "附近有车辆向左变道";
                tv_alert.setText(otherInfo);
                break;
            case "5" :
                otherInfo = "附近有车辆向右变道";
                tv_alert.setText(otherInfo);
                break;
            case "6" :
                otherInfo = "附近有车辆刹车";
                tv_alert.setText(otherInfo);
                break;
            case "7" :
                otherInfo = "附近有车辆急刹车";
                tv_alert.setText(otherInfo);
                break;
        }


        Log.d(TAG, "Record advertise flags: 0x" + Integer.toHexString(record.getAdvertiseFlags()));
        Log.d(TAG, "Record Tx power level: " + record.getTxPowerLevel());
        Log.d(TAG, "Record device name: " + record.getDeviceName());
        Log.d(TAG, "Record service UUIDs: " + record.getServiceUuids());
        Log.d(TAG, "Record service data: " + record.getServiceData());

    }

    private final AdvertiseCallback mBleAdvertiseCallback = new AdvertiseCallback() {

        private static final String TAG = "SampleBLE.AdvertiseCallback";

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "onStartSuccess: " + settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            String description;
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    description = "ADVERTISE_FAILED_ALREADY_STARTED";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    description = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    description = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    description = "ADVERTISE_FAILED_INTERNAL_ERROR";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    description = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                    break;
                default:
                    description = "Unknown error code " + errorCode;
                    break;
            }
            Log.e(TAG, "onStartFailure: " + description);
        }
    };



    class UpdateTask extends TimerTask {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(0x123);
        }
    }

    public void startTimer() {
        Log.d("started", "adStarting");
        if (isActive) {
            timer = new Timer();
            timerTask = new UpdateTask();
            if ((timer != null) && (timerTask != null)) {
                timer.schedule(timerTask, 0, DEFAULT_ADVERTISE_INTERVAL);

            }
        } else {
            Toast.makeText(getActivity(), "广播已开启", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTimer() {
        if ((timer != null) && (timerTask != null)) {
            timer.cancel();
            timerTask.cancel();
//            timer = null;
//            timerTask = null;
            isActive = true;
            Toast.makeText(getActivity(), "广播已关闭", Toast.LENGTH_SHORT).show();
        }
    }
}