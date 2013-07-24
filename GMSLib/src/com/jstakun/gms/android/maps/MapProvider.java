/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.maps;

import java.io.IOException;

/**
 *
 * @author jstakun
 */
public interface MapProvider {
    public abstract byte[] retrieveStaticImage(int width, int height, double lat, double lng, int zoom) throws Exception;

    public abstract void close() throws IOException;
            
    public abstract int getMaxZoom();

    public abstract int getMinZoom();

    public abstract double[] adjust(double lat, double lng, int deltaX, int deltaY, int z);

    public abstract byte[] retrieveStaticImage(int x, int y, int zoom) throws Exception;

    public abstract String getResponseCodeErrorMessage();
}
