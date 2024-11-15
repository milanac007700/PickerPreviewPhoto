package com.milanac007.demo.videocropdemo.ui;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.milanac007.demo.videocropdemo.R;
import com.milanac007.demo.videocropdemo.library.CommonFunction;
import com.milanac007.demo.videocropdemo.library.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by zqguo on 2016/10/19.
 */
public class CustomVideoCaptureView extends SurfaceView implements SurfaceHolder.Callback{
    public static final String TAG = CustomVideoCaptureView.class.getName();
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;

    private int sizePicture = 0;
    private int mCaptureWidth; //视频录制分辨率宽度
    private int mCaptureHeight; //视频录制分辨率高度

    private int mMaxDefaultMilliSeconds = 10 * 1000;
    private int mCurrentMilliSeconds;
    private VideoCaptureListener mVideoCaptureListener;
    private Activity mActivity;
    private File mVideoFile;
    private Handler mHandler = null;
    private Runnable mRunable = null;
    private static int orientation;

    public interface VideoCaptureListener{
        void onTimeout();
        void onProgress(int count, int total);
        void onError(String errorMsg);
    }

    private void init(Context context){
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mActivity = (Activity) context;
    }

    public CustomVideoCaptureView(Context context){
        super(context);
        init(context);
    }

    public CustomVideoCaptureView(Context context, AttributeSet attr){
        super(context, attr);
        init(context);
    }

    public CustomVideoCaptureView(Context context, AttributeSet attr, int def){
        super(context, attr, def);
        init(context);
    }


    public void setVideoCaptureListener(VideoCaptureListener listener){
        mVideoCaptureListener = listener;
    }

    public int[] getVideoSize(){
        return new int[]{mCaptureWidth, mCaptureHeight};
    }

    public File getmVideoFile() {
        return mVideoFile;
    }

    public void setMaxDefaultSeconds(int mMaxDefaultSeconds) {
        this.mMaxDefaultMilliSeconds = mMaxDefaultSeconds;
    }

    /**
     * 切换前置/后置摄像头
     */
    public void switchCamera() {

        if(mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT){
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }else if(mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }

        cancelRecord();
    }

