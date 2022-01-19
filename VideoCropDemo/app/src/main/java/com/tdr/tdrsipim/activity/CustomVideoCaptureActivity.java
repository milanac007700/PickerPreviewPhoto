package com.tdr.tdrsipim.activity;

/**
 * Created by zqguo on 2017/10/23.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.runtime.permission.PermissionUtils;
import com.tdr.tdrsipim.App;
import com.tdr.tdrsipim.library.CommonFunction;
import com.tdr.tdrsipim.library.CustomHorizonalProgressBar;
import com.tdr.tdrsipim.library.CustomVideoCaptureView;
import com.tdr.tdrsipim.library.HandlerPost;
import com.tdr.tdrsipim.library.VideoCaptureButton;
import com.tdr.tdrsipim.library.VoiceSendButton;
//import com.yixia.videoeditor.adapter.UtilityAdapter;


import org.ffmpeg.android.FfmpegController;
import org.ffmpeg.android.ShellUtils;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import test.milanac007.com.videocropdemo.R;


/**
 * Created by milanac007 on 2016/10/19.
 */
public class CustomVideoCaptureActivity extends Activity implements View.OnClickListener, CustomVideoCaptureView.VideoCaptureListener, VoiceSendButton.RecordListener {
    public static final String TAG = CustomVideoCaptureActivity.class.getName();
    public static final int VIDEO_CAPTURE_CODE = 2;

    private CustomVideoCaptureView mVideoCapturePreview;
    private TextView video_capture_time;
    private CustomHorizonalProgressBar mProgressBar;
    private VideoCaptureButton mVideoCaptureBtn;
    private TextView mFinish;
    private CheckBox flashLightSwitch;
    private TextView mBack;
    private View operate_layout;
    private String[] requestPermissions;
    private FfmpegController fc;
    public void setMaxCaptureTime(int maxCaptureTime) {
        if(mVideoCapturePreview != null)
            mVideoCapturePreview.setMaxDefaultSeconds(maxCaptureTime);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        initialize(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.custom_video_capture);

        View fragment_head1 = findViewById(R.id.fragment_head1);
        fragment_head1.setBackgroundColor(getResources().getColor(R.color.color_purple));

        mBack = (TextView)findViewById(R.id.fragment_head1_back);
        Drawable drawable = getResources().getDrawable(R.drawable.record_camera_cancel_style);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        mBack.setCompoundDrawables(drawable, null, null, null);

        mFinish = (TextView)findViewById(R.id.fragment_head1_finish);
        mFinish.setVisibility(View.VISIBLE);
        drawable = getResources().getDrawable(R.drawable.record_camera_switch_selector);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        mFinish.setCompoundDrawables(drawable, null, null, null);

        drawable = getResources().getDrawable(R.drawable.record_camera_flash_led_selector);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());

