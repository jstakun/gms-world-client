/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

/**
 *
 * @author jstakun
 */
import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class SocialListActivity extends ListActivity {

    /** Called when the activity is first created. */
    private TextView footer = null;
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        setContentView(R.layout.gms_list);
        AdsUtils.loadAd(this);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        footer = new TextView(this);        
        ListView listView = getListView();
        listView.addFooterView(footer);
    }

    @Override
    public void onResume() {
        super.onResume();

        String username = ConfigurationManager.getUserManager().getLoggedInUsername();
        if (username != null) {
            footer.setText(Locale.getMessage(R.string.Social_login_string, username));
        } else {
            footer.setText(Locale.getMessage(R.string.Social_notLogged));
        }

        setListAdapter(new SocialArrayAdapter(this, footer));
    }

    @Override
    protected void onStop() {
        super.onStop();
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	IntentsHelper.getInstance().setActivity(this);
    	if (requestCode == IntentsHelper.INTENT_LAYERS) {
    		if (resultCode == Activity.RESULT_OK) {
    			String action = intent.getStringExtra("action");
    			if (StringUtils.equals(action, "load")) {
    				String layer = intent.getStringExtra("layer");
    				if (layer != null) {
    					Intent result = new Intent();
    					result.putExtra("action", "load");
    					result.putExtra("layer", layer);
    					setResult(RESULT_OK, result);
    					finish();
    				}
    			}
    		}   
    	}
    }
}
