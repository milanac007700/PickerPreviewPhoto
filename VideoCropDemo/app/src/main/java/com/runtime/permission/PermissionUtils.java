package com.runtime.permission;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by zqguo on 2017/10/18.
 */

public class PermissionUtils{

    private static final String TAG = PermissionUtils.class.getName();
    public static final int CODE_MULTI_PERMISSIONS = 10000; //批量申请权限, 需满足 requestCode >= CODE_MULTI_PERMISSIONS,否则是单个权限申请

    public static final String PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO; //microphone
    public static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    public static final String PERMISSION_READ_CONTACTS = Manifest.permission.READ_CONTACTS; //通讯录
    public static final String PERMISSION_BLUETOOTH = Manifest.permission.BLUETOOTH;
    public static final String PERMISSION_ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION; //位置
    public static final String PERMISSION_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE; //存储
    public static final String PERMISSION_SEND_SMS = Manifest.permission.SEND_SMS; //发送短信
    public static final String PERMISSION_READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE; //读取手机状态信息
    private static final String PACKAGE_URL_SCHEME = "package:";

    public static final String STRING_HELP_TEXT =
            "    当前应用缺少必要权限。\n" +
                    "    请点击\"设置\"-\"权限\"-打开所需权限。最后点击两次后退按钮，即可返回。";

    private static final String[] requestPermissions = {
            PERMISSION_RECORD_AUDIO,
            PERMISSION_CAMERA,
            PERMISSION_READ_CONTACTS,
            PERMISSION_BLUETOOTH,
            PERMISSION_ACCESS_FINE_LOCATION,
            PERMISSION_READ_EXTERNAL_STORAGE,
            PERMISSION_SEND_SMS,
            PERMISSION_READ_PHONE_STATE
    };


    private static Map<String, String> tipPermissionMap = new HashMap<String, String>(){
        {
            put(PERMISSION_RECORD_AUDIO, "麦克风");
            put(PERMISSION_CAMERA, "相机");
            put(PERMISSION_READ_CONTACTS, "通讯录");
            put(PERMISSION_BLUETOOTH, "蓝牙");
            put(PERMISSION_ACCESS_FINE_LOCATION, "位置");
            put(PERMISSION_READ_EXTERNAL_STORAGE, "存储");
            put(PERMISSION_SEND_SMS, "短信");
            put(PERMISSION_READ_PHONE_STATE, "手机状态");
        }

    };

    //判断权限集合
    public static boolean lacksPermission(Context context, String... permissions){
        for(String permission : permissions){
            if(lacksPermission(context, permission))
                return true;
        }
        return false;
    }

    //判断是否缺少权限
    public static boolean lacksPermission(Context context, String permission){
        try {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED;
        } catch (RuntimeException e) {
            Toast.makeText(context, "please open this permission", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "RuntimeException:" + e.getMessage());
            return true;
        }
    }

    public interface PermissionGrantCallback{
        void onPermissionGranted(int requestCode);
        void onPermissionDenied(int requestCode);
        void onError();
    }

    //申请单个权限
    public static void requestPermission(final Activity activity, final int requestCode, final String requestPermission, PermissionGrantCallback callback){
        if(activity == null || requestCode < 0 ){
            callback.onError();
            return;
        }

        Log.i(TAG,  "requestPermission requestCode:" + requestCode + ", requestPermission:" + requestPermission);
        boolean isFind = false;
        for(String permission : PermissionUtils.requestPermissions){
            if(permission.equalsIgnoreCase(requestPermission)){
                isFind = true;
                break;
            }
        }

        if(!isFind){
            callback.onError();
            return;
        }

        /**
         * 如何6.0以下的手机，ActivityCompat.checkSelfPermission()会始终返回PERMISSION_GRANTED,但如果用户关闭了你申请的权限，
         * 执行该函数会导致程序崩溃(java.lang.RuntimeException: Unknown exception code: 1 msg null)，可以用try...catch...处理
         * 异常，也可以判断系统版本，低于23就不申请权限，直接返回callback.onPermissionGranted(requestCode)
         */
//        if(Build.VERSION.SDK_INT < 23){
//            callback.onPermissionDenied(requestCode);
//            return;
//        }
        int checkSelfPermission;
        try{
            checkSelfPermission = ActivityCompat.checkSelfPermission(activity, requestPermission);
        }catch (RuntimeException e){
            Toast.makeText(activity, "please open this permission\n" + requestPermission, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "RuntimeException: " + e.getMessage());
            callback.onError();
            return;
        }

        if(checkSelfPermission != PackageManager.PERMISSION_GRANTED){
            Log.i(TAG, "ActivityCompat.checkSelfPermission != PackageManager.PERMISSION_GRANTED");

            if(ActivityCompat.shouldShowRequestPermissionRationale(activity, requestPermission)){
                Log.i(TAG, String.format("requestPermission %s shouldShowRequestPermissionRationale", requestPermission));
                shouldShowRationale(activity, requestCode, new String[]{requestPermission}, callback);
            }else {
                ActivityCompat.requestPermissions(activity, new String[]{requestPermission}, requestCode);
            }

        }else {
            Log.d(TAG, "opened:" + requestPermission);
            callback.onPermissionGranted(requestCode);
        }
    }

