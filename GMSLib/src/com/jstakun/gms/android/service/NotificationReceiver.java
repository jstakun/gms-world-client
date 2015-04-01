package com.jstakun.gms.android.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.location.AndroidDevice;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;

public class NotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		LoggerUtils.debug("NotificationReceiver.onReceive() calling running...");
		if (ConfigurationManager.getInstance().getContext() == null) {
    		ConfigurationManager.getInstance().setContext(context);
    	}
		
		Location location = AndroidDevice.getLastKnownLocation(context);
		
		if (location != null)
		{
			new SendNotificationTask(10).execute(location.getLatitude(), location.getLongitude());
		} else {
			new SendNotificationTask(10).execute();
		}
	}
	
	private class SendNotificationTask extends GMSAsyncTask<Double, Void, String> {

		public SendNotificationTask(int priority) {
            super(priority);
        }
		
		@Override
		protected String doInBackground(Double... params) {
			HttpUtils utils = new HttpUtils();
	        
	        try {
	        	String url = ConfigurationManager.SERVER_URL + "notifications";
	            List<NameValuePair> postParams = new ArrayList<NameValuePair>();
	        	postParams.add(new BasicNameValuePair("type", "u"));
	        	postParams.add(new BasicNameValuePair(ConfigurationManager.APP_ID, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID)));
	        	postParams.add(new BasicNameValuePair("lst", ConfigurationManager.getInstance().getString(ConfigurationManager.LAST_STARTING_DATE)));
	        	postParams.add(new BasicNameValuePair("uc", Integer.toString(ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT, 0))));
	            
	            String email = ConfigurationManager.getUserManager().getUserEmail();
	            if (StringUtils.isNotEmpty(email)) {
	            	postParams.add(new BasicNameValuePair("e", email));
	            }
	            
	            if (params != null && params.length == 2) {
	            	postParams.add(new BasicNameValuePair("lat", params[0].toString()));
	            	postParams.add(new BasicNameValuePair("lng", params[1].toString()));
	            }
	            
	            utils.sendPostRequest(url, postParams, true);
	            
	            String response = utils.getPostResponse();
	            if (StringUtils.isNotEmpty(response)) {
	                LoggerUtils.debug("Received response: " + response);
	            }              
	            if (utils.getResponseCode() == HttpStatus.SC_OK) {
	            	//user has been engaged
	            	long timestamp = 0l;
	            	if (StringUtils.startsWith(response, "{")) {
	            		JSONObject jsonResponse = new JSONObject(response);
	            		timestamp = jsonResponse.optLong("timestamp");
	            	}
	            	if (timestamp == 0l) {
            			timestamp = System.currentTimeMillis();
            		}
	            	ConfigurationManager.getInstance().putLong(ConfigurationManager.LAST_STARTING_DATE, timestamp);	
	            } //else if (utils.getResponseCode() == HttpStatus.SC_ACCEPTED) {
	            	//nothing to do
	            //}
	            ConfigurationManager.getInstance().putLong(ConfigurationManager.NOTIFICATION_RUNNING_DATE, System.currentTimeMillis());	
	            ConfigurationManager.getDatabaseManager().saveConfiguration(false);
	        } catch (Exception ex) {
	            LoggerUtils.error("NotificationReceiver.onReceive() exception:", ex);
	        } finally {
	            try {
	                if (utils != null) {
	                    utils.close();
	                }
	            } catch (Exception e) {
	            }
	        }
			return null;
		}	
	}
}
