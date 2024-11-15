package com.milanac007.demo.videocropdemo.library;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HandlerProxy extends Handler {
    private static final String TAG = "HandlerProxy";

    private Handler mHandler;

    public HandlerProxy(Handler handler) {
        this.mHandler = handler;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            mHandler.handleMessage(msg);
        }catch (Throwable throwable) {
            Log.i(TAG, "toast error: " + throwable.getMessage());
        }
    }
}
