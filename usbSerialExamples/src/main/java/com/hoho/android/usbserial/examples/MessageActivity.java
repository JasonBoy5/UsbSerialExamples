package com.hoho.android.usbserial.examples;

import android.content.Context;
import android.os.Handler;

/**
 * Created by Administrator on 2017/8/18.
 */

public class MessageActivity {
    private final Handler mHandler;
    private SendThread mSendThread;
    public MessageActivity(Context context,Handler handler){
        mHandler = handler;
    }

    private class SendThread extends Thread{

    }
}

