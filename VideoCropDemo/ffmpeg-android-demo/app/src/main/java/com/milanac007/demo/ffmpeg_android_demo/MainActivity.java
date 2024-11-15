package com.milanac007.demo.ffmpeg_android_demo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.jni.FFmpeg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private File videoFile;
    private Activity mContext;
    private ImageView imageView;
    private ImageView iv_preview;
    private View btn_play;
    private ViewGroup fl_video_capture;
    private String dirName;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        TextView tv = findViewById(R.id.sample_text);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance()); // 设置TextView可上下滚动
        tv.setText(FFmpeg.run());

        imageView = findViewById(R.id.imageView);
        fl_video_capture = findViewById(R.id.fl_video_capture);
        iv_preview = findViewById(R.id.iv_preview);
        btn_play = findViewById(R.id.btn_play);
        int result = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }else {
            copyVideoToSDCard();
        }

        findViewById(R.id.btn_2gif).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processVideo2Gif();
            }
        });

        findViewById(R.id.btn_crop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processCropVideo();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            copyVideoToSDCard();
        }
    }

    private void copyVideoToSDCard() {
        dirName = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            dirName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + getPackageName() + File.separator + "files";
            dirName = getExternalFilesDir(null).getAbsolutePath() + File.separator + "files";
        } else {
            dirName = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getPackageName() + File.separator + "files";
        }
        Log.i("FFmpeg", "dirName: " + dirName); //dirName: /storage/emulated/0/Android/data/com.milanac007.demo.ffmpeg_android_demo/files/files
        File dir = new File(dirName);
        if(!dir.exists()) {
            dir.mkdirs();
        }

        String[] fileNames = {"bbb.m4v", "sample.mp4"};
        String[] list = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Arrays.asList(fileNames).contains(name);
            }
        });
        if(list == null || list.length == 0) {
            copyFiles();
        }
    }

    int i = 0;
    public void processVideo2Gif() {

//        Glide.with(mContext)
//                .load(R.mipmap.test)
//                .into(imageView);
//        if (true) return;

        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("处理中,请稍候...");
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                videoFile = new File(dirName, "bbb.m4v");
               int index = videoFile.getPath().lastIndexOf(".");
//                String outputFilePath = videoFile.getPath().substring(0, index) + "_10s.gif";

//                String cmd = String.format("ffmpeg -y -i %s -ss 0.0 -t 60.0 %s", videoFile.getPath(), outputFilePath); //将视频的第0s～60s转换gif
//                String cmd = String.format("ffmpeg -threads 4 -y -i %s -ss 0.0 -t 10.0 -r 25 %s", videoFile.getPath(), outputFilePath); //截取0-5s，帧率25
//                String cmd = String.format("ffmpeg -d -threads 4 -y -i %s -ss 0.0 -t 10.0 -r 25 %s", videoFile.getPath(), outputFilePath); //-d: debug，输出日志. -y:自动覆盖

                String outputFilePath, cmd;
                if (i++ % 2 == 1) {
                    outputFilePath = videoFile.getPath().substring(0, index) + "_0_5s.gif";
                    cmd = String.format("ffmpeg -d -y -i %s -ss 0.0 -t 5.0 -r 25 %s", videoFile.getPath(), outputFilePath); //-d: debug，输出日志. -y:自动覆盖
                } else {
                    outputFilePath = videoFile.getPath().substring(0, index) + "_30_5s.gif";
                    cmd = String.format("ffmpeg -d -y -i %s -ss 30.0 -t 5.0 -r 25 %s", videoFile.getPath(), outputFilePath); //-d: debug，输出日志. -y:自动覆盖
                }

                final String[] cmds = cmd.split("[ \\t]+"); //空格或Tab
                FFmpeg.runCmd(cmds);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Glide.with(mContext)
                                .load(new File(outputFilePath))
                                .into(imageView);

                    }
                });

            }
        }).start();


