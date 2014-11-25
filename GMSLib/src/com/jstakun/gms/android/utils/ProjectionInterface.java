package com.jstakun.gms.android.utils;

import android.graphics.Point;

/**
 *
 * @author jstakun
 */
public interface ProjectionInterface {
   public void toPixels(int latE6, int lngE6, Point point);

   public int[] fromPixels(int x, int y);

   public boolean isVisible(int latE6, int lngE6);
   
   public boolean isVisible(Point point);
   
   public BoundingBox getBoundingBox();
   
   public float getViewDistance();
}
