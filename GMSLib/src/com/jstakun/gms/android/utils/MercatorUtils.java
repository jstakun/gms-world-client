/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

/**
 *
 * @author jstakun
 */
public class MercatorUtils {

    
    //the current center latitute and longitude coordinates
    //the deltaX and deltaY, in pixels, of new map center
    //the map zoom level
    private static double[] adjust(final double lat, final double lng, final int deltaX, final int deltaY, final int z) {
        //Due to bug in OSM we'll use Google Mercator

        //double om[] = OsmMercator.adjust(lat, lng, deltaX, deltaY, z);
        double gm[] = GoogleMercator.adjust(lat, lng, deltaX, deltaY, z);
        
        //System.out.println("GM: " + gm[0] + " " + gm[1]);
        //System.out.println("OM: " + om[0] + " " + om[1]);
        //System.out.println("Diff0: " + (om[0] - gm[0]));
        //System.out.println("Diff1: " + (om[1] - gm[1]));
        
        //MapProvider mapProvider = MapProviderFactory.getMapProvider(ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER));
        //return mapProvider.adjust(lat, lng, deltaX, deltaY, z);

        return gm;
    }

    public static BoundingBox getBoundingBox(final int width, final int height, final double cursorLatitude, final double cursorLongitude, final int zoom) {

    	//TODO fix this code not to use adjust
    	
        double se[] = adjust(cursorLatitude, cursorLongitude, width / 2, height / 2, zoom);
        double nw[] = adjust(cursorLatitude, cursorLongitude, -width / 2, -height / 2, zoom);
        
        BoundingBox bbox = new BoundingBox();
        bbox.north = (nw[1] > 90.0) ? 90.0 : nw[1]; //lat
        bbox.south = (se[1] < -90.0) ? -90.0 : se[1]; //lat
        bbox.east = (se[0] > 180.0) ? 180.0 : se[0];  //lon
        bbox.west = (nw[0] < -180.0) ? -180.0 : nw[0];  //lon

        //System.out.println("n" + bbox.north + " s" + bbox.south  + " w" + bbox.west + " e" + bbox.east);

        return bbox;
    }

    public static int[] coordsToXY(final int width, final int height, final double latitude, final double longitude, BoundingBox bbox) {
        int xy[] = new int[2];

        //linear approx
        //xy[0] = (int) MicroDouble.round(width - (width * (bbox.east - longitude) / (bbox.east - bbox.west)));
        //xy[1] = (int) MicroDouble.round(height * (bbox.north - latitude) / (bbox.north - bbox.south));

        double xprimwest = xprim(bbox.west);
        double yprimnorth = yprim(bbox.north);

        xy[0] = (int)((xprim(longitude) - xprimwest) * (width / (xprim(bbox.east) - xprimwest)));
        xy[1] = (int)((yprimnorth - yprim(latitude)) * (height / (yprimnorth - yprim(bbox.south))));

        return xy;
    }

    public static int lonToX(final int width, final double longitude, BoundingBox bbox) {
        double xprimwest = xprim(bbox.west);
        return (int)((xprim(longitude) - xprimwest) * (width / (xprim(bbox.east) - xprimwest)));
    }

    public static int latToY(final int height, final double latitude, BoundingBox bbox) {
        double yprimnorth = yprim(bbox.north);
        return (int)((yprimnorth - yprim(latitude)) * (height / (yprimnorth - yprim(bbox.south))));
    }

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

    private static double xprim(double lng)
    {
        return Math.toRadians(lng);
    }

    private static double yprim(double lat)
    {
        return Math.log(Math.tan((Math.PI/4.0d) + (Math.toRadians(lat)/2.0d)));
    }
}
