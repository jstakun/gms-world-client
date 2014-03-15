/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 *
 * @author jstakun
 */
public class ResizableTextView extends TextView {

    public ResizableTextView(Context context, AttributeSet attr) {
        super(context, attr);

    }

    public ResizableTextView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        adjustTextSize();
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
        adjustTextSize();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw) {
            adjustTextSize();
        }
    }

    private void adjustTextSize() {

        /*if (getWidth() > 0) {
            float trySize = getPaint().getTextSize();
            TextPaint textPaintClone = new TextPaint();
            textPaintClone.set(getPaint());

            int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();

            // note that Paint text size works in px not sp
            textPaintClone.setTextSize(trySize);

            while (textPaintClone.measureText(getText().toString()) > availableWidth) {
                trySize--;
                textPaintClone.setTextSize(trySize);
            }

            //System.out.println("Setting text size: " + trySize + " ------------------------ ");
            if (trySize < 8f) {
                setText("");
            } else {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, trySize);
            }
        }*/
    	
    	if (getWidth() > 0) {
    		int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
    	    if (getPaint().measureText(getText().toString()) > availableWidth) {
    	    	setText("");
    	    }
    	}
    }
}