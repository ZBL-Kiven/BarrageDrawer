package com.zj.danmaku.drawer;

import android.content.Context;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import java.util.List;


@SuppressWarnings("unused")
public abstract class BaseDrawer {

    protected Context context;
    protected int width, height;
    protected List<HoldersInfo<?>> holders;

    public BaseDrawer(Context context) {
        this.context = context;
    }

    public abstract List<HoldersInfo<?>> getHolders();

    private void initData() {
        if (holders != null) for (HoldersInfo<?> info : holders) {
            info.initHolders(context);
        }
    }

    void draw(Canvas canvas, float alpha) {
        if (holders != null) if (canDraw(canvas, alpha)) {
            for (HoldersInfo<?> info : holders) {
                info.upDateHoldersValue(canvas, width, height, alpha);
            }
        }
    }

    public boolean canDraw(Canvas canvas, float alpha) {
        return true;
    }

    void setSize(int width, int height) {
        boolean isSameWidth = this.width == width;
        boolean isSameHeight = this.height == height;
        if (!isSameWidth || !isSameHeight) {
            this.width = width;
            this.height = height;
            setHolders();
        }
    }

    protected int convertAlphaColor(float percent, final int originalColor) {
        int newAlpha = (int) (percent * 255) & 0xFF;
        return (newAlpha << 24) | (originalColor & 0xFFFFFF);
    }

    private void setHolders() {
        holders = getHolders();
        initData();
    }

    protected int getDisplayWidth(boolean inVertical) {
        DisplayMetrics dpm = context.getResources().getDisplayMetrics();
        return inVertical ? dpm.widthPixels : dpm.heightPixels;
    }

    protected float dp2px(float value) {
        return value * context.getResources().getDisplayMetrics().density + 0.5f;
    }

    void idleAllHolders() {
        if (holders != null) for (HoldersInfo<?> info : holders) {
            info.idleAllHolders();
        }
    }
}
