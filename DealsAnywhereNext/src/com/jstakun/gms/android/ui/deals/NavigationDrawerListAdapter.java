package com.jstakun.gms.android.ui.deals;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.NavigationDrawerListItem;
import com.jstakun.gms.android.utils.ProjectionInterface;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class NavigationDrawerListAdapter extends ArrayAdapter<NavigationDrawerListItem>{

	/*0 <item>Deals List</item>
    1 <item>Hot deals</item>
    2 <item>Newest Deals</item>
    3 <item>Deals Nearby</item>
    4 <item>Find location</item>
    5 <item>Saved locations</item>
    6 <item>Recently viewed deals</item>
    7 <item>Show deal of the day</item>
    8 <item>Deals calendar</item> */
	
	private final Activity parentActivity;
    private String[] names;
	
	public NavigationDrawerListAdapter(Activity context, int resource) {
		super(context, resource);
		this.names = context.getResources().getStringArray(R.array.navigation);
		this.parentActivity = context;
		rebuild(null);
	}
	
	public void rebuild(ProjectionInterface projection) {
		clear();
		
		LandmarkManager landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
		
		add(new NavigationDrawerListItem(names[0], R.id.listMode));
		
		final String[] excluded = new String[]{Commons.MY_POSITION_LAYER, Commons.ROUTES_LAYER, Commons.LOCAL_LAYER};
        if (landmarkManager != null && landmarkManager.hasDealsOfTheDay(excluded)) {
        	add(new NavigationDrawerListItem(names[1], R.id.hotDeals));
        }
        
        if (landmarkManager != null && landmarkManager.hasNewLandmarks(2, excluded)) {
        	add(new NavigationDrawerListItem(names[2], R.id.newestDeals));
        }
		
        if (landmarkManager != null && projection != null && landmarkManager.hasVisibleLandmarks(projection, true)) {
        	add(new NavigationDrawerListItem(names[3], R.id.nearbyDeals));
        }
		
        add(new NavigationDrawerListItem(names[4], R.id.pickMyPos));
		add(new NavigationDrawerListItem(names[5], R.id.showMyLandmarks));
		
		if (landmarkManager != null && landmarkManager.hasRecentlyOpenedLandmarks()) {
		    add(new NavigationDrawerListItem(names[6], R.id.recentLandmarks));
		}
		
		CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
		if (landmarkManager != null && cm != null && landmarkManager.hasRecommendedCategory(cm.getTopCategory(), cm.getTopSubCategory())) {
			add(new NavigationDrawerListItem(names[7], R.id.showDoD));
		}
		add(new NavigationDrawerListItem(names[8], R.id.events));
		
		notifyDataSetChanged();
	}
	
	@Override
	public long getItemId(int position) {
		return getItem(position).getResource();
	}
	
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {
		//return super.getView(position, convertView, parent);
		TextView itemView = (TextView)convertView;
        if (null == itemView) {
        	LayoutInflater inflater = parentActivity.getLayoutInflater();
        	itemView = (TextView)inflater.inflate(R.layout.drawerrow_parent, null, true);
        }
        itemView.setText(getItem(position).getName());
        return itemView;
	}
}
