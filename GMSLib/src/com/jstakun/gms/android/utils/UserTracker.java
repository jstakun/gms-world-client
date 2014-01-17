/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Logger;
import com.google.analytics.tracking.android.Tracker;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class UserTracker {

	private static Tracker tracker;
	//private boolean running = false;
    private boolean dryRun = false;
    private static UserTracker userTracker = null;

    private UserTracker() {
    }

    public static UserTracker getInstance() {
        if (userTracker == null) {
            userTracker = new UserTracker();
        }
        return userTracker;
    }

    public void initialize(final Context context) {
    	try {
    		if (tracker == null) {
        		tracker = GoogleAnalytics.getInstance(context).getTracker(getId(context));
        	}
    	} catch (Throwable t) {
    		LoggerUtils.error("UserTracker.initialize exception:", t);
    	}
    }
    
    /*public void startSession(final Context context) {
    	//System.out.println("UserTracker.startSession()");
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && !running) {
            trackEvent("SessionStart", "", "", 0);
            running = true;
        }
    }

    public void stopSession(final Context context) {
    	//System.out.println("UserTracker.stopSession()");
    	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && running) {
            trackEvent("SessionStop", "", "", 0);
        	running = false;
        }
    }*/

    public void trackActivity(final String activityName) {
    	//System.out.println("UserTracker.trackAcivity() " + activityName);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && tracker != null) {
            Map<String, String> hitParameters = new HashMap<String, String>();
        	hitParameters.put(Fields.HIT_TYPE, "appview");
        	hitParameters.put(Fields.SCREEN_NAME, activityName);
        	tracker.send(hitParameters);
        }
    }

    public void trackEvent(final String category, final String action, final String label, final int value) {
    	//System.out.println("UserTracker.trackEvent() " + category + " " + action);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && tracker != null) {
            HashMap<String, String> hitParameters = new HashMap<String, String>();
        	hitParameters.put(Fields.EVENT_CATEGORY, category);
        	hitParameters.put(Fields.EVENT_ACTION, action);
        	hitParameters.put(Fields.EVENT_LABEL, label);
        	hitParameters.put(Fields.EVENT_VALUE, Integer.toString(value));
        	tracker.send(hitParameters);
        }
    }

    public void setDebug(boolean debug, Context context) {
    	try {
    		Logger.LogLevel logLevel = Logger.LogLevel.ERROR;
    		if (debug) {
    			logLevel = Logger.LogLevel.VERBOSE;
    		}
    		GoogleAnalytics.getInstance(context).getLogger().setLogLevel(logLevel);
    	} catch (Throwable t) {
    		LoggerUtils.error("UserTracker.setDebug() error:", t);
    	}
    }

    public void setDryRun(boolean dryRun, Context context) {
    	try {
    		this.dryRun = dryRun;
    		GoogleAnalytics.getInstance(context).setDryRun(dryRun);
    	} catch (Throwable t) {
    		LoggerUtils.error("UserTracker.setDryRun() error:", t);
    	}
    }
    
    private String getId(final Context context) {
    	return context.getResources().getString(R.string.gaId);
    }
    
    //send my location action
    
    public void sendMyLocation() {
        int useCount = ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT, 0);
        ConfigurationManager.getInstance().putInteger(ConfigurationManager.USE_COUNT, useCount + 1);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)
             && ConfigurationManager.getInstance().isOn(ConfigurationManager.SEND_MY_POS_AT_STARTUP)) {
            ConfigurationManager.getInstance().remove(ConfigurationManager.SEND_MY_POS_AT_STARTUP);
            ConfigurationManager.getDatabaseManager().saveConfiguration(false);
            LoggerUtils.debug("Sending my location at startup.");
            new SendMyLocationTask(10).execute();
        } else {
        	LoggerUtils.debug("Skipping sending my location at startup.");
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
            } else {
            	LoggerUtils.debug("Can't send my location is missing");
            }

            return errorMessage;
        }

        @Override
        protected void onPostExecute(String errorMessage) {
        }
    }
}
