package com.jstakun.gms.android.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

public class PicassoTextViewTarget implements Target {

	public enum Position {LEFT, RIGHT, BOTTOM, TOP};
	
	private TextView targetTextView;
	private Position position;
	
	public PicassoTextViewTarget(TextView targetTextView, Position position) {
		this.targetTextView = targetTextView;
		this.position = position;
	}
	
	@Override
	public void onBitmapFailed(Drawable img) {
		if (position == Position.LEFT) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
		} else if (position == Position.TOP) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
		} else if (position == Position.RIGHT) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
		} else if (position == Position.BOTTOM) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, img);
		}
	}

	@Override
	public void onBitmapLoaded(Bitmap bmp, LoadedFrom from) {
		BitmapDrawable img = getBitmapDrawable(bmp);
		if (position == Position.LEFT) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
		} else if (position == Position.TOP) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
		} else if (position == Position.RIGHT) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
		} else if (position == Position.BOTTOM) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, img);
		}
	}

	@Override
	public void onPrepareLoad(Drawable img) {
		if (position == Position.LEFT) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
		} else if (position == Position.TOP) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, img, null, null);
		} else if (position == Position.RIGHT) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, img, null);
		} else if (position == Position.BOTTOM) {
			targetTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, img);
		}
	}

	private static BitmapDrawable getBitmapDrawable(Bitmap bitmap) {
    	try {
    		//API version >= 4
    		Context ctx = ConfigurationManager.getInstance().getContext();
    		return BitmapDrawableHelperInternal.getBitmapDrawable(bitmap, ctx.getResources());
    	} catch (Throwable e) {
    		//API version 3
    		return new BitmapDrawable(bitmap);
    	}
    }
	
	private static class BitmapDrawableHelperInternal { 
        private static BitmapDrawable getBitmapDrawable(Bitmap bitmap, Resources res) {
            return new BitmapDrawable(res, bitmap);
        }
    }
}
