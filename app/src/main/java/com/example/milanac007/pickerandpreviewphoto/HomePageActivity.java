package com.example.milanac007.pickerandpreviewphoto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.milanac007.demo.videocropdemo.activity.CustomVideoCaptureActivity;
import com.milanac007.scancode.MainActivity;

import java.io.File;
import java.util.List;

public class HomePageActivity extends Activity implements View.OnClickListener{
    public static final String TAG = "HomePageActivity";

    private Button photo_album;
    private Button crop_header;
    private Button smaill_video;
    private Button btn_qrcode;

    private LinearLayout photo_layout;
    private int mMode = PickerAlbumActivity.PICKER_ALBUM_CODE;
    private ImageView iv_preview;
    private Button btn_play;
    private ViewGroup fl_video_capture;
    private Uri videoUri;
    private int videoWidth;
    private int videoHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        photo_album = (Button)findViewById(R.id.photo_album);
        crop_header = (Button)findViewById(R.id.crop_header);
        smaill_video = findViewById(R.id.smaill_video);
        btn_qrcode = findViewById(R.id.btn_qrcode);
        photo_layout = (LinearLayout)findViewById(R.id.photo_layout);

        fl_video_capture = findViewById(R.id.fl_video_capture);
        iv_preview = findViewById(R.id.iv_preview);
        btn_play = findViewById(R.id.btn_play);

        View[] views = {photo_album, crop_header, smaill_video, btn_qrcode, btn_play};
        for (View view: views){
            view.setOnClickListener(this);
        }
    }

    private void onClickPhotoAlbum(){
        Intent intent = new Intent(this, PickerAlbumActivity.class);
        mMode = PickerAlbumActivity.PICKER_ALBUM_CODE;
        startActivityForResult(intent, PickerAlbumActivity.PICKER_ALBUM_CODE);
    }

    private void onClickCropHeader(){
        Intent intent = new Intent(this, PickerAlbumActivity.class);
        mMode = PickerAlbumActivity.PICKER_HEADICO_FROM_ALBUM_CODE;
        intent.putExtra(PhotoPreviewActivity.MODE, PickerAlbumActivity.PICKER_HEADICO_FROM_ALBUM_CODE);
        startActivityForResult(intent, PickerAlbumActivity.PICKER_ALBUM_CODE);
    }

    private void onClickSmallVideo(){
        Intent intent = new Intent(this, CustomVideoCaptureActivity.class);
        startActivityForResult(intent, CustomVideoCaptureActivity.VIDEO_CAPTURE_CODE);
    }

    private void onClickQrCode(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.photo_album){
            onClickPhotoAlbum();
        }else if(v.getId() == R.id.crop_header){
            onClickCropHeader();
        }else if(v.getId() == R.id.smaill_video) {
            onClickSmallVideo();
        }else if(v.getId() == R.id.btn_qrcode) {
            onClickQrCode();
        } else if (v.getId() == R.id.btn_play) {
            playVideo(this, videoUri, null, videoWidth, videoHeight);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PickerAlbumActivity.PICKER_ALBUM_CODE){
            if(resultCode == Activity.RESULT_OK){
                mFilePaths = data.getStringArrayListExtra(PickerAlbumActivity.SELECTED_KEY);
                setPhotoForResult();
            }
        } else if (requestCode == CustomVideoCaptureActivity.VIDEO_CAPTURE_CODE) {
            if(resultCode == Activity.RESULT_OK){
                videoUri = data.getData();
                String previewSizeStr = data.getStringExtra("previewSize");
                String[] size = previewSizeStr.split("x");
                videoWidth = Integer.parseInt(size[0]);
                videoHeight = Integer.parseInt(size[1]);
                Log.i(TAG, "小视频输出目录：" + videoUri);
                Toast.makeText(this, "小视频输出目录：" + videoUri, Toast.LENGTH_SHORT).show();

                ViewGroup.LayoutParams lp = iv_preview.getLayoutParams();
                lp.width = videoWidth;
                lp.height = videoHeight;
                iv_preview.setLayoutParams(lp);
                btn_play.setVisibility(View.VISIBLE);
            }
        }
    }

    private void playVideo(Context context, final Uri videoUri, final String thumbnailPath, final int width, final int height) {
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

        videoView.setVideoURI(videoUri);
//         videoView.setMediaController(new MediaController(context)); //设置了一个播放控制器。
        videoView.start(); //程序运行时自动开始播放视频。
        videoView.requestFocus(); //播放窗口为当前窗口
    }

    private List<String> mFilePaths = null;
    private void setPhotoForResult(){
        if(mFilePaths == null ||  mFilePaths.isEmpty())
            return;

        photo_layout.removeAllViews();

        for(String path : mFilePaths){

            if(mMode == PickerAlbumActivity.PICKER_HEADICO_FROM_ALBUM_CODE){ //这里不保存头像截图
                File file = new File(path);
                if(file.exists()){
                    file.delete();
                }
            }

            Bitmap bitmap = CacheManager.getInstance().getBitmapFormCache(path);
            if(bitmap != null){
                ImageView imageView = new ImageView(this);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(CommonFunction.dip2px(100), CommonFunction.dip2px(100));
                imageView.setLayoutParams(params);
                imageView.setImageBitmap(bitmap);
                photo_layout.addView(imageView);
            }
        }
    }

}
