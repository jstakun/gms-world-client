/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import net.dclausen.microfloat.MicroDouble;

/**
 *
 * @author jstakun
 */
public class GoogleMercator {

    private static double offset = 268435456;
    private static double radius = offset / Math.PI;

    //the current center latitute and longitude coordinates
    //the deltaX and deltaY, in pixels, of new map center
    //the map zoom level
    public static double[] adjust(final double lat, final double lng, final int deltaX, final int deltaY, final int z) {
        return new double[]{XToL(LToX(lng) + (deltaX << (21 - z))), YToL(LToY(lat) + (deltaY << (21 - z)))};
    }

    private static double LToX(final double x) {
        return MicroDouble.round(offset + radius * x * Math.PI / 180);
    }

    private static double LToY(final double y) {
        return MicroDouble.round(offset - radius * Double.longBitsToDouble(MicroDouble.log(Double.doubleToLongBits((1 + Math.sin(y * Math.PI / 180)) / (1 - Math.sin(y * Math.PI / 180))))) / 2);
    }

    private static double XToL(final double x) {
        return ((MicroDouble.round(x) - offset) / radius) * 180 / Math.PI;
    }

    private static double YToL(final double y) {
        return (Math.PI / 2 - 2 * Double.longBitsToDouble(MicroDouble.atan(MicroDouble.exp(Double.doubleToLongBits((MicroDouble.round(y) - offset) / radius))))) * 180 / Math.PI;
    }
}
