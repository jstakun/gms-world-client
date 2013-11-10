/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

/**
 *
 * @author jstakun
 */
public class GMSNotificationManager {

    private int notificationCounter;
    private NotificationManager mNotificationManager = null;
    private Context context;

    public GMSNotificationManager(Context parent) {
        mNotificationManager = (NotificationManager) parent.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = parent;
        notificationCounter = 0;
    }

    public int createNotification(int icon, String ticker, String title, boolean delete) {
        int drawble = icon;
        if (drawble == -1) {
            drawble = R.drawable.globe24_new;        // icon from resources
        }

        int num = notificationCounter;
        notificationCounter++;

        String tickerText = Locale.getMessage(R.string.Task_started, ticker);              // ticker-text
        String contentTitle = Locale.getMessage(R.string.Task_in_progress, title);  // expanded message title
        String contentText = (delete == true) ? Locale.getMessage(R.string.Task_Click_to_cancel) : Locale.getMessage(R.string.Task_Click_to_open);      // expanded message text
        Intent notificationIntent = new Intent(context, context.getClass());
        //System.out.println("putExtra(\"notification\"," +  num + ")-----------------------------------");
        Bundle extras = new Bundle();
        extras.putInt("notification", num);
        extras.putBoolean("delete", delete);
        notificationIntent.putExtras(extras);
        PendingIntent contentIntent = PendingIntent.getActivity(context, num, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // the next two lines initialize the Notification, using the configurations above
        Notification notification = new Notification(drawble, tickerText, System.currentTimeMillis());
        //notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

        //System.out.println("Created Notification: " + num + "---------------------------------");
        mNotificationManager.notify(num, notification);

        return num;
    }

    public void cancelNotification(int num) {
        if (num >= 0) {
            //System.out.println("Cancelled Notification: " + num + "---------------------------------");
            mNotificationManager.cancel(num);
        }
    }

    public void cancelAll() {
        mNotificationManager.cancelAll();
    }
}
