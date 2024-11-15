package com.milanac007.demo.videocropdemo;

import android.app.Application;

/**
 * Created by milanac007 on 2017/10/22.
 */
public class App extends Application {

    private static App insance;
    public static App getInstance(){
        return insance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        insance = this;
    }

}
