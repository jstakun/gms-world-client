package com.jstakun.gms.android.ui.deals;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout.LayoutParams;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
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

    private View lvCloseButton, lvCallButton, lvOpenButton, lvShareButton, lvRouteButton;
    private CheckBox showAtStratup;
    private DealMapAmzActivity activity;
    private ExtendedLandmark recommended;
    
    public DealOfTheDayDialog(DealMapAmzActivity activity, ExtendedLandmark recommended) {
        super(activity);
        this.activity = activity;
        this.recommended = recommended;

        UserTracker.getInstance().trackActivity(getClass().getName());

        setContentView(R.layout.dod);

        initComponents();
    }

    private void initComponents() {
        setTitle(Locale.getMessage(R.string.titleDodDialog));
        setCancelable(true);

        getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        setOnCancelListener(this);

        activity.getActionBar().hide();
        IntentsHelper.getInstance().showLandmarkDetailsView(recommended, findViewById(R.id.lvView), activity.getMyPosition(), false);

        showAtStratup = (CheckBox) findViewById(R.id.showDodCheckbox);

        lvCloseButton = findViewById(R.id.lvCloseButton);
        lvOpenButton = findViewById(R.id.lvOpenButton);
        lvShareButton = findViewById(R.id.lvShareButton);
        lvCallButton = findViewById(R.id.lvCallButton);
        lvRouteButton = findViewById(R.id.lvRouteButton);

        showAtStratup.setVisibility(View.VISIBLE);
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY)) {
            showAtStratup.setChecked(true);
        } else {
            showAtStratup.setChecked(false);
        }

        showAtStratup.setOnClickListener(this);
        lvCloseButton.setOnClickListener(this);
        lvOpenButton.setOnClickListener(this);
        lvShareButton.setOnClickListener(this);
        lvCallButton.setOnClickListener(this);
        lvRouteButton.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v == lvCloseButton) {
            dismiss();
            if (!activity.findViewById(R.id.lvView).isShown()) {
                activity.getActionBar().show();
            }
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.CloseSelectedDealView", "", 0);
            //UserTracker.getInstance().stopSession(getContext());
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        } else if (v == lvOpenButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.OpenURLSelectedDeal", recommended.getLayer(), 0);
            IntentsHelper.getInstance().openButtonPressedAction(recommended);
        } else if (v == lvCallButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.CallSelectedDeal", recommended.getLayer(), 0);
            activity.callButtonPressedAction(recommended);
        } else if (v == lvRouteButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.ShowRouteSelectedDeal", recommended.getLayer(), 0);
            activity.loadRoutePressedAction(recommended);
        } else if (v == lvShareButton) {
            UserTracker.getInstance().trackEvent("Clicks", "DealOfTheDayDialog.ShareSelectedDeal", recommended.getLayer(), 0);
            activity.sendMessageAction();
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
            activity.getActionBar().show();
        }
        //UserTracker.getInstance().stopSession(getContext());
        ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
    }
}
