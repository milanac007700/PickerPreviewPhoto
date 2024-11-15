package com.milanac007.demo.videocropdemo.library;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by zqguo on 2016/9/14.
 */
public abstract class HandlerPost implements Runnable {

    private Handler mHandler = null;

    public HandlerPost(long time){
        super();
        post(time);
    }

    public HandlerPost(long time, boolean isUIThread){
        super();
        if(isUIThread)
            postUIThread(time);
        else
            post(time);
    }

    private void post(long time){
        if(mHandler == null){
            mHandler = new Handler(Looper.myLooper());
        }
        mHandler.postDelayed(this, time);
    }

    private void postUIThread(long time){
        if(mHandler == null){
            mHandler = new Handler(Looper.getMainLooper());
        }
        mHandler.postDelayed(this, time);
    }

    public abstract void doAction();
    @Override
    public void run() {
        doAction();
    }

    public void cancel(){
        if(mHandler != null){
            mHandler.removeCallbacks(this);
            mHandler = null;
        }
    }
}
