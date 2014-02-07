/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TimePicker;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
public class SendBlogeoPostActivity extends Activity implements OnClickListener {

    private View sendButton, cancelButton;
    private EditText nameText, descText;
    private TimePicker timePicker;
    private long validityTime = 0;
    private Intents intents;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.blogeo);
        setContentView(R.layout.blogeoview);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
        
        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        intents = new Intents(this, null, null);

        initComponents();
    }

    private void initComponents() {
        sendButton = findViewById(R.id.sendButton);
        cancelButton = findViewById(R.id.cancelButton);
        descText = (EditText) findViewById(R.id.descText);
        nameText = (EditText) findViewById(R.id.nameText);
        timePicker = (TimePicker) findViewById(R.id.timePicker);

        sendButton.setOnClickListener(SendBlogeoPostActivity.this);
        cancelButton.setOnClickListener(SendBlogeoPostActivity.this);

        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(1);
        timePicker.setCurrentMinute(0);

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener()
        {
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
            {
                validityTime = (hourOfDay * DateTimeUtils.ONE_HOUR) + (minute * DateTimeUtils.ONE_MINUTE);
            }
        });

        AdsUtils.loadAd(this);
    }

    public void onClick(View v) {
        if (v == sendButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".SendBlogeoPostAction", "", 0);
            sendBlogeoPostAction();
        } else if (v == cancelButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".SendBlogeoPostCancelled", "", 0);
            setResult(RESULT_CANCELED, null);
            finish();
        }
    }

    private void sendBlogeoPostAction() {

        if (nameText.getText().toString().length() > 0) {
            Intent result = new Intent();
            result.putExtra("name", nameText.getText().toString());
            result.putExtra("desc", descText.getText().toString());
            result.putExtra("validityTime", validityTime);
            setResult(RESULT_OK, result);
            finish();
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.Landmark_name_empty_error));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //UserTracker.getInstance().stopSession(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
