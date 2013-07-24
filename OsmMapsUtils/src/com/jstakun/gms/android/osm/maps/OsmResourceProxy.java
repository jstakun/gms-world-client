/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.osmdroid.DefaultResourceProxyImpl;

/**
 *
 * @author jstakun
 */
public class OsmResourceProxy extends DefaultResourceProxyImpl {

    private final Context context;

    public OsmResourceProxy(final Context pContext) {
        super(pContext);
        context = pContext;
    }

    @Override
    public Bitmap getBitmap(final bitmap pResId) {

        //if (pResId.name().equals("person")) {
        //    return BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_maps_indicator_current_position);
        //} else {
            final int res = context.getResources().getIdentifier(pResId.name(), "drawable", context.getPackageName());

            if (res > 0) {
                return BitmapFactory.decodeResource(context.getResources(), res);
            } else {
                return super.getBitmap(pResId);
            }
        //}
    }
}
