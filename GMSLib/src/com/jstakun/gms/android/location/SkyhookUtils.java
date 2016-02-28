package com.jstakun.gms.android.location;

import org.spongycastle.util.encoders.Base64;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.OsUtil;

import com.skyhookwireless.wps.IPLocation;
import com.skyhookwireless.wps.IPLocationCallback;
import com.skyhookwireless.wps.RegistrationCallback;
import com.skyhookwireless.wps.TilingListener;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.WPSStreetAddressLookup;
import com.skyhookwireless.wps.XPS;

/**
 *
 * @author jstakun
 */
public class SkyhookUtils {

	private static final int ERROR = 666;
    private static final WPSAuthentication auth = new WPSAuthentication(new String(Base64.decode(Commons.SKYHOOK_USERNAME)), 
    		new String(Base64.decode(Commons.SHYHOOK_REALM)));
    private final GMSRegistrationCallback regCallback = new GMSRegistrationCallback();
    private XPS xps;
    private boolean isRegistered, hasRunOnFirstFix, isRegistering;
    private Handler locationHandler;
    private Runnable runOnFirstFix;
    private GMSAsyncTask<Void, Void, Void> getLocationTask;
    
    // Callback objects
    private WPSLocationCallback oneTimeCallback = new WPSLocationCallback() {
        public void handleWPSLocation(WPSLocation wpsLocation) {
            handleWPSLocationStatus(wpsLocation);
        }

        public void done() {
            LoggerUtils.debug("Calling WPSLocationOnTimeCallback.done()...");
        }

        public WPSContinuation handleError(WPSReturnCode error) {
        	return handleErrorStatus(error);
        }
    };
    private WPSPeriodicLocationCallback periodicCallback = new WPSPeriodicLocationCallback() {
        public WPSContinuation handleWPSPeriodicLocation(WPSLocation wpsLocation) {
            return handleWPSLocationStatus(wpsLocation);
        }

        public void done() {
            LoggerUtils.debug("Calling WPSPeriodicLocationCallback.done()...");
        }

        public WPSContinuation handleError(WPSReturnCode error) {
            return handleErrorStatus(error);
        }
    };
    private IPLocationCallback ipLocationCallback = new IPLocationCallback() {

		@Override
		public void done() {
			LoggerUtils.debug("Calling WPS IPLocationCallback.done()...");
		}

		@Override
		public WPSContinuation handleError(WPSReturnCode error) {
			return handleErrorStatus(error);
		}

		@Override
		public void handleIPLocation(IPLocation ipLocation) {
			LoggerUtils.debug("WPS Found IP location: " + ipLocation.getLatitude() + "," + ipLocation.getLongitude());
			
		}
    	
    };

    public SkyhookUtils(Context context, Handler locationHandler) {
        xps = new XPS(context);
        //xps.setKey(Commons.SHYHOOK_API_KEY);
        FileManager fm = PersistenceManagerFactory.getFileManager();
        String cache = fm.getExternalDirectory(FileManager.getTilesFolder(), null).getAbsolutePath();
        LoggerUtils.debug("Setting WPS tiling at " + cache + "...");
        xps.setTiling(cache, 460800, 4608000, new GMSTilingCallback());
        this.locationHandler = locationHandler;
        hasRunOnFirstFix = false;
        //
        isRegistered = false;
        xps.registerUser(auth, null, regCallback);
    }

    private WPSContinuation handleErrorStatus(WPSReturnCode error) {
    	String errorMsg = "WPS location request error: " + error.toString();
        LoggerUtils.error(errorMsg);
        Message msg = locationHandler.obtainMessage(ERROR, errorMsg);
        locationHandler.handleMessage(msg);
        return WPSContinuation.WPS_CONTINUE;
    }

