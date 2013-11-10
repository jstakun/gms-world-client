package com.jstakun.gms.android.service;

import java.util.Calendar;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GMSScheduleReceiver extends BroadcastReceiver {

	  private static final long ONE_DAY = 24 * 60 * 60 * 1000;
	  //private static final long ONE_HOUR = 60 * 60 * 1000;
			  
	  @Override
	  public void onReceive(Context context, Intent intent) {
		  LoggerUtils.debug("GMSScheduleReceiver.onReceive() executed...");
		  //run auto check-in broadcast
		  AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		  Calendar cal = Calendar.getInstance();
		  // Start x seconds after boot completed
		  cal.add(Calendar.SECOND, 60);
		  
		  if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
			  Intent i = new Intent(context, AutoCheckinStartServiceReceiver.class);
			  PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
			  // Fetch every x seconds
			  long repeat_time = ConfigurationManager.getInstance().getLong(ConfigurationManager.AUTO_CHECKIN_REPEAT_TIME);
			  if (repeat_time == -1) {
				  repeat_time = ConfigurationManager.DEFAULT_REPEAT_TIME;
			  }   
			  repeat_time = repeat_time * 1000;
			  LoggerUtils.debug("GMSScheduleReceiver.onReceive() setting AutoCheckinStartServiceReceiver interval " + repeat_time + " milliseconds...");
			  service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), repeat_time, pending);
		  }
		  
		  //run notification broadcast once a day
		  Intent i = new Intent(context, NotificationReceiver.class);
		  PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		  LoggerUtils.debug("GMSScheduleReceiver.onReceive() setting NotificationReceiver interval " + ONE_DAY + " milliseconds...");
		  service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), ONE_DAY, pending);
	  }
}
