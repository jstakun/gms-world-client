package com.jstakun.gms.android.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
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

    public synchronized static UserTracker getInstance() {
        if (userTracker == null) {
            userTracker = new UserTracker();
        }
        return userTracker;
    }

    public synchronized void initialize(Application context) {
    	try {
    		if (tracker == null && OsUtil.isDonutOrHigher()) {
    			GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
    			analytics.setLocalDispatchPeriod(30);
    			analytics.enableAutoActivityReports(context);
    			tracker = analytics.newTracker(getId(context));
    			tracker.enableAutoActivityTracking(true);
    			tracker.enableExceptionReporting(true);
        	}
    	} catch (Throwable t) {
    		LoggerUtils.error("UserTracker.initialize exception:", t);
    	}
    }
    
    public void trackActivityStart(Activity activity) {
    	GoogleAnalytics.getInstance(activity).reportActivityStart(activity);
    }
    
    public void trackActivityStop(Activity activity) {
    	GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
    }
    
    public void trackActivity(final String activityName) {
    	//System.out.println("UserTracker.trackAcivity() " + activityName);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && tracker != null) {
            tracker.setScreenName(activityName);
        	tracker.send(new HitBuilders.EventBuilder().setCategory(activityName).setAction("start").build());
        }
    }

    public void trackEvent(final String category, final String action, final String label, final long value) {
    	//System.out.println("UserTracker.trackEvent() " + category + " " + action);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER) && tracker != null) {
            tracker.send(new HitBuilders.EventBuilder()
            	.setCategory(category)
            	.setAction(action)
            	.setLabel(label).
            	setValue(value).build());
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
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.SEND_MY_POS_AT_STARTUP)) {
        //if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER)
        //     && ConfigurationManager.getInstance().isOn(ConfigurationManager.SEND_MY_POS_AT_STARTUP)) {
        	ConfigurationManager.getInstance().remove(ConfigurationManager.SEND_MY_POS_AT_STARTUP);
            ConfigurationManager.getDatabaseManager().saveConfiguration(false);
            LoggerUtils.debug("I'm sending current location.");
            new SendMyLocationTask(10).execute();
        } else {
        	LoggerUtils.debug("I'm skipping sending current location.");
        }
    }

    private class SendMyLocationTask extends GMSAsyncTask<Void, Void, String> {

        public SendMyLocationTask(int priority) {
            super(priority, SendMyLocationTask.class.getName());
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
