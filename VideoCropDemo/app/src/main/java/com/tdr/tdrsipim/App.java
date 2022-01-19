package com.tdr.tdrsipim;

import android.app.Application;

import org.ffmpeg.android.FfmpegController;

import java.io.File;
import java.io.IOException;

/**
 * Created by milanac007 on 2017/10/22.
 */
public class App extends Application {

    private static App insance;
    private FfmpegController fc;

    public static App getInstance(){
        return insance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        insance = this;
        initFFmpeg();
    }

    public FfmpegController getFc() {
        return fc;
    }

    private void initFFmpeg() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File fileAppRoot = new File(getApplicationInfo().dataDir);
                try {
                    fc = new FfmpegController(getApplicationContext(), fileAppRoot);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
