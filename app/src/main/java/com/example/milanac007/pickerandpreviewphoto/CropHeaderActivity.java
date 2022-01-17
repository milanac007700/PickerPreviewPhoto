package com.example.milanac007.pickerandpreviewphoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class CropHeaderActivity extends Activity implements View.OnClickListener{
    public static final String TAG = "CropHeaderActivity";
    public static final int CODE = 0xfff3;
    public static final String SELECTED_KEY = "selected_key";
    private TextView send_photos;
    private TextView mBack;
    private String mImgPath;

    private GestureImageView cropMaskView;
    private int mScreenWidth;
    private ArrayList<String> mSelectedPaths = new ArrayList<>();
    private Activity mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.crop_header_activity);
        mBack = (TextView)findViewById(R.id.back);
        send_photos = (TextView)findViewById(R.id.send_photos);
        cropMaskView = (GestureImageView)findViewById(R.id.cropMaskView);

        setListener();

        mImgPath = getIntent().getStringArrayListExtra(SELECTED_KEY).get(0);

        Bitmap bitmap =  CacheManager.getInstance().getBitmapFormCache(mImgPath);
        if(bitmap == null){
            int mItemWidth = (mScreenWidth - 4*4)/3; //item减4个像素
            bitmap = CacheManager.getInstance().addCacheData(mImgPath, mItemWidth, mItemWidth);//加入内存、磁盘缓存
        }
        if(bitmap != null){
            GlideView(bitmap, cropMaskView);
        }

    }

    private void GlideView(Bitmap bitmap, ImageView imageView){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes=baos.toByteArray();

        Glide.with(mContext)
                .load(bytes)
                .placeholder(R.mipmap.msg_pic_fail)
                .error(R.mipmap.msg_pic_fail)
//                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.NONE) //不缓存到SD卡
                .skipMemoryCache(true) //不缓存内存
                .into(imageView);
    }


    private void setListener(){
        View[] views = {mBack, send_photos};
        for (View view : views){
            view.setOnClickListener(this);
        }
    }


    /**
     * 裁剪
     * @return 裁剪后的图像
     */
    private void outCropHeadIco(){
        Bitmap cropBitmap = cropMaskView.outputMaskBitmap();
        File cropFile = new File(CommonFunction.getDirUserTemp() + UUID.randomUUID() + ".jpg");
        try {
            OutputStream out = new FileOutputStream(cropFile);
            cropBitmap.compress(Bitmap.CompressFormat.JPEG, 10, out);//压缩成10%
            CacheManager.getInstance().addBitmapCache(cropFile.getPath(), cropBitmap, true);
            out.flush();
            out.close();
            mSelectedPaths.clear();
            mSelectedPaths.add(cropFile.getPath());
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.back){
            Intent intent = new Intent();
            intent.putStringArrayListExtra(SELECTED_KEY, new ArrayList<String>());
            intent.putExtra("finish", true);
            setResult(RESULT_OK, intent);
            finish();
        }else if(v.getId() == R.id.send_photos){
            outCropHeadIco();
            //TODO 可能需要用接口做
            if(!mSelectedPaths.isEmpty()){
                Intent intent = new Intent();
                intent.putStringArrayListExtra(SELECTED_KEY, mSelectedPaths);
                intent.putExtra("finish", true);
                setResult(RESULT_OK, intent);
            }
            finish();
        }

    }
}

