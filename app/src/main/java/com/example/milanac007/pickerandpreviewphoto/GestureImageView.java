package com.example.milanac007.pickerandpreviewphoto;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;

/**
 * Created by milanac007 on 2017/2/11.
 */
public class GestureImageView extends androidx.appcompat.widget.AppCompatImageView {
    public static final String TAG = "GestureImageView";
    private Context mContext = null;
    private ScaleType mScaleType = ScaleType.FIT_CENTER;

    private final Matrix mBaseMatrix = new Matrix();
    private final Matrix mSuppMatrix = new Matrix();
    private final Matrix mDrawMatrix = new Matrix();

    private final float[] mMatrixValues = new float[9];

    public static final float DEFAULT_MIN_SCALE = 1.0f;
    public static final float DEFAULT_MID_SCALE = 1.75f;
    public static final float DEFAULT_MAX_SCALE = 3.0f;

    public static final int DEFAULT_ZOOM_DURATION = 200;
    private int ZOOM_DURATION = DEFAULT_ZOOM_DURATION;
    static final Interpolator sInterpolator = new AccelerateDecelerateInterpolator();

    private GestureDetector mGestureDetector = null;
    private  DragAndScaleGestureDetector mDragAndScaleGestureDetector = null;

    private float mMinScale = DEFAULT_MIN_SCALE;
    private float mMidScale = DEFAULT_MID_SCALE;
    private float mMaxScale = DEFAULT_MAX_SCALE;

    public float getMinimumScale() {
        return mMinScale;
    }

    public float getMidiumScale() {
        return mMidScale;
    }

    public float getMaximumScale() {
        return mMaxScale;
    }


