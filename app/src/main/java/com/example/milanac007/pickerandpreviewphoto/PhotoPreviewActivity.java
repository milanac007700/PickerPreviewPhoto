package com.example.milanac007.pickerandpreviewphoto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by zqguo on 2016/12/7.
 */
public class PhotoPreviewActivity extends Activity implements View.OnClickListener{
    public static final String TAG = "PhotoPreviewActivity";
    public static final int CODE = 0xfff0;

    public static final String MODE = "mode";
    public static final int PickerPreview = 0; // PickerPreview:选择预览，可操作。
    public static final int NormalPreview = 1;// NormalPreview:普通预览； 只能查看；

    private int mMode = PickerPreview;
    public static final String SELECTED_KEY = "selected_key";
    public static final String CURRENT_INDEX = "current_index";

    private TextView current_item_index;
    private PhotoViewPager photo_viewpager;
    private TextView send_photos;
    private TextView select_photo_state;
    private TextView mBack;
    private ArrayList<String> mImgPaths;
    private ArrayList<String> mSelectedPaths = new ArrayList<String>(); //标识当前选中的集合

    private int mTotalSum;
    private View operate_layout;
    private long[] mImgSizes;
    private TextView select_original_state;
    private int currentIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.photo_preview_layout);
        mBack = (TextView)findViewById(R.id.back);
        current_item_index = (TextView)findViewById(R.id.current_item_index);
        select_photo_state = (TextView)findViewById(R.id.select_photo_state);
        select_original_state = (TextView)findViewById(R.id.select_original_state);
        send_photos = (TextView)findViewById(R.id.send_photos);
        photo_viewpager = (PhotoViewPager)findViewById(R.id.photo_viewpager);

        operate_layout = findViewById(R.id.operate_layout);
        setListener();

        mMode = getIntent().getIntExtra(MODE, PickerPreview);
        mImgPaths = getIntent().getStringArrayListExtra(SELECTED_KEY);
        currentIndex = getIntent().getIntExtra(CURRENT_INDEX, 0);
        mImgSizes = getIntent().getLongArrayExtra("size");


        if(mImgPaths != null && !mImgPaths.isEmpty()){
            mTotalSum = mImgPaths.size();

            for(String path : mImgPaths){
                mSelectedPaths.add(path);
            }

            PhotoPreviewPagerAdapter adapter = new PhotoPreviewPagerAdapter(this, mImgPaths);
            photo_viewpager.setAdapter(adapter);
            photo_viewpager.setCurrentItem(currentIndex);
        }

        updateUI();
    }

    public void updateUI(){

        current_item_index.setText(String.format("%d/%d", photo_viewpager.getCurrentItem()+1, mTotalSum));
        if(mMode == NormalPreview){
            operate_layout.setVisibility(View.GONE);
            send_photos.setVisibility(View.GONE);
            return;
        }

        if(mSelectedPaths.isEmpty()){
            send_photos.setText("发送");
            send_photos.setEnabled(false);
        }else {
            send_photos.setText(String.format("发送(%d/9)", mSelectedPaths.size()));
            send_photos.setEnabled(true);
        }

        Drawable drawable = null;
        String path = mImgPaths.get(photo_viewpager.getCurrentItem());
        if(mSelectedPaths.contains(path)){
            drawable = getResources().getDrawable(R.mipmap.checkbox_on);
        }else {
            drawable = getResources().getDrawable(R.mipmap.checkbox_off);
        }
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        select_photo_state.setCompoundDrawables(drawable, null, null, null);

        if(mImgSizes != null && mImgSizes.length >0){
            String showSizeStr = CommonFunction.formatFileSize(mImgSizes[photo_viewpager.getCurrentItem()]);
            select_original_state.setText(showSizeStr);
            select_original_state.setVisibility(View.VISIBLE);
        }else {
            select_original_state.setVisibility(View.GONE);
        }

    }



    private void setListener(){
        View[] views = {mBack, select_photo_state, send_photos};
        for (View view : views){
            view.setOnClickListener(this);
        }

        operate_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        photo_viewpager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }

            @Override
            public void onPageSelected(int position) {
                updateUI();
            }
        });

    }


    @Override
    public void onClick(View v) {

        if(v.getId() == R.id.back){
            if(mSelectedPaths != null){
                Intent intent = new Intent();
                intent.putStringArrayListExtra(SELECTED_KEY, mSelectedPaths);
                setResult(RESULT_OK, intent);
            }
            finish();
        }else if(v.getId() == R.id.send_photos){
            //TODO 可能需要用接口做
            if(!mSelectedPaths.isEmpty()){
                Intent intent = new Intent();
                intent.putStringArrayListExtra(SELECTED_KEY, mSelectedPaths);
                intent.putExtra("finish", true);
                setResult(RESULT_OK, intent);
            }
            finish();

        }else if(v.getId() == R.id.select_photo_state){
            String path = mImgPaths.get(photo_viewpager.getCurrentItem());
            if(mSelectedPaths.contains(path)){
                mSelectedPaths.remove(path);
            }else {
                mSelectedPaths.add(path);
            }
            updateUI();
        }

    }


    class PhotoPreviewPagerAdapter extends PagerAdapter {

        private ArrayList<String> mImgPaths;
        private Context mContext;

        public PhotoPreviewPagerAdapter(Context context, ArrayList<String> imgPaths){
            mImgPaths = imgPaths;
            mContext = context;
        }

        private void GlideView(Bitmap bitmap, ImageView imageView){
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] bytes=baos.toByteArray();

            Glide.with(mContext)
                    .load(bytes)
                    .placeholder(R.mipmap.msg_pic_fail)
                    .error(R.mipmap.msg_pic_fail)
//                    .crossFade()
                    .diskCacheStrategy(DiskCacheStrategy.NONE) //不缓存到SD卡
                    .skipMemoryCache(true) //不缓存内存
                    .into(imageView);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(mContext);
            String path = mImgPaths.get(position);

            Bitmap bitmap =  CacheManager.getInstance().getBitmapFormCache(path);
            if(bitmap != null){
                GlideView(bitmap, photoView);
            }else if(new File(path).exists()){
                bitmap = CacheManager.getInstance().addCacheData(path, CommonFunction.getWidthPx()/3, CommonFunction.getHeightPx()/3);
                GlideView(bitmap, photoView);
            }

            container.addView(photoView);
            return photoView;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((PhotoView)object);
        }


        @Override
        public int getCount() {
            return mImgPaths == null ? 0 : mImgPaths.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

}
