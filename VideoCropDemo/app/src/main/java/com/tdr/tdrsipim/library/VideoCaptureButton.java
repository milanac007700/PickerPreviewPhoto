package com.tdr.tdrsipim.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.tdr.tdrsipim.App;

import test.milanac007.com.videocropdemo.R;

/**
 * Created by zqguo on 2016/10/25.
 */
public class VideoCaptureButton extends VoiceSendButton {

    public VideoCaptureButton(Context context) {
        this(context, null, 0);
    }

    public VideoCaptureButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoCaptureButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    protected void init() {
        super.init();
        textOn = mContext.getString(R.string.m_press_video);
    }

    protected void setBackgroundByAction(int action){
        if(action == MotionEvent.ACTION_DOWN){
            setBackgroundResource(R.drawable.video_capture_button_press_style);
        }else if(action == MotionEvent.ACTION_UP){
            setBackgroundResource(R.drawable.video_capture_button_default_style);
        }
    }
}
