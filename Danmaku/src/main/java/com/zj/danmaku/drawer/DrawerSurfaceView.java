package com.zj.danmaku.drawer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import java.util.concurrent.atomic.AtomicBoolean;


@SuppressWarnings("unused")
public class DrawerSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final DrawThread mDrawThread;
    private RemoveFormParentListener removeFormParentListener;

    public DrawerSurfaceView(Context context) {
        this(context, null);
    }

    public DrawerSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawerSurfaceView(Context context, AttributeSet attrs, int def) {
        super(context, attrs, def);
        mDrawThread = new DrawThread();
        init();
    }

    private BaseDrawer preDrawer, curDrawer;
    private float curDrawerAlpha = 0f;
    private int mWidth, mHeight;
    private Object currentKey;

    private void init() {
        curDrawerAlpha = 0f;
        final SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        mDrawThread.start();
    }

    public void setDrawer(Object key, BaseDrawer drawer) {
        if (key != currentKey) {
            currentKey = key;
            setDrawer(drawer);
        }
    }

    private void setDrawer(BaseDrawer drawer) {
        curDrawerAlpha = 0f;
        if (this.curDrawer != null) {
            this.preDrawer = curDrawer;
        }
        this.curDrawer = drawer;
        post(this::onResume);
    }

    public void setOnRemoveFormParentListener(RemoveFormParentListener l) {
        this.removeFormParentListener = l;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mWidth = w;
        mHeight = h;
    }

    private void drawSurface(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        final int w = mWidth;
        final int h = mHeight;
        if (w == 0 || h == 0) {
            return;
        }
        if (curDrawer != null) {
            curDrawer.setSize(w, h);
            curDrawer.draw(canvas, curDrawerAlpha);
        } else {
            if (curDrawerAlpha >= 1f) onPause();
        }
        if (preDrawer != null && curDrawerAlpha < 1f) {
            preDrawer.setSize(w, h);
            preDrawer.draw(canvas, 1f - curDrawerAlpha);
        }
        if (curDrawerAlpha < 1f) {
            curDrawerAlpha += 0.04f;
            if (curDrawerAlpha > 1) {
                curDrawerAlpha = 1f;
                if (preDrawer != null) preDrawer.idleAllHolders();
                preDrawer = null;
            }
        }
    }

    public void onResume() {
        if (mDrawThread.mRunning.get()) return;
        synchronized (mDrawThread) {
            mDrawThread.mRunning.set(true);
            mDrawThread.notify();
        }
    }

    public void onPause() {
        if (!mDrawThread.mRunning.get()) return;
        mDrawThread.mRunning.set(false);
        synchronized (mDrawThread) {
            mDrawThread.notify();
        }
    }

    public void onDestroy() {
        if (curDrawer != null) curDrawer.idleAllHolders();
        mDrawThread.mQuit.set(true);
        synchronized (mDrawThread) {
            mDrawThread.notify();
        }
    }

    public void removeFormParent() {
        Object parent = getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(this);
        }
        if (removeFormParentListener != null) removeFormParentListener.onRemoved();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (mDrawThread) {
            mDrawThread.mSurface = holder;
            mDrawThread.notify();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (mDrawThread) {
            mDrawThread.mSurface = holder;
            mDrawThread.notify();
            while (mDrawThread.mActive.get()) {
                try {
                    mDrawThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        holder.removeCallback(this);
    }

    @Override
    protected void onAttachedToWindow() {
        setZOrderOnTop(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        super.onAttachedToWindow();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null || curDrawer == null) return true;
        return curDrawer.onTouchEvent(this, event) || super.onTouchEvent(event);
    }

    private class DrawThread extends Thread {
        SurfaceHolder mSurface;
        AtomicBoolean mRunning = new AtomicBoolean(false);
        AtomicBoolean mActive = new AtomicBoolean(false);
        AtomicBoolean mQuit = new AtomicBoolean(false);
        private Canvas canvas;

        private void release() {
            try {
                if (mSurface != null) {
                    Surface surface = mSurface.getSurface();
                    if (surface != null) {
                        if (canvas != null) surface.unlockCanvasAndPost(canvas);
                        surface.release();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mSurface = null;
                post(DrawerSurfaceView.this::removeFormParent);
            }
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    if (mQuit.get()) {
                        release();
                        return;
                    }
                    while (mSurface == null || !mSurface.getSurface().isValid() || getParent() == null || !mRunning.get()) {
                        if (mActive.get()) {
                            mActive.set(false);
                            notify();
                        }
                        if (mQuit.get()) {
                            release();
                            return;
                        }
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!mActive.get()) {
                        mActive.set(true);
                        notify();
                    }
                    final long startTime = AnimationUtils.currentAnimationTimeMillis();
                    if (mSurface.getSurface().isValid()) {
                        Canvas canvas = mSurface.lockCanvas();
                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            drawSurface(canvas);
                            if (mSurface.getSurface().isValid()) mSurface.unlockCanvasAndPost(canvas);
                        }
                    }
                    final long drawTime = AnimationUtils.currentAnimationTimeMillis() - startTime;
                    final long needSleepTime = 16 - drawTime;
                    if (needSleepTime > 0) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(needSleepTime);
                        } catch (InterruptedException | IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    interface RemoveFormParentListener {
        void onRemoved();
    }
}
