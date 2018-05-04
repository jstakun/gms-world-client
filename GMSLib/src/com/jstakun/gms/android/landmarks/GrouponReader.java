package com.jstakun.gms.android.landmarks;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.ui.lib.R;

/**
 *
 * @author jstakun
 */
public class GrouponReader extends AbstractSerialReader {

   	@Override
	protected void init(double latitude, double longitude, int zoom) {
		super.init(latitude, longitude, zoom);
   		int dist = radius * 4;
		params.put("radius", Integer.toString(dist));
	    String categoryid = CategoriesManager.getInstance().getEnabledCategoriesString();
        if (StringUtils.isNotEmpty(categoryid)) {
           params.put("categoryid", categoryid);
        }
    }

	@Override
	protected String getUri() {
		return "grouponProvider";
	}
	
	@Override
	public String getLayerName(boolean formatted) {
		return Commons.GROUPON_LAYER;
	}

	@Override
	public int getDescriptionResource() {
		return R.string.Layer_Groupon_desc;
	}

	@Override
	public int getSmallIconResource() {
		return R.drawable.groupon_icon;
	}

	@Override
	public int getLargeIconResource() {
		return R.drawable.groupon_24;
	}

	@Override
	public int getImageResource() {
		return R.drawable.groupon_128;
	}
	
	@Override
	public int getPriority() {
		return 11;
	}
}
