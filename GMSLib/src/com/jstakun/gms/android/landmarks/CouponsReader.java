package com.jstakun.gms.android.landmarks;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class CouponsReader extends AbstractSerialReader {

	@Override
	protected void init(double latitude, double longitude, int zoom, int width, int height) {
		super.init(latitude, longitude, zoom, width, height);
		String categoryid = CategoriesManager.getInstance().getEnabledCategoriesString();
        if (StringUtils.isNotEmpty(categoryid)) {
            params.put("categoryid", categoryid);
        }
	}

	@Override
	protected String getUri() {
		return "couponsProvider";
	}

	@Override
	public String getLayerName(boolean formatted) {
		return Commons.COUPONS_LAYER;
	}
	
	@Override
	public boolean isEnabled() {
    	return false;
    }

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_8Coupons_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.dollar;
	}

	@Override
	public int getLargeIconResource() {
		return -1;
	}

	@Override
	public int getImageResource() {
		return R.drawable.coupon_128;
	}
	
	@Override
	public int getPriority() {
		return 11;
	}
}