//        int index = videoFile.getPath().lastIndexOf(".");
//        String outputFilePath = videoFile.getPath().substring(0, index) + "_10s.gif";
//        String cmd = String.format("ffmpeg -d -threads 4 -y -i %s -ss 0.0 -t 10.0 -r 25 %s", videoFile.getPath(), outputFilePath); //-d: debug，输出日志. -y:自动覆盖
//        final String[] cmds = cmd.split("[ \\t]+"); //空格或Tab
//        FFmpeg.exec(cmds, new FFmpeg.OnCmdExecListener() {
//            @Override
//            public void onSuccess() {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (progressDialog != null && progressDialog.isShowing()) {
//                            progressDialog.dismiss();
//                        }
//
//                        Glide.with(mContext)
//                                .load(new File(outputFilePath))
//                                .into(imageView);
//
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure() {
//
//            }
//
//            @Override
//            public void onProgress(float progress) {
//
//            }
//        });

    }

    public void processCropVideo() {
        final ProgressDialog progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("处理中,请稍候...");
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {

                //将视频的第0.1秒装换成png，类似获取视频封面。
//                String cmd = "ffmpeg -i /storage/emulated/0/DCIM/Camera/VID_20211205_103838.mp4 -t 0.1 /storage/emulated/0/DCIM/Camera/VID_20211205_1038381111.png";

//                String cmd = String.format("ffmpeg -threads 4 -y -i %s -strict -2 -s 480x360 -r 25 %s", videoFile.getPath(), outputFilePath);//分辨率480x360，帧率25

//                String videoName = "202241611131_temp.mp4"; //"device-2023-01-08-115615.mp4";
//                videoFile = new File(dirName, videoName);
////
//                int index = videoFile.getPath().lastIndexOf(".");
//                String outputFilePath = videoFile.getPath().substring(0, index) + "_new.mp4"; //".output.avi";
//                File outputVideoFile = new File(outputFilePath);
//
//                String cmd = String.format("ffmpeg -threads 4 -y -i %s -strict -2 -metadata:s:v rotate=\"0\" -vf transpose=1,crop=480:480:0:0 -s 480x480 -r 25 %s", videoFile.getPath(), outputVideoFile.getPath());
////                String cmd = String.format("ffmpeg -threads 4 -y -i %s -strict -2 -vf crop=480:480:0:0 -preset ultrafast -tune zerolatency -s 480x480 -r 25 -vcodec libx264 -acodec copy %s", videoFile.getPath(), outputVideoFile.getPath());

                videoFile = new File(dirName, "sample.mp4");
                int index = videoFile.getPath().lastIndexOf(".");
                String outputFilePath = videoFile.getPath().substring(0, index) + "_new.mp4";
                String cmd = String.format("ffmpeg -threads 4 -y -i %s -strict -2 -metadata:s:v rotate=\"0\" -vf transpose=1,crop=480:480:0:0 -s 480x480 -r 25 %s", videoFile.getPath(), outputFilePath);
//                String cmd = String.format("ffmpeg -threads 4 -y -i %s -strict -2 -vf crop=480:480:0:0 -preset ultrafast -tune zerolatency -s 480x480 -r 25 -vcodec libx264 -acodec copy %s", videoFile.getPath(), outputVideoFile.getPath());

                final String[] cmds = cmd.split("[ \\t]+"); //空格或Tab
                FFmpeg.runCmd(cmds);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Toast.makeText(MainActivity.this, "剪切成功", Toast.LENGTH_SHORT).show();
                        ViewGroup.LayoutParams lp0 = iv_preview.getLayoutParams();
                        lp0.width = 480;
                        lp0.height = 480;
                        iv_preview.setLayoutParams(lp0);
                        btn_play.setVisibility(View.VISIBLE);
                        btn_play.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                playVideo(MainActivity.this, outputFilePath);
                            }
                        });

                    }
                });

            }
        }).start();
    }

    private void copyFiles() {
        videoFile = new File(dirName, "bbb.m4v");
        try (InputStream in = getResources().openRawResource(R.raw.bbb);
             OutputStream out = new FileOutputStream(videoFile)) {
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        videoFile = new File(dirName, "sample.mp4");
        try (InputStream in = getResources().openRawResource(R.raw.sample);
             OutputStream out = new FileOutputStream(videoFile)) {
            byte[] buf = new byte[1024];
            int len;
            while((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playVideo(Context context, final String videoPath) {
        final VideoView videoView = new VideoView(context);
        ViewGroup.LayoutParams lp = iv_preview.getLayoutParams();
        final int reqWidth = lp.width;
        final int reqHeight = lp.height;
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(reqWidth, reqHeight); //根据屏幕宽度设置预览控件的尺寸，为了解决预览拉伸问题
        videoView.setLayoutParams(layoutParams);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                iv_preview.setVisibility(View.VISIBLE);
                btn_play.setVisibility(View.VISIBLE);
                fl_video_capture.removeView(videoView);
            }
        });

        fl_video_capture.addView(videoView, 0);
        videoView.setVisibility(View.VISIBLE);
        iv_preview.setVisibility(View.GONE);
        btn_play.setVisibility(View.GONE);

//        videoView.setVideoURI(videoUri);
        videoView.setVideoPath(videoPath);
//         videoView.setMediaController(new MediaController(context)); //设置了一个播放控制器。
        videoView.start(); //程序运行时自动开始播放视频。
        videoView.requestFocus(); //播放窗口为当前窗口
    }

}