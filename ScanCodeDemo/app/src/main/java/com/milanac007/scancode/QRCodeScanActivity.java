package com.milanac007.scancode;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.qrcode.camera.CameraManager;
import com.google.zxing.qrcode.decoding.CaptureActivityHandler;
import com.google.zxing.qrcode.decoding.DecodeImageCallback;
import com.google.zxing.qrcode.decoding.DecodeImageThread;
import com.google.zxing.qrcode.decoding.DecodeManager;
import com.google.zxing.qrcode.decoding.InactivityTimer;
import com.google.zxing.qrcode.view.LoadingDialog;
import com.google.zxing.qrcode.view.ViewfinderView;

import org.json.JSONArray;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by zqguo on 2016/10/27.
 */

public class QRCodeScanActivity extends Activity implements SurfaceHolder.Callback, TextureView.SurfaceTextureListener, View.OnClickListener {
    public static final int QRCODE_MASK = 0;

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private boolean hasPermission;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private TextView txtResult,timerecoder;
    private InactivityTimer inactivityTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private ProgressBar dialog_progress2;
    int dialogWidth = 0;
    Context context;
    private final int UPLOAD_DATA102 = 102;
    private final int UPLOAD_DATA103 = 103;
    private final int UPLOAD_DATA104 = 104;
    private final int UPLOAD_DATA105 = 105;
    private final int UPLOAD_DATA106 = 106;
    private final int UPLOAD_DATA107 = 107;

    public String selfId = "";
    public String security = "";

    Dialog loadingDialog;
    boolean hasGone = false;

