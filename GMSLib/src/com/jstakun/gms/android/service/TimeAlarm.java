package com.jstakun.gms.android.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

/**
 *
 * @author jstakun
 */
public class TimeAlarm extends BroadcastReceiver {

    private NotificationManager nm;

    @Override
    public void onReceive(Context context, Intent intent) {
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        CharSequence from = Locale.getMessage(R.string.app_name);
        CharSequence message = Locale.getMessage(R.string.New_version_short_message);
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL)));
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, view, 0);
        Notification notification = new Notification(R.drawable.globe64_new, message, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(context, from, message, contentIntent);
        nm.notify(1, notification);
    }
}
