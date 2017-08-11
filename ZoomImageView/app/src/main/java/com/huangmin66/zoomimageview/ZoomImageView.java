package com.huangmin66.zoomimageview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * 作者：Administrator on 2017/8/9 10:41
 * 描述：
 */

public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener
        , ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private boolean mOnce;
    //初始化时缩放的值
    private float mInitScale;
    //放大的最大值
    private float mMaxScale;
    //双击放大值到达的值
    private float mMidScale;

    private Matrix mScaleMatrix;

    //捕获用户多指触控时缩放的比例
    private ScaleGestureDetector mScaleGestureDetector;

    //记录上一次多点触控的数量
    private int mLastPointerCount;

    private float mLastX;
    private float mLastY;

    //系统判断控件移动的标准值
    private int mTouchSlop;
    //是否可以拖拽
    private boolean isCanDrag;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    private GestureDetector mGestureDetector;
    private boolean isAutoScale;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //init
        mScaleMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);

        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);

        //getScaledTouchSlop是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件
        // 。如果小于这个距离就不触发移动控件，如viewpager就是用这个距离来判断用户是否翻页
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                if (isAutoScale) return true;

                float x = e.getX();
                float y = e.getY();

                if (getScale() < mMidScale){
//                    mScaleMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
//                    setImageMatrix(mScaleMatrix);

                    postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
                } else {
//                    mScaleMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
//                    setImageMatrix(mScaleMatrix);

                    postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
                }

                isAutoScale = true;

                return true;
            }
        });
    }

    /**
     * 自动放大与缩小
     */
    private class AutoScaleRunnable implements Runnable{

        //目标缩放值
        private float mTargetScale;
        private float mTmpScale;
        //缩放中心点
        private float x;
        private float y;
        //放大缩小系数
        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if (getScale() < mTargetScale){
                mTmpScale = BIGGER;
            }

            if (getScale() > mTargetScale){
                mTmpScale = SMALL;
            }
        }

        @Override
        public void run() {

            //进行缩放
            mScaleMatrix.postScale(mTmpScale, mTmpScale, x, y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

            float currentScale = getScale();

            if ((mTmpScale > 1.0f && currentScale < mTargetScale)
                    || (mTmpScale < 1.0f && currentScale > mTargetScale)){
                postDelayed(this, 16);
            } else {

                float scale = mTargetScale / currentScale;
                mScaleMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);
                isAutoScale = false;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     * 获取ImageView加载完成的图片
     */
    @Override
    public void onGlobalLayout() {
        if (!mOnce) {
            //得到控件的宽和高
            int width = getWidth();
            int height = getHeight();

            //得到图片宽高
            Drawable d = getDrawable();
            if (d == null) return;
            int dw = d.getIntrinsicWidth();
            int dh = d.getIntrinsicHeight();

            float scale = 1.0f;

            //缩小宽度
            if (dw > width && dh < height) {
                scale = width * 1.0f / dw;
            }

            //缩小高度
            if (dw < width && dh > height) {
                scale = height * 1.0f / dh;
            }

            //都要缩小
            if ((dw > width && dh > height) || (dw < width && dh < height)) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }

            /**
             * 得到初始化时缩放的比例
             */
            mInitScale = scale;
            mMidScale = mInitScale * 2;
            mMaxScale = mInitScale * 4;

            //将图片移动要控件中心
            int dx = width / 2 - dw / 2;
            int dy = height / 2 - dh / 2;

            mScaleMatrix.postTranslate(dx, dy);
            mScaleMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }

    /**
     * 获取当前图片缩放的值
     *
     * @return
     */
    public float getScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        //缩放进行时
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor(); //图片缩放的值

        if (getDrawable() == null) {
            return true;
        }

        //缩放范围的控制
        if ((scale < mMaxScale && scaleFactor > 1.0f)
                || (scale > mInitScale && scaleFactor < 1.0f)) {

            if (scale * scaleFactor < mInitScale) {
                //不允许比缩小值比初始值还小
                scaleFactor = mInitScale / scale;
            }

            if (scale * scaleFactor > mMaxScale) {
                //不允许比缩小值比最大值还大
                scaleFactor = mMaxScale / scale;
            }

            //缩放
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            checkBorderAndCenterWhenScale();

            setImageMatrix(mScaleMatrix);
        }

        return true;
    }

    /**
     * 获取图片放大缩小后的宽和高
     *
     * @return
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable d = getDrawable();
        if (d != null) {
            rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            matrix.mapRect(rectF); //用matrix改变rect的4个顶点的坐标，并将改变后的坐标调整后存储到rect当中
        }

        return rectF;
    }

    /**
     * 在缩放的时候进行边界的控制 和 位置的控制 防止出现白边 图片不居中
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rect = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        //控制水平方向
        if (rect.width() >= width) {
            //rect.left = 图片左边坐标 - 屏幕左边坐标
            if (rect.left > 0) {
                //说明图片超出屏幕左边
                deltaX = -rect.left;
            }

            if (rect.right < width) {
                //说明图片和屏幕右边有间隙
                deltaX = width - rect.right;
            }
        }

        //控制垂直方向
        if (rect.height() >= height) {
            if (rect.top > 0) {
                deltaY = -rect.top;
            }

            if (rect.bottom < width) {
                deltaY = height - rect.bottom;
            }
        }

        //如果宽度或者高度小于控件的宽或者高 则让其居中
        if (rect.width() < width) {
            deltaX = width / 2f - rect.right + rect.width() / 2f;
        }
        if (rect.height() < height) {
            deltaY = height / 2f - rect.bottom + rect.height() / 2f;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    /**
     * 在平移的时候进行边界的控制 和 位置的控制 防止出现白边 图片不居中
     */
    private void checkBorderAndCenterWhenTranslate() {
        RectF rect = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        //控制水平方向
        if (rect.left > 0 && isCheckLeftAndRight) {
            deltaX = -rect.left;
        }

        if (rect.right < width && isCheckLeftAndRight) {
            deltaX = width - rect.right;
        }

        //控制垂直方向
        if (rect.top > 0 && isCheckTopAndBottom) {
            deltaY = -rect.top;
        }

        if (rect.bottom < height && isCheckTopAndBottom) {
            deltaY = height - rect.bottom;
        }


        //如果宽度或者高度小于控件的宽或者高 则让其居中
        if (rect.width() < width) {
            deltaX = width / 2f - rect.right + rect.width() / 2f;
        }
        if (rect.height() < height) {
            deltaY = height / 2f - rect.bottom + rect.height() / 2f;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        //缩放开始时
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        //缩放结束时
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //传递操作事件
        if (mGestureDetector.onTouchEvent(event)) return true;
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;
        //拿到多点触控的数量
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        x /= pointerCount;
        y /= pointerCount;

        if (mLastPointerCount != pointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;

        RectF rectF = getMatrixRectF();
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01){
                    if (getParent() instanceof ViewPager)
                    //请求父控件不拦截
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }

                if (isCanDrag) {
                    if (getDrawable() != null) {

                        isCheckLeftAndRight = isCheckTopAndBottom = true;

                        //如果图片宽度小于控件宽度，不允许水平移动
                        if (rectF.width() <= getWidth()) {
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        //如果图片高度小于控件高度，不允许垂直移动
                        if (rectF.height() <= getHeight()) {
                            isCheckLeftAndRight = false;
                            dy = 0;
                        }

                        mScaleMatrix.postTranslate(dx, dy);
                        checkBorderAndCenterWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }
                }

                mLastX = x;
                mLastY = y;

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mLastPointerCount = 0;
                break;

            case MotionEvent.ACTION_DOWN:
                if (rectF.width() > getWidth() + 0.01 || rectF.height() > getHeight() + 0.01){
                    if (getParent() instanceof ViewPager)
                    //请求父控件不拦截
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
        }

        return true;
    }

    /**
     * 判断是否是move
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }
}
