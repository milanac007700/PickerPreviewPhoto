package com.example.milanac007.pickerandpreviewphoto;

import android.content.Context;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by zqguo on 2016/12/8.
 */
public class CacheManager {
    private static CacheManager mInstance = null;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    public final Object mDiskCacheLock = new Object();
    private static final int DISK_CACHE_SIZE = 1024 * 0124 *10; //10M
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private boolean mDiskCacheStarting = true;
    private HandlerThread addBitmapToCacheHandlerThread;

    public static CacheManager getInstance(){
        if(mInstance == null){
            synchronized (CacheManager.class){
                if(mInstance == null){
                    mInstance = new CacheManager();
                }
            }
        }

        return mInstance;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            if(mMemoryCache == null || bitmap == null)
                return ;

            mMemoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        if(mMemoryCache == null)
            return null;

        return mMemoryCache.get(key);
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.
    public File getDiskCecheDir(Context context, String uniqueName){

        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String chchePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())||
                !Environment.isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();

        return new File(chchePath + File.separator + uniqueName);
    }

    public int getAppVersion(Context context){
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        }catch (PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return 1;
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock){
                File chcheDir = params[0];
                try{
                    mDiskLruCache = DiskLruCache.open(chcheDir, getAppVersion(MyApplication.getContext()), 1, DISK_CACHE_SIZE);
                    mDiskCacheStarting = false;// Finished initialization
                    mDiskCacheLock.notifyAll(); // Wake any waiting threads
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    };

    //MD5?????? key ??? ?????? mDiskCacheLock?????? keys must match regex [a-z0-9_-]{1,64}
    public String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public Bitmap getBitmapFromDiskCeche(String key){

        synchronized (mDiskCacheLock){

            try{
                while (mDiskCacheStarting){
                    mDiskCacheLock.wait();
                }

                if(mDiskLruCache != null){

                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hashKeyForDisk(key));
                    if(snapshot != null){
                        //???????????????????????????DiskLruCache.Snapshot?????????????????????getInputStream()???????????????????????????????????????????????????????????????getInputStream()????????????????????????index?????????????????????0??????
                        InputStream in = snapshot.getInputStream(0);
                        return BitmapFactory.decodeStream(in);
                    }
                }

            }catch (InterruptedException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *
     * @param key
     * @param bitmap
     * @param Async ????????????????????????, ?????????????????????????????????
     */
    public void addBitmapToCache(final String key, final Bitmap bitmap, boolean Async){

        if(Async){
            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        if(bitmap == null)
                            return null;

                        String md5Key = hashKeyForDisk(key);
                        if(getBitmapFromDiskCeche(md5Key) == null){
                            DiskLruCache.Editor editor = mDiskLruCache.edit(md5Key);
                            if(editor != null) {
                                OutputStream out = editor.newOutputStream(0);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                editor.commit();
                            }
                            mDiskLruCache.flush();
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }

                    return null;
                }
            }.execute();
        }else {
            try {
                if(bitmap == null)
                    return ;

                String md5Key = hashKeyForDisk(key);
                if(getBitmapFromDiskCeche(md5Key) == null){
                    DiskLruCache.Editor editor = mDiskLruCache.edit(md5Key);
                    if(editor != null) {
                        OutputStream out = editor.newOutputStream(0);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        editor.commit();
                    }
                    mDiskLruCache.flush();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private InitDiskCacheTask mDiskCacheTask = null;
    public void initCache(){
        if(mDiskCacheStarting == true){
            // Initialize memory cache

            if(mMemoryCache == null){
                // Get max available VM memory, exceeding this amount will throw an
                // OutOfMemory exception. Stored in kilobytes as LruCache takes an
                // int in its constructor.
                final int maxMemory = (int)(Runtime.getRuntime().maxMemory()/1024);
                // Use 1/8th of the available memory for this memory cache.
                final int cacheSize =  maxMemory/8;

                mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                    @Override
                    protected int sizeOf(String key, Bitmap bitmap) {
                        // The cache size will be measured in kilobytes rather than number of items.
                        return bitmap.getByteCount() / 1024;
                    }
                };
            }

            // Initialize disk cache on background thread
            File cacheDir = getDiskCecheDir(MyApplication.getContext(), DISK_CACHE_SUBDIR);
            if(mDiskCacheTask == null){
                mDiskCacheTask = new InitDiskCacheTask();
                mDiskCacheTask.execute(cacheDir);
            }

        }
    }


    public void flushDiskCache(){
        try {
            //??????????????????????????????????????????????????????????????????journal???????????????
            //DiskLruCache?????????????????????????????????????????????journal?????????????????????
            if(mDiskLruCache != null && !mDiskLruCache.isClosed()) {
                mDiskLruCache.flush();
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }


    public void closeDiskCache() {
        try {
            if(mDiskLruCache != null) {
                mDiskLruCache.close();
                mDiskCacheTask = null;
                mDiskCacheStarting = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteDiskCache() {
        try {
            if(mDiskLruCache != null) {
                mDiskCacheStarting = true;
                mDiskLruCache.close();
                mDiskLruCache.delete();
                mMemoryCache = null;
                mDiskLruCache = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        final int width = options.outWidth;
        final int height = options.outHeight;

        int sampleSize = 1;
        if(width > reqWidth || height > reqHeight){
            final int halfWidth = width/2;
            final int halfHeight = height/2;

            while (halfHeight/sampleSize > reqHeight && halfWidth/sampleSize > reqWidth){
                sampleSize *= 2;
            }
        }

        return sampleSize;
    }

    public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight){
        if(!TextUtils.isEmpty(path)){
            if(reqWidth <= 0 || reqHeight <= 0){
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                return bitmap;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inJustDecodeBounds = true; //????????????????????????????????????????????????
            BitmapFactory.decodeFile(path, options);//??????????????????-1?????????
            Logger.getLogger().e("original width: %d, original height: %d" , options.outWidth, options.outHeight);
            if(options.outWidth == -1 || options.outHeight == -1){
                try {
                    ExifInterface exifInterface = new ExifInterface(path);
                    int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);//?????????????????????
                    int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);//??????????????????
                    Logger.getLogger().e("original width: %d, original height: %d", options.outWidth, options.outHeight);
                    options.outWidth = width;
                    options.outHeight = height;
                }catch (IOException e){

                }
            }
            int sampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            Logger.getLogger().e("sampleSize: %d", sampleSize);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            return BitmapFactory.decodeFile(path, options);

        }else {
            return null;
        }
    }


    /**
     * ???????????????????????????
     * @param path ??????????????????
     * @return ?????????????????????
     */
    public static int getBitmapDegree(String path){
        int degree = 0;
        try {
            //?????????????????????????????????????????????EXIF??????
            ExifInterface exifInterface = new ExifInterface(path);
            //???????????????????????????
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90: degree = 90;break;
                case ExifInterface.ORIENTATION_ROTATE_180: degree = 180;break;
                case ExifInterface.ORIENTATION_ROTATE_270: degree = 270;break;
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return degree;
    }

    /**
     * ??????????????????????????????????????????
     * @param bitmap ?????????????????????
     * @param degree ?????????????????????
     * @return ??????????????????
     */
    public static Bitmap rotateBitmaoDegree(Bitmap bitmap, int degree){
        // ???????????????????????????????????????
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // ?????????????????????????????????????????????????????????????????????
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if(bitmap != null && !bitmap.isRecycled()){
            bitmap.recycle();
        }
        return  newBitmap;
    }

    /**
     *
     * @param path
     * @param reqWidth
     * @param reqHeight
     * ??????????????????????????????
     * @return
     */
    public Bitmap addCacheData(String path, int reqWidth, int reqHeight){

        Bitmap bitmap = null;
        if(getBitmapDegree(path) == 90){ //???????????????90?????? ??????h???w ????????????.
            bitmap = decodeSampledBitmapFromPath(path, reqHeight, reqWidth);
        }else {
            bitmap = decodeSampledBitmapFromPath(path, reqWidth, reqHeight);
        }

        if(getBitmapDegree(path) != 0){
            bitmap = rotateBitmaoDegree(bitmap, getBitmapDegree(path));
        }

        addBitmapCache(path, bitmap, true);
        return bitmap;
    }

    /**
     * @param path
     * @param bitmap
     * Async ????????????????????????, ?????????????????????????????????
     */
    public void addBitmapCache(String path, Bitmap bitmap, boolean isAsync){
        addBitmapToMemoryCache(path, bitmap); //??????????????????
        synchronized (mDiskCacheLock){ //??????????????????
            addBitmapToCache(path, bitmap, isAsync);
        }
    }

    public Bitmap getBitmapFormCache(String path){
        Bitmap bitmap = getBitmapFromMemCache(path); //memory cache
        if(bitmap == null){
            bitmap = getBitmapFromDiskCeche(path); //disk cache ?????????????????????????????????
        }
        return bitmap;
    }
}
