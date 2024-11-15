package com.milanac007.demo.videocropdemo.ui;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.milanac007.demo.videocropdemo.R;

import androidx.appcompat.widget.AppCompatTextView;


public class
VoiceSendButton extends AppCompatTextView {
	public static final String TAG = VoiceSendButton.class.getName();
	private boolean mIsCancel = false;
	private int mYpositon = -100;
	private RecordListener listener;
	protected String textOn;
	private String textOff;
	private String textCancel;
	protected Context mContext;

	public VoiceSendButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		init();
	}

	public VoiceSendButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VoiceSendButton(Context context) {
		this(context, null, 0);
	}

	protected void init() {
		textOn = mContext.getString(R.string.m_press_speak);
		textOff = mContext.getString(R.string.m_loosen_end);
		textCancel = mContext.getString(R.string.m_send_cancel);
	}

	public RecordListener getListener() {
		return listener;
	}

	public void setListener(RecordListener listener) {
		this.listener = listener;
	}

	protected void setBackgroundByAction(int action){
		if(action == MotionEvent.ACTION_DOWN){
			setBackgroundResource(R.drawable.search_click_bg);
		}else if(action == MotionEvent.ACTION_UP){
			setBackgroundResource(R.drawable.search_bg);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (MotionEvent.ACTION_MASK & event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Log.e(TAG, "onTouchEvent ACTION_DOWN: getY = "+ event.getY());
			setText(textOff);
			setBackgroundByAction(MotionEvent.ACTION_DOWN);
			if (listener != null) {
				listener.onStartRecord();
			}
			break;

		case MotionEvent.ACTION_UP:
			Log.e(TAG, "onTouchEvent ACTION_UP: getY = "+ event.getY());
			setText(textOn);
			setBackgroundByAction(MotionEvent.ACTION_UP);
			if (listener != null) {
				if (mIsCancel) {
					listener.onCancelRecord();
				} else {
					listener.onFinishRecord();

				}
			}
			break;

		case MotionEvent.ACTION_MOVE:

			Log.e(TAG, "onTouchEvent ACTION_MOVE: getY = "+ event.getY());
			if (event.getY() < mYpositon) {
				mIsCancel = true;
				if (listener != null) {
					listener.onMoveLayout(mIsCancel);
					setText(textCancel);
				}
			} else {
				mIsCancel = false;
				if (listener != null) {
					listener.onMoveLayout(mIsCancel);
					setText(textOff);
				}
			}
			break;
		case MotionEvent.ACTION_CANCEL:{
			Log.e(TAG, "cencel!!!");
			timeoutMotionActionUp();
		}break;
		default:
			break;
		}
		return true;
	}

	//发送ACTION_UP事件
	public void timeoutMotionActionUp(){
		int[] location = new int[2];
		this.getLocationInWindow(location);

		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis() + 100;
		float x = location[0] + 5;
		float y = location[1] + 5;

		int metaSate = 0;
		MotionEvent motionEvent = MotionEvent.obtain(downTime, eventTime,MotionEvent.ACTION_UP, x, y, metaSate);
		dispatchTouchEvent(motionEvent);
	}

	public interface RecordListener {

		void onStartRecord();

		void onFinishRecord();

		void onCancelRecord();

		void onMoveLayout(boolean isInLayout);
	}
}
