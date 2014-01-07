/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.Display;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 *
 * @author jstakun
 */
public class FlowTextHelper_old {

    private static boolean mNewClassAvailable;         /* class initialization fails when this throws an exception */

    static {
        try {
            Class.forName("android.text.style.LeadingMarginSpan$LeadingMarginSpan2");
            mNewClassAvailable = true;
        } catch (Throwable ex) {
            mNewClassAvailable = false;
        }
    }

    public static void tryFlowText(String text, View thumbnailView, TextView messageView, Display display, int addPadding, ImageGetter imageGetter) {
        // There is nothing I can do for older versions, so just return
        if (!mNewClassAvailable) {
            messageView.setText(Html.fromHtml(text, imageGetter, null));
        } else {
            try {
                HelperInternal.tryFlowText(text, thumbnailView, messageView, display, addPadding, imageGetter);
            } catch (Throwable ex) {
                messageView.setText(Html.fromHtml(text, imageGetter, null));
            }
        }
    }

    private static class HelperInternal {

        private static void tryFlowText(String text, View thumbnailView, TextView messageView, Display display, int addPadding, ImageGetter imageGetter) {
            thumbnailView.measure(display.getWidth(), display.getHeight());
            int height = thumbnailView.getMeasuredHeight();
            int width = thumbnailView.getMeasuredWidth() + addPadding;
            messageView.measure(width, height);
            //to allow getTotalPaddingTop
            int padding = messageView.getTotalPaddingTop();
            float textLineHeight = messageView.getPaint().getTextSize();
            // Set the span according to the number of lines and width of the image
            int lines = (int) Math.round((height - padding) / textLineHeight);
            //For an html text you can use this line:
            SpannableStringBuilder ss = (SpannableStringBuilder) Html.fromHtml(text, imageGetter, null);
            ss.setSpan(new GMSLeadingMarginSpan2_old(lines, width), 0, ss.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            messageView.setText(ss);
            // Align the text with the image by removing the rule that the text is to the right of the image
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageView.getLayoutParams();
            int[] rules = params.getRules();
            rules[RelativeLayout.RIGHT_OF] = 0;
        }
    }
}
