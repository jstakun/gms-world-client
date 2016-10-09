package com.jstakun.gms.android.utils;

/**
 *
 * @author jstakun
 */
public class BoundingBox {
	
	public static final String BBOX = "bbox";
	
    public double north;
    public double south;
    public double east;
    public double west;

    @Override
    public String toString() {
        return "north: " + north + ",south: " + south + ",east: " + east + ",west: " + west;
    }
}
