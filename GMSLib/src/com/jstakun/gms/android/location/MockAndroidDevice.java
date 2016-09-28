package com.jstakun.gms.android.location;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.KMLParser;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MercatorUtils;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;

/**
 *
 * @author jstakun
 */
public class MockAndroidDevice extends AndroidDevice {

    private TrackThread thread = null;
    private static final String ROUTE = "route_a.xml";
    private static float bearing = 0.0f;

    public MockAndroidDevice(Context context) {
    	super(context);
        initTrackThread();
    }

    private void initTrackThread() {
        thread = new TrackThread();
        thread.start();
    }

    @Override
    public void onLocationChanged(Location location) {

        ConfigurationManager.getInstance().setLocation(location);

        if (getPositionHandler() != null) {
            double lat = MercatorUtils.normalizeE6(location.getLatitude());
            double lon = MercatorUtils.normalizeE6(location.getLongitude());
            float altitude = (float)location.getAltitude();
            float accuracy = location.getAccuracy();

            Message msg = getPositionHandler().obtainMessage();
            Bundle b = new Bundle();
            b.putDouble("lat", lat);
            b.putDouble("lng", lon);
            b.putFloat("alt", altitude);
            b.putFloat("acc", accuracy);
            msg.setData(b);
            getPositionHandler().handleMessage(msg);
        }
    }
    
    @Override
    public void stopListening() {
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (Exception e) {
            }
        }

        super.stopListening();
    }

    private class TrackThread extends Thread {

        private List<ExtendedLandmark> routePoints = new ArrayList<ExtendedLandmark>();
        private ExtendedLandmark currentLandmark = null;
        private boolean closing = false;

        public TrackThread() {
            super();
        }

        @Override
        public void interrupt() {
            closing = true;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LoggerUtils.debug("TrackThread interrupted: " + e.getMessage());
            }

            if (routePoints.isEmpty()) {
                KMLParser parser = new KMLParser();
                createExternalStoragePrivateFile();

                try {
                    parser.parse(FileManager.getInstance().getRoutesFolderPath(), ROUTE, routePoints, false, null, null);
                } catch (Exception ex) {
                    LoggerUtils.debug("LMCanvas.loadRouteAction exception", ex);
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LoggerUtils.debug("TrackThread interrupted: " + e.getMessage());
            }

            int size = routePoints.size();

            LoggerUtils.debug("Route size: " + size);

            for (int i = 0; i < size; i++) {
                if (closing) {
                    return;
                }
                currentLandmark = routePoints.get(i);

                LoggerUtils.debug("Loaded landmark " + i + " from " + size);

                bearing += 6.0f;
                if (bearing > 360.0f) {
                    bearing = 0.0f;
                }

                Location l = new Location("test");
                l.setLatitude(currentLandmark.getQualifiedCoordinates().getLatitude());
                l.setLongitude(currentLandmark.getQualifiedCoordinates().getLongitude());
                l.setBearing(bearing);
                l.setAccuracy(0.0f);

                onLocationChanged(l);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LoggerUtils.debug("TrackThread interrupted: " + e.getMessage());
                }

            }
        }

        private void createExternalStoragePrivateFile() {
            Context ctx = ConfigurationManager.getInstance().getContext();
            File file = FileManager.getInstance().getExternalDirectory(FileManager.getInstance().getRoutesFolderPath(), ROUTE);
            InputStream is = null;
            OutputStream os = null;

            try {
                is = ctx.getResources().openRawResource(R.raw.route_a);
                os = new FileOutputStream(file);

                byte[] data = new byte[is.available()];

                is.read(data);
                os.write(data);
            } catch (IOException e) {
                LoggerUtils.error("MockAndroidDevice.createExternalStoragePrivateFile() error", e);
            } finally {

                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                    }
                }

                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ex) {
                    }
                }

            }
        }
    }
}
