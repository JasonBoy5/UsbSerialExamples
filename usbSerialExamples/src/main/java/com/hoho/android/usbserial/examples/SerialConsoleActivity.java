/* Copyright 2011 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: http://code.google.com/p/usb-serial-for-android/
 */

package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Monitors a single {@link UsbSerialDriver} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity implements View.OnClickListener {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialDriver)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialDriver sDriver = null;

    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;
    //    private String message =  "没有接收到数据\n"
    private static final int BUFSIZ = 4096;
    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);
    private StringBuffer Msg = new StringBuffer();
    private static long CurrentTime;

    private int FlagUpdateList = 0;//更新列表标志位
    private int FlagLink = 0;//连接巡更机标志位
    private int Flag = 0;//巡更机应答消息类型标志位
    private int FlagReadRecord = 0;//读取巡更记录条目标志位
    private int FlagRecordSum = 0;//巡更记录总数

    private byte[] RecordNUM = new byte[2];
    //巡更机应答消息类型
    private static final int TestLED = 1;
    private static final int TestBUZZER = 2;
    private static final int ShowTIME = 3;
    private static final int ReadMachineID = 4;
    private static final int ReadMachineType = 5;
    private static final int ReadCompanyName = 6;
    private static final int ReadCompanyLOG = 7;
    private static final int ReadMachineVersion = 8;
    private static final int ReadRecordNUM = 9;
    private static final int ReadRecordNumIfo = 10;
    private static final int ReadRecordClick = 11;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
//            List<byte[]> list = new ArrayList<>();
//            list.add(data);
            Observable.just(data)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<byte[]>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(byte[] bytes) {
                            dealData(bytes);
                        }
                    });



