package com.example.milanac007.pickerandpreviewphoto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridView;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

/**
 * Created by zqguo on 2016/12/7.
 */
public class PickerAlbumActivity extends Activity implements View.OnClickListener{
    public static final String TAG = "PickerAlbumActivity";

    public static final int PHOTO_CODE = 0xfff1; // 拍照
    public static final int PHOTO_PREVIEW_CODE = 0xfff2; // 拍照预览

    public static final int PICKER_ALBUM_CODE = 0; //选择照片输出， 复选
    public static final int PICKER_HEADICO_FROM_ALBUM_CODE = 2;//设置头像， 只输出框内截图, 单选

    public static final String MODE = "mode";
    private int mMode = PICKER_ALBUM_CODE;

    public static final String SELECTED_KEY = "selected_key";
    public static final String MAX_SUM_KEY = "MAX_SUM_KEY";
    private TextView mBack;
    private TextView send_photos;
    private TextView select_album;
    private TextView preview_photos;
    private PhotoAlbumAdapter adapter;
    private int mSrceenWidth;
    private int mScreenHeight;
    private File mCurrentCameraFile;

    public static final int MULTIMODE = 0;
    public static final int SINGLEMODE = 1;

    private int MAX_NUM = 9; //最多选择的照片数


    private void setMaxNum(int maxNum){
        MAX_NUM = maxNum;
    }
    public int getMaxNum(){
        return MAX_NUM;
    }

    public int getSrceenWidth(){
        return mSrceenWidth;
    }

    public int getScreenHeight(){
        return mScreenHeight;
    }

    String requestPermissions[] = new String[]{PermissionUtils.PERMISSION_CAMERA, PermissionUtils.PERMISSION_READ_EXTERNAL_STORAGE};

    @Override
    protected void onResume() {
        super.onResume();
        if(!PermissionUtils.lacksPermission(this, requestPermissions)){
            setData();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CacheManager.getInstance().flushDiskCache();
    }


    static public class AlbumImgData{
        String id;
        String path;
        String name;
        long size;
    }

    private ArrayList<AlbumImgData> imgDatas = new ArrayList<AlbumImgData>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mSrceenWidth = dm.widthPixels;
        mScreenHeight = dm.heightPixels;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.picker_album_layout);
        mBack = (TextView)findViewById(R.id.back);
        select_album = (TextView)findViewById(R.id.select_album);
        preview_photos = (TextView)findViewById(R.id.preview_photos);
        send_photos = (TextView)findViewById(R.id.send_photos);

        ViewGroup operate_layout = findViewById(R.id.operate_layout);
        operate_layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        View[] views = {mBack, select_album, preview_photos, send_photos};
        for (View view : views){
            view.setOnClickListener(this);
        }

        if(PermissionUtils.lacksPermission(this, requestPermissions)){
            PermissionUtils.requestMultiPermissions(this, PermissionUtils.CODE_MULTI_PERMISSIONS, requestPermissions, mPermissionGrantCallback);
        }else {
            setData();
        }
    }

    private void setData(){
        if(adapter != null) return;

        Uri photoAlbumUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        String[] project = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE};
        String selectStr = String.format("%s=? or %s=?", MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.MIME_TYPE);
        Cursor cursor = contentResolver.query(photoAlbumUri, project, selectStr, new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_ADDED + " DESC");
        if(cursor == null)
            return;

        imgDatas.clear();
        AlbumImgData imgData = new AlbumImgData();
        imgData.id = String.valueOf(-1); //假数据，用于拍照图标
        imgDatas.add(imgData);

        while (cursor.moveToNext()){
            @SuppressLint("Range") String fileId = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            @SuppressLint("Range") String filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            @SuppressLint("Range") String fileName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            @SuppressLint("Range") long fileSize = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));

            imgData = new AlbumImgData();
            imgData.id = fileId;
            imgData.path = filePath;
            imgData.name = fileName;
            imgData.size = fileSize;
            imgDatas.add(imgData);

