package com.ripple.core;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import static com.ripple.core.RippleDrawable.*;

/**
 * Created by musk on 2017/8/16.
 */

public class RippleView extends View {

    private RippleDrawable mRippleDrawable;

    public RippleView(Context context) {
        this(context, null);
    }

    public RippleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RippleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获取属性
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ripple);
        int rippleColor = ta.getColor(R.styleable.ripple_rippleColor, DEFAULT_COLOR);
        //int radiusPercent = ta.getFraction(R.styleable.ripple_rippleRadiusPercent, DEFAULT_COLOR);

        //设置属性
        mRippleDrawable = new RippleDrawable();
        mRippleDrawable.setRippleColor(rippleColor);
        mRippleDrawable.setRadiusPercent(0.9f);
        mRippleDrawable.setCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mRippleDrawable.draw(canvas);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mRippleDrawable.onTouch(event);
        return true;
    }

    //---------解决View.verifyDrawable(Drawable)问题-------------------------
    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mRippleDrawable || super.verifyDrawable(who);
    }

    //---------解决Drawable.getDirtyBounds()==ZERO_RECT零区域问题----------
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRippleDrawable.setBounds(0, 0, getWidth(), getHeight());
    }
}