package com.jstakun.gms.android.location;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class AndroidDevice implements LocationListener {

    private static final int MILLIS = 0; //5000;
    private static final int METERS = 0; //5;
    //private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int THREE_MINUTES = 1000 * 60 * 3;
    
    private LocationManager locationManager;
    private Location previousLocation;
    protected Handler positionHandler = null;
    private boolean isListening = false;
    
    private Listener gpsStatusListener = new GpsStatus.Listener() {
        public synchronized void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    if (locationManager != null) {
                        GpsStatus status = locationManager.getGpsStatus(null);
                        int satellites = 0;
                        Iterable<GpsSatellite> list = status.getSatellites();
                        for (GpsSatellite satellite : list) {
                            if (satellite.usedInFix()) {
                                satellites++;
                            }
                        }
                        LoggerUtils.debug("Number of available satellites: " + satellites);
                    }
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    break;
                default:
                    break;
            }
        }
    };

    public AndroidDevice(Context context) {

    	if (context == null) {
    		context = ConfigurationManager.getInstance().getContext();
    	}
        if (locationManager == null && context != null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        Location lastKnownLocation = null; 
        
        if (locationManager != null) {
        	lastKnownLocation = getLastKnownLocation();
        }

        if (lastKnownLocation != null) {
            setMyLocation(lastKnownLocation);
         }
    }

    public void setPositionHandler(Handler handler) {
        this.positionHandler = handler;
    }

    public void startListening() {
        if (!isListening) {

            Criteria crit = new Criteria();
            crit.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = locationManager.getBestProvider(crit, true);
            try {
                locationManager.requestLocationUpdates(provider, MILLIS, METERS, this);
                isListening = true;
            } catch (Exception e) {
                stopListening();
            }

            locationManager.addGpsStatusListener(gpsStatusListener);
        }
    }

    public void onLocationChanged(Location location) {

        Location currentLocation = null;

        if (isBetterLocation(location, previousLocation)) {
            currentLocation = location; //filterLocation(location);
        }

        if (currentLocation != null) {
            LoggerUtils.debug("Setting current gps location.");
            ConfigurationManager.getInstance().setLocation(currentLocation);
            previousLocation = currentLocation;
            updatePositionUi(currentLocation);
        }
    }

    public void stopListening() {
        if (isListening) {
            locationManager.removeUpdates(this);
            locationManager.removeGpsStatusListener(gpsStatusListener);
            isListening = false;
        }
    }

    public static int getBearingIndex(Location location) {

        final double sector = 22.5; // = 360 degrees / 16 sectors
        final int[] compass = {0 /* N */, 1 /* NNE */, 2 /* NE */, 3 /* ENE */,
            4 /* E */, 5 /* ESE */, 6 /* SE */, 7 /* SSE */,
            8 /* S */, 9 /* SSW */, 10 /* SW */, 11 /* WSW */,
            12 /* W */, 13 /* WNW */, 14 /* NW */, 15 /* NNW */, 0 /* N */};
        final int directionIndex = (int) (Math.floor((location.getBearing() - 11.25) / sector) + 1);// we add one because north would otherwise be a -1 index, and we add a reference to N as the zero index
        int heading = compass[directionIndex];
        return heading;
    }

    private Location getLastKnownLocation() {
        Location lastKnownLocation = null;

        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            LoggerUtils.error("AndroidDevice() error:", e);
        }

        if (lastKnownLocation == null) {
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            } catch (Exception e) {
                LoggerUtils.error("AndroidDevice() error:", e);
            }
        }

        return lastKnownLocation;
    }

    private void setMyLocation(Location lastKnownLocation) {
        LoggerUtils.debug("Setting last known location.");
        ConfigurationManager.getInstance().setLocation(lastKnownLocation);
        updatePositionUi(lastKnownLocation);
    }

    private void updatePositionUi(Location l) {
        if (positionHandler != null) {
            Message msg = positionHandler.obtainMessage(LocationServicesManager.UPDATE_LOCATION, l);
            positionHandler.handleMessage(msg);
        } else {
            //the same code as in IntentsHelper.addMyLocationLandmark
            String date = DateTimeUtils.getDefaultDateTimeString(System.currentTimeMillis(), ConfigurationManager.getInstance().getCurrentLocale());
            LandmarkManager.getInstance().addLandmark(l.getLatitude(), l.getLongitude(), (float)l.getAltitude(), Locale.getMessage(R.string.Your_Location), Locale.getMessage(R.string.Your_Location_Desc, l.getProvider(), l.getAccuracy(), date), Commons.MY_POSITION_LAYER, false);
        }
    }

    public void onProviderDisabled(String provider) {
        LoggerUtils.debug("Provider Disabled: " + provider);
    }

    public void onProviderEnabled(String provider) {
        LoggerUtils.debug("Provider Enabled: " + provider);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        LoggerUtils.debug("Provider Status Changed: " + provider + ", Status=["
                + status + "], extras=" + extras);
        //if (status == LocationProvider.AVAILABLE) {
        //}
    }

    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }
        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > THREE_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -THREE_MINUTES;
        boolean isNewer = timeDelta > 0; //millis
        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            // If the new location is more than two minutes older, it must be worse
            return false;
        }
        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 100; //meters
        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same *
     */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    public static Location getLastKnownLocation(Context context, long validityMinutes) {
    	Location location = ConfigurationManager.getInstance().getLocation();

		if (location == null) {
			LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
			if (locationManager != null) {
				try {
					location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				} catch (Exception e) {
					LoggerUtils.error("AndroidDevice.getLastKnownLocation() exception:", e);
				}
				if (location == null) {
					try {
						location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					} catch (Exception e) {
						LoggerUtils.error("AndroidDevice.getLastKnownLocation() exception:", e);
					}
				}
				if (location == null) {
					LoggerUtils.debug("AndroidDevice.getLastKnownLocation() no location from location manager available");
				}
			} else {
				LoggerUtils.debug("AndroidDevice.getLastKnownLocation() no location manager available");
			}
		} else {
			LoggerUtils.debug("AndroidDevice.getLastKnownLocation() no saved location");
		}
		
		if (location != null && (System.currentTimeMillis() - location.getTime()) < (validityMinutes * 60 * 1000)) {
			return location;
		} else {
			return null;
		}
    }
}
