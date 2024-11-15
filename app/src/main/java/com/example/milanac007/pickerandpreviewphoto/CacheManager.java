package com.example.milanac007.pickerandpreviewphoto;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by zqguo on 2016/12/8.
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    private static CacheManager mInstance = null;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    public final Object mDiskCacheLock = new Object();
    private static final int DISK_CACHE_SIZE = 1024 * 1024 *50; //10M -> 50M
    private static final String DISK_CACHE_SUBDIR = "thumbnails";
    private boolean mDiskCacheStarting = true;

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
    public File getDiskCacheDir(Context context, String uniqueName){

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

    static class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        private final WeakReference<CacheManager> reference;

        public InitDiskCacheTask() {
            super();
            reference = new WeakReference<>(CacheManager.getInstance());
        }

        @Override
        protected Void doInBackground(File... params) {
            CacheManager cacheManager = reference.get();
            if(cacheManager == null) return null;

            synchronized (cacheManager.mDiskCacheLock){
                File cacheDir = params[0];
                try{
                    cacheManager.mDiskLruCache = DiskLruCache.open(cacheDir, cacheManager.getAppVersion(MyApplication.getContext()), 1, DISK_CACHE_SIZE);
                    cacheManager.mDiskCacheStarting = false;// Finished initialization
                    cacheManager.mDiskCacheLock.notifyAll(); // Wake any waiting threads
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            return null;
        }
    };

    //MD5编码 key ，因为 mDiskCacheLock要求 keys must match regex [a-z0-9_-]{1,64}
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

    public Bitmap getBitmapFromDiskCache(String key){

        synchronized (mDiskCacheLock){

            try{
                while (mDiskCacheStarting){
                    mDiskCacheLock.wait();
                }

                if(mDiskLruCache != null){

                    DiskLruCache.Snapshot snapshot = mDiskLruCache.get(hashKeyForDisk(key));
                    if(snapshot != null){
                        //这里获取到的是一个DiskLruCache.Snapshot对象，调用它的getInputStream()方法就可以得到缓存文件的输入流了。同样地，getInputStream()方法也需要传一个index参数，这里传入0就好
                        InputStream in = snapshot.getInputStream(0);
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        addBitmapToMemoryCache(key, bitmap); //尝试更新memory cache
                        return bitmap;
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

    static class addDiskCacheTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<CacheManager> reference;
        private final String key;
        private final Bitmap bitmap;
        public addDiskCacheTask(final String key, final Bitmap bitmap) {
            super();
            reference = new WeakReference<>(CacheManager.getInstance());
            this.key = key;
            this.bitmap = bitmap;
        }

        @Override
        protected Void doInBackground(Void... param) {
            if(reference.get() == null) return null;

            CacheManager cacheManager = reference.get();
            try {
                String md5Key = cacheManager.hashKeyForDisk(key);
                if(cacheManager.getBitmapFromDiskCache(md5Key) == null){
                    DiskLruCache.Editor editor = cacheManager.mDiskLruCache.edit(md5Key);
                    if(editor != null) {
                        OutputStream out = editor.newOutputStream(0);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        editor.commit();
                    }
                    cacheManager.mDiskLruCache.flush();
                }
            }catch (IOException e){
                e.printStackTrace();
            }

            return null;
        }
    };

    /**
     *
     * @param key
     * @param bitmap
     * @param Async 是否需要后台处理, 根据具体的上层逻辑调用
     */
    public void addBitmapToDiskCache(final String key, final Bitmap bitmap, boolean Async){

        if(bitmap == null)
            return ;

        if(Async){
            new addDiskCacheTask(key, bitmap).execute();
        }else {
            try {
                String md5Key = hashKeyForDisk(key);
                if(getBitmapFromDiskCache(md5Key) == null){
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
        if(mDiskCacheStarting){
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
            File cacheDir = getDiskCacheDir(MyApplication.getContext(), DISK_CACHE_SUBDIR);
            if(mDiskCacheTask == null){
                mDiskCacheTask = new InitDiskCacheTask();
                mDiskCacheTask.execute(cacheDir);
            }

        }
    }


    public void flushDiskCache(){
        try {
            //用于将内存中的操作记录同步到日志文件（也就是journal文件）当中
            //DiskLruCache能够正常工作的前提就是要依赖于journal文件中的内容。
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

    public Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not visit network from UI thread.");
        }

        if(mDiskLruCache == null)
            return null;

        try {
            String key = hashKeyForDisk(url);
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if(editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                if(downloadUrlToStream(url, outputStream)){
                    editor.commit();
                }else {
                    editor.abort();
                }
                mDiskLruCache.flush();
            }
            return loadBitmapFromDiskFromDiskCache(url, reqWidth, reqHeight);
        }catch (IOException e) {
            return null;
        }

    }

    private Bitmap loadBitmapFromDiskFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if(Looper.myLooper() == Looper.getMainLooper()) {
            Log.w(TAG, "load bitmap from UI thread, it's not recommended.");
        }

        if(mDiskLruCache == null) {
            return null;
        }

        Bitmap bitmap = null;
        String key = hashKeyForDisk(url);
        DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
        FileInputStream fileInputStream = (FileInputStream)snapshot.getInputStream(0);
        FileDescriptor fd = fileInputStream.getFD();
        bitmap = decodeSampledBitmapFromFileDecscriptor(fd, reqWidth, reqHeight);
        if(bitmap != null) {
            addBitmapToMemoryCache(key, bitmap);
        }

        return bitmap;
    }

    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private   boolean downloadUrlToStream(String urlString, OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try {
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection)url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream(), IO_BUFFER_SIZE);
            out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
            int len;
            byte[] buf = new byte[1024];
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            return true;
        }catch (IOException e) {
            Log.e(TAG, "downloadBitmap failed." + e);
        }finally {
            if(urlConnection != null) {
                urlConnection.disconnect();
            }

            try {
                out.close();
            } catch (IOException e) { }

            try {
                in.close();
            } catch (IOException e) { }
        }
        return false;
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
                return BitmapFactory.decodeFile(path);
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inJustDecodeBounds = true; //不加载图片到内存，仅获得图片宽高
            BitmapFactory.decodeFile(path, options);//有时候会返回-1的宽高
            Logger.getLogger().e("original width: %d, original height: %d" , options.outWidth, options.outHeight);
            if(options.outWidth == -1 || options.outHeight == -1){
                try {
                    ExifInterface exifInterface = new ExifInterface(path);
                    int width = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, ExifInterface.ORIENTATION_NORMAL);//获取图片的宽度
                    int height = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.ORIENTATION_NORMAL);//获取图片高度
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


    public Bitmap decodeSampledBitmapFromFileDecscriptor(FileDescriptor fd, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    /**
     * 获取图片的旋转角度
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path){
        int degree = 0;
        try {
            //从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            //获取图片的旋转信息
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
     * 将图片按照指定的角度进行旋转
     * @param bitmap 需要旋转的图片
     * @param degree 指定的旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmaoDegree(Bitmap bitmap, int degree){
        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if(bitmap != null && !bitmap.isRecycled()){
            bitmap.recycle();
        }
        return  newBitmap;
    }

    //用特定的key将特定path的bitmap存入缓存
    public Bitmap addCacheData(String key, String path, int reqWidth, int reqHeight, boolean isAsync){

        Bitmap bitmap = null;
        if(getBitmapDegree(path) == 90){ //照片翻转了90度， 此时h和w 交换位置.
            bitmap = decodeSampledBitmapFromPath(path, reqHeight, reqWidth);
        }else {
            bitmap = decodeSampledBitmapFromPath(path, reqWidth, reqHeight);
        }

        //需检查是否旋转了角度
        if(getBitmapDegree(path) != 0){
            bitmap = rotateBitmaoDegree(bitmap, getBitmapDegree(path));
        }

        addBitmapCache(key, bitmap, isAsync);
        return bitmap;
    }

    /**
     *
     * @param path
     * @param reqWidth
     * @param reqHeight
     * @param isAsync
     * @return
     */
    public Bitmap addCacheData(String path, int reqWidth, int reqHeight, boolean isAsync){

        Bitmap bitmap = null;
        if(getBitmapDegree(path) == 90){ //照片翻转了90度， 此时h和w 交换位置.
            bitmap = decodeSampledBitmapFromPath(path, reqHeight, reqWidth);
        }else {
            bitmap = decodeSampledBitmapFromPath(path, reqWidth, reqHeight);
        }

        //需检查是否旋转了角度
        if(getBitmapDegree(path) != 0){
            bitmap = rotateBitmaoDegree(bitmap, getBitmapDegree(path));
        }

        addBitmapCache(path, bitmap, isAsync);
        return bitmap;
    }

    public Bitmap addCacheData(String path, int reqWidth, int reqHeight){
        return addCacheData(path, reqWidth, reqHeight, true);
    }

    /**
     * @param path
     * @param bitmap
     * Async 是否需要后台处理, 根据具体的上层逻辑调用
     */
    public void addBitmapCache(String path, Bitmap bitmap, boolean isAsync){
        addBitmapToMemoryCache(path, bitmap); //加入内存缓存
        synchronized (mDiskCacheLock){ //加入磁盘缓存
            addBitmapToDiskCache(path, bitmap, isAsync);
        }
    }

    public Bitmap getBitmapFormCache(String path){
        Bitmap bitmap = getBitmapFromMemCache(path); //memory cache
        if(bitmap == null){
            bitmap = getBitmapFromDiskCache(path); //disk cache
        }
        return bitmap;
    }
}
