/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.graphics.Point;
import com.devahead.util.objectpool.PoolObject;

/**
 *
 * @author jstakun
 */
public class LayerPoint extends Point implements PoolObject {

    private static final int INTERVAL = 16;
    public boolean isVisible;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LayerPoint) {
            LayerPoint lp = (LayerPoint) o;
            int xi = lp.x - x;
            int yi = lp.y - y;

            return (MathUtils.abs(xi) < INTERVAL && MathUtils.abs(yi) < INTERVAL);
        } else {
            return false;
        }
    }

    public void initializePoolObject() {
        x = 0;
        y = 0;
        isVisible = false;
    }

    public void finalizePoolObject() {
        //no implementation needed
    }
}
