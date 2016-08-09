package com.jstakun.gms.android.ui;

import android.app.Activity;

/**
 *
 * @author jstakun
 */
public class ActionBarHelper {

    public static void setDisplayHomeAsUpEnabled(Activity activity) {
        try {
            HelperInternal.setDisplayHomeAsUpEnabled(activity);
        } catch (Throwable ex) {
        }
    }
    
    public static void hide(Activity activity) {
        try {
            HelperInternal.hide(activity);
        } catch (Throwable ex) {
        }
    }
    
    public static void show(Activity activity) {
        try {
            HelperInternal.show(activity);
        } catch (Throwable ex) {
        }
    }
    
    private static class HelperInternal {

        private static void setDisplayHomeAsUpEnabled(Activity activity) {
            activity.getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        private static void hide(Activity activity) {
            activity.getActionBar().hide();
        }
        
        private static void show(Activity activity) {
            activity.getActionBar().show();
        }
    }
}
