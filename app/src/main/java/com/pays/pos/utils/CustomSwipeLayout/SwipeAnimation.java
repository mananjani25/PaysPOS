package com.pays.pos.utils.CustomSwipeLayout;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import androidx.core.view.ViewCompat;

import static com.pays.pos.utils.CustomSwipeLayout.Utils.setViewWidth;


/**
 * Created by Alexander Pataridze
 */

class SwipeAnimation extends Animation {
    private final int width;
    private int startWidth = -1;
    private final View resizeView;
    private final View changeXView;
    private final boolean left;

    SwipeAnimation(View resizeView, int width, View changeXView, boolean left) {
        this.resizeView = resizeView;
        this.width = width;
        this.changeXView = changeXView;
        this.left = left;
        setDuration(300);
        setInterpolator(new DecelerateInterpolator());
    }


    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        if (startWidth < 0) {
            startWidth = resizeView.getWidth();
        }

        setViewWidth(resizeView, startWidth + (int) (((float) width - (float) startWidth) * interpolatedTime));

        if (left) {
            ViewCompat.setTranslationX(changeXView, resizeView.getWidth());
        } else {
            ViewCompat.setTranslationX(changeXView, -resizeView.getWidth());
        }

    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }
}