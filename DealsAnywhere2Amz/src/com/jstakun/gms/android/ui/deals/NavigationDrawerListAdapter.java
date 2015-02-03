package com.jstakun.gms.android.ui.deals;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jstakun.gms.android.ui.NavigationDrawerListItem;

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
		rebuild();
	}
	
	public void rebuild() {
		clear();
		//TODO add conditions to show items
		add(new NavigationDrawerListItem(names[0], R.id.listMode));
		//TODO condition for Hot deals
		add(new NavigationDrawerListItem(names[1], R.id.hotDeals));
		//TODO condition for Newest Deals
		add(new NavigationDrawerListItem(names[2], R.id.newestDeals));
		//TODO condition for Deals Nearby
		add(new NavigationDrawerListItem(names[3], R.id.nearbyDeals));
		add(new NavigationDrawerListItem(names[4], R.id.pickMyPos));
		add(new NavigationDrawerListItem(names[5], R.id.showMyLandmarks));
		//TODO condition for Recently viewed deals
		add(new NavigationDrawerListItem(names[6], R.id.recentLandmarks));
		//TODO condition for Show deal of the day
		add(new NavigationDrawerListItem(names[7], R.id.showDoD));
		add(new NavigationDrawerListItem(names[8], R.id.events));
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
