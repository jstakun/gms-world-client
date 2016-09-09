package com.jstakun.gms.android.ui;

/**
 *
 * @author jstakun
 */
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.GMSUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

public class SocialArrayAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final TextView footer;
    private IntentsHelper intents;
    private static final String[] services = {Commons.GMS_WORLD, Commons.FACEBOOK, Commons.FOURSQUARE,
        Commons.GOOGLE, Commons.TWITTER, Commons.LINKEDIN};
    private static final String[] send_status = {"0", ConfigurationManager.FB_SEND_STATUS, ConfigurationManager.FS_SEND_STATUS,
        ConfigurationManager.GL_SEND_STATUS, ConfigurationManager.TWEET_SEND_STATUS, ConfigurationManager.LN_SEND_STATUS};
    private static final String[] auth_status = {ConfigurationManager.GMS_AUTH_STATUS, ConfigurationManager.FB_AUTH_STATUS, ConfigurationManager.FS_AUTH_STATUS,
        ConfigurationManager.GL_AUTH_STATUS, ConfigurationManager.TWEET_AUTH_STATUS, ConfigurationManager.LN_AUTH_STATUS};
    private static final boolean[] checkbox_status = {false, true, false, true, true, true};
    private static final int[] icons = {R.drawable.globe16_new, R.drawable.facebook_icon, R.drawable.foursquare,
        R.drawable.google_plus, R.drawable.twitter_icon, R.drawable.linkedin_icon};
    
    public SocialArrayAdapter(Activity context, TextView footer) {
        super(context, R.layout.socialrow,
                new String[]{
                    ConfigurationManager.GMS_WORLD,
                    OAuthServiceFactory.getServiceName(Commons.FACEBOOK),
                    OAuthServiceFactory.getServiceName(Commons.FOURSQUARE),
                    OAuthServiceFactory.getServiceName(Commons.GOOGLE),
                    OAuthServiceFactory.getServiceName(Commons.TWITTER),
                    OAuthServiceFactory.getServiceName(Commons.LINKEDIN)});
        this.context = context;
        this.footer = footer;
        intents = new IntentsHelper(context);
    }
    
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.socialrow, null, true);
            holder = new ViewHolder();
            holder.headerText = (TextView) rowView.findViewById(R.id.socialStatusHeader);
            holder.statusText = (TextView) rowView.findViewById(R.id.socialStatusText);
            holder.socialCheckbox = (CheckBox) rowView.findViewById(R.id.socialStatusCheckbox);
            holder.loginButton = (Button) rowView.findViewById(R.id.socialLoginButton);
            holder.footer = footer;

            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        rowView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (ConfigurationManager.getInstance().isOn(auth_status[position])) {
                	String username;
                	username =  OAuthServiceFactory.getUsername(services[position]);
                	intents.startActionViewIntent(ConfigurationManager.SERVER_URL + "socialProfile?uid=" + username);
                }  else if (ConfigurationManager.getInstance().isOff(auth_status[position])) {
                    if (position == 0) {
                        intents.startLoginActivity();
                    } else {
                        intents.startOAuthActivity(services[position]);
                    }
                }
            }
        });    

        String adapter = getItem(position);
        
        if (checkbox_status[position] && !ConfigurationManager.getInstance().isDisabled(send_status[position])
                && ConfigurationManager.getInstance().isOn(auth_status[position])) {
        	holder.socialCheckbox.setText(Locale.getMessage(R.string.Social_allow_sending, adapter));
            holder.socialCheckbox.setVisibility(View.VISIBLE);
            holder.socialCheckbox.setEnabled(true);
            holder.socialCheckbox.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (((CheckBox) v).isChecked()) {
                        ConfigurationManager.getInstance().setOn(send_status[position]);
                    } else {
                        ConfigurationManager.getInstance().setOff(send_status[position]);
                    }
                }
            });

            if (ConfigurationManager.getInstance().isOn(send_status[position])) {
                holder.socialCheckbox.setChecked(true);
            } else {
                holder.socialCheckbox.setChecked(false);
            }
        } else if (!checkbox_status[position] || ConfigurationManager.getInstance().isOff(auth_status[position])) {
            holder.socialCheckbox.setVisibility(View.GONE);
        }

        holder.headerText.setText(adapter);
        if (ConfigurationManager.getInstance().isOn(auth_status[position])) {
        	String username = OAuthServiceFactory.getDisplayname(services[position]);
        	String loginDate = OAuthServiceFactory.getLoginDate(services[position]);
        	if (loginDate != null) {
        		holder.statusText.setText(Locale.getMessage(R.string.Social_login_statusyeswithdate, username, loginDate));
        	} else {
        		holder.statusText.setText(Locale.getMessage(R.string.Social_login_statusyes, username));
        	}
        } else {
            holder.statusText.setText(Locale.getMessage(R.string.Social_login_statusno, adapter));
        }

        holder.headerText.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0);
        
        holder.loginButton.setOnClickListener(new PositionClickListener(position, holder, intents, context, this));

        if (ConfigurationManager.getInstance().isOn(auth_status[position])) {
            holder.loginButton.setText(Locale.getMessage(R.string.Social_logoutButton, adapter));
        } else {
            holder.loginButton.setText(Locale.getMessage(R.string.Social_loginButton, adapter));
        }
        
        return rowView;
    }

    private static class ViewHolder {

        protected CheckBox socialCheckbox;
        protected TextView headerText;
        protected TextView statusText;
        protected Button loginButton;
        protected TextView footer;
    }

    private static class PositionClickListener implements View.OnClickListener {

        private int position;
        private ViewHolder holder;
        private IntentsHelper intents;
        private Activity context;
        private SocialArrayAdapter socialAdapter;
        
        public PositionClickListener(int pos, ViewHolder holder, IntentsHelper intents, Activity context, SocialArrayAdapter socialAdapter) {
            this.position = pos;
            this.holder = holder;
            this.intents = intents;
            this.context = context;
            this.socialAdapter = socialAdapter;
        }

        public void onClick(View v) {
            if (position == 0) { //GMS World
                if (ConfigurationManager.getInstance().isOn(auth_status[0])) {
                    //gms logout
                	GMSUtils.logout();
                	socialAdapter.notifyDataSetChanged();
                    intents.showInfoToast(Locale.getMessage(R.string.Social_Logout_successful));
                    //refresh listview footer
                    String username = ConfigurationManager.getUserManager().getLoggedInUsername();
                    if (username != null) {
                        holder.footer.setText(Locale.getMessage(R.string.Social_login_string, username));
                    } else {
                    	holder.footer.setText(Locale.getMessage(R.string.Social_notLogged));
                    }
                } else if (ConfigurationManager.getInstance().isOff(auth_status[0])) {
                    //gms login
                	intents.startLoginActivity();
                    context.finish();
                }
            } else { //OAuth
                if (ConfigurationManager.getInstance().isOn(auth_status[position])) {
                    //oauth logout
                    OAuthServiceFactory.getSocialUtils(services[position]).logout();
                    socialAdapter.notifyDataSetChanged();
                    intents.showInfoToast(Locale.getMessage(R.string.Social_Logout_successful));
                    //refresh listview footer
                    String username = ConfigurationManager.getUserManager().getLoggedInUsername();
                    if (username != null) {
                    	holder.footer.setText(Locale.getMessage(R.string.Social_login_string, username));
                    } else {
                    	holder.footer.setText(Locale.getMessage(R.string.Social_notLogged));
                    }
                    //
                } else if (ConfigurationManager.getInstance().isOff(auth_status[position])) {
                    //oauth login
                    intents.startOAuthActivity(services[position]);
                }
            }
        }
    }
}
