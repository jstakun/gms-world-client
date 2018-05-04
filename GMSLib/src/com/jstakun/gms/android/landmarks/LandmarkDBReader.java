package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager.ClearPolicy;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public class LandmarkDBReader implements LayerReader {

	@Override
	public String readRemoteLayer(List<ExtendedLandmark> landmarks, double latitude, double longitude, int zoom, String layer, GMSAsyncTask<?, ?, ?> task) {
		if (landmarks.isEmpty()) {
			landmarks.addAll(ConfigurationManager.getDatabaseManager().getLandmarkDatabase());
		}
		return null;
	}

	@Override
	public void close() {
		
	}

	@Override
	public String[] getUrlPrefix() {
		return null;
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.LOCAL_LAYER;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Phone_Landmarks_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.ok16;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.ok;
	}

	@Override
	public int getImageResource() {
		return -1;
	}

	@Override
	public boolean isCheckinable() {
		return true;
	}

	@Override
	public boolean isPrimary() {
		return true;
	}

	@Override
	public ClearPolicy getClearPolicy() {
		return ClearPolicy.ONE_MONTH;
	}
	
	@Override
	public int getPriority() {
    	return 1;
    }
}