//            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.w(TAG,Arrays.toString(data));
//                    SerialConsoleActivity.this.updateReceivedData(data);
//                }
//            });
        }
    };

    private static String GetGbkChar(byte[] codes){
        if( (byte)0xAA==codes[0]){
            int offset = 0xB0 - (int)'0';
            char enChar = (char)(codes[1]&0xFF - offset);//在Java中，codes[1]有符号位，codes[1]&0xFF将有符号位改为无符号位
            return new String(new char[]{enChar});
        }
        else {
            try {
                return new String(codes,"GBK");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }
    }//获得单个GBK编码字符（4个字节转换成一个中文或英文）
    private static String Byte2Gbk(byte[] codes){
        if(null == codes || 0 == codes.length || 0 != codes.length % 2){
            return null;
        }
        int retLength = codes.length / 2;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < retLength; i++){
            byte b1 = codes[i * 2];
            byte b2 = codes[i * 2 + 1];
            if ((byte)0xFF == b1 && (byte) 0XFF == b2){
                break;
            }
            String str = GetGbkChar(new byte[]{b1,b2});
            sb.append(str);
        }
        return sb.toString();
    }//字节数组转为GBK编码
    private void TestLED(byte bytes){
        if(0X00 == bytes){
            String temp = "LED正常\n";
            Msg.append(temp);
        }else {
            String temp = "LED异常\n";
            Msg.append(temp);
        }
    }//测试LED灯
    private void TestBuzzer(byte bytes){
        if(0X00 == bytes){
            String temp = "蜂鸣器正常\n";
            Msg.append(temp);
        }else {
            String temp = "蜂鸣器异常\n";
            Msg.append(temp);
        }
    }//测试蜂鸣器
    private void ShowTime(byte[] bytes){
        if(0X07 == bytes[2]){
            byte[] tmp = new byte[6];
            System.arraycopy(bytes,3,tmp,0,6);
            Calendar c = Calendar.getInstance();//获取一个日历实例
            c.set((int)tmp[0],(int)tmp[1]-1,(int)tmp[2],(int)tmp[3],(int)tmp[4],(int)tmp[5]);//设定日历的日期
            Date date =c.getTime();
            String temp = "时间：" + date + "\n";
            Msg.append(temp);
        }
    }//显示时间
    private void ReadMachineID(byte[] bytes){
        byte[] temp = new byte[5];
        System.arraycopy(bytes,3,temp,0,temp.length);
        String string = "巡更机机号：" + bytesToHexString(temp) + "\n";
        Msg.append(string);
    }//读取巡更机机号
    private void ReadMachineType(byte[] bytes){
        byte[] temp = new byte[16];
        System.arraycopy(bytes,3,temp,0,temp.length);
        Msg.append("型号：");
        Msg.append(Byte2Gbk(temp));
        Msg.append("\n");
    }//读取巡更机型号
    private void ReadMachineVersion(byte[] bytes){
        byte[] temp = new byte[8];
        System.arraycopy(bytes,3,temp,0,temp.length);
        Msg.append("版本号：");
        Msg.append(Byte2Gbk(temp));
        Msg.append("\n");
    }//读取巡更机版本号
    private void ReadCompanyName(byte[] bytes){
        byte[] temp = new byte[32];
        System.arraycopy(bytes,3,temp,0,temp.length);
        Msg.append(Byte2Gbk(temp));
        Msg.append("\n");
    }
    private void ReadRecordNUM(byte[] bytes){
        System.arraycopy(bytes,3,RecordNUM,0,RecordNUM.length);
        FlagRecordSum = (int)RecordNUM[1]&0XFF + 256 * (int)RecordNUM[0]&0XFF;
        String str = "巡更记录总数：" + FlagRecordSum + "\n";
        Msg.append(str);
    }//读巡更记录数
    private void ReadRecordNumIfo(byte[] bytes){
        String str1 = "第" + FlagReadRecord + "条记录：\n";
        byte[] tmp = new byte[6];
        System.arraycopy(bytes,3,tmp,0,6);
        Calendar c = Calendar.getInstance();//获取一个日历实例
        c.set((int)tmp[0],(int)tmp[1]-1,(int)tmp[2],(int)tmp[3],(int)tmp[4],(int)tmp[5]);//设定日历的日期
        Date date =c.getTime();
        String str2 = "时间：" + date + "\n";
        byte[] num = new byte[5];
        System.arraycopy(bytes,9,num,0,num.length);
        String str3 = "卡号：" + bytesToHexString(num) + "\n";
        Msg.append(str1);
        Msg.append(str2);
        Msg.append(str3);
    }//读每条巡更记录内容
    private void ReadRecordClick(byte[] bytes){
        byte[] tmp = new byte[2];
        System.arraycopy(bytes,3,tmp,0,tmp.length);
        int i = (int)tmp[1]&0XFF + 256 * (int)tmp[0]&0XFF;
        String str = "撞击记录数：" + i + "\n";
        Msg.append(str);
    }

    private void dealData(byte[] bytes){
        switch (bytes[0]){
            case (byte) 0XAA:
                FlagLink = 1;
                break;

            case (byte) 0XEB:
                switch (bytes[1]){
                    case (byte) 0X81:
                        Flag = TestLED;
                        TestLED(bytes[3]);
                        break;

                    case (byte) 0X82:
                        Flag = TestBUZZER;
                        TestBuzzer(bytes[3]);
                        break;

                    case (byte) 0X32:
                        Flag = ShowTIME;
                        FlagUpdateList = 1;
                        ShowTime(bytes);
                        break;

                    case (byte) 0X34:
                        Flag = ReadMachineID;
                        ReadMachineID(bytes);
                        break;

                    case (byte) 0X61:
                        Flag = ReadMachineType;
                        ReadMachineType(bytes);
                        break;

                    case (byte) 0X6B:
                        Flag = ReadMachineVersion;
                        ReadMachineVersion(bytes);
                        break;

                    case (byte) 0X92:
                        Flag = ReadCompanyName;
                        ReadCompanyName(bytes);
                        break;

                    case (byte) 0X95:
                        Flag = ReadCompanyLOG;
                        ReadCompanyName(bytes);
                        break;

                    case (byte) 0X36:
                        Flag = ReadRecordNUM;
                        ReadRecordNUM(bytes);
                        break;

                    case (byte) 0X38:
                        Flag = ReadRecordNumIfo;
                        FlagReadRecord++;
                        if(FlagRecordSum == FlagReadRecord){
                            FlagUpdateList = 1;
                        }
                        ReadRecordNumIfo(bytes);
                        break;

                    case (byte) 0x3D:
                        Flag = ReadRecordClick;
                        FlagUpdateList = 1;
                        ReadRecordClick(bytes);

                    default:
                        break;
                }
//                String temp = bytesToHexString(bytes);
//                Msg.append(temp);
//                Msg.append("\n");
//                Msg.append(temp.length());
//                Msg.append("\n");
                break;

            default:
                break;
        }
//        if(0X32 == bytes[1]){
        if(1 == FlagUpdateList){
            updateList();
        }
    }

    private void updateList(){
        mDumpTextView.append(Msg);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
        Msg.delete(0,Msg.length());
        FlagRecordSum = 0;
        FlagReadRecord = 0;
        FlagUpdateList = 0;
        Flag = 0;
//        FlagLink = 0;
    }//更新列表

