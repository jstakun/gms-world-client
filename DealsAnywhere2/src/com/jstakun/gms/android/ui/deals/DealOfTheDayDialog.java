/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui.deals;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout.LayoutParams;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.ui.ActionBarHelper;
import com.jstakun.gms.android.ui.AlertDialogBuilder;
import com.jstakun.gms.android.ui.IntentsHelper;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
public class DealOfTheDayDialog extends Dialog implements OnClickListener, OnCancelListener {

    protected static final int OPEN = 10;
    protected static final int CALL = 11;
    protected static final int ROUTE = 12;
    protected static final int SEND_MAIL = 13;
    
    private View lvCloseButton, lvCallButton, lvOpenButton, lvSendMailButton, lvRouteButton;
    private CheckBox showAtStratup;
    private Activity activity;
    private ExtendedLandmark recommended;
    private IntentsHelper intents;
    private Handler parentHandler;

    public DealOfTheDayDialog(Activity activity, ExtendedLandmark recommended, double[] myPos, Handler parentHandler, IntentsHelper intents) {
        super(activity);
        this.activity = activity;
        this.recommended = recommended;
        this.parentHandler = parentHandler;
        this.intents = intents;

        //UserTracker.getInstance().startSession(getContext());
        UserTracker.getInstance().trackActivity(getClass().getName());

        setContentView(R.layout.dod);

        initComponents(myPos);
    }

    private void initComponents(double[] myPos) {
        setTitle(Locale.getMessage(R.string.titleDodDialog));
        setCancelable(true);

        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        setOnCancelListener(this);

        ActionBarHelper.hide(activity);

        intents.showLandmarkDetailsView(recommended, findViewById(R.id.lvView), myPos, false);

        showAtStratup = (CheckBox) findViewById(R.id.showDodCheckbox);

        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvSendMailButton = findViewById(R.id.lvSendMailButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvCarRouteButton);

        showAtStratup.setVisibility(View.VISIBLE);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY)) {
            showAtStratup.setChecked(true);
        } else {
            showAtStratup.setChecked(false);
        }

        showAtStratup.setOnClickListener(this);
        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvSendMailButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v == lvCloseButton) {
            dismiss();
            if (!activity.findViewById(R.id.lvView).isShown()) {
                ActionBarHelper.show(activity);
            }
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.CloseSelectedDealView", "", 0);
            //UserTracker.getInstance().stopSession(getContext());
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        } else if (v == lvOpenButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.OpenURLSelectedDeal", recommended.getLayer(), 0);
            parentHandler.sendEmptyMessage(OPEN);
        } else if (v == lvCallButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.CallSelectedDeal", recommended.getLayer(), 0);
            parentHandler.sendEmptyMessage(CALL);
        } else if (v == lvRouteButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.ShowRouteSelectedDeal", recommended.getLayer(), 0);
            parentHandler.sendEmptyMessage(ROUTE);
        } else if (v == lvSendMailButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.ShareSelectedDeal", recommended.getLayer(), 0);
            parentHandler.sendEmptyMessage(SEND_MAIL);
        } else if (v == showAtStratup) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.CheckboxSelectedDeal", recommended.getLayer(), 0);
            if (((CheckBox) v).isChecked()) {
                ConfigurationManager.getInstance().setOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY);
            } else {
                ConfigurationManager.getInstance().setOff(ConfigurationManager.SHOW_DEAL_OF_THE_DAY);
            }
        }
    }

    public void onCancel(DialogInterface di) {
        if (!activity.findViewById(R.id.lvView).isShown()) {
            ActionBarHelper.show(activity);
        }
        //UserTracker.getInstance().stopSession(getContext());
        ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
    }
}
