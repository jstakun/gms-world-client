/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 *
 * @author jstakun
 */
public class StatusBarLinearLayout extends LinearLayout {

    private ViewResizeListener viewResizeListener = null;

    public StatusBarLinearLayout(Context context) {
        super(context);
    }

    public StatusBarLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (viewResizeListener != null && h > 0) {
            viewResizeListener.onResize(getId(), w, h, oldh, oldh);
        }
    }

    public void setViewResizeListener(ViewResizeListener viewResizeListener)
    {
       this.viewResizeListener = viewResizeListener;
    }
}