//            Log.i(TAG, fileId + ", " + filePath + ", " + fileName + ", " + fileSize);
        }
        cursor.close();

        mMode = getIntent().getIntExtra(MODE, PICKER_ALBUM_CODE);
        GridView photo_gridview = (GridView)findViewById(R.id.photo_gridview);
        adapter = new PhotoAlbumAdapter(this, imgDatas);
        if(mMode == PICKER_HEADICO_FROM_ALBUM_CODE){
            adapter.setChooseMode(SINGLEMODE);
        }
        photo_gridview.setAdapter(adapter);

        //设置最大选取的照片数(只针对多选)
        int maxSum = getIntent().getIntExtra(MAX_SUM_KEY, MAX_NUM);
        setMaxNum(maxSum);

        updateSendBtnUI();
    }

    public void onClickPreviewBtn(){

        if(selectedList.isEmpty())
            return;

        if(mMode == PICKER_HEADICO_FROM_ALBUM_CODE){
            Intent intent = new Intent(this, CropHeaderActivity.class);
            ArrayList<String> selectedImgs = new ArrayList<String>();
            for(AlbumImgData item : selectedList){
                selectedImgs.add(item.path);
            }
            intent.putExtra(SELECTED_KEY, selectedImgs);
            startActivityForResult(intent, CropHeaderActivity.CODE);
        }else {
            Intent intent = new Intent(this, PhotoPreviewActivity.class);
            ArrayList<String> selectedImgs = new ArrayList<String>();
            for(AlbumImgData item : selectedList){
                selectedImgs.add(item.path);
            }
            intent.putExtra(SELECTED_KEY, selectedImgs);
            startActivityForResult(intent, PhotoPreviewActivity.CODE);
        }
    }

    @Override
    public void onClick(View v) {
        int vId = v.getId();
        if(vId == R.id.back){
            setResult(RESULT_CANCELED, null);
            finish();
        }else if(vId == R.id.preview_photos){
            onClickPreviewBtn();
        }else if(vId == R.id.send_photos){

            if(selectedList.isEmpty())
                return;

            ArrayList<String> results = new ArrayList<String>();
            for(AlbumImgData item : selectedList){
                results.add(item.path);
            }
            notifyResult(results, false);
        }
    }

    private void notifyResult(ArrayList<String> results, boolean isHidden){

        if(results != null && !results.isEmpty()){
            Intent intent = new Intent();
            intent.putStringArrayListExtra(SELECTED_KEY, results);
            setResult(RESULT_OK, intent);
        }else {
            setResult(RESULT_CANCELED, null);
        }

        setVisible(!isHidden);
        finish();
    }

    private ArrayList<AlbumImgData> selectedList = new ArrayList<AlbumImgData>();

    public int ContainSelectedSum(){
        return selectedList.size();
    }
    public boolean isContain(AlbumImgData item){
        return selectedList.contains(item);
    }

    public void clear(){
        selectedList.clear();
    }
    public void removeOne(AlbumImgData item){
        if(isContain(item)) {
            selectedList.remove(item);
        }
    }

    public void addOne(AlbumImgData item){
        if(!isContain(item)) {
            selectedList.add(item);
        }
    }

    public void updateSendBtnUI(){

        if(mMode == PICKER_HEADICO_FROM_ALBUM_CODE){
            send_photos.setVisibility(View.GONE);
            preview_photos.setVisibility(View.GONE);
            return;
        }

        preview_photos.setVisibility(View.VISIBLE);
        if(selectedList.isEmpty()){
            send_photos.setText("发送");
            send_photos.setEnabled(false);
            preview_photos.setEnabled(false);
        }else {
            send_photos.setText(String.format("发送(%d/%d)", selectedList.size(), MAX_NUM));
            send_photos.setEnabled(true);
            preview_photos.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults, mPermissionGrantCallback);
    }

    private PermissionUtils.PermissionGrantCallback mPermissionGrantCallback = new PermissionUtils.PermissionGrantCallback() {
        @Override
        public void onPermissionGranted(int requestCode) {
            switch (requestCode){
                case PermissionUtils.CODE_MULTI_PERMISSIONS:{
                    setData();
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

    /**
     *
     * 把targetSdkVersion指定成24及之上并且在API> = 24(N, Android7.0)的设备上运行时，会抛出异常：FileUriExposedException:
     *
     * FileUriExposedException:
     * 应用程序将file://Uri 暴露给另一个应用程序时引发的异常。不鼓励这种曝光，因为接收应用可能无法访问共享路径。
     * 例如，接收应用可能没有请求READ_EXTERNAL_STORAGE运行时权限，或者平台可能跨用户配置文件边界共享Uri。
     * 相反，应用程序应使用content://Uris，以便平台可以扩展接收应用程序的临时权限以访问资源。
     * 仅针对N或更高的应用程序抛出此操作。针对早期SDK版本的应用程序可以共享file://Uri，但强烈建议不要这样做。
     *
     * 总而言之，就是Android不再允许在app中把file://Uri暴露给其他app，包括但不局限于通过Intent或ClipData等方法。
     *
     * 解决方案：谷歌提供了FileProvider，使用它可以生成content://URI 来替代 file://URI。
     * FileProvider会隐藏共享文件的真实路径，将它转换成content://开放的路径，因此还需要设定转换的规则。
     * android:resource="@xml/provider_paths"这个属性指定了规则所在的文件。
     *
     * 转换规则：
     * 替换前缀：把file://  替换为  content://${android:authorities}。
     *
     * TODO 适配 API30(Android10作用域存储)，否则：java.io.FileNotFoundException: open failed: EACCES (Permission denied)  /storage/emulated/0/CustomCamera/oxmRwHPoQP1/temp/202211811319_temp.jpg: open failed: EACCES (Permission denied)
     */
    public void cameraPhoto() {
//        String fileName = Environment.getExternalStorageDirectory() + "/CustomCamera/"  + UUID.randomUUID().toString() + ".jpg";
        String fileName = CommonFunction.getDirUserTemp() + UUID.randomUUID().toString() + ".jpg";
        mCurrentCameraFile = new File(fileName);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        Uri photoUri = null;
        if(Build.VERSION.SDK_INT < 24) {
            photoUri = Uri.fromFile(mCurrentCameraFile);
        }else {
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", mCurrentCameraFile);
        }
        Log.i(TAG, "photoUri: " + photoUri);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, PHOTO_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PhotoPreviewActivity.CODE){ // 预览
            if(resultCode == RESULT_OK){
                ArrayList<String> mSelectedPaths = data.getStringArrayListExtra(SELECTED_KEY);

                ArrayList<AlbumImgData> tempList = new ArrayList<AlbumImgData>();
                for(AlbumImgData item : selectedList){
                    if(!mSelectedPaths.contains(item.path))
                        tempList.add(item);
                }
                selectedList.removeAll(tempList);
                tempList.clear();

                boolean isFinish = data.getBooleanExtra("finish", false);
                if(!isFinish){
                    updateSendBtnUI();
                    adapter.notifyDataSetChanged();
                }else {
                    ArrayList<String> results = new ArrayList<String>();
                    for(AlbumImgData item : selectedList){
                        results.add(item.path);
                    }
                    notifyResult(results, true);
                }
            }
        }else if(requestCode == CropHeaderActivity.CODE){
            if(resultCode == RESULT_OK){
                ArrayList<String> mSelectedPaths = data.getStringArrayListExtra(SELECTED_KEY);
                notifyResult(mSelectedPaths, true);
            }
        }else if(requestCode == PHOTO_CODE){ //拍照
            if(resultCode == RESULT_OK){
                new BitmapWorkerTask(PickerAlbumActivity.this).execute(mCurrentCameraFile.getPath());
            }
        }else if(requestCode == PHOTO_PREVIEW_CODE){ //拍照预览
            if(resultCode == RESULT_OK){
                boolean isFinish = data.getBooleanExtra("finish", false);
                if(isFinish){
                    ArrayList<String> mSelectedPaths = data.getStringArrayListExtra(SELECTED_KEY);
                    notifyResult(mSelectedPaths, true);
                }
            }
        }
    }


    /**
     * static内部类+WeakReference 防止页面退出时，非静态内部类正在执行耗时操作、仍持有当前页面的Context而导致内存泄漏
     */
    static class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap>{

        private String path;
        private long fileSize;

        private final WeakReference<PickerAlbumActivity> activityWeakReference;
        public BitmapWorkerTask(PickerAlbumActivity context){
            super();
            activityWeakReference = new WeakReference<PickerAlbumActivity>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PickerAlbumActivity pickerAlbumActivity = activityWeakReference.get();
            if(pickerAlbumActivity.isFinishing()) return;

            CommonFunction.showProgressDialog(pickerAlbumActivity, "处理中...");
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            PickerAlbumActivity pickerAlbumActivity = activityWeakReference.get();
            if(pickerAlbumActivity.isFinishing()) return null;

            path = params[0];
            fileSize = new File(path).length();

            return  CacheManager.getInstance().addCacheData(path,pickerAlbumActivity.mSrceenWidth/3,pickerAlbumActivity.mScreenHeight/3);//加入内存、磁盘缓存
        }

        @Override
        protected void onCancelled() {
            CommonFunction.dismissProgressDialog();
            CommonFunction.showToast("已取消");
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            CommonFunction.dismissProgressDialog();

            PickerAlbumActivity pickerAlbumActivity = activityWeakReference.get();
            if(pickerAlbumActivity.isFinishing()) return ;

            if(bitmap != null){
                Intent intent = null;

                if(pickerAlbumActivity.mMode == PICKER_HEADICO_FROM_ALBUM_CODE){
                    intent = new Intent(pickerAlbumActivity, CropHeaderActivity.class);
                }else {
                    intent = new Intent(pickerAlbumActivity, PhotoPreviewActivity.class);
                }

                ArrayList<String> selectedImgs = new ArrayList<String>();
                selectedImgs.add(path);
                intent.putExtra(SELECTED_KEY, selectedImgs);
                long[] fileSizes = new long[]{fileSize};
                intent.putExtra("size", fileSizes);
                pickerAlbumActivity.startActivityForResult(intent, PHOTO_PREVIEW_CODE);
            }
        }
    }

}
