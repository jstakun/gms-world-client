package com.jstakun.gms.android.social;

import org.json.JSONObject;

import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.Token;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public abstract class AbstractSocialUtils implements ISocialUtils {

	protected Token accessToken;
    
	protected AbstractSocialUtils() {
	    accessToken = loadAccessToken();	
	}
	
	@Override
	public abstract boolean initOnTokenPresent(JSONObject json);

	@Override
	public void storeAccessToken(Token accessToken)
	{
		this.accessToken = accessToken;
	}
	
	@Override
    public Token getAccessToken() {
        return accessToken;
    }

	@Override
	public String sendPost(ExtendedLandmark landmark, int type) {
		return null;
	}

	@Override
	public void logout() {
		//ConfigurationManager.getInstance().resetUser();
        
        accessToken = null;

        ConfigurationManager.getDatabaseManager().saveConfiguration(false);
	}

	@Override
	public String checkin(String placeId, String name, Double lat, Double lng) {
		return null;
	}

	@Override
	public String sendComment(String placeId, String message, String name) {
		return null;
	}

	@Override
	public String addPlace(String name, String desc, String category,
			double lat, double lng) {
		return null;
	}
	
	public String getKey(String url) {
		return url;
	}
	
	protected abstract Token loadAccessToken();
	
    protected void onCheckin(String key) {
    	LoggerUtils.debug("Updating check-in with key " + key);
        FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        if (fdb != null) {
        	fdb.updateOnCheckin(key);
        } else {
        	LoggerUtils.debug("AbstractSocialUtils.onCheckin() fdb == null");
        }
    }
    
    protected class SendPostTask extends GMSAsyncTask<Void, Void, Void> {

    	private int type;
    	private ExtendedLandmark landmark;
    	
        public SendPostTask(int priority, int type, ExtendedLandmark landmark) {
			super(priority);
			this.type = type;
			this.landmark = landmark;
		}

		@Override
		protected Void doInBackground(Void... params) {
			sendPost(landmark, type);
			return null;
		}
	}
}
