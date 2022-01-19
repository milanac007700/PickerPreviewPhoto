package com.example.milanac007.pickerandpreviewphoto;

import android.app.Application;
import android.content.Context;

import com.tdr.tdrsipim.App;

import org.ffmpeg.android.FfmpegController;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by zqguo on 2016/12/8.
 */
public class MyApplication extends Application {

    private static Context mContext = null;
    private App mApp; //VideoCrop的Application

    public static void setContext(Context context){
        mContext = context;
    }

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        if(mApp != null) {
            mApp.onCreate(); //用于执行module的一些自定义初始化操作
        }

        initCache();
    }

    private void  initCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CacheManager.getInstance().initCache();

            }
        }).start();
    }

    /**
     * Application本身是没有getApplicationContext()和getBaseContext()这两个方法的，这两个方法其实在Application
     * 的父类ContextWrapper中，其中context是在attachBaseContext(Context base)中赋值的，所以我们重写attachBaseContext的时候，
     * 一定要记得调一遍super.attachBaseContext(base)传入当前context。
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        mApp = getModuleApplicationInstance(this);
        try {
            //通过反射调用moduleApplication的attach方法
            Method method = Application.class.getDeclaredMethod("attach", Context.class);
            if(method != null) {
                method.setAccessible(true);
                method.invoke(mApp, getBaseContext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private App getModuleApplicationInstance(Context context) {
        try {
            if(mApp == null) {
                ClassLoader classLoader = context.getClassLoader();
                if(classLoader != null) {
                    Class<?> aClass = classLoader.loadClass(App.class.getName());
                    if(aClass != null) {
                        mApp = (App)aClass.newInstance();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mApp;
    }


}
