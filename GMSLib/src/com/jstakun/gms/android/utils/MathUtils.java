/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.utils;

/**
 *
 * @author jstakun
 */
public class MathUtils {

    public static double pow(double base, int exponent) {
        boolean reciprocal = false;
        if (exponent < 0) {
            reciprocal = true;
        }
        double result = 1;
        while (exponent-- > 0) {
            result *= base;
        }
        return reciprocal ? 1 / result : result;
    }

    public static int abs(int arg) {
        return  arg < 0 ? -arg : arg;
    }

    public static double abs(double arg) {
        return  arg < 0.0 ? -arg : arg;
    }

    public static int coordDoubleToInt(double coord) {
        return (int)(coord * 1E6);
    }

    public static double coordIntToDouble(int coord) {
        return coord * 0.000001; //coord / 1E6;
    }
}
