package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class NavigationDrawerExpandableListAdapter extends BaseExpandableListAdapter {

	private Activity parentActivity;
	private List<NavigationDrawerListItem> parents = new ArrayList<NavigationDrawerListItem>();
	private List<NavigationDrawerListItem> landmarks = new ArrayList<NavigationDrawerListItem>();
	private List<NavigationDrawerListItem> checkins = new ArrayList<NavigationDrawerListItem>(); 
	private static final int CHECKIN = -1;
	private static final int LANDMARK = -2;
	
	public NavigationDrawerExpandableListAdapter(Activity parent) {
		this.parentActivity = parent;
		rebuild();
	}
	
	public void rebuild() {
		String[] parentGroup = parentActivity.getResources().getStringArray(R.array.navigation);
		String[] landmark = parentActivity.getResources().getStringArray(R.array.landmarks_submenu);
		String[] checkin = parentActivity.getResources().getStringArray(R.array.checkin_submenu);
	
		//0 <item>Layers</item>
        //1 <item>Landmarks</item> <!-- submenu -->
        //2 <item>Check-in</item> <!-- submenu -->
        //3 <item>Friends activities</item>
		//4 <item>Deals</item>
        //5 <item>Social Networks</item>
		
		//TODO add conditions to show items
		
		parents.clear();	    
	    parents.add(new NavigationDrawerListItem(parentGroup[0], R.id.showLayers));
	    parents.add(new NavigationDrawerListItem(parentGroup[1], LANDMARK));
	    parents.add(new NavigationDrawerListItem(parentGroup[2], CHECKIN));
	    parents.add(new NavigationDrawerListItem(parentGroup[3], R.id.friendsCheckins));
	    parents.add(new NavigationDrawerListItem(parentGroup[4], R.id.deals));
	    parents.add(new NavigationDrawerListItem(parentGroup[5], R.id.socialNetworks));
	
	    //<item>Nearby landmarks</item>
    	//<item>Create new landmark</item>
    	//<item>Recently opened</item>
    	//<item>Newest landmarks</item>
    	//<item>Saved landmarks</item>
    	//<item>Calendar</item>
    	//<item>Poi files</item>
    	
	    landmarks.clear();
	    landmarks.add(new NavigationDrawerListItem(landmark[0], R.id.listLandmarks));
	    landmarks.add(new NavigationDrawerListItem(landmark[1], R.id.addLandmark));
	    landmarks.add(new NavigationDrawerListItem(landmark[2], R.id.recentLandmarks));
	    landmarks.add(new NavigationDrawerListItem(landmark[3], R.id.newestLandmarks));
	    landmarks.add(new NavigationDrawerListItem(landmark[4], R.id.showMyLandmarks));
	    landmarks.add(new NavigationDrawerListItem(landmark[5], R.id.events));
	    landmarks.add(new NavigationDrawerListItem(landmark[6], R.id.loadPoiFile));
	    
	    //<item>Auto Check-In</item>
        //<item>Check-in at location</item>
        //<item>Check-in with QR code</item>
    	
	    checkins.clear();
	    checkins.add(new NavigationDrawerListItem(checkin[0], R.id.autocheckin));
	    checkins.add(new NavigationDrawerListItem(checkin[1], R.id.searchcheckin));
	    checkins.add(new NavigationDrawerListItem(checkin[2], R.id.qrcheckin));
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		if (parents.get(groupPosition).getResource() == LANDMARK) {
			return landmarks.get(childPosition).getResource();
		} else if (parents.get(groupPosition).getResource() == CHECKIN) {
			return checkins.get(childPosition).getResource();
		} else {
			return 0;
		}
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.drawerrow_child, null, true);
        }    
        if (parents.get(groupPosition).getResource() == LANDMARK) {
        	((TextView)rowView).setText(landmarks.get(childPosition).getName());
        } else if (parents.get(groupPosition).getResource() == CHECKIN) {
        	((TextView)rowView).setText(checkins.get(childPosition).getName());
        } 
      	return rowView;
	}

	@Override
	public int getChildrenCount(int position) {
		if (parents.get(position).getResource() == LANDMARK) {
			return landmarks.size();
		} else if (parents.get(position).getResource() == CHECKIN) {
			return checkins.size();
		} else {
			return 0;
		}
	}

	@Override
	public Object getGroup(int arg0) {
		return null;
	}

	@Override
	public int getGroupCount() {
		return parents.size();
	}

	@Override
	public long getGroupId(int position) {
		return parents.get(position).getResource();
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.drawerrow_parent, null, true);
        }    
        TextView textView = (TextView)rowView;
        textView.setText(parents.get(groupPosition).getName());
        return rowView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}

}