    public GestureImageView(Context context) {
        this(context, null, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        mContext = context;
        setDrawingCacheEnabled(true);

        mGestureDetector = new GestureDetector(mContext, new DefaultOnGestureTabListener());
        mDragAndScaleGestureDetector = new DragAndScaleGestureDetector(mContext, new DefaultOnDragAndScaleGestureListener());

        setScaleType(this.getScaleType());
        setImageViewScaleTypeMatrix();

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateBaseMatrix();
            }
        });

        initMaskRect();
    }

    private void checkImageViewScaleType() {
        if (!ScaleType.MATRIX.equals(getScaleType())) {
            throw new IllegalStateException("The ImageView's ScaleType has been changed!");
        }
    }

    /**
     * 将ImageView的ScaleType类型设置为Matrix
     */
    private void setImageViewScaleTypeMatrix() {
        if (!ScaleType.MATRIX.equals(this.getScaleType())) {
            super.setScaleType(ScaleType.MATRIX);
        }
    }

    /**
     * 保存初始的ScaleType
     * @param scaleType
     */
    public void setScaleType(ScaleType scaleType){
        if(isSupportedScaleType(scaleType) && mScaleType != scaleType){
            mScaleType = scaleType;
        }
    }

    /**
     * 检查设置的ScaleType是否符合要求
     * @param scaleType
     * @return
     */
    private static boolean isSupportedScaleType(final ScaleType scaleType) {
        if (null == scaleType) {
            return false;
        }

        switch (scaleType) {
            case MATRIX:
                throw new IllegalArgumentException(scaleType.name()
                        + " is not supported in GestureImageView");

            default:
                return true;
        }
    }

    private int getImageViewWidth(){
        return this.getWidth() - this.getPaddingLeft() - this.getPaddingRight();
    }

    private int getImageViewHeight(){
        return this.getHeight() - this.getPaddingTop() - this.getPaddingBottom();
    }


    private float getValue(Matrix matrix, int whichValue){
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    public float getScale(){
        float mscale_x = getValue(mSuppMatrix, Matrix.MSCALE_X);
        float mskew_y = getValue(mSuppMatrix, Matrix.MSKEW_Y);
        return (float) Math.sqrt((float) Math.pow(getValue(mSuppMatrix, Matrix.MSCALE_X), 2) + (float) Math.pow(getValue(mSuppMatrix, Matrix.MSKEW_Y), 2));
    }

    private class DefaultOnGestureTabListener extends GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onDoubleTap(MotionEvent e) {
//            Toast.makeText(mContext,"双击事件", Toast.LENGTH_SHORT).show();
            float x = e.getX();
            float y = e.getY();
            float scale = getScale();
            if (scale >= getMinimumScale() && scale < getMidiumScale()) {
                scale = getMidiumScale();
            } else if (scale >= getMidiumScale() && scale < getMaximumScale()) {
                scale = getMaximumScale();
            } else {
                scale = getMinimumScale();
            }
            setScale(scale, x, y, true);
            return true;
        }

        //有bug ，暂未解决
//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            Toast.makeText(mContext,"快速滑动事件", Toast.LENGTH_SHORT).show();
//            mCurrentFlingRunnable = new FlingRunnable(getContext());
//            mCurrentFlingRunnable.fling((int)(-velocityX), (int)(-velocityY));
//            post(mCurrentFlingRunnable);
//            return true;
//        }
    };

    FlingRunnable mCurrentFlingRunnable;

    private void cancelFling() {
        if (null != mCurrentFlingRunnable) {
            mCurrentFlingRunnable.cancelFling();
            mCurrentFlingRunnable = null;
        }
    }

    private class AnimatedZoomRunnable implements Runnable {
        float fromScale, toScale, focalX, focalY;
        long startTime;

        public AnimatedZoomRunnable(float fromScale, float toScale, float focalX, float focalY) {
            this.fromScale = fromScale;
            this.toScale = toScale;
            this.focalX = focalX;
            this.focalY = focalY;
            startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            float t = interpolate();
            float scale = fromScale + (toScale - fromScale) * t;
            float deltaScale = scale / getScale();

            mSuppMatrix.postScale(deltaScale, deltaScale, focalX, focalY);
            checkAndDisplayMatrix();

            if (t < 1f) {
                post(this);
            }
        }

        private float interpolate() {
            float t = 1f * (System.currentTimeMillis() - startTime) / ZOOM_DURATION;
            t = Math.min(1f, t);
            t = sInterpolator.getInterpolation(t);
            return t;
        }

    }

    private class FlingRunnable implements Runnable{
        Scroller scroller;
        int currentX, currentY;

        public FlingRunnable(Context context){
            scroller = new Scroller(context);
        }

        public void fling(int velocityX, int velocityY){
            final RectF rect = getDisplayRect();
            if(rect == null)
                return;

            final int viewWidth = getImageViewWidth();
            final int viewHeight = getImageViewHeight();

            final int minX, maxX, minY, maxY;
            final int startX = Math.round(-rect.left);
            if (viewWidth < rect.width()) {
                minX = 0;
                maxX = Math.round(rect.width() - viewWidth);
            } else {
                minX = maxX = startX;
            }

            final int startY = Math.round(-rect.top);
            if (viewHeight < rect.height()) {
                minY = 0;
                maxY = Math.round(rect.height() - viewHeight);
            } else {
                minY = maxY = startY;
            }

            currentX = startX;
            currentY = startY;

            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY);

        }

        public void cancelFling(){
            scroller.forceFinished(true);
        }

        @Override
        public void run() {
            if(scroller.isFinished())
                return;

            if(scroller.computeScrollOffset()){
                final int newX = scroller.getCurrX();
                final int newY = scroller.getCurrY();

                mSuppMatrix.postTranslate(currentX-newX, currentY-newY);
                checkAndDisplayMatrix();

                currentX = newX;
                currentX = newY;

                postDelayed(this, 10);
            }
        }
    };

    public void setScale(float scale, float focalX, float focalY, boolean animate) {
        if (animate) {
            post(new AnimatedZoomRunnable(getScale(), scale, focalX, focalY));
        } else {
            mSuppMatrix.setScale(scale, scale, focalX, focalY);
            checkAndDisplayMatrix();
        }
    }


    /**
     * getDrawMatrix显示就是让mBaseMatrix和mSuppMatrix合并起来，这个matrix就是最终要设置到ImageView上的matrix值。这个matrix值
     * 是改变的原始的图片，通过matirx.mapRect()方法，正好可以得到原始图片经过matrix变换后的新的位置。根据这个位置，我们就可以
     * 判断我们的图片是不是在合适的位置了。注意这里只是做了这样一个变换，并没有真的将matrix应用到ImageView上去。checkMatrixBounds方法
     * 剩下的部分，也就只是一个判断了，注意最后将这个修正后的距离post到了mSuppMatrix上。这时候，这个mSuppMatrix就可以设置到ImageView
     * 上而不会出问题了。
     * @return
     */
    private Matrix getDrawMatrix(){
        mDrawMatrix.set(mBaseMatrix);
        mDrawMatrix.postConcat(mSuppMatrix);
        return mDrawMatrix;
    }

    private void checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageMatrix(getDrawMatrix());
        }
    }

    /**
     * Gets the Display Rectangle of the currently displayed Drawable. The Rectangle is relative to
     * this View and includes all scaling and translations.
     *
     * @return - RectF of Displayed Drawable
     */
    public RectF getDisplayRect() {
        checkMatrixBounds();
        return getDisplayRect(getDrawMatrix());
    }

    /**
     * 那我们怎样知道在调用checkMatrixBounds()方法之前，图片如果变化后的位置呢？这就是getDisplayRect()方法的作用了。
     * @param matrix
     * @return
     */
    private RectF getDisplayRect(Matrix matrix){
        if(matrix == null)
            return null;

        Drawable d = getDrawable();
        if(d == null)
            return null;

        RectF r = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(r);
        return r;
    }

    /**
     * checkMatrixBounds()。这个方法是在应用手势变化后调用，比如某个手势操作要让图片移动一段距离，在mSuppMatrix应用了这个变化距离后，
     * 在将它设置到ImageView之前调用，如果这个移动距离使图片的边缘出现在了控件中间，我们就需要移回去，
     * 即让mSuppMatrix调用一次postTranslate方法。
     * @return
     */
    private boolean checkMatrixBounds() {
        RectF rect = getDisplayRect(getDrawMatrix());
        if (rect == null) {
            return false;
        }
        final float width = rect.width(), height = rect.height();
        float deltaX = 0, deltaY = 0;

        final int viewHeight = getImageViewHeight();
        if (height <= viewHeight) {
            switch (mScaleType) {
                case FIT_START:
                    deltaY = -rect.top;
                    break;

                case FIT_END:
                    deltaY = viewHeight - rect.bottom;
                    break;

                default:
                    deltaY = (viewHeight - height) / 2F - rect.top;
            }
        } else {
            if (rect.top > 0) {
                deltaY = -rect.top;
            } else if (rect.bottom < viewHeight) {
                deltaY = viewHeight - rect.bottom;
            }
        }

        final int viewWidth = getImageViewWidth();
        if (width <= viewWidth) {
            switch (mScaleType) {
                case FIT_START:
                    deltaX = -rect.left;
                    break;

                case FIT_END:
                    deltaX = viewWidth - rect.right;
                    break;

                default:
                    deltaX = (viewWidth - width) / 2F - rect.left;
                    break;
            }
        } else {
            if (rect.left > 0) {
                deltaX = -rect.left;
            } else if (rect.right < viewWidth) {
                deltaX = viewWidth - rect.right;
            }
        }

        mSuppMatrix.postTranslate(deltaX, deltaY);

        return true;
    }

    private void updateBaseMatrix() {
        Drawable d = getDrawable();

        if (d == null) {
            return;
        }

        final int viewWidth = getImageViewWidth();
        final int viewHeight = getImageViewHeight();
        final int drawableWidth = d.getIntrinsicWidth();
        final int drawableHeight = d.getIntrinsicHeight();

        mBaseMatrix.reset();

        final float widthScale = 1.0f * viewWidth / drawableWidth;
        final float heightScale = 1.0f * viewHeight / drawableHeight;

        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate((viewWidth - drawableWidth) / 2f, (viewHeight - drawableHeight) / 2f);
        } else if (mScaleType == ScaleType.CENTER_CROP) {
            float scale = Math.max(widthScale, heightScale);
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f);
        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            float scale = Math.min(widthScale, heightScale);
            mBaseMatrix.postScale(scale, scale);
            mBaseMatrix.postTranslate((viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f);
        } else {
            RectF tempSrc = new RectF(0, 0, drawableWidth, drawableHeight);
            RectF tempDst = new RectF(0, 0, viewWidth, viewHeight);
            switch (mScaleType) {
                case FIT_CENTER:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);
                    break;

                case FIT_START:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.START);
                    break;

                case FIT_END:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.END);
                    break;

                case FIT_XY:
                    mBaseMatrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.FILL);
                    break;
            }
        }

        resetMatrix();
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays it.s
     */
    private void resetMatrix() {
        mSuppMatrix.reset();
        setImageMatrix(getDrawMatrix());
        checkMatrixBounds();
    }

    public void setImageMatrix(Matrix matrix) {
        checkImageViewScaleType();
        super.setImageMatrix(matrix);
    }

    public interface OnDragAndScaleGestureListener {
         void onDrag(float dx, float dy);

         void onScale(float scaleFactor, float focusX, float focusY);
    }

    public class DragAndScaleGestureDetector implements ScaleGestureDetector.OnScaleGestureListener{
        ScaleGestureDetector mScaleGestureDetector;
        OnDragAndScaleGestureListener  mListener;
        int mTouchSlop;
        boolean mIsDragging;
        float mLastMotionX, mLastMotionY;



        public DragAndScaleGestureDetector(Context context, OnDragAndScaleGestureListener listener) {
            mScaleGestureDetector = new ScaleGestureDetector(context, this);
            mListener = listener;
            mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mListener != null) {
                mListener.onScale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }

        public boolean onTouchEvent(MotionEvent e) {
            mScaleGestureDetector.onTouchEvent(e);

            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mIsDragging = false;
                    mLastMotionX = e.getX();
                    mLastMotionY = e.getY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    float x = e.getX();
                    float y = e.getY();
                    float dx = x - mLastMotionX, dy = y - mLastMotionY;
                    if (!mIsDragging) {
                        mIsDragging = Math.hypot(dx, dy) >= mTouchSlop;//计算三角形的斜边长
                    }
                    if (mIsDragging) {
                        mLastMotionX = x;
                        mLastMotionY = y;
                        if (mListener != null) {
                            mListener.onDrag(dx, dy);
                        }
                    }
                    break;
            }

            return true;
        }
    };


    private class DefaultOnDragAndScaleGestureListener implements OnDragAndScaleGestureListener {

        @Override
        public void onDrag(float dx, float dy) {
//            Toast.makeText(mContext,"滑动事件", Toast.LENGTH_SHORT).show();
            if(getScale() <= mMinScale){
                mBaseMatrix.postTranslate(dx, dy);
                setImageMatrix(getDrawMatrix());
            }else {
                mSuppMatrix.postTranslate(dx, dy);
                checkAndDisplayMatrix();
            }

        }

        @Override
        public void onScale(float scaleFactor, float focusX, float focusY) {
//            Toast.makeText(mContext,"缩放事件", Toast.LENGTH_SHORT).show();
            mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);

            float scale = getScale();
            if(scale > mMaxScale){ //如果大于mMaxScale 则设置为最大
                mSuppMatrix.setScale(mMaxScale, mMaxScale);
            }

            checkAndDisplayMatrix();
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                cancelFling();
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
//                RectF rect = getDisplayRect();
//                final float minScale = getMinimumScale();
//                if (getScale() < minScale) {
//                    setScale(minScale, rect.centerX(), rect.centerY(), true);
//                }
                break;
        }

        mDragAndScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    private Paint mPaint;
    private  int maskColor;
    private  int frameColor;
    private int ScreenW = CommonFunction.getWidthPx();
    private int ScreenH = CommonFunction.getHeightPx();
    private Rect mMaskRect;

    private void initMaskRect(){
        mPaint = new Paint();
        Resources resources = getResources();
        maskColor = resources.getColor(com.example.milanac007.pickerandpreviewphoto.R.color.viewfinder_mask);
        frameColor = resources.getColor(com.example.milanac007.pickerandpreviewphoto.R.color.viewfinder_frame);
        mMaskRect = new Rect(Math.max(ScreenW/2 - ScreenH/4, 0), ScreenH/4, Math.min(ScreenW/2 + ScreenH/4, ScreenW), ScreenH * 3/4);
    }

    public Bitmap outputMaskBitmap(){
        Bitmap bitmap = this.getDrawingCache();
        if(bitmap == null)
            return null;

        return Bitmap.createBitmap(bitmap, mMaskRect.left, mMaskRect.top, mMaskRect.width(), mMaskRect.height());
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

}