    private String mams_id ="";
    private double jingdu = 0;
    private double weidu = 0;
    private String result;
    JSONArray sendJson;
    boolean mFlashLight = false; //闪关灯开关状态
    private SurfaceView surfaceView;
    private TextureView textureView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrcode_scan_activity);
        context = this;
        setScanWidthHeight();

        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        dialogWidth = display.getWidth();

        //50秒不拍照  自动关闭
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    Thread.sleep(50000);
                    if(mams_id.length()==0 && !hasGone){
                        shandler.sendEmptyMessageDelayed(7, 0);
                        finish();
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }).start();

        CameraManager.init(getApplication());
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        } else {
            hasPermission = true;
        }

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        txtResult = (TextView) findViewById(R.id.txtResult);
        timerecoder= (TextView)findViewById(R.id.timerecoder);
        View codeCancel = findViewById(R.id.codeCancel);
        dialog_progress2 = (ProgressBar) findViewById(R.id.dialog_progress2);
        View more_operation = findViewById(R.id.more_operation);

        View[] views = {codeCancel,more_operation};
        for(View view: views){
            view.setOnClickListener(this);
        }

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);

        mQrCodeExecutor = Executors.newSingleThreadExecutor();
        mHandler = new WeakHandler(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true;
//            SurfaceHolder surfaceHolder = surfaceView.getHolder();
//            initCamera(surfaceHolder);
            initCamera(null, textureView);
        }
    }

    private void setScanWidthHeight(){
        //设置扫描的大小
        DisplayMetrics metrics=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels=metrics.widthPixels;
        int heightPixels=metrics.heightPixels;
        int width=widthPixels<heightPixels?widthPixels:heightPixels;
        if(width<=0)
            width=320;
        CameraManager.MIN_FRAME_WIDTH = (int)(width*3/5);
        CameraManager.MIN_FRAME_HEIGHT = (int)(width*3/5);
        CameraManager.MAX_FRAME_WIDTH = (int)(width*3/5);
        CameraManager.MAX_FRAME_HEIGHT = (int)(width*3/5);
    }


    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.codeCancel) {
            clickCancel();
        }else if(v.getId() == R.id.more_operation){
            showMoreOperationDialog();
        }
    }

    private String[] getRightMenu() {
        String[] menuStr = new String[2];
        if(!mFlashLight){
            menuStr[0] = "打开闪关灯";
        }else {
            menuStr[0] = "关闭闪关灯";
        }
        menuStr[1] = "从相册选取二维码";
        return menuStr;
    }

    OperateListDialog operateListDialog;
    private ArrayList<OperateListDialog.OperateItem> operateItems = new ArrayList<>();

    private void showMoreOperationDialog() {

        final String[] menuStr = getRightMenu();
        if (menuStr == null || menuStr.length <= 0) {
            return;
        }

        if(operateListDialog == null) {
            operateListDialog = new OperateListDialog(this);
            operateListDialog.setIconType(OperateListDialog.EIconType.RIGHT);
        }
        operateItems.clear();


        int size = menuStr.length;
        for (int i = 0; i< size; i++) {
            final OperateListDialog.OperateItem item = operateListDialog.new OperateItem();
            item.setmItemNameStr(menuStr[i]);
            item.setmOperateKey(String.valueOf(i));

            item.setItemClickLister(new OperateListDialog.OperateItemClickListener() {
                @Override
                public void clickItem(int position) {
                    switch (Integer.valueOf(item.getmOperateKey())) {
                        case 0: {
                            mFlashLight = !mFlashLight;
                            CameraManager.get().setFlashLight(mFlashLight);
                        }break;
                        case 1: {
                            openSystemAlbum();
                        }break;
                        default:
                            break;
                    }

                    if (operateListDialog != null) {
                        operateListDialog.dismiss();
                    }
                }
            });
            operateItems.add(item);
        }

//        operateListDialog.setTitle("请选择");
        operateListDialog.showTitle(false);
        operateListDialog.setGravityType(1);
        operateListDialog.updateOperateItems(operateItems);
        operateListDialog.show();
    }


    private void clickCancel(){
        shandler.removeMessages(UPLOAD_DATA105);
        shandler.removeMessages(UPLOAD_DATA106);
        timerecoder.setVisibility(View.GONE);
        dialog_progress2.setVisibility(View.GONE);
        if(!TextUtils.isEmpty(code)){
            code="";
            txtResult.setText("");
            viewfinderView.drawViewfinder();
            inactivityTimer.onActivity();
            handler.restartPreviewAndDecode();
        }else {
            finish();
        }
    }

    private Handler shandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPLOAD_DATA102:
                    Toast.makeText(QRCodeScanActivity.this,"操作失败，请重试！",Toast.LENGTH_SHORT).show();
                    break;
                case UPLOAD_DATA103:
                    Toast.makeText(QRCodeScanActivity.this,"操作成功！",Toast.LENGTH_SHORT).show();
                    break;
                case UPLOAD_DATA104:
                    Toast.makeText(QRCodeScanActivity.this,"操作失败,请尝试重新登录运维平台！",Toast.LENGTH_SHORT).show();
                    break;
                case UPLOAD_DATA105:
                    entertime--;
                    if(entertime<=0){
                        entertime=0;
                        timerecoder.setVisibility(View.GONE);
                        dialog_progress2.setVisibility(View.GONE);
                        clickOK();
                    }else {
//                        timerecoder.setText(entertime+"秒后自动进入");
                        timerecoder.setVisibility(View.VISIBLE);
                        dialog_progress2.setVisibility(View.VISIBLE);
                        timerecoder.setText("已扫描，正在处理中");
                        shandler.sendEmptyMessageDelayed(UPLOAD_DATA105, 1000);
                    }
                    break;
                case UPLOAD_DATA106:
                    closetime--;
                    if(closetime<=0){
                        closetime=0;
                        timerecoder.setVisibility(View.GONE);
                        dialog_progress2.setVisibility(View.GONE);
                        finish();
                    }else {
                        timerecoder.setText(closetime+"秒后自动退出");
                        timerecoder.setVisibility(View.VISIBLE);
                        dialog_progress2.setVisibility(View.VISIBLE);
                        shandler.sendEmptyMessageDelayed(UPLOAD_DATA106, 1000);
                    }
                    break;

                case 6:
                    if (loadingDialog != null) {
                        loadingDialog.dismiss();
                    }
                    break;
                case 7:
                    Toast.makeText(QRCodeScanActivity.this,"相机超时，退出",Toast.LENGTH_LONG).show();
                    break;
                case 8:
                    Toast.makeText(QRCodeScanActivity.this,"请检查网络连接",Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        if (CameraManager.get() != null) {
            CameraManager.get().closeDriver();
        }
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        hasGone = true;

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        surfaceView = (SurfaceView) findViewById(R.id.preview_view);
//        SurfaceHolder surfaceHolder = surfaceView.getHolder();
//        if (hasSurface) {
//            initCamera(surfaceHolder);
//        } else {
//            surfaceHolder.addCallback(this);
//            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        }

        textureView = findViewById(R.id.preview_textureview);
        textureView.setSurfaceTextureListener(this);
        if (hasSurface) {
            initCamera(null, textureView);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    int entertime = 0;
    JSONArray sendArray=null;
    int closetime=0;
    private void clickOK() {
        shandler.removeMessages(UPLOAD_DATA105);
        timerecoder.setVisibility(View.GONE);
        dialog_progress2.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(code)) {
            if (codetype.equals("QR_CODE")) {//二维码
                if (code.startsWith("http://") || code.startsWith("https://")) {
                    Uri myBlogUri = Uri.parse(code);
                    Intent returnIt = new Intent(Intent.ACTION_VIEW, myBlogUri);
                    startActivity(returnIt);
                } else {
                    try {
                        if (dataJSONArray != null) {
                            sendArray = new JSONArray();
                            sendArray.put(dataJSONArray.getString(0));
                            sendArray.put(dataJSONArray.getString(1));
                            sendArray.put(selfId);
                            sendArray.put(security);
//                            qrcode_login();

                            LoadingDialog ld = new LoadingDialog(QRCodeScanActivity.this,
                                    dialogWidth, "数据处理中，请稍后...");
                            loadingDialog = ld.getLoadingDialog();
                        } else {
                            Intent intent = getIntent();
                            if (intent != null) {
                                intent.putExtra("code", code);
                                setResult(200, intent);
                                finish();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                finish();
            }
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder, TextureView textureView) {
        if (!hasPermission || !hasSurface)
            return;

        try {
            CameraManager.get().openDriver(surfaceHolder, textureView);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            Toast.makeText(this, "打开摄像头失败, 请确认开启相应权限", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder, null);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }
    String codetype="";
    JSONArray dataJSONArray=null;
    public void handleDecode(Result obj, Bitmap barcode) {
        timerecoder.setVisibility(View.GONE);
        dialog_progress2.setVisibility(View.GONE);
        shandler.removeMessages(UPLOAD_DATA106);
        inactivityTimer.onActivity();
        codetype=obj.getBarcodeFormat().getName();
        playBeepSoundAndVibrate();
        code = obj.getText();
        if(!TextUtils.isEmpty(code)){
            if(codetype.equals("QR_CODE")){
                //二维码
                if (code.startsWith("http://") || code.startsWith("https://")) {
//                    entertime=6;
                    entertime=2;
                }else{
                    try{
                        dataJSONArray = new JSONArray(code);
                        entertime=1;
                    }catch(Exception e){
                        dataJSONArray=null;
//                        entertime=6;
                        entertime=2;
                    }
                }
            }
            txtResult.setText(code);
            shandler.sendEmptyMessage(UPLOAD_DATA105);
        }
    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    private String code;


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            clickCancel();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static final int REQUEST_SYSTEM_PICTURE = 0;
    private Executor mQrCodeExecutor;
    public static final int MSG_DECODE_SUCCEED = 1;
    public static final int MSG_DECODE_FAIL = 2;
    private Handler mHandler;
    private void openSystemAlbum() {
//        Intent intent = new Intent();
//        /* 开启Pictures画面Type设定为image */
//        intent.setType("image/*");
//        /* 使用Intent.ACTION_GET_CONTENT这个Action */
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//        /* 取得相片后返回本画面 */

        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_SYSTEM_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {

            case REQUEST_SYSTEM_PICTURE:
                Uri uri = data.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (null != cursor) {
                    cursor.moveToFirst();
                    String imgPath = cursor.getString(1); // 图片文件路径
                    cursor.close();
                    if (null != mQrCodeExecutor && !TextUtils.isEmpty(imgPath)) {
                        mQrCodeExecutor.execute(new DecodeImageThread(imgPath, mDecodeImageCallback));
                    }
                }
                break;
        }
    }

    private DecodeImageCallback mDecodeImageCallback = new DecodeImageCallback() {
        @Override
        public void decodeSucceed(Result result) {
            mHandler.obtainMessage(MSG_DECODE_SUCCEED, result).sendToTarget();
        }

        @Override
        public void decodeFail(int type, String reason) {
            mHandler.sendEmptyMessage(MSG_DECODE_FAIL);
        }
    };

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(null, textureView);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        hasSurface = false;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }

    private static class WeakHandler extends Handler {
        private WeakReference<QRCodeScanActivity> mWeakQrCodeActivity;
        private DecodeManager mDecodeManager = new DecodeManager();

        public WeakHandler(QRCodeScanActivity imagePickerActivity) {
            super();
            this.mWeakQrCodeActivity = new WeakReference<>(imagePickerActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            QRCodeScanActivity qrCodeActivity = mWeakQrCodeActivity.get();
            switch (msg.what) {
                case MSG_DECODE_SUCCEED:
                    Result result = (Result) msg.obj;
                    if (null == result) {
                        mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity);
                    } else {
                        handleResult(result);
                    }
                    break;
                case MSG_DECODE_FAIL:
                    mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity);
                    break;
            }
            super.handleMessage(msg);
        }

        private void handleResult(Result result) {
            QRCodeScanActivity imagePickerActivity = mWeakQrCodeActivity.get();
            imagePickerActivity.handleDecode(result, null);
//            String resultString = result.getText();
//            mDecodeManager.showResultDialog(imagePickerActivity, resultString, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
        }

    }

}
