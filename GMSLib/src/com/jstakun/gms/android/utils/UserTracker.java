/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import android.content.Context;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author jstakun
 */
public class UserTracker {

    private static GoogleAnalyticsTracker tracker;
    private TrackerThread trackerThread;
    private boolean running = false;
    private boolean dryRun = false;
    private final LinkedBlockingQueue<Runnable> trackerQueue = new LinkedBlockingQueue<Runnable>();
    private final Object lock = new Object();
    private static UserTracker userTracker;

    private UserTracker() {
    }

    public static UserTracker getInstance() {
        if (userTracker == null) {
            userTracker = new UserTracker();
        }
        return userTracker;
    }

    public void startSession(final Context context) {
        if (tracker == null) {
            tracker = GoogleAnalyticsTracker.getInstance();
        }

        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)) {

            if (trackerThread == null) {
                trackerThread = new TrackerThread();
                trackerThread.start();
            }

            if (!running) {
                running = true;
                queueToTrackerThreadIfEnabled(new Runnable() {
                    @Override
                    public void run() {
                        String gaid = ConfigurationManager.getInstance().getString(ConfigurationManager.GA_ID);
                        tracker.startNewSession(gaid, context);
                        tracker.trackEvent("", "", "", 0);
                    };
            });
            }
        }
    }

    public void stopSession() {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && running) {
            queueToTrackerThreadIfEnabled(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.trackEvent("", "", "", 0);
                    } catch (Exception e) {
                        LoggerUtils.error("UserTracker.stopSession() exception: ", e);
                    } finally {
                        tracker.stopSession();
                    }
                }
            ;
        }
    

    );
        }
    }

    //public boolean isRunning() {
    //    return running;
    //}

    public void trackActivity(final String activityName) {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && tracker != null) {
            queueToTrackerThreadIfEnabled(new Runnable() {
                @Override
                public void run() {
                    tracker.trackPageView(activityName);
                }
            ;
            });
            dispatch();
        }
    }

    public void trackEvent(final String category, final String action, final String label, final int value) {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && tracker != null) {
            queueToTrackerThreadIfEnabled(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.trackEvent(category, action, label, value);
                    } catch (Exception e) {
                        LoggerUtils.error("UserTrecker.trackEvent() exception", e);
                    }
                }
            ;
            });
            dispatch();
        }
    }

    public void setDebug(boolean debug) {
        tracker.setDebug(debug);
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
        tracker.setDryRun(dryRun);
    }

    public void sendMyLocation() {
        int useCount = ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT, 0);
        ConfigurationManager.getInstance().putInteger(ConfigurationManager.USE_COUNT, useCount + 1);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.SEND_MY_POS_AT_STARTUP)) {
            ConfigurationManager.getInstance().remove(ConfigurationManager.SEND_MY_POS_AT_STARTUP);
            ConfigurationManager.getInstance().saveConfiguration(false);
            LoggerUtils.debug("Sending my location at startup.");

            //AsyncTaskExecutor.executeStringTask(new SendMyLocationTask(), null);
            new SendMyLocationTask(10).execute();
        } //else {
        //System.out.println("Skipping sending my location at startup... -----------------------------------");
        //}
    }

    private void dispatch() {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)) {
            queueToTrackerThreadIfEnabled(new Runnable() {
                @Override
                public void run() {
                    try {
                        tracker.dispatch();
                    } catch (Exception e) {
                        LoggerUtils.error("TrackerThread.dispatch() exception", e);
                    }
                }
            ;
        }
    

    );
        }
    }
    
    private void queueToTrackerThreadIfEnabled(Runnable r) {
        synchronized (lock) {
            trackerQueue.add(r);
        }
    }

    private class TrackerThread extends Thread {

        TrackerThread() {
            super("TrackerThread");
        }

        @Override
        public void run() {
            while (true) {
                Runnable r;
                try {
                    r = trackerQueue.take();
                    r.run();
                } catch (InterruptedException e) {
                    LoggerUtils.error("TrackerThread.run() exception", e);
                }
            }
        }
    }

    private class SendMyLocationTask extends GMSAsyncTask<Void, Void, String> {

        public SendMyLocationTask(int priority) {
            super(priority);
        }

        @Override
        protected String doInBackground(Void... arg0) {
            String errorMessage = null;

            if (ConfigurationManager.getInstance().getLocation() != null) {
                if (!ConfigurationManager.getInstance().isUserLoggedIn()) {
                    ConfigurationManager.getInstance().setMyPosUser();
                }

                if (!dryRun) {
                    try {
                    	AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
                 		if (asyncTaskManager != null) {
                 			errorMessage = asyncTaskManager.sendMyPos();
                 		}
                    } catch (Exception e) {
                        LoggerUtils.error("UserTracker.sendMyPos() exception: ", e);
                    }
                } else {
                    LoggerUtils.error("Sending my location at startup is dryRun mode.");
                }

                if (ConfigurationManager.getInstance().isMyPosUser()) {
                    ConfigurationManager.getInstance().resetUser();
                }
            }

            return errorMessage;
        }

        @Override
        protected void onPostExecute(String errorMessage) {
        }
    }
}
