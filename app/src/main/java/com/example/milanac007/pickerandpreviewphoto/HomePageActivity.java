package com.example.milanac007.pickerandpreviewphoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.milanac007.scancode.MainActivity;
import com.tdr.tdrsipim.activity.CustomVideoCaptureActivity;

import java.io.File;
import java.util.List;

public class HomePageActivity extends Activity implements View.OnClickListener{

    private Button photo_album;
    private Button crop_header;
    private Button smaill_video;
    private Button btn_qrcode;

    private LinearLayout photo_layout;
    private int mMode = PickerAlbumActivity.PICKER_ALBUM_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        photo_album = (Button)findViewById(R.id.photo_album);
        crop_header = (Button)findViewById(R.id.crop_header);
        smaill_video = findViewById(R.id.smaill_video);
        btn_qrcode = findViewById(R.id.btn_qrcode);
        photo_layout = (LinearLayout)findViewById(R.id.photo_layout);

        View[] views = {photo_album, crop_header, smaill_video, btn_qrcode};
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
        }
    }

    private List<String> mFilePaths = null;
    private void setPhotoForResult(){
        if(mFilePaths == null ||  mFilePaths.isEmpty())
            return;

        photo_layout.removeAllViews();

        for(String path : mFilePaths){

            if(mMode == PickerAlbumActivity.PICKER_HEADICO_FROM_ALBUM_CODE){ //???????????????????????????
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
