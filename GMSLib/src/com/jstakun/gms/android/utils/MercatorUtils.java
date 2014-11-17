package com.jstakun.gms.android.utils;

/**
 *
 * @author jstakun
 */
public class MercatorUtils {
    public static double[] normalizeE6(double c1, double c2) {
        double[] norm = new double[2];
        norm[0] = normalizeE6(c1);
        norm[1] = normalizeE6(c2);
        return norm;
    }

    public static double[] normalizeE6(final double[] coords)
    {
        double[] norm = new double[2];
        norm[0] = normalizeE6(coords[0]);
        norm[1] = normalizeE6(coords[1]);
        return norm;
    }

    public static double normalizeE6(final double coord)
    {
        int tmp = (int)(coord * 1E6);
        return (tmp / 1E6d);
    }

    public static double normalizeE2(final double coord)
    {
        int tmp = (int)(coord * 1E2);
        return (tmp / 1E2d);
    }
}
