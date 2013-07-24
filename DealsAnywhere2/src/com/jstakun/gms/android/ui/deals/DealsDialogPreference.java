/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui.deals;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import com.jstakun.gms.android.ui.AsyncTaskManager;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.Locale;

/**
 *
 * @author jstakun
 */
public class DealsDialogPreference extends DialogPreference {

    public DealsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            AsyncTaskManager asyncTaskManager = (AsyncTaskManager) ConfigurationManager.getInstance().getObject("asyncTaskManager", AsyncTaskManager.class);
            if (asyncTaskManager != null) {
                asyncTaskManager.executeClearStatsTask(Locale.getMessage(R.string.titleDealPreferences));
            }
        }
    }
}
