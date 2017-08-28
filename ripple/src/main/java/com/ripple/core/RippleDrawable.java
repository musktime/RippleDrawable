package com.ripple.core;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by musk on 2017/8/16.
 */

public class RippleDrawable extends Drawable {

    private static final int MAX_ALPHA_BG = 172;
    private static final int MAX_ALPHA_CIRCLE = 255;
    public static final int DEFAULT_COLOR = 0x66000000;
    //元素
    private Paint mPaint;
    //背景透明度，圆形透明度
    private int mBgAlpha = 0, mCircleAlpha = MAX_ALPHA_CIRCLE;
    //涟漪颜色、透明度
    private int mRippleColor, mAlpha = 255;
    private float mRadiusPercent = 0.7f;

    //涟漪坐标、半径
    private float mRipplePointX, mRipplePointY, mRippleRadius = 0;
    //标记用户手指是否抬起
    private boolean mTouchRelease;

    //按下的坐标
    private float mDownX, mDownY;
    //中心点坐标
    private float mCenterX, mCenterY;
    //开始和结束的半径
    private float mStartRadius, mEndRadius;

    public RippleDrawable() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);
    }

    //---------------------------必须覆写的方法--------------------
    @Override
    public void draw(Canvas canvas) {
        int preAlpha = mPaint.getAlpha();
        int bgAlpha = (int) (preAlpha * (mBgAlpha / 255f));
        int maxCircleAlpha = getCircleAlpha(preAlpha, bgAlpha);
        int circleAlpha = (int) (maxCircleAlpha * (mBgAlpha / 255f));

        //绘制背景
        mPaint.setAlpha(bgAlpha);
        canvas.drawColor(mPaint.getAlpha());
        //绘制圆形
        mPaint.setAlpha(circleAlpha);
        canvas.drawCircle(mRipplePointX, mRipplePointY, mRippleRadius, mPaint);
        //恢复原来透明度
        mPaint.setAlpha(preAlpha);
    }

    //设置透明度值
    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        onAlphaOrColorChange();
    }

    //设置颜色过滤器
    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (mPaint.getColorFilter() != colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }
        invalidateSelf();
    }

    //返回不透明度
    @Override
    public int getOpacity() {
        int alpha = mPaint.getAlpha();
        if (alpha == 255) {
            return PixelFormat.OPAQUE;//不透明
        } else if (alpha == 0) {
            return PixelFormat.TRANSPARENT;//全透明
        } else {
            return PixelFormat.TRANSLUCENT;//半透明
        }
    }

    //-----------------------额外覆写的方法---------------------
    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mCenterX = bounds.centerX();
        mCenterY = bounds.centerY();
        //　获取最大半径
        float maxRadius = Math.max(mCenterX, mCenterY);
        mStartRadius = maxRadius * 0f;
        mEndRadius = maxRadius * mRadiusPercent;
    }

    //-------------------------对外提供-------------------
    //设置涟漪颜色
    public void setRippleColor(int color) {
        mRippleColor = color;
        onAlphaOrColorChange();
    }

    /**
     * 设置涟漪半径[0-1]
     */
    public void setRadiusPercent(float radiusPercent) {
        mRadiusPercent = radiusPercent;
    }

    private void onAlphaOrColorChange() {
        mPaint.setColor(mRippleColor);
        int pAlpha = mPaint.getAlpha();
        int realAlpha = (int) (pAlpha * (mAlpha / 255f));
        mPaint.setAlpha(realAlpha);
        invalidateSelf();
    }


    public void onTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_CANCEL:
                touchCancele(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_UP:
                touchUp(event.getX(), event.getY());
                break;
            default:
                break;
        }
    }

    private void touchDown(float x, float y) {
        mDownX = x;
        mDownY = y;
        //按下没抬起
        mTouchRelease = false;
        startEnterRunnable();
    }

    private void touchMove(float x, float y) {
    }

    private void touchCancele(float x, float y) {
        //用户手指抬起
        mTouchRelease = true;
        //开始退出动画
        if (mEnterDone) {
            startExitRunnable();
        }
    }

    private void touchUp(float x, float y) {
        //用户手指抬起
        mTouchRelease = true;
        //开始退出动画
        if (mEnterDone) {
            startExitRunnable();
        }
    }


    //----------------------------进入动画----------------------------
    private boolean mEnterDone;

    private void startEnterRunnable() {
        mCircleAlpha = MAX_ALPHA_CIRCLE;

        mEnterProgress = 0;
        mEnterDone = false;
        unscheduleSelf(mExitRunnable);
        unscheduleSelf(mEnterRunnable);
        //注入runnable到队列
        scheduleSelf(mEnterRunnable, SystemClock.uptimeMillis());
    }

    //进入动画速率值
    private float mEnterProgress = 0;
    //进入动画递增值
    private float mEnterIncrement = 16f / 360;
    //插值器：由快到慢
    private Interpolator mEnterInterpolator = new DecelerateInterpolator(1.6f);
    private Runnable mEnterRunnable = new Runnable() {
        @Override
        public void run() {
            mEnterProgress = mEnterProgress + mEnterIncrement;
            if (mEnterProgress > 1) {
                onEnterProgressChange(1);
                onEnterDone();
                return;
            }

            float realPorgress = mEnterInterpolator.getInterpolation(mEnterProgress);
            onEnterProgressChange(realPorgress);
            //帧率60f/s:1000/60
            scheduleSelf(this, SystemClock.uptimeMillis() + 16);
        }
    };

    /**
     * 进入动画完成时触发
     */
    private void onEnterDone() {
        MLog.i("===onEnterDone===");
        mEnterDone = true;
        if (mTouchRelease) {
            startExitRunnable();
        }
    }

    /**
     * 进入动画刷新
     *
     * @param progress
     */
    private void onEnterProgressChange(float progress) {
        mRippleRadius = computeProgressValue(mStartRadius, mEndRadius, progress);
        mRipplePointX = computeProgressValue(mDownX, mCenterX, progress);
        mRipplePointY = computeProgressValue(mDownY, mCenterY, progress);

        mBgAlpha = (int) computeProgressValue(0, MAX_ALPHA_BG, progress);
        mCircleAlpha = (int) computeProgressValue(0, MAX_ALPHA_CIRCLE, progress);
        invalidateSelf();
    }

    //------------------------------退出动画------------------------
    private void startExitRunnable() {
        //重置
        MLog.i("startExitRunnable:" + mCircleAlpha);
        mExitProgress = 0;
        unscheduleSelf(mEnterRunnable);
        unscheduleSelf(mExitRunnable);
        //注入runnable到队列
        scheduleSelf(mExitRunnable, SystemClock.uptimeMillis());
    }

    //退出动画速率
    private float mExitProgress = 0;
    //退出动画递增值
    private float mExitIncrement = 16f / 360;
    //插值器：由慢到快
    private Interpolator mExitInterpolator = new AccelerateInterpolator(2f);
    private Runnable mExitRunnable = new Runnable() {
        @Override
        public void run() {
            if (!mEnterDone) {
                return;
            }

            mExitProgress += mExitIncrement;
            if (mExitProgress > 1) {
                onExitProgressChange(1);
                onExitDone();
                return;
            }

            float realPorgress = mExitInterpolator.getInterpolation(mExitProgress);
            onExitProgressChange(realPorgress);
            //帧率60f/s:1000/60
            scheduleSelf(this, SystemClock.uptimeMillis() + 16);
        }
    };

    //退出动画完成时触发
    private void onExitDone() {
        MLog.i("===onExitDone===");
    }

    //退出动画刷新
    private void onExitProgressChange(float progress) {
        //背景减淡
        mBgAlpha = (int) computeProgressValue(MAX_ALPHA_BG, 0, progress);
        //圆形减淡
        mCircleAlpha = (int) computeProgressValue(MAX_ALPHA_CIRCLE, 0, progress);
        invalidateSelf();
    }

    //-----------------------------计算方法---------------------
    //按进度计算递增到某值
    private float computeProgressValue(float start, float end, float progress) {
        return start + (end - start) * (progress);
    }

    /**
     * 通过两块玻璃叠加后颜色加深，光线透过更少的算法
     * 反向推出其中一块玻璃值的算法
     *
     * @param preAlpha Z 叠加后的值
     * @param bgAlpha  Y 其中一块玻璃的值
     * @return 返回另外一块玻璃的值
     */
    private int getCircleAlpha(int preAlpha, int bgAlpha) {
        int dAlpha = preAlpha - bgAlpha;
        return (int) ((dAlpha * 255f) / (255f - bgAlpha));
    }
}