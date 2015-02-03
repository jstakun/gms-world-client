package com.jstakun.gms.android.ui.deals;

public class NavigationDrawerListItem {
	private String name;
	private int resource;
	
	public NavigationDrawerListItem (String name, int resource) {
		this.setName(name);
		this.setResource(resource);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getResource() {
		return resource;
	}

	public void setResource(int resource) {
		this.resource = resource;
	}
}