    /**
     * 打开或关闭闪光灯
     *
     * @param open 控制是否打开
     * @return 打开或关闭失败，则返回false。
     */
    public boolean setFlashLight(boolean open) {

        if (mCamera == null) {
            return false;
        }

        Camera.Parameters parameters = null;
        try {
            parameters = mCamera.getParameters();
        }catch (RuntimeException e){
            e.printStackTrace();
        }

        if (parameters == null) {
            return false;
        }
        List<String> flashModes = parameters.getSupportedFlashModes();
        // Check if camera flash exists
        if (null == flashModes || 0 == flashModes.size()) {
            // Use the screen as a flashlight (next best thing)
            return false;
        }
        String flashMode = parameters.getFlashMode();
        if (open) {
            if (Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                return true;
            }
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                return true;
            } else {
                return false;
            }
        } else {
            if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                return true;
            }
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                return true;
            } else
                return false;
        }
    }


    public static int mCameraId = 0;
    public static int getCurrentCameraId(){
        return mCameraId;
    }

    public static Camera getCaremaInstance(){
        Camera c = null;
        try {
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                c = Camera.open();// attempt to get a Camera instance
            else
                c = Camera.open(mCameraId);
        }catch (Exception e){ // Camera is not available (in use or does not exist)
            Logger.getLogger().e("%s", e.getMessage());
        }

        return c; // returns null if camera is unavailable
    }

    //TODO
    public void initCamera() throws IOException{
        if(mCamera != null){
            releaseCamera();
        }

        // Create an instance of Camera
        if(mCamera == null) {
            mCamera = getCaremaInstance();
        }

        if(mCamera == null){
            if(mVideoCaptureListener != null){
                mVideoCaptureListener.onError("打开摄像头失败, 请确认开启相应权限");
            }
            return;
        }

        setCameraParams();
        setCameraDisplayOrientation(mActivity, mCameraId, mCamera); //90
//        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.startPreview();
        //TODO 自动对焦
        requestAutoFocus(autoFocusHandler, R.id.auto_focus);
    }

    private void setCameraParams(){
        Camera.Parameters params = mCamera.getParameters();
        params.set("orientation", "portrait"); //竖屏
//        setPreviewSize(params);
        mCaptureWidth = 640;
        mCaptureHeight = 480;
        Logger.getLogger().e("%s", "best width: " + mCaptureWidth + ", " + mCaptureHeight);
        params.setPreviewSize(640, 480);//预览比率
        mCamera.setParameters(params); //设置才能生效
    }


    //根据手机支持的视频分辨率，设置预览尺寸
    private void setPreviewSize(Camera.Parameters params){

        List<Camera.Size> previewSizes = params.getSupportedPreviewSizes();
        Logger.getLogger().e("%s", "supportedPictureSize: ");
        for(Camera.Size size : previewSizes){
            Logger.getLogger().e("%s", size.width + ", " +size.height);
            sizePicture = (size.width * size.height) > sizePicture ? size.width * size.height : sizePicture;
        }
        //获取手机支持的分辨率集合，并以宽度为基准降序排序
        Collections.sort(previewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lsz, Camera.Size rsz) {
                if(lsz.width > rsz.width)
                    return -1;
                else if(lsz.width == rsz.width)
                    return 0;
                else
                    return 1;
            }
        });

        float tmp = 0f;
        float minDiff = 100f;
        float ratio = 3.0f/4.0f; //高宽比率3:4，且最接近屏幕宽度的分辨率，可以自己选择合适的想要的分辨率
        Camera.Size bestSize = null;
        for(Camera.Size s : previewSizes){
            tmp = Math.abs((float) s.height/(float) s.width - ratio);
            if(tmp < minDiff){
                minDiff = tmp;
                bestSize = s;
            }
        }

        params.setPreviewSize(bestSize.width, bestSize.height);//预览比率

        //TODO 大部分手机支持的预览尺寸和录制尺寸是一样的，也有特例，有些手机获取不到，那就把设置录制尺寸放到设置预览的方法里面
        if(params.getSupportedVideoSizes() == null || params.getSupportedVideoSizes().isEmpty()){
            mCaptureWidth = bestSize.width;
            mCaptureHeight = bestSize.height;
            Logger.getLogger().e("%s", "best width: " + mCaptureWidth + ", " + mCaptureHeight);
        }else {
            setVideoSize(params);
        }
    }

    //根据手机支持的视频分辨率，设置录制尺寸
    private void setVideoSize(Camera.Parameters params){
        List<Camera.Size> videoSizes = params.getSupportedVideoSizes();
        Logger.getLogger().e("%s", "supportedPictureSize: ");

        //获取手机支持的分辨率集合，并以宽度为基准降序排序
        Collections.sort(videoSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lsz, Camera.Size rsz) {
                if(lsz.width > rsz.width)
                    return -1;
                else if(lsz.width == rsz.width)
                    return 0;
                else
                    return 1;
            }
        });

        float tmp = 0f;
        float minDiff = 100f;
        float ratio = 3.0f/4.0f; //高宽比率3:4，且最接近屏幕宽度的分辨率，可以自己选择合适的想要的分辨率
        Camera.Size bestSize = null;
        for(Camera.Size s : videoSizes){
            tmp = Math.abs((float) s.height/(float) s.width - ratio);
            if(tmp < minDiff){
                minDiff = tmp;
                bestSize = s;
            }
        }

        mCaptureWidth = bestSize.width;
        mCaptureHeight = bestSize.height;

        Logger.getLogger().e("%s", "best width: " + mCaptureWidth + ", " + mCaptureHeight);
    }


    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        orientation = result;
        camera.setDisplayOrientation(result);
    }

    public void releaseResource(){
        releaseMediaRecorder();// if you are using MediaRecorder, release it first
        releaseCamera(); // release the camera immediately on pause event
        destroyHandlerAndRunnable();
    }

    private void releaseCamera(){
        try{
            if (mCamera != null){
                mCamera.stopPreview();
                mCamera.lock();
                mCamera.release();        // release the camera for other applications
                mCamera = null;
            }
        }catch (Exception e){
            Logger.getLogger().e("%s", e.getMessage());
        }finally {
            mCamera = null;
        }
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

    /**
     * 获取系统时间
     *
     * @return
     */
    private String getDate() {
        Calendar ca = Calendar.getInstance();
        int year = ca.get(Calendar.YEAR);           // 获取年份
        int month = ca.get(Calendar.MONTH);         // 获取月份
        int day = ca.get(Calendar.DATE);            // 获取日
        int minute = ca.get(Calendar.MINUTE);       // 分
        int hour = ca.get(Calendar.HOUR);           // 小时
        int second = ca.get(Calendar.SECOND);       // 秒

        String date = "" + year + (month + 1) + day + hour + minute + second;
        Logger.getLogger().d("%s", "date:" + date);

        return date;
    }

    private boolean prepareVideoRecorder(){
        if(mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }
        try {
            //Unlock and set camera to MediaRecorder
            mCamera.unlock();
        }catch (Exception e){
            e.printStackTrace();
        }


        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        // Step 2: Set sources ,before setOutputFormat()
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        //视频输出格式 也可设为3gp等其他格式
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setVideoSize(mCaptureWidth, mCaptureHeight);//设置分辨率 ,after setVideoSource(),after setOutFormat()
        mMediaRecorder.setAudioEncodingBitRate(44100);
        if(mProfile.videoBitRate > 2*1024*1024){
            mMediaRecorder.setVideoEncodingBitRate(2*1024*1024);
        }else {
            mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate); ///after setVideoSource(),after setOutFormat()
        }

        //这句话导致 荣耀手机录视频 MediaRecorder: start failed: -19， 故注调
//        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate>15 ? 15 : mProfile.videoFrameRate);

        //设置编码格式 after setOutputFormat()
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);//音频编码格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);//视频编码格式

        //后续的视频裁减会做方向处理，所以这里设置没什么用
