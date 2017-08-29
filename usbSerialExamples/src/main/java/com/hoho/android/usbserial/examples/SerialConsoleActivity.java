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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
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
    private Button mButtonSend;
    private Button mButtonTry;
//    private String message =  "没有接收到数据\n"
    private static final int BUFSIZ = 4096;
    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);
    private StringBuffer Msg;

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
                            Msg.append(bytesToHexString(bytes));
                            Msg.append("\n");
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

    public void upList(final String data){
        SerialConsoleActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG,data);
                SerialConsoleActivity.this.updateReceivedData(data);
            }
        });
    }

//    /////////////////////////
//    private Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            mPbTest.setProgress(msg.arg1);
//        }
//    };
    ////////////////////////

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

    public void onClick(View view){
        switch (view.getId()){
            case R.id.btn_send:
                try {
//                    sDriver.write(new byte[]{(byte) 0xAA},0);
                    Send2Machine((byte) 0XAA,null);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.btn_try:
                String str = "1";
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        try {
//                            int i;
//                            for(i = 0; i < 3; i++){
////                                mSerialIoManager.clearBuf();
//                                Send2Machine((byte) 0XAA,null);
//                                byte[] by = mSerialIoManager.getData();
//                                if(null != by){
//                                    upList(by);
//                                    mSerialIoManager.clearBuf();
//                                    break;
//                                }
//                            }
//                            for(i = 0; i < 3; i++){
//                                Send2Machine((byte) 0X21,null);
//                                byte[] by = mSerialIoManager.getData();
//                                if(null != by){
//                                    upList(by);
//                                    mSerialIoManager.clearBuf();
//                                    break;
//                                }
//                            }
//                            for(i = 0; i < 3; i++){
//                                Send2Machine((byte) 0X31,null);
//                                byte[] by = mSerialIoManager.getData();
//                                if(null != by){
//                                    upList(by);
//                                    mSerialIoManager.clearBuf();
//                                    break;
//                                }
//                            }

                            Send2Machine((byte) 0X21,null);
                            if(Msg.length() > 5){
                                Send2Machine((byte) 0X31,null);
                            }
                            upList(Msg);

//                            sleep(1);
//                            Send2Machine((byte) 0X33,new byte[]{0X00,0X04});
//                    Send2Machine((byte) 0X35,new byte[]{0X08,0X08});
//                    Send2Machine((byte) 0X70,new byte[]{0X08,0X06,0X00,0X00,0X00,0X00,0X00,0X00});
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

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
        mButtonSend = (Button)findViewById(R.id.btn_send);
        mButtonTry = (Button)findViewById(R.id.btn_try);

        mButtonSend.setOnClickListener(this);
        mButtonTry.setOnClickListener(this);
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

    private void updatelist(){
//        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
    }

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
