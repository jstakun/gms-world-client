/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.location.Location;
import java.text.NumberFormat;
import javax.measure.Measure;
import javax.measure.MeasureFormat;
import javax.measure.converter.UnitConverter;
import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.UnitFormat;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class DistanceUtils {

    private static UnitConverter kilometerToMeter = SI.KILOMETRE.getConverterTo(SI.METRE);
    private static UnitConverter kilometerToMile = SI.KILOMETRE.getConverterTo(NonSI.MILE);
    private static UnitConverter kilometerToFoot = SI.KILOMETRE.getConverterTo(NonSI.FOOT);
    private static UnitConverter kilometerToNMile = SI.KILOMETRE.getConverterTo(NonSI.NAUTICAL_MILE);
    private static UnitConverter kmPerHourToMile = NonSI.KILOMETRES_PER_HOUR.getConverterTo(NonSI.MILES_PER_HOUR);
    private static MeasureFormat formatterLong;
    private static MeasureFormat formatterShort;
    private static boolean initialized = false;

    private static void initFormatters() {
    	try {
    		NumberFormat large = NumberFormat.getInstance(ConfigurationManager.getInstance().getCurrentLocale());
    		large.setMaximumFractionDigits(2);
    		formatterLong = MeasureFormat.getInstance(large, UnitFormat.getInstance(ConfigurationManager.getInstance().getCurrentLocale()));
    		NumberFormat small = NumberFormat.getInstance(ConfigurationManager.getInstance().getCurrentLocale());
    		small.setMaximumFractionDigits(0);
    		formatterShort = MeasureFormat.getInstance(small, UnitFormat.getInstance(ConfigurationManager.getInstance().getCurrentLocale()));
    		initialized = true;
    	} catch (Exception e) {
    		LoggerUtils.error("DistanceUtils.initFormatters() exception:", e);
    	}
    }

    public static float distanceInKilometer(int latitudeSpan, int longitudeSpan, int cursorLatitude, int cursorLongitude) {
        double minLatitude = MathUtils.coordIntToDouble(cursorLatitude - (latitudeSpan / 2));
        //double maxLatitude = MathUtils.coordIntToDouble(cursorLatitude + (latitudeSpan / 2));
        double minLongitude = MathUtils.coordIntToDouble(cursorLongitude - (longitudeSpan / 2));
        double maxLongitude = MathUtils.coordIntToDouble(cursorLongitude + (longitudeSpan / 2));
        return distanceInKilometer(minLatitude, minLongitude, minLatitude, maxLongitude);
    }

    public static float distanceInKilometer(int width, int height, double cursorLatitude, double cursorLongitude, int zoom) {
        BoundingBox bbox = MercatorUtils.getBoundingBox(width, height, cursorLatitude, cursorLongitude, zoom);
        return distanceInKilometer(bbox.south, bbox.east, bbox.south, bbox.west);
    }

    public static float distanceInKilometer(double lat1, double lon1, double lat2, double lon2) {
        return distanceInMeter(lat1, lon1, lat2, lon2) * 0.001f;
    }

    public static float distanceInMeter(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    public static int radiusInKilometer() {
        return ConfigurationManager.getInstance().getInt(ConfigurationManager.SEARCH_RADIUS, 10);
    }

    /*                0 is locale              */
    /*                2 is statute miles              */
    /*                1 is kilometers        */
    /*                3 is nautical miles             */
    public static String formatSpeed(double speedInKilometer) {
        Measure<Velocity> speed = null;

        if (!initialized) {
            initFormatters();
        }

        int uol = getUoL();

        if (uol == 1) {
            speed = Measure.valueOf(speedInKilometer, NonSI.KILOMETRES_PER_HOUR);
        } else if (uol == 2) {
            double speedInMiles = kmPerHourToMile.convert(speedInKilometer);
            speed = Measure.valueOf(speedInMiles, NonSI.MILES_PER_HOUR);
        } else if (uol == 3) {
            double speedInMiles = kmPerHourToMile.convert(speedInKilometer);
            speed = Measure.valueOf(speedInMiles, NonSI.MILES_PER_HOUR);
        }

        return formatterLong.format(speed);
    }

    /*                0 is locale              */
    /*                2 is statute miles              */
    /*                1 is kilometers        */
    /*                3 is nautical miles             */
    public static String formatDistance(double distanceInKilometer) {
        try {
            MeasureFormat formatter = null;
            Measure<Length> dist = null;

            if (!initialized) {
                initFormatters();
            }

            int uol = getUoL();

            if (uol == 1) {
                if (distanceInKilometer < 1.0) {
                    double distanceInMeter = kilometerToMeter.convert(distanceInKilometer);
                    dist = Measure.valueOf(distanceInMeter, SI.METRE);
                    formatter = formatterShort;
                } else {
                    dist = Measure.valueOf(distanceInKilometer, SI.KILOMETRE);
                    formatter = formatterLong;
                }
            } else if (uol == 2) {
                double distanceInMiles = kilometerToMile.convert(distanceInKilometer);
                if (distanceInMiles < 1.0) {
                    double distanceInFoot = kilometerToFoot.convert(distanceInKilometer);
                    dist = Measure.valueOf(distanceInFoot, NonSI.FOOT);
                    formatter = formatterShort;
                } else {
                    dist = Measure.valueOf(distanceInMiles, NonSI.MILE);
                    formatter = formatterLong;
                }
            } else if (uol == 3) {
                double distanceInMiles = kilometerToNMile.convert(distanceInKilometer);
                if (distanceInMiles < 1.0) {
                    double distanceInFoot = kilometerToFoot.convert(distanceInKilometer);
                    dist = Measure.valueOf(distanceInFoot, NonSI.FOOT);
                    formatter = formatterShort;
                } else {
                    dist = Measure.valueOf(distanceInMiles, NonSI.NAUTICAL_MILE);
                    formatter = formatterLong;
                }
            }

            if (formatter != null) {
            	return formatter.format(dist);
            } else {
            	return "";
            }
        } catch (Exception e) {
            LoggerUtils.error("DistanceUtils.formatDistance() exception", e);
            return "";
        }
    }

    public static int getUoL() {
        int uol = ConfigurationManager.getInstance().getInt(ConfigurationManager.UNIT_OF_LENGHT, 0);
        if (uol == 0) {
            java.util.Locale locale = ConfigurationManager.getInstance().getCurrentLocale();
            if (locale != null && locale.getISO3Language().equals("eng")) {
                uol = 2;
            } else {
                uol = 1;
            }
        }
        return uol;
    }
}