        LinearLayout right_view_layout = (LinearLayout)findViewById(R.id.right_view_layout);
        flashLightSwitch = new CheckBox(this);
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params1.gravity = Gravity.CENTER_VERTICAL;
        flashLightSwitch.setLayoutParams(params1);
        flashLightSwitch.setPadding(CommonFunction.dip2px(10), 0, CommonFunction.dip2px(10), 0);
        flashLightSwitch.setCompoundDrawables(drawable, null, null, null);
        //flashLightSwitch.setButtonDrawable(null);设置不起作用，因为CheckBox继承自CompoundButton， CompoundButton的setButtonDrawable的代码实现中屏蔽了null或resid为0的Drawable，迫使我们必须传入有效的Drawable对象。这时候，透明颜色就可以派上用场了
        flashLightSwitch.setButtonDrawable(new ColorDrawable(Color.TRANSPARENT));
        flashLightSwitch.setBackgroundDrawable(getResources().getDrawable(R.drawable.font_button_bg));
        right_view_layout.addView(flashLightSwitch, 0);//加入队首
        flashLightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setFlashLight(isChecked);
            }
        });

        video_capture_time = (TextView)findViewById(R.id.video_capture_time);
        mProgressBar = (CustomHorizonalProgressBar)findViewById(R.id.progressBar);
        operate_layout = findViewById(R.id.operate_layout);
        mVideoCaptureBtn = (VideoCaptureButton)findViewById(R.id.video_capture_button);
        mVideoCaptureBtn.setListener(this);
        View[] views = {mBack, mFinish};
        for(View view : views){
            view.setOnClickListener(this);
        }

        setMaxCaptureTime(10 * 1000);
        mProgressBar.setMax(10 * 1000);
        mProgressBar.setRemove(false);

        mVideoCapturePreview = (CustomVideoCaptureView)findViewById(R.id.video_capture_preview);


        //根据屏幕宽度设置预览控件的尺寸，为了解决预览拉伸问题
        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mVideoCapturePreview.getLayoutParams();
        params.width = width;
        params.height = width *4/3;
        mVideoCapturePreview.setLayoutParams(params);

        params  = (RelativeLayout.LayoutParams)operate_layout.getLayoutParams();
        params.topMargin = dm.widthPixels + CommonFunction.dip2px(45);
        operate_layout.setLayoutParams(params);

        fc = App.getInstance().getFc();
        requestPermissions = new String[]{PermissionUtils.PERMISSION_RECORD_AUDIO, PermissionUtils.PERMISSION_CAMERA, PermissionUtils.PERMISSION_READ_EXTERNAL_STORAGE};
        if(PermissionUtils.lacksPermission(this, requestPermissions)){
            PermissionUtils.requestMultiPermissions(this, PermissionUtils.CODE_MULTI_PERMISSIONS, requestPermissions, mPermissionGrantCallback);
        }else {
            initVideoCaptureView();
        }
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

    private void initVideoCaptureView(){
        mVideoCapturePreview.setVideoCaptureListener(this);
        try{
            mVideoCapturePreview.initCamera();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void releaseResource(){
        if(mVideoCapturePreview != null){
            mVideoCapturePreview.releaseResource();
        }
    }

    private PermissionUtils.PermissionGrantCallback mPermissionGrantCallback = new PermissionUtils.PermissionGrantCallback() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode){
                case PermissionUtils.CODE_MULTI_PERMISSIONS:{
                    initVideoCaptureView();
                }break;
            }
        }

        @Override
        public void onPermissionDenied(int requestCode) {
            mBack.performClick();
        }

        @Override
        public void onError() {
            mBack.performClick();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults, mPermissionGrantCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!PermissionUtils.lacksPermission(this, requestPermissions)){
            initVideoCaptureView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseResource();
    }

    @Override
    public void onBackPressed() {
        releaseResource();

        Intent intent=new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
//        super.onBackPressed();
    }

    /**
     * 切换前置/后置摄像头
     */
    public void switchCamera() {

        if(mVideoCapturePreview != null){
            mVideoCapturePreview.switchCamera();
            if(mVideoCapturePreview.getCurrentCameraId() == 0){
                flashLightSwitch.setEnabled(true);
                flashLightSwitch.setChecked(false);
            }else {
                flashLightSwitch.setEnabled(false); //切换到前置摄像头时，重置闪关灯的状态
            }
        }
    }

    /**
     * 打开或关闭闪光灯
     *
     * @param open 控制是否打开
     * @return 打开或关闭失败，则返回false。
     */

    public boolean setFlashLight(boolean open) {
        if(mVideoCapturePreview != null){
            return mVideoCapturePreview.setFlashLight(open);
        }
        return false;
    }
    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if(R.id.fragment_head1_back == viewId) {
            releaseResource();
            Intent intent=new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }else if (R.id.fragment_head1_finish == viewId) {
            switchCamera();
        }
    }

    @Override
    public void onTimeout() {
        mVideoCaptureBtn.timeoutMotionActionUp();
    }

    @Override
    public void onProgress(int count, int total) {

        if(count <= 0){
            video_capture_time.setText("00:00");

        } else {
            if(count < 10*1000)
                video_capture_time.setText("00:0" + count/1000);
            else
                video_capture_time.setText("00:" + count/1000);
        }

        mProgressBar.setProgress(count);
    }

    @Override
    public void onError(String errorMsg) {
        CommonFunction.showToast(errorMsg);
        releaseResource();
        Intent intent=new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    private boolean mIsRecording = false; //最大计时触发ACTION_UP事件后，当手指抬起时ACTION_UP依然有，故判断

    @Override
    public void onStartRecord() {

        mIsRecording = true;
        if(mVideoCapturePreview != null){
            mVideoCapturePreview.startRecord();
        }

        Toast mToast = CommonFunction.getToast(R.string.m_finger_slide_up);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();

        mFinish.setEnabled(false);
        flashLightSwitch.setEnabled(false);
    }

    private ProgressDialog progressDialog;
    public void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(CustomVideoCaptureActivity.this);
        }

        progressDialog.setMessage("处理中，请稍候");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void hideProgressDialog(){

        if(progressDialog != null){
            progressDialog.dismiss();
        }

    }


    /** 应用包名 */
    private static String mPackageName;
    /** 应用版本名称 */
    private static String mAppVersionName;
    /** 应用版本号 */
    private static int mAppVersionCode;
    /** SDK版本号 */
    public final static String VCAMERA_SDK_VERSION = "1.2.0";

    /**
     * 初始化SDK
     *
     * @param context
     */
    public static void initialize(Context context) {
        mPackageName = context.getPackageName();

        mAppVersionName = getVerName(context);
        mAppVersionCode = getVerCode(context);

        try {
            System.loadLibrary("utility");
        }catch (UnsatisfiedLinkError e){
            e.printStackTrace();
        }
        //初始化底层库
//        UtilityAdapter.FFmpegInit(context, String.format("versionName=%s&versionCode=%d&sdkVersion=%s&android=%s&device=%s", mAppVersionName, mAppVersionCode, VCAMERA_SDK_VERSION, getReleaseVersion(), getDeviceModel()));
    }

    /**
     * 获得设备型号
     *
     * @return
     */
    public static String getDeviceModel() {
        return Build.MODEL.trim() == null ? "" : Build.MODEL.trim().trim();
    }
    /**
     * 获得设备的固件版本号
     */
    public static String getReleaseVersion() {
        return TextUtils.isEmpty(Build.VERSION.RELEASE) ? "" : Build.VERSION.RELEASE;
    }

    /**
     * 获取当前应用的版本号
     * @param context
     * @return
     */
    public static int getVerCode(Context context) {
        int verCode = -1;
        try {
            verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return verCode;
    }

    /** 获取当前应用的版本名称 */
    public static String getVerName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return "";
    }

    File VideoFile = null;
    protected void concatVideoParts() {
        final String inputFilePath = mVideoCapturePreview.getmVideoFile().getPath();
        final String outputFilePath = inputFilePath.replace("_temp", "");
        VideoFile = new File(outputFilePath);

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                String cmd = "";
                int cameraId = mVideoCapturePreview.getCurrentCameraId();

                // ffmpeg -threads 4 -y -i /storage/emulated/0/CustomCamera/temp/1476598263062.mp4 -metadata:s:v rotate="0" -vf transpose=1, crop=width:height:x:y -preset ultrafast -tune zerolatency -r 25 -vcodec libx264 -acodec copy /storage/emulated/0/CustomCamera/VIDEO/1476598263062.mp4

                //处理翻转信息
                String vf = cameraId == Camera.CameraInfo.CAMERA_FACING_BACK ? "transpose=1" : "transpose=2,hflip";
                cmd = String.format("ffmpeg -threads 4 -y -i %s -metadata:s:v rotate=\"0\" -vf %s,crop=480:480:0:0 -preset ultrafast -tune zerolatency %s", inputFilePath, vf, outputFilePath);
                boolean mergeFlag = false;
                //boolean mergeFlag = UtilityAdapter.FFmpegRun("", cmd) == 0;
                File file = new File(inputFilePath);
                if(file.exists()){
                    file.delete();
                }
                return mergeFlag;

            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    mEncodeHanlder.sendEmptyMessage(MESSAGE_ENCODE_COMPLETE);
                } else {
                    Toast mToast = CommonFunction.getToast("转码错误");
                    mToast.setGravity(Gravity.CENTER, 0, 0);
                    mToast.show();
                    mEncodeHanlder.sendEmptyMessage(MESSAGE_ENCODE_ERROR);
                }
            }
        }.execute();
    }

    /**
     * 视频压缩
     *
     */
    private void compressThread() {

        new AsyncTask<Void, Void, Integer>() {

            private int[] videoSize;
            private String outputFilePath;
            private String inputFilePath;
            private int mergeFlag = -1;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                inputFilePath = mVideoCapturePreview.getmVideoFile().getPath();
                outputFilePath = inputFilePath.replace("_temp", "");
                VideoFile = new File(outputFilePath);
                videoSize = mVideoCapturePreview.getVideoSize();
            }

            @Override
            protected Integer doInBackground(Void... params) {

                int cameraId = mVideoCapturePreview.getCurrentCameraId();

                try {
                    fc.compress_clipVideo(inputFilePath, VideoFile.getPath(), cameraId, videoSize[0], videoSize[1],0, 0,
                            new ShellUtils.ShellCallback() {

                                @Override
                                public void shellOut(String shellLine) {
                                }

                                @Override
                                public void processComplete(int exitValue) {
                                    mergeFlag = exitValue;
                                }
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    return mergeFlag;
                }

            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result == 0) {
                    File file = new File(inputFilePath);
                    if(file.exists()){
                        file.delete();
                    }
                    mEncodeHanlder.sendEmptyMessage(MESSAGE_ENCODE_COMPLETE);
                } else {
                    Toast mToast = CommonFunction.getToast("转码错误");
                    mToast.setGravity(Gravity.CENTER, 0, 0);
                    mToast.show();
                    mEncodeHanlder.sendEmptyMessage(MESSAGE_ENCODE_ERROR);
                }
            }
        }.execute();
    }


    private static final int MESSAGE_ENCODE_ING = 0;
    private static final int MESSAGE_ENCODE_COMPLETE = 1;
    private static final int MESSAGE_ENCODE_ERROR = -1;

    private Handler mEncodeHanlder = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(msg.what == MESSAGE_ENCODE_ING){
                showProgressDialog();
//                concatVideoParts();
                compressThread();

            }else if(msg.what == MESSAGE_ENCODE_COMPLETE){
                hideProgressDialog();

                Intent intent=new Intent();
                if(VideoFile != null && VideoFile.exists()){
                    intent.setData(Uri.fromFile(VideoFile));
                    setResult(RESULT_OK, intent);
                }else {
                    setResult(RESULT_CANCELED, intent);
                }
                finish();
            }else if(msg.what == MESSAGE_ENCODE_ERROR){
                hideProgressDialog();
                new HandlerPost(1000){
                    @Override
                    public void doAction() {
                        CommonFunction.hideToast();
                    }
                };
                Intent intent = new Intent();
                if(VideoFile.exists())
                    VideoFile.delete();
                setResult(RESULT_CANCELED, intent);
                finish();
            }
            return true;
        }
    });

    @Override
    public void onFinishRecord() {
        if (!mIsRecording) {
            return;
        }
        mIsRecording = false;

        if(mVideoCapturePreview != null){
            int result = mVideoCapturePreview.stopRecord();
            if(result < 0){         //TODO  时间太短处理
                Toast mToast = CommonFunction.getToast(R.string.m_video_too_short);
                mToast.setGravity(Gravity.CENTER, 0, 0);
                mToast.show();
            }else {
                //TODO 转码 ffmpeg 裁减
                CommonFunction.hideToast();
                mEncodeHanlder.sendEmptyMessage(MESSAGE_ENCODE_ING);
            }

        }

    }

    @Override
    public void onCancelRecord() {
        if (!mIsRecording) {
            return;
        }
        mIsRecording = false;

        if(mVideoCapturePreview != null){
            mVideoCapturePreview.cancelRecord();
        }
        CommonFunction.hideToast();
        mProgressBar.setRemove(false);

        mFinish.setEnabled(true);
        if(mVideoCapturePreview.getCurrentCameraId() == 0){
            flashLightSwitch.setEnabled(true);
            flashLightSwitch.setChecked(false);
        }else {
            flashLightSwitch.setEnabled(false); //切换到前置摄像头时，重置闪关灯的状态
        }

    }

    @Override
    public void onMoveLayout(boolean isInLayout) {
        if(isInLayout){
            Toast mToast = CommonFunction.getToast(R.string.m_loosen_cancel);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }else {
            Toast mToast = CommonFunction.getToast(R.string.m_finger_slide_up);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }
        mProgressBar.setRemove(isInLayout);
    }

}