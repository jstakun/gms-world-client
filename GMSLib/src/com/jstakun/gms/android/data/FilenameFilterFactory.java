/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.data;

import java.io.FilenameFilter;

/**
 *
 * @author jstakun
 */
public class FilenameFilterFactory {

    private static KMLFilenameFilter kmlFilter = null;
    private static PNGFilenameFilter pngFilter = null;

    public static FilenameFilter getFilenameFilter(String type) {
        if (type.equals("kml")) {
            if (kmlFilter == null) {
               kmlFilter = new KMLFilenameFilter();
            }
            return kmlFilter;
        } else if (type.equals("png")) {
            if (pngFilter == null) {
               pngFilter = new PNGFilenameFilter();
            }
            return pngFilter;
        } else {
            return null;
        }
    }
}
