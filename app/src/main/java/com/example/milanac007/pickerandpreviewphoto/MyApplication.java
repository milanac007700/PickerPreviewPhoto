package com.example.milanac007.pickerandpreviewphoto;

import android.app.Application;
import android.content.Context;

/**
 * Created by zqguo on 2016/12/8.
 */
public class MyApplication extends Application {

    private static Context mContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        new Thread(new Runnable() {
            @Override
            public void run() {
                CacheManager.getInstance().initCache();
            }
        }).start();
    }

    public static void setContext(Context context){
        mContext = context;
    }

    public static Context getContext() {
        return mContext;
    }
}
