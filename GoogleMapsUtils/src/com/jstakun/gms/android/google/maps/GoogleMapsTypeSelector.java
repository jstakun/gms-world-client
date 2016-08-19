package com.jstakun.gms.android.google.maps;

import com.google.android.maps.MapView;
import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class GoogleMapsTypeSelector {

    public static void selectMapType(final MapView googleMapsView) {
        int googleMapsType = ConfigurationManager.getInstance().getInt(ConfigurationManager.GOOGLE_MAPS_TYPE);

        if (googleMapsType == 1) {
            googleMapsView.setSatellite(true);
        } else if (googleMapsType == 2) {
            googleMapsView.setTraffic(true);
        } else {
            googleMapsView.setSatellite(false);
            googleMapsView.setTraffic(false);
        }
    }
}
