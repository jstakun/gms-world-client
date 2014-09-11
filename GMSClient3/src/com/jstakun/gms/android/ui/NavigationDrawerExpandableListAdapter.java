package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class NavigationDrawerExpandableListAdapter extends BaseExpandableListAdapter {

	private Activity parentActivity;
	private String[] parentGroup, landmarks, checkin;
	
	public NavigationDrawerExpandableListAdapter(Activity parent) {
		this.parentActivity = parent;
		parentGroup = parent.getResources().getStringArray(R.array.navigation);
		landmarks = parent.getResources().getStringArray(R.array.landmarks_submenu);
		checkin = parent.getResources().getStringArray(R.array.checkin_submenu);
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		//1 Landmarks
      	//2 Check-in
      	if (groupPosition == 1) {
        	//<item>Nearby landmarks</item>
        	//<item>Create new landmark</item>
        	//<item>Recently opened</item>
        	//<item>Newest landmarks</item>
        	//<item>Saved landmarks</item>
        	//<item>Calendar</item>
        	//<item>Poi files</item>
        	if (childPosition == 0) {
        		return R.id.listLandmarks;
        	} else if (childPosition == 1) {
        		return R.id.addLandmark;
        	} else if (childPosition == 2) {
        		return R.id.recentLandmarks;
        	} else if (childPosition == 3) {
        		return R.id.newestLandmarks;
        	} else if (childPosition == 4) {
        		return R.id.showMyLandmarks;
        	} else if (childPosition == 5) {
        		return R.id.events;
        	} else if (childPosition == 6) {
        		return R.id.loadPoiFile;
        	} 
        } else if (groupPosition == 2) {
        	//<item>Auto Check-In</item>
            //<item>Check-in at location</item>
            //<item>Check-in with QR code</item>
        	if (childPosition == 0) {
        		return R.id.autocheckin;
        	} else if (childPosition == 1) {
        		return R.id.searchcheckin;
        	} else if (childPosition == 2) {
        		return R.id.qrcheckin;
        	}
        } 
        return 0;
	}

	@Override
	public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.drawerrow_child, null, true);
        }    
        //1 Landmarks
      	//2 Check-in
      	if (groupPosition == 1) {
        	((TextView)rowView).setText(landmarks[childPosition]);
        } else if (groupPosition == 2) {
        	((TextView)rowView).setText(checkin[childPosition]);
        } 
      	return rowView;
	}

	@Override
	public int getChildrenCount(int position) {
		//1 Landmarks - 7
		//2 Check-in - 3
		if (position == 1) {
			return 7;
		} else if (position == 2) {
			return 3;
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
		return parentGroup.length;
	}

	@Override
	public long getGroupId(int position) {
		//0 <item>Layers</item>
        //1 <item>Landmarks</item> <!-- submenu -->
        //2 <item>Check-in</item> <!-- submenu -->
        //3 <item>Deals</item>
        //4 <item>Social Networks</item>
        if (position == 0) {
			return R.id.showLayers;
		} else if (position == 3) {
			return R.id.friendsCheckins;
		} else if (position == 4) {
			return R.id.deals;
		} else if (position == 5) {
			return R.id.socialNetworks;
		} else {
			return 0;
		}
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.drawerrow_parent, null, true);
        }    
        TextView textView = (TextView)rowView;
        textView.setText(parentGroup[groupPosition]);
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
