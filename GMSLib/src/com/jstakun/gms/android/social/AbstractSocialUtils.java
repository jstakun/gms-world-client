package com.jstakun.gms.android.social;

import org.json.JSONObject;
import com.jstakun.gms.android.utils.Token;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.utils.GMSAsyncTask;

public abstract class AbstractSocialUtils implements ISocialUtils {

	protected Token accessToken;
    
	public AbstractSocialUtils() {
	    accessToken = loadAccessToken();	
	}
	
	@Override
	public abstract boolean initOnTokenPresent(JSONObject json);

	@Override
	public abstract void storeAccessToken(Token accessToken);
	
	@Override
	public boolean hasAccessToken() {
        return null != accessToken;
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
		if (!ConfigurationManager.getInstance().isUserLoggedIn()) {
            ConfigurationManager.getInstance().resetUser();
        }

        accessToken = null;

        ConfigurationManager.getInstance().saveConfiguration(false);
	}

	@Override
	public String checkin(String placeId, String name, String extras) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String sendComment(String placeId, String message, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addPlace(String name, String desc, String category,
			double lat, double lng) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected abstract Token loadAccessToken();
	
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
