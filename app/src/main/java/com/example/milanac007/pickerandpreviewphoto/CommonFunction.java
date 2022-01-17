package com.example.milanac007.pickerandpreviewphoto;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zqguo on 2016/9/14.
 */
public class CommonFunction {

    public static final String APPLICATION_PACKAGE_TAG = "CommonFunction";


    public static boolean isStringEmpty(Object obj) {
		boolean ret = false;

		if (obj == null || obj.toString().trim().isEmpty() || obj.toString().length() == 0) {
			ret = true;
		}
		return ret;
	}
    
    
    private static float scale = 0.0f; // 密度
    private static int widthPixels = 0;
    private static int heightPixels = 0;

    public static void init() {
        DisplayMetrics displaysMetrics = new DisplayMetrics();// 初始化一个结构
        WindowManager wm = (WindowManager) MyApplication.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displaysMetrics);// 对该结构赋值
        widthPixels = displaysMetrics.widthPixels;
        heightPixels = displaysMetrics.heightPixels;
        DisplayMetrics dm = MyApplication.getContext().getResources().getDisplayMetrics();
        scale = dm.density;
	}
    
    /**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(float dpValue) {
		if (scale == 0) {
			init();
		}
		return (int) (dpValue * scale + 0.5f);
	}

    /**
     * 得到的屏幕的宽度
     */
    public static int getWidthPx() {
        // DisplayMetrics 一个描述普通显示信息的结构，例如显示大小、密度、字体尺寸
        if (widthPixels == 0) {
            init();
        }
        return widthPixels;
    }

    /**
     * 得到的屏幕的高度
     */
    public static int getHeightPx() {
        // DisplayMetrics 一个描述普通显示信息的结构，例如显示大小、密度、字体尺寸
        if (heightPixels == 0) {
            init();
        }
        return heightPixels;
    }


    public static String getDir() {

        if (TextUtils.isEmpty(SDDir)) {
            initExtStorageDir();
        }

        String dir = SDDir + "/CustomCamera/";
        File f = new File(dir);
        if (!f.exists()) {
            boolean ret = f.mkdirs();
            Logger.getLogger().i("makedir ret=%s %s", ret?"true":"false", dir);
        }

        return dir;

    }

    private static String SDDir = null;

    protected static void initExtStorageDir() {
        String[] sfiles = { "/sdcard", "/storage/sdcard", "/storage/sdcard0", "/storage/sdcard1", "/storage/sdcard2", "/storage/sdcard-ext", "/storage/external_sd", "/storage/flash", "/storage/internal", "/storage/external", "/mnt/sdcard", "/mnt/sdcard0", "/mnt/sdcard1", "/mnt/sdcard2", "/mnt/sdcard-ext", "/mnt/external_sd", "/mnt/flash", "/mnt/internal", "/mnt/external" };
        for (String sfile : sfiles) {
            final File file = new File(sfile);
            if ((file != null) && file.isDirectory() && file.exists() && file.canWrite()) {
                File ffile = new File(sfile + "/CustomCamera");
                if (ffile.exists() && file.isDirectory()) {
                    if (ffile.canWrite()) {
                        SDDir = sfile;
                        return;
                    }
                } else {
                    boolean ret = ffile.mkdirs();
                    if (ret) {
                        SDDir = sfile;
                        return;
                    }
                }
            }
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/CustomCamera/");
            if (path.exists() && path.isDirectory()) {
                SDDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                return;
            } else {
                boolean ret = path.mkdirs();
                if (ret) {
                    SDDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                    return;
                }
            }

        }
    }


    private static String makeDirPath(String folder, String userUuid) {
        String dir = getDir();

        if (!TextUtils.isEmpty(userUuid)) {
            dir += userUuid;
            dir += "/";
        }

        dir += folder;
        dir += "/";

        File f = new File(dir);
        if (!f.exists()) {
            boolean ret = f.mkdirs();
            android.util.Log.i(APPLICATION_PACKAGE_TAG, "makedir ret=" + ret + dir);
        }

        return dir;
    }


    public static String getUserDBPath() {
        return makeDirPath("db", "oxmRwHPoQP1");
    }

    public static String getDirUserTemp() {
        return makeDirPath("temp", "oxmRwHPoQP1");
    }

    // 示例：http://file01.yugusoft.com/M00/00/7D/OkTuVVOwxrOAHsffAAAiIWbby2Q947.jpg
    // 根据url 截取文件名字 为OkTuVVOwxrOAHsffAAAiIWbby2Q947.jpg.
    public static String getImageFileNameByUrl(String imgUrl) {
        String[] strArr = imgUrl.split("/");

        if (strArr.length > 0) {
            String temp = strArr[strArr.length - 1];
            if (temp.contains(".")) {
                return temp;
            }
        }

        return imgUrl;
    }

    /**
     * 获取文件后缀名
     * @param file
     * @return
     */
    public static String getSuffix(File file){
        if(!file.exists())
            return null;

        String fileName=file.getName();
        String suffix = fileName.substring(fileName.lastIndexOf(".")+1);
        return suffix;
    }

    public static String getSuffix(String fileName){
        if(TextUtils.isEmpty(fileName))
            return null;

        String suffix=fileName.substring(fileName.lastIndexOf(".")+1);
        return suffix;
    }

    private static Toast mToast = null;

    public static final Toast showToast(int strId) {

        if(mToast == null){
            mToast = Toast.makeText(MyApplication.getContext(), strId, Toast.LENGTH_SHORT);
        }else {
            mToast.setText(strId);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }

        mToast.show();
        return mToast;
    }

    public static final Toast showToast(String msg) {

        if(mToast == null){
            mToast = Toast.makeText(MyApplication.getContext(), msg, Toast.LENGTH_SHORT);
        }else {
            mToast.setText(msg);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }

        mToast.show();
        return mToast;
    }

    public static final void hideToast(){
        if(mToast != null){
            mToast.cancel();
            mToast = null;
        }
    }


    //获取时位值
    public static int getHourOfDate(Date date){
        SimpleDateFormat format = new SimpleDateFormat("HH", Locale.getDefault());
        String hourStr= format.format(date);
        if(!isStringEmpty(hourStr)) {
            try {
                return Integer.parseInt(hourStr);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            return -1;
        }

        return -1;
    }

    //获取年份
    public static int getYearOfDate(Date date){
        SimpleDateFormat format = new SimpleDateFormat("yyyy", Locale.getDefault());
        String hourStr= format.format(date);
        if(!isStringEmpty(hourStr)) {
            try {
                return Integer.parseInt(hourStr);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            return -1;
        }

        return -1;
    }

    public static boolean isCurrentYear(Date cmpDate){
        int cmpYear = getYearOfDate(cmpDate);
        int curYear = getYearOfDate(new Date());

        return cmpYear == curYear;
    }

    //return true if the supplied when is today else false
    public static boolean isToday(long when){
        Time time = new Time();
        time.set(when);

        int whenYear = time.year;
        int whenMonth = time.month;
        int whenMonthDay = time.monthDay;

        time.set(System.currentTimeMillis());

        return (whenYear == time.year) && (whenMonth == time.month) &&(whenMonthDay == time.monthDay);
    }

    /**
     * Truncates a date to the date part alone.
     */
    @SuppressWarnings("deprecation")
    public static Date truncateToDate(Date d) {
        if (d instanceof java.sql.Date) {
            return d; // java.sql.Date is already truncated to date. And raises
            // an
            // Exception if we try to set hours, minutes or seconds.
        }

        d = (Date) d.clone();
        d.setHours(0);
        d.setMinutes(0);
        d.setSeconds(0);
        d.setTime(((d.getTime() / 1000) * 1000));
        return d;
    }

    /**
     * Returns the number of days between two dates. The time part of the days
     * is ignored in this calculation, so 2007-01-01 13:00 and 2007-01-02 05:00
     * have one day inbetween.
     */
    private final static long SECONDS_PER_DAY = 24 * 60 * 60;
    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String date2Str(Date d, String format){
        if(d == null)
            return "";

        if(isStringEmpty(format))
            format = FORMAT;

        SimpleDateFormat df = new SimpleDateFormat(format, Locale.getDefault());
        return df.format(d);
    }


    public static long daysBetween(Date firstDate, Date secondDate) {
        // We only use the date part of the given dates
        long firstSeconds = truncateToDate(firstDate).getTime() / 1000;
        long secondSeconds = truncateToDate(secondDate).getTime() / 1000;
        // Just taking the difference of the millis.
        // These will not be exactly multiples of 24*60*60, since there
        // might be daylight saving time somewhere inbetween. However, we can
        // say that by adding a half day and rounding down afterwards, we always
        // get the full days.
        long difference = secondSeconds - firstSeconds;
        // Adding half a day
        if (difference >= 0) {
            difference += SECONDS_PER_DAY / 2; // plus half a day in seconds
        } else {
            difference -= SECONDS_PER_DAY / 2; // minus half a day in seconds
        }
        // Rounding down to days
        difference /= SECONDS_PER_DAY;

        return difference;
    }


    public static String getDisplayTimeFormat(Date mDate){
        String ret = "";
        if(mDate == null)
            return ret;

        Date curDate = new Date();
        int curHour = getHourOfDate(curDate);

        if(isToday(mDate.getTime())){ //今天
            if(curHour >=0 && curHour <=6){
                ret = date2Str(mDate, "凌晨 HH:mm");
            }else if(curHour > 6 && curHour <= 11){
                ret = date2Str(mDate, "上午 HH:mm");
            }else if(curHour > 11 && curHour <= 17){
                ret = date2Str(mDate, "下午 HH:mm");
            }else if(curHour >17 && curHour <=24){
                ret = date2Str(mDate, "晚上 HH:mm");
            }
        }else if(daysBetween(curDate, mDate) == 1){ //昨天
            ret = date2Str(mDate, "昨天 HH:mm");
        }else { //昨天之前
            if(isCurrentYear(mDate)){
                ret = date2Str(mDate, "MM-dd HH:mm");
            }else {
                ret = date2Str(mDate, "yyyy-MM-dd HH:mm");
            }
        }

        return ret;
    }

    public static boolean isYestoday(Date mDate){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH)-1);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        long value1 =  c.getTime().getTime(); //前一天的23:59:59

        c.set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH)-1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        long value2 =  c.getTime().getTime();//前一天的0:0:0

        long mValue = mDate.getTime();
        if(mValue >= value1 && mValue <= value2)
            return true;
        else
            return false;

    }

    public static String getCallRecordDisplayTimeFormat(Date mDate){
        String ret = "";
        if(mDate == null)
            return ret;

        Date curDate = new Date();
        if(isToday(mDate.getTime())){ //今天
            ret = date2Str(mDate, "HH:mm");
        }else
//        if(daysBetween(curDate, mDate) == 1){ //昨天
        if(isYestoday(mDate)){ //昨天
            ret = date2Str(mDate, "昨天 HH:mm");
        }else { //昨天之前
            if(isCurrentYear(mDate)){
                ret = date2Str(mDate, "MM-dd HH:mm");
            }else {
                ret = date2Str(mDate, "yyyy-MM-dd HH:mm");
            }
        }

        return ret;
    }

    /**
     * 手机号正则
     */
    public static boolean isMobile(String mobileStr){
        Pattern pattern = Pattern.compile("^1(3[0-9]|4[57]|5[0-35-9]|8[0-9]|70)\\d{8}$");
        Matcher mather =  pattern.matcher(mobileStr);
        if(mather.find()){
            return true;
        }
        return false;
    }

    /**
     * 融意号
     * @param searchStr
     * @return
     */

    public static boolean isRongyihao(String searchStr){
        Pattern pattern = Pattern.compile("[a-zA-Z]+");
        Pattern pattern1 = Pattern.compile("[\\d]+");
        Matcher mather =  pattern.matcher(searchStr);
        Matcher matcher1 = pattern1.matcher(searchStr);
        if(searchStr.length() >= 8 && searchStr.length() <=16 && mather.find() && matcher1.find()){
            return true;
        }

        return false;
    }

    /**
     * 邮箱
     * @param searchStr
     * @return
     */
    public static boolean isEmail(String searchStr){
        Pattern pattern = Pattern.compile("^\\s*\\w+(?:\\.{0,1}[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$");
        Matcher mather =  pattern.matcher(searchStr);

        if(mather.find()){
            return true;
        }
        return false;
    }


    public static boolean isPasswordLegal(String mPassword){

        if(mPassword== null || mPassword.isEmpty()){
            return false;
        }

        Pattern pattern = Pattern.compile("[a-zA-Z]+");
        Pattern pattern1 = Pattern.compile("[\\d]+");
        Pattern pattern2 = Pattern.compile("[0-9A-Za-z]{8,16}");

        Matcher mather =  pattern.matcher(mPassword);
        Matcher matcher1 = pattern1.matcher(mPassword);
        Matcher matcher2 = pattern2.matcher(mPassword);

        if(mather.find() && matcher1.find() &&  matcher2.find()){
            return true;
        }

        return false;
    }

    public static String getNameFromUrl(String url){
        String[] strs = url.split("/");
        if(strs == null || strs.length ==0)
            return null;
        return strs[strs.length-1];
    }

    /**
     * 获取视频第一帧的缩略图
     * @param filePath
     * @return
     */
    public static Bitmap getVideoThumbnail(String filePath){
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }catch (RuntimeException e){
            e.printStackTrace();
        }finally {
            try{
                retriever.release();
            }catch (RuntimeException e){
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    public static String formatFileSize(long fileS) {// 转换文件大小
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS == 0) {
            fileSizeString = "0B";
        } else if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        }

        return fileSizeString;
    }

    public static String getVersion(Context context){
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    public static Object invoke(Object owner, String methodName, Object[] args) throws Exception {

        Class<?> ownerClass = owner.getClass();

        Class<?>[] argsClass = new Class<?>[args.length];

        for (int i = 0, len = args.length; i < len; i++) {
            argsClass[i] = args[i].getClass();
        }

        Method method = ownerClass.getMethod(methodName, argsClass);

        return method.invoke(owner, args);
    }

    /**
     * 全局显示等待框
     * @param context
     * @param msg
     * @return
     */
    private static ProgressDialog progressDialog = null;
    public static void showProgressDialog(Context context, String msg) {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(context);
        }

        progressDialog.setMessage(msg);
        progressDialog.setCancelable(false);

        try {
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void dismissProgressDialog() {
        if (progressDialog == null) {
            return;
        }
        try {
            progressDialog.dismiss();
            progressDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
