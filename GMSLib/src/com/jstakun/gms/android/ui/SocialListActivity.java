/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

/**
 *
 * @author jstakun
 */
import android.app.ListActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class SocialListActivity extends ListActivity {

    /** Called when the activity is first created. */
    private TextView footer = null;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.mylist);
        AdsUtils.loadAd(this);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        footer = new TextView(this);        
        ListView listView = getListView();
        listView.addFooterView(footer);
    }

    @Override
    public void onResume() {
        super.onResume();

        String oauthUser = ConfigurationManager.getInstance().getOAuthLoggedInUsername();
        if (oauthUser != null) {
            footer.setText(Locale.getMessage(R.string.Social_login_string, oauthUser));
        } else if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GMS_AUTH_STATUS)) {
            footer.setText(Locale.getMessage(R.string.Social_login_string, ConfigurationManager.getInstance().getString(ConfigurationManager.USERNAME)));
        } else {
            footer.setText(Locale.getMessage(R.string.Social_notLogged));
        }

        setListAdapter(new SocialArrayAdapter(this, footer));
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession();
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
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
