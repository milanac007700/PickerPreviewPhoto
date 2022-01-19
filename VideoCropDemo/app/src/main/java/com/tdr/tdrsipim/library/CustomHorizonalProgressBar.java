package com.tdr.tdrsipim.library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import test.milanac007.com.videocropdemo.R;


/**
 * Created by zqguo on 2017/1/18.
 */
public class CustomHorizonalProgressBar extends View{

    private Paint mProgressPaint; //进度条
    private Paint mRemovePaint; //删除进度条
    private int mMax; //最长时长
    private int mProgress; //进度
    private boolean isRemove; //是否删除

    public CustomHorizonalProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        mProgressPaint = new Paint();
        mRemovePaint = new Paint();
        setBackgroundColor(getResources().getColor(R.color.transparent));
        mProgressPaint.setColor(Color.GREEN);
        mProgressPaint.setStyle(Paint.Style.FILL);
        mProgressPaint.setAntiAlias(true); //消除锯齿
        mRemovePaint.setColor(Color.RED);
        mRemovePaint.setStyle(Paint.Style.FILL);
        mRemovePaint.setAntiAlias(true); //消除锯齿
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        float progressLength = mProgress/(mMax * 1.0f)*(width/2);
        canvas.drawRect(progressLength, 0, width-progressLength, height, isRemove? mRemovePaint:mProgressPaint);
//        try {
//            canvas.restore();
//        }catch (IllegalStateException e){
//            e.printStackTrace();
//        }
    }

    public void setMax(int max){
        this.mMax = max;
    }

    public void setProgress(int progress){
        this.mProgress = progress;
        invalidate();
    }

    public void setRemove(boolean isRemove){
        this.isRemove = isRemove;
        invalidate();
    }

}
