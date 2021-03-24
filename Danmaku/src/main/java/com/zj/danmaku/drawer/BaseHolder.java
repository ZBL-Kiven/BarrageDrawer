package com.zj.danmaku.drawer;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.CallSuper;


public abstract class BaseHolder<T> {

    private boolean isIdleState = true;
    protected T bindData = null;
    protected final int position;
    protected Context context;

    boolean isInitialized() {
        return !isIdleState;
    }

    boolean isIdle() {
        return isIdleState;
    }

    public BaseHolder(int position) {
        this.position = position;
    }

    @CallSuper
    public void initData(Context context, T data) {
        this.bindData = data;
        this.context = context;
        this.isIdleState = false;
    }

    public abstract void updateFrame(Canvas canvas, int width, int height, float changedAlpha);

    @CallSuper
    protected void destroyAndIdle() {
        bindData = null;
        context = null;
        isIdleState = true;
    }
}
