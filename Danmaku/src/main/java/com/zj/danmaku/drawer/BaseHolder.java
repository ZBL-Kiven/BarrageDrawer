package com.zj.danmaku.drawer;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;


@SuppressWarnings("unused")
public abstract class BaseHolder<T> {

    private boolean isIdleState = true;
    public final int position;
    public Context context;
    public T bindData = null;

    public abstract void updateFrame(Canvas canvas, int width, int height, float changedAlpha);

    public boolean onTouchEvent(@NonNull DrawerSurfaceView v, @NonNull MotionEvent event) {
        return true;
    }

    final boolean isInitialized() {
        return !isIdleState;
    }

    final boolean isIdle() {
        return isIdleState;
    }

    public boolean isDrawInTopLayer() {
        return false;
    }

    public void bindData(T data) {
        this.bindData = data;
        this.isIdleState = false;
    }

    public BaseHolder(int position) {
        this.position = position;
    }

    @CallSuper
    public void initData(Context context) {
        this.context = context;
    }

    @CallSuper
    public void destroyAndIdle() {
        bindData = null;
        context = null;
        isIdleState = true;
    }
}
