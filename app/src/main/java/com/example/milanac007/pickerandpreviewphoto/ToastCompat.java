package com.example.milanac007.pickerandpreviewphoto;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.milanac007.demo.videocropdemo.library.HandlerPost;

import java.lang.reflect.Field;


public class ToastCompat {
    private static Field sField_TN;
    private static Field sField_TN_Handler;
    private static Toast mToast;

    static {
        try {
            sField_TN = Toast.class.getDeclaredField("mTN");
            sField_TN.setAccessible(true);
            sField_TN_Handler = sField_TN.getType().getDeclaredField("mHandler");
            sField_TN_Handler.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void hook(Toast toast) {
        try {
            Object tn = sField_TN.get(toast);
            Handler tnHandler = (Handler) sField_TN_Handler.get(tn);
            sField_TN_Handler.set(tn, new HandlerProxy(tnHandler));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Toast getToast(CharSequence str){

//        if(mToast == null) {
//            mToast = Toast.makeText(App.getContext(), str, Toast.LENGTH_SHORT);
//        }else {
//            mToast.setDuration(Toast.LENGTH_SHORT);
//            mToast.setText(str);
//        }

        //暂时解决快速调用该方法，有些版本的手机(Vivo Android 7.11)会不显示toast的bug
        if(mToast != null) {
            mToast.cancel();
            mToast = null;
        }

        //解决小米手机 toast带应用名称的问题
//        mToast = Toast.makeText(App.getContext(), str, Toast.LENGTH_SHORT);
        mToast = Toast.makeText(MyApplication.getContext(), "", Toast.LENGTH_SHORT);
        mToast.setText(str);
        hook(mToast);
        return mToast;
    }

    public void showToast(final CharSequence msg) {

        if(Thread.currentThread() != Looper.getMainLooper().getThread()){
            new HandlerPost(0, true){
                @Override
                public void doAction() {
                    getToast(msg).show();
                }
            };
        }else {
            getToast(msg).show();
        }

        mToast.show();
    }

}
