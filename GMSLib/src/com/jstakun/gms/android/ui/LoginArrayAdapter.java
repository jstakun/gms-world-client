package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jstakun
 */
public class LoginArrayAdapter extends ArrayAdapter<String> {

    private Activity context;
    private static final Map<String, Integer> icons = new HashMap<String, Integer>();

    static {
        icons.put(OAuthServiceFactory.getServiceName(Commons.FACEBOOK), R.drawable.facebook_icon);
        icons.put(OAuthServiceFactory.getServiceName(Commons.FOURSQUARE), R.drawable.foursquare);
        icons.put(OAuthServiceFactory.getServiceName(Commons.GOOGLE), R.drawable.google_plus);
        icons.put(OAuthServiceFactory.getServiceName(Commons.TWITTER), R.drawable.twitter_icon);
        icons.put(OAuthServiceFactory.getServiceName(Commons.LINKEDIN), R.drawable.linkedin_icon);
        icons.put(ConfigurationManager.GMS_WORLD,R.drawable.globe16_new);
    };

    public LoginArrayAdapter(Activity context, List<String> logins) {
      super(context, R.layout.intentrow, logins);
      this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        convertView = newView(parent);
      }

      bindView(position, convertView);
      return(convertView);
    }

    private View newView(ViewGroup parent) {
      return context.getLayoutInflater().inflate(R.layout.intentrow, parent, false);
    }

    private void bindView(int position, View row) {
      TextView label=(TextView)row.findViewById(R.id.intentLabel);
      label.setText(getItem(position));
      label.setCompoundDrawablesWithIntrinsicBounds(icons.get(getItem(position)), 0, 0, 0);
    }
}