//        mMediaRecorder.setOrientationHint(orientation);

        //TODO 适配 API30(Android10作用域存储)，否则：java.io.FileNotFoundException: open failed: EACCES (Permission denied)  /storage/emulated/0/CustomCamera/oxmRwHPoQP1/temp/202211811319_temp.mp4: open failed: EACCES (Permission denied)
        String videoPath = CommonFunction.getDirUserTemp() + getDate() +"_temp.mp4";
        Log.i(TAG, "##@@ videoPath: " + videoPath);
        mVideoFile = new File(videoPath);
        mMediaRecorder.setOutputFile(videoPath);


        // Step 6: Prepare configured MediaRecorder
        try{
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            return true;
        }catch (IllegalStateException e){
            String errorMsg = "IllegalStateException preparing MediaRecorder: " + e.getMessage();
            Logger.getLogger().d("%s", errorMsg);
            releaseResource();
            if(mVideoCaptureListener != null){
                mVideoCaptureListener.onError(errorMsg);
            }
            return false;
        }catch (IOException e){
            e.printStackTrace();
            String errorMsg = "IOException preparing MediaRecorder: " + e.getMessage();
            Logger.getLogger().d("%s", errorMsg);
            releaseResource();
            if(mVideoCaptureListener != null){
                mVideoCaptureListener.onError(errorMsg);
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private void createHandlerAndRunable(){
        if(mHandler == null){
            mHandler = new Handler();
        }

        mCurrentMilliSeconds = -200;//reset

        if(mRunable == null){
            mRunable = new Runnable() {
                @Override
                public void run() {
                    mCurrentMilliSeconds += 200;

                    Logger.getLogger().e("%s", "mCurrentMilliSecond: " + mCurrentMilliSeconds);
                    if(mVideoCaptureListener != null){
                        if(mCurrentMilliSeconds >= mMaxDefaultMilliSeconds){
                            mVideoCaptureListener.onProgress(mMaxDefaultMilliSeconds, mMaxDefaultMilliSeconds);
                            mVideoCaptureListener.onTimeout();
                        }else {
                            mVideoCaptureListener.onProgress(mCurrentMilliSeconds, mMaxDefaultMilliSeconds);
                            mHandler.postDelayed(this, 200);
                        }
                    }
                }
            };
        }

    }

    private void destroyHandlerAndRunnable(){
        if(mRunable != null){
            mHandler.removeCallbacks(mRunable);
            mRunable = null;
        }

        if(mHandler != null){
            mHandler = null;
        }
    }

    public void startRecord(){
        if(prepareVideoRecorder()){
            createHandlerAndRunable();
            mHandler.postDelayed(mRunable, 200);
        }else {
            stopRecord();
        }
    }

    public int stopRecord(){

        if(mCurrentMilliSeconds < 2000){ //拍摄时间太短处理
            cancelRecord();
            return  -1;
        }

        if(mHandler != null) {
            mHandler.removeCallbacks(mRunable);
        }

        stopMediaRecorder();

        releaseResource();

        return 0;
    }

    private void stopMediaRecorder(){
        if(mMediaRecorder != null){
            //报错为：RuntimeException:stop failed
            try {
                //下面三个参数必须加，不加的话会奔溃，在mediarecorder.stop();
                //报错为：RuntimeException:stop failed
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                mMediaRecorder.stop(); //Note that a RuntimeException is intentionally thrown to the application, if no valid audio/video data has been received when stop() is called. This happens if stop() is called immediately after start().
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }catch (RuntimeException e) {
                e.printStackTrace();
            }catch (Exception e) {
                e.printStackTrace();
            }

            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    public void cancelRecord(){
        if(mVideoFile != null &&  mVideoFile.exists()){
            mVideoFile.delete();
            mVideoFile = null;
        }
        if(mHandler != null) {
            mHandler.removeCallbacks(mRunable);
        }

        stopMediaRecorder();
        releaseMediaRecorder();

        mCurrentMilliSeconds = 0;
        if(mVideoCaptureListener != null) {
            mVideoCaptureListener.onProgress(0, mMaxDefaultMilliSeconds);
        }

        try {
            initCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            initCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseResource();
        mCameraId = 0; //清除static变量的值，保证每次进来都是后置摄像头
    }

    private AutoFocusCallback autoFocusCallback = new AutoFocusCallback();
    /**
     * Asks the camera hardware to perform an autofocus.
     *
     * @param handler The Handler to notify when the autofocus completes.
     * @param message The message to deliver.
     */
    public void requestAutoFocus(Handler handler, int message) {
        if (mCamera != null) {
            autoFocusCallback.setHandler(handler, message);
            //Log.d(TAG, "Requesting auto-focus callback");

            try {
                mCamera.autoFocus(autoFocusCallback);
            }catch (Exception e) {

            }
//            mCamera.autoFocus(autoFocusCallback);
        }
    }

    private Handler autoFocusHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int value = msg.what;
            if(value == R.id.auto_focus){
                //Log.d(TAG, "Got auto-focus message");
                // When one auto focus pass finishes, start another. This is the closest thing to
                // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
                requestAutoFocus(autoFocusHandler, R.id.auto_focus);
            }
            return true;
        }
    });
}

final class AutoFocusCallback implements Camera.AutoFocusCallback {

    private static final String TAG = AutoFocusCallback.class.getSimpleName();

    private static final long AUTOFOCUS_INTERVAL_MS = 1500L;

    private Handler autoFocusHandler;
    private int autoFocusMessage;

    void setHandler(Handler autoFocusHandler, int autoFocusMessage) {
        this.autoFocusHandler = autoFocusHandler;
        this.autoFocusMessage = autoFocusMessage;
    }

    public void onAutoFocus(boolean success, Camera camera) {
        Log.e(TAG, "onAutoFocus: " + success);
        if (autoFocusHandler != null) {
            Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
            // Simulate continuous autofocus by sending a focus request every
            // AUTOFOCUS_INTERVAL_MS milliseconds.
            //Log.d(TAG, "Got auto-focus callback; requesting another");
            autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
            autoFocusHandler = null;
        } else {
            Log.d(TAG, "Got auto-focus callback, but no handler for it");
        }
    }
}
