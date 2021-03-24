package com.zj.danmaku.drawer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AnimationUtils;


public class DrawerSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final DrawThread mDrawThread;

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

    private BaseDrawer curDrawer;
    private float curDrawerAlpha = 0f;
    private int mWidth, mHeight;
    private Object currentKey;

    private void init() {
        curDrawerAlpha = 0f;
        final SurfaceHolder surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        mDrawThread.start();
    }

    public void setDrawer(Object key, BaseDrawer drawer) {
        if (key != currentKey) {
            currentKey = key;
            setDrawer(drawer);
            setZOrderOnTop(true);
        }
    }

    private void setDrawer(BaseDrawer drawer) {
        if (drawer == null) {
            return;
        }
        curDrawerAlpha = 0f;
        this.curDrawer = drawer;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
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
        }
        if (curDrawerAlpha < 1f) {
            curDrawerAlpha += 0.04f;
            if (curDrawerAlpha > 1) {
                curDrawerAlpha = 1f;
            }
        }
    }

    public void onResume() {
        if (mDrawThread.mRunning) return;
        synchronized (mDrawThread) {
            mDrawThread.mRunning = true;
            mDrawThread.notify();
        }
    }

    public void onPause() {
        if (!mDrawThread.mRunning) return;
        mDrawThread.mRunning = false;
        synchronized (mDrawThread) {
            mDrawThread.notify();
        }
    }

    public void onDestroy() {
        mDrawThread.mQuit = true;
        synchronized (mDrawThread) {
            mDrawThread.notify();
        }
        curDrawer.idleAllHolders();
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
        synchronized (mDrawThread) {
            mDrawThread.mSurface = holder;
            mDrawThread.notify();
            while (mDrawThread.mActive) {
                try {
                    mDrawThread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        holder.removeCallback(this);
    }

    private class DrawThread extends Thread {
        SurfaceHolder mSurface;
        boolean mRunning;
        boolean mActive;
        boolean mQuit;

        @Override
        public void run() {
            while (true) {
                synchronized (this) {
                    while (mSurface == null || !mRunning) {
                        if (mActive) {
                            mActive = false;
                            notify();
                        }
                        if (mQuit) {
                            return;
                        }
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!mActive) {
                        mActive = true;
                        notify();
                    }
                    final long startTime = AnimationUtils.currentAnimationTimeMillis();
                    Canvas canvas = mSurface.lockCanvas();
                    if (canvas != null) {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        drawSurface(canvas);
                        mSurface.unlockCanvasAndPost(canvas);
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
}
