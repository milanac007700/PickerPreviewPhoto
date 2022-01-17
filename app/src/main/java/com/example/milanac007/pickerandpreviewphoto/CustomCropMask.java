package com.example.milanac007.pickerandpreviewphoto;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import uk.co.senab.photoview.PhotoView;

/**头像截取的遮罩
 * Created by zqguo on 2016/12/28.
 */
public class CustomCropMask extends PhotoView{

    public static final String TAG = "CustomCropMask";
    private Paint mPaint;
    private  int maskColor;
    private  int frameColor;
    private int ScreenW = CommonFunction.getWidthPx();
    private int ScreenH = CommonFunction.getHeightPx();
    private Rect mMaskRect;

    private void init(){
        setDrawingCacheEnabled(true);

        mPaint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(com.example.milanac007.pickerandpreviewphoto.R.color.viewfinder_mask);
        frameColor = resources.getColor(com.example.milanac007.pickerandpreviewphoto.R.color.viewfinder_frame);

        mMaskRect = new Rect(ScreenW/2-ScreenH/4, ScreenH/4, ScreenW/2+ScreenH/4, ScreenH*3/4);
    }

    public CustomCropMask(Context context) {
        super(context);
        init();
    }

    public CustomCropMask(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomCropMask(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setColor(frameColor);
        mPaint.setStyle(Paint.Style.STROKE);//设置画笔的填充方式为无填充、仅仅是画线
        mPaint.setStrokeWidth(2);

        canvas.drawRect(mMaskRect, mPaint);

        mPaint.reset();
        mPaint.setColor(Color.TRANSPARENT);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(mMaskRect.left+2, mMaskRect.top+2, mMaskRect.right-4, mMaskRect.bottom-4, mPaint);

        mPaint.reset();
        mPaint.setColor(maskColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, CommonFunction.getWidthPx(), mMaskRect.top, mPaint);
        canvas.drawRect(0, mMaskRect.top, mMaskRect.left, mMaskRect.bottom, mPaint);
        canvas.drawRect(mMaskRect.right, mMaskRect.top, CommonFunction.getWidthPx(), mMaskRect.bottom, mPaint);
        canvas.drawRect(0, mMaskRect.bottom, CommonFunction.getWidthPx(), CommonFunction.getHeightPx(), mPaint);
    }

    private boolean startDrag = false;
    private int mLastX = 0;
    private int mLastY = 0;

    private int mLeft = 0;
    private int mRight = 0;
    private int mTop = 0;
    private int mBottom = 0;


    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        final int action = motionEvent.getAction();
        Log.d(TAG, "action: " + String.valueOf(action & MotionEvent.ACTION_MASK));
        switch (action & MotionEvent.ACTION_MASK){

            case MotionEvent.ACTION_DOWN:
            {
                if(mMaskRect.contains((int)motionEvent.getX(), (int)motionEvent.getY())){
                    startDrag = true;
                    mLastX = (int)motionEvent.getX();
                    mLastY = (int)motionEvent.getY();
                    mLeft = mMaskRect.left;
                    mRight = mMaskRect.right;
                    mTop = mMaskRect.top;
                    mBottom = mMaskRect.bottom;
                }else {
                    startDrag = false;
                    return false;
                }
            }break;
            case MotionEvent.ACTION_MOVE:{
                int mX = (int)motionEvent.getX() - mLastX;
                int mY = (int)motionEvent.getY()  - mLastY;

                if(startDrag){
                    mMaskRect.left =  mLeft+mX;
                    mMaskRect.right = mRight + mX;

                    mMaskRect.top = mTop+ mY;
                    mMaskRect.bottom = mBottom + mY;

                    invalidate();
                }else {
                    return false;
                }

            }break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                startDrag = false;
                return false;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:{
                startDrag = false;
            }break;
            default: break;
        }

        return true;
    }

    public Bitmap outputMaskBitmap(){
        Bitmap bitmap = this.getDrawingCache();
        if(bitmap == null)
            return null;

        return Bitmap.createBitmap(bitmap, mMaskRect.left, mMaskRect.top, mMaskRect.width(), mMaskRect.height());
    }

}
