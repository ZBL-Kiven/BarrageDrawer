package com.zj.danmaku.drawer;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class HoldersInfo<H extends BaseHolder<T>, T> {

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

    private List<H> holders;

    @NonNull
    public abstract H getHolderType(int position);

    @Nullable
    public abstract T getHolderData(int width, int height, @NonNull H holder);

    public abstract void updateDrawers(int width, int height, @NonNull List<H> holders);

    void upDateHoldersValue(Canvas canvas, int width, int height, float changedAlpha) {
        if (holders == null || holders.isEmpty() || width <= 0 || height <= 0) return;
        List<H> afterDrawerList = new ArrayList<>();
        for (H holder : holders) {
            if (holder.isIdle()) {
                T holderData = getHolderData(width, height, holder);
                if (holderData != null) holder.bindData(holderData);
            } else {
                updateDrawers(width, height, holders);
                if (holder.isDrawInTopLayer()) {
                    afterDrawerList.add(holder);
                } else if (holder.bindData != null) holder.updateFrame(canvas, width, height, changedAlpha);
            }
        }
        if (!afterDrawerList.isEmpty()) for (H holder : afterDrawerList) {
            holder.updateFrame(canvas, width, height, changedAlpha);
        }
    }

    boolean onTouchEvent(@NonNull DrawerSurfaceView v, @NonNull MotionEvent event) {
        if (holders == null || holders.isEmpty()) return false;
        for (H holder : holders) {
            if (holder.isInitialized() && holder.onTouchEvent(v, event)) return true;
        }
        return false;
    }

    void initHolders(Context context) {
        if (holders == null) return;
        for (H holder : holders) {
            holder.initData(context);
        }
    }

    void idleAllHolders() {
        if (holders == null) return;
        for (H holder : holders) {
            if (holder.isInitialized()) holder.destroyAndIdle();
        }
    }
}
