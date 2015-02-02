package com.jstakun.gms.android.ui.deals;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

public class NavigationDrawerListAdapter extends ArrayAdapter<String>{

	public NavigationDrawerListAdapter(Context context, int resource, List<String> objects) {
		super(context, resource, objects);
	}
	
	@Override
	public long getItemId(int position) {
		/*0 <item>Deals List</item>
          1 <item>Hot deals</item>
          2 <item>Newest Deals</item>
          3 <item>Deals Nearby</item>
          4 <item>Find location</item>
          5 <item>Saved locations</item>
          6 <item>Recently viewed deals</item>
          7 <item>Show deal of the day</item>
          8 <item>Deals calendar</item>*/
		
		if (position == 0) {
			return R.id.listMode;
		} else if (position == 1) {
			return R.id.hotDeals;
		} else if (position == 2) {
			return R.id.newestDeals;
		} else if (position == 3) {
			return R.id.nearbyDeals;
		} else if (position == 4) {
			return R.id.pickMyPos;
		} else if (position == 5) {
			return R.id.showMyLandmarks;
		} else if (position == 6) {
			return R.id.recentLandmarks;
		} else if (position == 7) {
			return R.id.showDoD;
		} else if (position == 8) {
			return R.id.events;
		}   
		
		return 0;	
	}
	
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {
		return super.getView(position, convertView, parent);
	}
}