    /**
     *
     * @param activity
     * @param isShouldRationale true: return no granted and shouldShowRequestPermissionRationale permissions,
     *                          false:return no granted and !shouldShowRequestPermissionRationale
     * @return
     */
    public static ArrayList<String> getNoGrantedPermission(Activity activity, String[] requestPermissions, boolean isShouldRationale){

        ArrayList<String> permissions = new ArrayList<>();
        for(int i=0; i<requestPermissions.length; i++){
            String requestPermission = requestPermissions[i];

            int checkSelfPermission = -1;
            try {
                checkSelfPermission = ActivityCompat.checkSelfPermission(activity, requestPermission);
            }catch (RuntimeException e){
                Toast.makeText(activity, "please open those permission", Toast.LENGTH_SHORT)
                        .show();
                Log.e(TAG, "RuntimeException:" + e.getMessage());
                return null;
            }

            if(checkSelfPermission != PackageManager.PERMISSION_GRANTED){
                Log.i(TAG, "getNoGrantedPermission ActivityCompat.checkSelfPermission != PackageManager.PERMISSION_GRANTED:" + requestPermission);
                if(ActivityCompat.shouldShowRequestPermissionRationale(activity, requestPermission)){
                    if(isShouldRationale){
                        permissions.add(requestPermission);
                    }
                }else {
                    if(!isShouldRationale){
                        permissions.add(requestPermission);
                    }
                }
            }
        }
        return permissions;
    }

    //批量申请权限
    public static void requestMultiPermissions(final Activity activity, final int requestCode, String[] requestPermissions, final PermissionGrantCallback callback){
        final List<String> permissionList = getNoGrantedPermission(activity, requestPermissions, false);
        final List<String> shouldRationalePermissionList = getNoGrantedPermission(activity, requestPermissions, true);
        final List<String> totalRequestPermissionList = new ArrayList<>();
        totalRequestPermissionList.addAll(permissionList);
        totalRequestPermissionList.addAll(shouldRationalePermissionList);

        if (permissionList == null || shouldRationalePermissionList == null || requestCode < CODE_MULTI_PERMISSIONS) {
            return;
        }
        Log.d(TAG, "requestMultiPermissions permissionsList:" + permissionList.size() +
                ",shouldRationalePermissionsList:" + shouldRationalePermissionList.size());


        if (permissionList.size() > 0 && shouldRationalePermissionList.size() <= 0) {
            ActivityCompat.requestPermissions(activity, permissionList.toArray(new String[permissionList.size()]), requestCode);
            Log.d(TAG, "showMessageOKCancel requestPermissions");

        } else if (shouldRationalePermissionList.size() > 0) {
            shouldShowRationale(activity, requestCode, totalRequestPermissionList.toArray(new String[totalRequestPermissionList.size()]), callback);
        } else {
            callback.onPermissionGranted(requestCode);
        }

    }


    public static void onRequestPermissionsResult(final Activity activity, final int requestCode, @NonNull String[] permissions,
                                                  @NonNull int[] grantResults, final PermissionGrantCallback callback) {
        if(activity == null) {
            callback.onError();
            return;
        }

        Log.d(TAG, "requestPermissionsResult requestCode:" + requestCode);

        if(requestCode >= CODE_MULTI_PERMISSIONS){
            requestMultiResult(activity, requestCode, permissions, grantResults, callback);
            return;
        }

        if(requestCode < 0){
            Log.w(TAG, "requestPermissionsResult illegal requestCode:" + requestCode);
            Toast.makeText(activity, "illegal requestCode:" + requestCode, Toast.LENGTH_SHORT).show();
            callback.onError();
            return;
        }

        Log.i(TAG, "onRequestPermissionsResult requestCode:" + requestCode + ",permissions:" + permissions.toString()
                + ",grantResults:" + grantResults.toString() + ",length:" + grantResults.length);

        if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            callback.onPermissionGranted(requestCode);
        }else {
            //TODO
            showMessageOKCancel(activity, STRING_HELP_TEXT, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startAppSettings(activity);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onPermissionDenied(requestCode);
                }
            });
        }
    }

    private static void requestMultiResult(final Activity activity, final int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults, final PermissionGrantCallback callback){
        if(activity == null || requestCode < CODE_MULTI_PERMISSIONS){
            callback.onError();
            return;
        }
        ArrayList<String> notGranted = new ArrayList<>();
        Log.d(TAG, "onRequestPermissionsResult permissions length:" + permissions.length);
        for(int i=0; i<permissions.length; i++){
            Log.d(TAG, "permissions: [i]:" + i + ", permissions[i]" + permissions[i] + ",grantResults[i]:" + grantResults[i]);
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                notGranted.add(permissions[i]);
            }
        }

        if(notGranted.size() == 0){
            callback.onPermissionGranted(requestCode);
        }else {
            showMessageOKCancel(activity, STRING_HELP_TEXT, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startAppSettings(activity);
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    callback.onPermissionDenied(requestCode);
                }
            });
        }
    }

    private static void shouldShowRationale(final Activity activity, final int requestCode, final String[] requestPermisssions, final PermissionGrantCallback callback){

        String tip = "";
        for(int i=0, size = requestPermisssions.length; i<size; i++){
            String tipPermission = requestPermisssions[i];
            tip += tipPermissionMap.get(tipPermission);
            if(size>1 && i < size -1){
                tip += "/";
            }
        }

        String tipStr = String.format("使用该功能需要以下权限：\n%s\n请开启该权限。 ",tip);
        showMessageOKCancel(activity, tipStr, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(activity, requestPermisssions, requestCode);
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onPermissionDenied(requestCode);
            }
        });
    }

    private static void showMessageOKCancel(final Activity context, String message, DialogInterface.OnClickListener listener){
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .create()
                .show();
    }

    private static void showMessageOKCancel(final Activity context, String message,
                                            DialogInterface.OnClickListener okCallback, DialogInterface.OnClickListener cancelCallback){
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", okCallback)
                .setNegativeButton("Cancel", cancelCallback)
                .setCancelable(false)
                .create()
                .show();
    }

    //启动应用的设置页面
    private static void startAppSettings(final Activity activity){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + activity.getPackageName()));
        activity.startActivity(intent);
    }
}
