/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
public class LoginActivity extends Activity implements OnClickListener {

    private View loginButton, cancelButton;
    private EditText loginText, passwordText;
    private IntentsHelper intents;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.login);
        setContentView(R.layout.login);
        
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        intents = new IntentsHelper(this, null, null);

        initComponents();
    }

    private void initComponents() {
        loginButton = findViewById(R.id.loginButton);
        cancelButton = findViewById(R.id.cancelButton);
        loginText = (EditText) findViewById(R.id.loginText);
        passwordText = (EditText) findViewById(R.id.passwordText);

        //if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GMS_AUTH_STATUS)) {
        //    loginText.setText(ConfigurationManager.getInstance().getString(ConfigurationManager.USERNAME));
        //    passwordText.setText(ConfigurationManager.getInstance().getString(ConfigurationManager.PASSWORD));
        //}

        AdsUtils.loadAd(this);

        loginButton.setOnClickListener(LoginActivity.this);
        cancelButton.setOnClickListener(LoginActivity.this);
    }

    public void onClick(View v) {
        if (v == loginButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".LoginAction", "", 0);
            if (loginText.getText().length() > 0 && passwordText.getText().length() > 0) {
                AsyncTaskManager asyncTaskManager = ConfigurationManager.getInstance().getTaskManager();
                if (asyncTaskManager != null) {
                	asyncTaskManager.executeLoginTask(loginText.getText().toString(), passwordText.getText().toString());
                	finish();
                } else {
                	intents.showInfoToast(Locale.getMessage(R.string.Unexpected_error));
                }
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Empty_credentials_error));
            }
        } else if (v == cancelButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".LoginCancelled", "", 0);
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //UserTracker.getInstance().stopSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