//    public void updateListUi(){
//        SerialConsoleActivity.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                SerialConsoleActivity.this.updateList();
//            }
//        });
//    }
//
//    public void upList(final byte[] data){
//        SerialConsoleActivity.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Log.w(TAG,Arrays.toString(data));
//                SerialConsoleActivity.this.updateReceivedData(data);
//            }
//        });
//    }

//    /////////////////////////
//    private Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            mPbTest.setProgress(msg.arg1);
//        }
//    };
    ////////////////////////

    private void MyTimer(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask (){
            public void run() {
                try {
                    Send2Machine((byte)0xAA,null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule (task, 0L, 2000L);
    }//定时器
    public void Send2Machine(final byte cmd, final byte[] data) throws IOException {
        new Thread(){
            @Override
            public void run() {
                super.run();
                int n = 0;
                if(null != data){
                    n = data.length;
                }
                byte[] Sum = new byte[n+4];

                Sum[0] = (byte) 0XEB;//发送帧头

                Sum[1] = cmd;//命令

                int num;
                if(null == data){//数据长度
                    num =0;
                    Sum[2] = 0X00;
                }else{
                    num = data.length;
                    Sum[2] = (byte) num;
                }
                if(0 != num){
                    System.arraycopy(data,0,Sum,3,num);//数据
                }

                byte CheckSum = 0X00;//帧校验和
                for(int i = 0; i < num + 3; i++){
                    CheckSum += Sum[i];
                }

                Sum[3+num] = CheckSum;
                try {
                    sDriver.write(Sum,0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                try {
//                    sDriver.write(new byte[]{(byte) 0XEB},0);//发送帧头
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    sDriver.write(cmd,0);//命令
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                int num;
//                if(data == null){
//                    num =0;
//                }else{
//                    num = data.length;
//                }
//
//                try {
//                    sDriver.write(new byte[]{(byte) num},0);//数据长度
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                byte CheckSum = 0X00;
//                if(num != 0){
//                    for(int i = 0; i < data.length; i++){
//                        try {
//                            sDriver.write(new byte[]{data[i]},0);//数据
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        CheckSum += data[i];
//                    }
//                }
//
//                try {
//                    sDriver.write(new byte[]{CheckSum},0);//帧校验和
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }.start();
    }
    private void Connect2Machine (){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Send2Machine((byte) 0XAA,null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void btn_look(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Connect2Machine();
                    while (true){
                        if(1 == FlagLink){
                            break;
                        }
                        else{
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X35,new byte[]{0X08,0X08});
                    while (true){
                        if(ReadRecordNUM == Flag){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    int Sum = (int)RecordNUM[1]&0xFF + 256 * (int) RecordNUM[0]&0xFF;
                    for (int t = 0; t < Sum; t ++){
                        int i = t / 256;
                        int j = t % 256;
                        Send2Machine((byte) 0X37,new byte[]{(byte) i,(byte) j,0X08});
                        while (true){
                            if (t + 1 == FlagReadRecord){
                                break;
                            }
                            else {
                                sleep(1);
                            }
                        }

                    }
//                    Send2Machine((byte) 0X31,null);

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void btn_lookClick(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Connect2Machine();
                    while (true){
                        if(1 == FlagLink){
                            break;
                        }
                        else{
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0x3C,new byte[]{0x0D,0x0D});
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void btn_tryClick(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Connect2Machine();
                    while (true){
                        if(1 == FlagLink){
                            break;
                        }
                        else{
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X21,null);
                    while (true){
                        if(Flag == TestLED){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X20,null);
                    while (true){
                        if(Flag == TestBUZZER){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X31,null);


                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    private void btn1Click(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Connect2Machine();
                    while (true){
                        if(1 == FlagLink){
                            break;
                        }
                        else{
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X33,new byte[]{0X00,0X04});
                    while (true){
                        if(ReadMachineID == Flag){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X51,new byte[]{0X05,0X01});
                    while (true){
                        if(ReadMachineType == Flag){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X5B,new byte[]{0X0A ,0X0B});
                    while (true){
                        if(ReadMachineVersion == Flag){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X12,new byte[]{0X01,0X02});
                    while (true){
                        if(ReadCompanyName == Flag){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X15,new byte[]{0X01,0X05});
                    while (true){
                        if(ReadCompanyLOG == Flag){
                            break;
                        }
                        else {
                            sleep(1);
                        }
                    }
                    Send2Machine((byte) 0X31,null);
                }
                catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_send:
                btn_look();
//                updateList123();
                break;

            case R.id.btn_try:
                btn_tryClick();
//                updateList123();
                break;

            case R.id.btn1:
                btn1Click();
                break;

            case R.id.btn2:
                btn_lookClick();
                break;
            case R.id.btn3:
                break;

            default:
                break;

        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        Button mButtonSend = (Button) findViewById(R.id.btn_send);
        Button mButtonTry = (Button) findViewById(R.id.btn_try);
        Button mBtnMachineIfo = (Button) findViewById(R.id.btn1);
        Button mBtnRecordIfo = (Button) findViewById(R.id.btn2);

        mButtonSend.setOnClickListener(this);
        mButtonTry.setOnClickListener(this);
        mBtnMachineIfo.setOnClickListener(this);
        mBtnRecordIfo.setOnClickListener(this);

//////////////////////////////////////////

        /////////////////////////////////
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sDriver != null) {
            try {
                sDriver.close();
            } catch (IOException e) {
                // Ignore.
            }
            sDriver = null;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, sDriver=" + sDriver);
        if (sDriver == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            try {
                sDriver.open();
                sDriver.setParameters(115200, 8, UsbSerialDriver.STOPBITS_1, UsbSerialDriver.PARITY_NONE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sDriver.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sDriver = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sDriver.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sDriver != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sDriver, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
///////////////////////////////////
public static int byte2int(byte[] bRefArr) {
    int iOutcome = 0;
    byte bLoop;

    for (int i = bRefArr.length - 1; i >= 0; i--) {
        bLoop = bRefArr[i];
        iOutcome += (bLoop & 0xFF) >> (8 * i);
    }
    return iOutcome;
}
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]).toUpperCase();
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
///////////////////////////////////////////////////////////
    private void updateReceivedData(byte[] data) {
        mDumpTextView.append(bytesToHexString(data));
        mDumpTextView.append("\n");
//        final String message = "Read " + data.length + " bytes: \n"
//                + HexDump.dumpHexString(data) + "\n\n";
//        String message = "Read " + data.length + " bytes: \n"
//                + bytesToHexString(data) + "\n\n";
        String message = "没有接收到数据\n";
        switch (data[0]){
            case (byte) 0XAA:
                message = "成功连接巡更机，2s内没有任何操作将断开连接！\n";
                break;
            case (byte) 0XEB:
                switch (data[1]){
                    case (byte) 0X81:
                        if(0X00 == data[3]){
                            message = "LED正常\n";
                        }else {
                            message = "LED异常\n";
                        }
                        break;
                    case (byte) 0X82:
                        if(0X00 == data[3]){
                            message = "蜂鸣器正常\n";
                        }else {
                            message = "蜂鸣器异常\n";
                        }
                        break;
                    case (byte) 0X32:
                        if(0X07 == data[2]){
                            byte[] tmp = new byte[6];
                            System.arraycopy(data,3,tmp,0,6);
                            String time = bytesToHexString(tmp);

                            Calendar c = Calendar.getInstance();//获取一个日历实例
                            c.set((int)tmp[0],(int)tmp[1]-1,(int)tmp[2],(int)tmp[3],(int)tmp[4],(int)tmp[5]);//设定日历的日期
                            Date date =c.getTime();
//                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            String formatTime = formatter.format(tmp);
                            message = "时间：" + date + "\n";
                        }
                        break;
                    case (byte) 0X34:
                        if(0X05 == data[2]){
                            byte[] tmp = new byte[5];
                            System.arraycopy(data,3,tmp,0,5);
                            String num = bytesToHexString(tmp);
                            message = "机号：" + num + "\n";
                        }
                        break;
                    case (byte) 0X36:
                        if(0X02 == data[2]){
                            byte[] tmp = new byte[2];
                            System.arraycopy(data,3,tmp,0,2);
                            String num = bytesToHexString(tmp);
                            message = "巡更记录数：" + num + "\n";
                        }
                        break;
                }
                break;
        }
//        String message = bytesToHexString(data) + "\n";

        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

//    private void updatelist(){
////        mDumpTextView.append(message);
//        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
//    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialDriver driver) {
        sDriver = driver;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

}
