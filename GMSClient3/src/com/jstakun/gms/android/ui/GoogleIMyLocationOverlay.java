/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import com.google.android.maps.MapView;
import com.jstakun.gms.android.google.maps.GoogleMyLocationOverlay;
import org.osmdroid.api.IMyLocationOverlay;

/**
 *
 * @author jstakun
 */
public class GoogleIMyLocationOverlay extends GoogleMyLocationOverlay implements IMyLocationOverlay {
    public GoogleIMyLocationOverlay(Context context, MapView mapView, Handler handler, Drawable drawable) {
        super(context, mapView, handler, drawable);
    }
}
