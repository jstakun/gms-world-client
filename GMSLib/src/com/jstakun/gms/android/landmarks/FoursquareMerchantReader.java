package com.jstakun.gms.android.landmarks;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class FoursquareMerchantReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		params.put("lang", Locale.getDefault().getLanguage());
		
		String categoryid = CategoriesManager.getInstance().getEnabledCategoriesString();
        if (StringUtils.isNotEmpty(categoryid)) {
            params.put("&categoryid", categoryid);
        }
        
		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
			ISocialUtils fsUtils = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE);
			String token = fsUtils.getAccessToken().getToken();
			if (token != null) {
				params.put("token", token);
			}	
		}
	}
	
	@Override
	protected String getUri() {
		return "foursquareMerchant";		
	}

	@Override
	public String getLayerName(boolean formatted) {
		if (formatted) {
			return "Merchants by Foursquare";
		} else {
			return Commons.FOURSQUARE_MERCHANT_LAYER;
		}
	}
	
	@Override
	public boolean isCheckinable() {
    	return true;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Foursquare_Merchant_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.gift;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.gift_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.gift_128;
	}
	
	@Override
	public int getPriority() {
		return 11;
	}
}
