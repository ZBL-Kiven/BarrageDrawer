package com.zj.danmaku.drawer;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public abstract class HoldersInfo<T> {

    public HoldersInfo(int maxCount) {
        if (holders == null) {
            holders = new ArrayList<>();
        } else {
            holders.clear();
        }
        for (int i = 0; i < maxCount; i++) {
            holders.add(getHolderType(i));
        }
    }

    private List<BaseHolder<T>> holders;

    @NonNull
    public abstract BaseHolder<T> getHolderType(int position);

    @NonNull
    public abstract T getHolderData(@NonNull BaseHolder<T> holder);

    void upDateHoldersValue(Canvas canvas, int width, int height, float changedAlpha) {
        if (holders == null || width <= 0 || height <= 0) return;
        for (BaseHolder<T> holder : holders) {
            if (holder.isInitialized()) holder.updateFrame(canvas, width, height, changedAlpha);
        }
    }

    void initHolders(Context context) {
        if (holders == null) return;
        for (BaseHolder<T> holder : holders) {
            T holderData = getHolderData(holder);
            if (holderData != null && holder.isIdle()) holder.initData(context, holderData);
        }
    }

    void idleAllHolders() {
        if (holders == null) return;
        for (BaseHolder<T> holder : holders) {
            if (holder.isInitialized()) holder.destroyAndIdle();
        }
    }
}
