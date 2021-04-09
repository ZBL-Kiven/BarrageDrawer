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

    protected final DrawThread mDrawThread;
    protected RemoveFormParentListener removeFormParentListener;

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

    protected BaseDrawer preDrawer, curDrawer;
    protected float curDrawerAlpha = 0f;
    protected int mWidth, mHeight;
    protected Object currentKey;

    private void init() {
        curDrawerAlpha = 0f;
        final SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        mDrawThread.start();
    }

    public void setDrawer(Object key, BaseDrawer drawer) {
        if (key != currentKey && drawer != null) {
            currentKey = key;
            setDrawer(drawer);
        }
    }

    private void setDrawer(BaseDrawer drawer) {
        if (preDrawer != null) {
            preDrawer.idleAllHolders();
            preDrawer = null;
        }
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
            curDrawerAlpha += 0.05f;
            if (curDrawerAlpha >= 1) {
                curDrawerAlpha = 1f;
                if (preDrawer != null) preDrawer.idleAllHolders();
                preDrawer = null;
                if (curDrawer == null) {
                    canvas.restore();
                    onPause();
                }
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

    public void stop() {
        currentKey = "";
        if (curDrawer != null) {
            preDrawer = curDrawer;
        }
        curDrawer = null;
        curDrawerAlpha = 0f;
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
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mDrawThread.mRunning.set(false);
        synchronized (mDrawThread) {
            mDrawThread.mSurface = holder;
            mDrawThread.notify();
        }
        holder.removeCallback(this);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null || curDrawer == null) return true;
        return curDrawer.onTouchEvent(this, event) || super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        setZOrderOnTop(true);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        super.onAttachedToWindow();
        if (mDrawThread.mQuit.get()) {
            init();
        }
    }

    private class DrawThread extends Thread {
        SurfaceHolder mSurface;
        AtomicBoolean mRunning = new AtomicBoolean(false);
        AtomicBoolean mQuit = new AtomicBoolean(false);

        private void release() {
            synchronized (this) {
                try {
                    if (mSurface != null) {
                        Surface surface = mSurface.getSurface();
                        if (surface != null) {
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
        }

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    if (mSurface == null || mQuit.get() || !mRunning.get()) {
                        if (mQuit.get()) {
                            release();
                            return;
                        }
                        try {
                            this.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    final long startTime = AnimationUtils.currentAnimationTimeMillis();
                    Canvas canvas = null;
                    try {
                        canvas = mSurface.lockCanvas();
                        if (canvas != null) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            drawSurface(canvas);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (canvas != null) try {
                            mSurface.unlockCanvasAndPost(canvas);
                        } catch (IllegalStateException ie) {
                            ie.printStackTrace();
                        }
                    }
                    final long drawTime = AnimationUtils.currentAnimationTimeMillis() - startTime;
                    long needSleepTime = Math.max(1L, 16L - drawTime);
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

    public interface RemoveFormParentListener {
        void onRemoved();
    }
}
