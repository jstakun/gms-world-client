package com.jstakun.gms.android.ui;

import java.util.List;

import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.utils.UserTracker;

import android.os.Bundle;

public class ConfigurationViewerActivity extends AbstractLandmarkList {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UserTracker.getInstance().trackActivity(getClass().getName());
        
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
        	 List<LandmarkParcelable> configuration = extras.getParcelableArrayList("configuration");
        	 setListAdapter(new LandmarkArrayAdapter(this, configuration));
             sort(ORDER_TYPE.ORDER_BY_NAME, ORDER.ASC, false);
        } else {
        	finish();
        }
	}
}