    private WPSContinuation handleWPSLocationStatus(WPSLocation wpsLocation) {
        String msgStr = "WPS found location: " + wpsLocation.getLatitude() + "," + wpsLocation.getLongitude();
        LoggerUtils.debug(msgStr);
        //b.putString("msg", msgStr);

        Location location = new Location("Skyhook");
        location.setLatitude(wpsLocation.getLatitude());
        location.setLongitude(wpsLocation.getLongitude());
        location.setAltitude(wpsLocation.getAltitude());
        location.setBearing((float) wpsLocation.getBearing());
        location.setSpeed((float) wpsLocation.getSpeed());
        location.setTime(wpsLocation.getTime());

        if (AndroidDevice.isBetterLocation(location, ConfigurationManager.getInstance().getLocation())) {

            ConfigurationManager.getInstance().setLocation(location);

            if (!hasRunOnFirstFix && runOnFirstFix != null) {
                runOnFirstFix.run();
                hasRunOnFirstFix = true;
            }

            Message msg = locationHandler.obtainMessage(LocationServicesManager.UPDATE_LOCATION, location);
            locationHandler.handleMessage(msg);
        }

        return WPSContinuation.WPS_CONTINUE;
    }

    private void getLocation() {
        LoggerUtils.debug("Calling SkyhookUtils.getLocation()...");
        //xps.getLocation(auth, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, oneTimeCallback);
        xps.getIPLocation(auth, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, ipLocationCallback);
        xps.getPeriodicLocation(auth, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP, 10000, 0, periodicCallback);
    }

    public void enableMyLocation() {
    	//byte[] key = Commons.SKYHOOK_KEY.getBytes();
    	byte[] key = OsUtil.getDeviceId(ConfigurationManager.getInstance().getContext()).getBytes();
        byte[] token = xps.getOfflineToken(auth, key);
        if (token != null) {
            xps.getOfflineLocation(auth, key, token, oneTimeCallback);
        } else {
            LoggerUtils.debug("WPS offline token missing...");
        }

        getLocationTask = new GetLocationTask();
        //AsyncTaskExecutor.execute(getLocationTask, null);
        getLocationTask.execute();
    }

    public void disableMyLocation() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    xps.abort();

                    if (getLocationTask != null) {
                        getLocationTask.cancel(true);
                    }
                } catch (Exception e) {
                }
            }
        };
        thread.start();
    }

    public void runOnFirstFix(Runnable runOnFirstFix) {
        this.runOnFirstFix = runOnFirstFix;
    }

    private class GMSTilingCallback implements TilingListener {

        public WPSContinuation tilingCallback(int tileNumber, int tileTotal) {
            LoggerUtils.debug("Calling WPS GMSTilingCallback.tilingCallback() with params " + tileNumber + "," + tileTotal);
            return WPSContinuation.WPS_CONTINUE;
        }
    }
   

    private class GMSRegistrationCallback implements RegistrationCallback {

        public void handleSuccess() {
            isRegistered = true;
            LoggerUtils.debug("WPS registration successfull...");
        }

        public WPSContinuation handleError(final WPSReturnCode error) {
            String errorMsg = "WPS registration error: " + error.name();
            Message msg = locationHandler.obtainMessage(ERROR, errorMsg);
            locationHandler.handleMessage(msg);
            return WPSContinuation.WPS_CONTINUE;
        }

        public void done() {
            LoggerUtils.debug("Calling WPS GMSRegistrationCallback.done()...");
            isRegistering = false;
        }
    }

    private class GetLocationTask extends GMSAsyncTask<Void, Void, Void> {

        public GetLocationTask() {
            super(1, GetLocationTask.class.getName());
        }
        
        @Override
        protected Void doInBackground(Void... params) {
            LoggerUtils.debug("Running WPS GetLocationTask...");
            
            while (!isRegistered && !isCancelled()) {
                try {
                    LoggerUtils.debug("Skyhook user not registered ...");
                    Thread.sleep(1000L);
                } catch (InterruptedException ex) {
                    //LoggerUtils.error("SkyhookUtils.GetLocationTask exception", ex);
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                //LoggerUtils.error("SkyhookUtils.GetLocationTask exception", ex);
            }

            while (isRegistering && !isCancelled()) {
                try {
                    Thread.sleep(1000L);
                    LoggerUtils.debug("Finishing WPS registration ...");
                } catch (InterruptedException ex) {
                    //LoggerUtils.error("SkyhookUtils.GetLocationTask exception", ex);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            if (isRegistered && !isRegistering) {
                getLocation();
            }
            LoggerUtils.debug("Finishing WPS GetLocationTask...");
        }
    }
}
