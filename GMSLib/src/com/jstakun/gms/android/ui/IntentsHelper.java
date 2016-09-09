package com.jstakun.gms.android.ui;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDAO;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.FilenameFilterFactory;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.landmarks.LandmarkParcelableFactory;
import com.jstakun.gms.android.landmarks.LayerLoader;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.location.LocationServicesManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.service.AutoCheckinStartServiceReceiver;
import com.jstakun.gms.android.social.GMSUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.UserTracker;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.method.LinkMovementMethod;
import android.view.Display;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 *
 * @author jstakun
 */
public final class IntentsHelper {

    private static final int INTENT_ADD_LANDMARK = 0;
    private static final int INTENT_BLOGEO_POST = 1;
    private static final int INTENT_PREFS = 2;
    private static final int INTENT_FILES = 3;
    private static final int INTENT_QRCODECHECKIN = 4;
    private static final int INTENT_CHECKIN = 5;
    public static final int INTENT_PICKLOCATION = 6;
    public static final int INTENT_MULTILANDMARK = 7;
    public static final int INTENT_MYLANDMARKS = 8;
    protected static final int INTENT_LAYERS = 9;
    private static final int INTENT_CATEGORIES = 10;
    protected static final int INTENT_AUTO_CHECKIN = 11;
    public static final int INTENT_CALENDAR = 12;
    private static final int INTENT_CONFIGURATION_VIEWER = 13;
    
    public static final String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
    
    private Activity activity;
    private AsyncTaskManager asyncTaskManager;
    private List<ResolveInfo> activities;
    private static Toast longToast, shortToast;
    private static final Object shortMutex = new Object();
    private static final Object longMutex = new Object();   
    
    private static final ImageGetter imgGetter = new Html.ImageGetter() {
        @Override
        public Drawable getDrawable(String source) {
            Drawable drawable = null;
            Context context = ConfigurationManager.getInstance().getContext();
            if (context != null) {
            	int resId = context.getResources().getIdentifier(source, "drawable", context.getPackageName());
            	if (resId > 0) {
                	drawable = context.getResources().getDrawable(resId);
                	drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            	}
            }
            return drawable;
        }
    };

    public IntentsHelper(Activity activity, AsyncTaskManager asyncTaskManager) {
        this.activity = activity;
        this.asyncTaskManager = asyncTaskManager;
    }

    public void startPickLocationActivity() {
    	Intent intent = null;
    	if (isGoogleApiAvailable()) {
    		try {
    			AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
    			.setTypeFilter(AutocompleteFilter.TYPE_FILTER_GEOCODE) //.TYPE_FILTER_NONE) //everything
    			.build();

    			intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)//.MODE_FULLSCREEN)
    			.setFilter(typeFilter)
    			.build(activity);
    		} catch (Exception e) {
    			LoggerUtils.error("Intents.startPickLocationActivity() exception:", e);
    			intent = new Intent(activity, PickLocationActivity.class);
    		}
    	} else {
    		intent = new Intent(activity, PickLocationActivity.class);
    	}
    	activity.startActivityForResult(intent, INTENT_PICKLOCATION);
    }

    public void startSettingsActivity(Class<?> settingsActivityClass) {
        Intent intent = new Intent(activity, settingsActivityClass);
        Bundle extras = LayerManager.getInstance().loadLayersGroup();
        intent.putExtras(extras);
        activity.startActivityForResult(intent, INTENT_PREFS);
    }

    public void startAddLandmarkActivity() {
        activity.startActivityForResult(new Intent(activity, AddLandmarkActivity.class), INTENT_ADD_LANDMARK);
    }

    public void startAddLayerActivity() {
        activity.startActivity(new Intent(activity, AddLayerActivity.class));
    }

    public void startBlogeoActivity() {
        activity.startActivityForResult(new Intent(activity, SendBlogeoPostActivity.class), INTENT_BLOGEO_POST);
    }

    public boolean startRouteFileLoadingActivity() {
        ArrayList<LandmarkParcelable> routes = PersistenceManagerFactory.getFileManager().readFolderAsLandmarkParcelable(FileManager.getRoutesFolderPath(), FilenameFilterFactory.getFilenameFilter("kml"), Commons.ROUTES_LAYER);

        if (!routes.isEmpty()) {
            Bundle extras = new Bundle();
            extras.putParcelableArrayList("files", routes);
            extras.putInt("type", FilesActivity.ROUTES);
            Intent intent = new Intent(activity, FilesActivity.class);
            intent.putExtras(extras);
            activity.startActivityForResult(intent, INTENT_FILES);
        }

        return (routes.isEmpty());
    }

    public boolean startFilesLoadingActivity() {
        ArrayList<LandmarkParcelable> files = PersistenceManagerFactory.getFileManager().readFolderAsLandmarkParcelable(FileManager.getFileFolderPath(), FilenameFilterFactory.getFilenameFilter("kml"), null);

        if (!files.isEmpty()) {
            Bundle extras = new Bundle();
            extras.putParcelableArrayList("files", files);
            extras.putInt("type", FilesActivity.FILES);
            Intent intent = new Intent(activity, FilesActivity.class);
            intent.putExtras(extras);
            activity.startActivityForResult(intent, INTENT_FILES);
        }

        return files.isEmpty();
    }
    
    public boolean startConfigurationViewerActivity() {
    	ArrayList<LandmarkParcelable> configuration = new ArrayList<LandmarkParcelable>();
    	
    	Map<String, String> config = ConfigurationManager.getInstance().getConfiguration();
    	
    	Function<Map.Entry<String, String>, LandmarkParcelable> transformFunction = new ConfigurationEntryToLandmarkParcelableFunction();

    	configuration.addAll(Lists.transform(new ArrayList<Map.Entry<String, String>>(config.entrySet()), transformFunction));
    	
    	if (!configuration.isEmpty()) {
    		Bundle extras = new Bundle();
            extras.putParcelableArrayList("configuration", configuration);
            Intent intent = new Intent(activity, ConfigurationViewerActivity.class);
            intent.putExtras(extras);
            activity.startActivityForResult(intent, INTENT_CONFIGURATION_VIEWER);
    	}
    	
    	return configuration.isEmpty();
    }

    public void startSocialListActivity() {
        activity.startActivityForResult(new Intent(activity, SocialListActivity.class), INTENT_LAYERS);
    }

    public void startLayersListActivity(boolean dynamicLayersMode) {
    	Intent intent = null;
 
    	if (dynamicLayersMode) {
    		intent = new Intent(activity, GridLayerListActivity.class);
    		intent.putExtra("mode", ConfigurationManager.DYNAMIC_LAYERS_MODE);
    	} else {
    		intent = new Intent(activity, GridLayerListActivity.class);
    		intent.putExtra("mode", ConfigurationManager.ALL_LAYERS_MODE);
    	}
    	
    	activity.startActivityForResult(intent, INTENT_LAYERS);
    }

    public void startCategoryListActivity(int cursorLatitude, int cursorLongitude, int parent, int radius) {
    	Intent intent = new Intent(activity, GridCategoryListActivity.class);
    	Bundle appData = new Bundle();
        appData.putInt("parent", parent);
        appData.putInt("lat", cursorLatitude);
        appData.putInt("lng", cursorLongitude);
        if (radius < 0) {
            appData.putInt("radius", DistanceUtils.radiusInKilometer());
        } else {
            appData.putInt("radius", radius);
        }
        intent.putExtras(appData);
        activity.startActivityForResult(intent, INTENT_CATEGORIES);
    }

    public void startRegisterActivity() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigurationManager.REGISTER_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public void startOAuthActivity(String service) {
        Bundle extras = new Bundle();
        extras.putString("service", service);
        Intent intent = new Intent(activity, OAuth2Activity.class);
        intent.putExtras(extras);
        activity.startActivityForResult(intent, INTENT_LAYERS);
    }

    public void startLoginActivity() {
        activity.startActivity(new Intent(activity, LoginActivity.class));
    }
    
    public boolean checkAuthStatus(ExtendedLandmark selectedLandmark) {
        String selectedLayer = selectedLandmark.getLayer();

        if ((selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER))
                && ConfigurationManager.getInstance().isOff(ConfigurationManager.FS_AUTH_STATUS)) {
            startOAuthActivity(Commons.FOURSQUARE);
            return false;
        } else if (selectedLayer.equals(Commons.FACEBOOK_LAYER)
                && ConfigurationManager.getInstance().isOff(ConfigurationManager.FB_AUTH_STATUS)) {
            startOAuthActivity(Commons.FACEBOOK);
            return false;
        } else if (selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER)
                && ConfigurationManager.getInstance().isOff(ConfigurationManager.GL_AUTH_STATUS)) {
            startOAuthActivity(Commons.GOOGLE);
            return false;
        } else if (LayerManager.getInstance().isLayerCheckinable(selectedLayer)
                && !ConfigurationManager.getUserManager().isUserLoggedIn()) {
            startLoginActivity();
            return false;
        } else if (!ConfigurationManager.getUserManager().isUserLoggedIn()) {
            return false;
        }

        return true;
    }
    
    public boolean checkAuthStatus(String selectedLayer) {
        if ((selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER))
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
            return true;
        } else if (selectedLayer.equals(Commons.FACEBOOK_LAYER)
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
            return true;
        } else if (selectedLayer.equals(Commons.GOOGLE_PLACES_LAYER)
                && ConfigurationManager.getInstance().isOn(ConfigurationManager.GL_AUTH_STATUS)) {
            return true;
        } else if (LayerManager.getInstance().isLayerCheckinable(selectedLayer)
                && ConfigurationManager.getUserManager().isUserLoggedIn()) {
            return true;
        } else {
        	return false;
        }	
    }


    protected void startLoginActivity(int type) {
        List<String> items = ConfigurationManager.getUserManager().getLoginItems(true);
        String[] selected = items.get(type).split(";");
        if (selected[0].startsWith(ConfigurationManager.GMS_WORLD)) {
            startLoginActivity();
        } else {
            startOAuthActivity(selected[1]);
        }
    }

    public boolean isIntentAvailable(String action) {
        final Intent intent = new Intent(action);
        return (intent.resolveActivity(activity.getPackageManager()) != null);      
        //List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        //return (list.size() > 0);
    }

    public void startQrCodeCheckinActivity(double[] currentLocation) {
        final boolean scanAvailable = isIntentAvailable(SCAN_INTENT);
        if (scanAvailable) {
        	try {
        		Intent intent = new Intent(SCAN_INTENT);
        		intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        		activity.startActivityForResult(intent, INTENT_QRCODECHECKIN);
        	} catch (Exception e) {
        		LoggerUtils.error("IntentsHelper.startQrCodeCheckinActivity() exception:", e);
        		showInfoToast(Locale.getMessage(R.string.QRCode_scanner_missing_error));
        	}      	
        } else {
            showInfoToast(Locale.getMessage(R.string.QRCode_scanner_missing_error));
        }
    }

    public void startLocationCheckinActivity(double[] currentLocation) {
        startLandmarkListActivity(INTENT_CHECKIN, null, LandmarkListActivity.SOURCE.CHECKIN, currentLocation);
    }

    public void showNearbyLandmarks(double[] currentLocation, ProjectionInterface projection) {
    	LandmarkManager.getInstance().findVisibleLandmarks(projection, true);
        startMultiLandmarkIntent(currentLocation);
    }

    public void showLandmarksInDay(double[] currentLocation, int year, int month, int day) {
        Intent src = new Intent();
        src.putExtra("year", year);
        src.putExtra("month", month);
        src.putExtra("day", day);
        startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.DAY_LANDMARKS, currentLocation);
    }

    public void startAutoCheckinListActivity(double[] currentLocation) {
    	FavouritesDbDataSource fdb = ConfigurationManager.getDatabaseManager().getFavouritesDatabase();
        if (fdb != null) {
            List<FavouritesDAO> favourites = fdb.fetchAllLandmarks();
            ArrayList<LandmarkParcelable> dataList = new ArrayList<LandmarkParcelable>();

            if (!favourites.isEmpty()) {
                Function<FavouritesDAO, LandmarkParcelable> transformFunction = new FavouritesDAOToLandmarkParcelableFunction(currentLocation[0], currentLocation[1]);
            	dataList.addAll(Lists.transform(favourites, transformFunction));
                Intent intent = new Intent(activity, AutoCheckinListActivity.class);
                intent.putParcelableArrayListExtra("favourites", dataList);
                activity.startActivityForResult(intent, INTENT_AUTO_CHECKIN);
            } else {
                showInfoToast(Locale.getMessage(R.string.autoCheckinListEmpty));
            }
        } else {
            showInfoToast(Locale.getMessage(R.string.autoCheckinListEmpty));
        }
    }

    public void startHelpActivity() {
        Intent intent = new Intent(activity, HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.WHATS_NEW_PAGE);
        activity.startActivity(intent);
    }

    public void startPhoneCallActivity(ExtendedLandmark landmark) {
        try {
        	if (OsUtil.hasTelephony(activity)) {
        		if (landmark != null) {
                	String number = "tel:" + landmark.getPhone();
                	Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number));
                	activity.startActivity(callIntent);
            	} else {
                	showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
            	}
        	} else {
        		showInfoToast(Locale.getMessage(R.string.Call_not_supported_error));
        	}
        } catch (Exception e) {
            LoggerUtils.error("Intents.startPhoneCallActivity() exception", e);
            showInfoToast(Locale.getMessage(R.string.Call_not_supported_error));
        }
    }

    public void openButtonPressedAction(ExtendedLandmark landmark) {
    	final String[] actionLayers = new String[]{Commons.YOUTUBE_LAYER, Commons.PANORAMIO_LAYER, Commons.COUPONS_LAYER, 
        		Commons.FLICKR_LAYER, Commons.GOOGLE_PLACES_LAYER};
        
    	String url = buildUrl(landmark);
    	
    	if (StringUtils.indexOfAny(landmark.getLayer(), actionLayers) >= 0) {
    		startActionViewIntent(url);
        } else {
        	Intent intent = new Intent(activity, WebViewActivity.class);
            intent.putExtra("url", url);
            if (StringUtils.isNotEmpty(landmark.getName())) {
                intent.putExtra("title", landmark.getName());
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        }
    }
    
    private static String buildUrl(ExtendedLandmark landmark) {
    	String url = ConfigurationManager.SERVER_URL + "landmarkRedirect?lat=" + StringUtil.formatCoordE6(landmark.getQualifiedCoordinates().getLatitude()) +
	             "&lng=" + StringUtil.formatCoordE6(landmark.getQualifiedCoordinates().getLongitude()) + "&layer=" + landmark.getLayer();
   
    	if (landmark.getUrl() != null) {
    		try {
    			url += "&url=" + URLEncoder.encode(landmark.getUrl(), "UTF-8");
    		} catch (Exception e) {
    			LoggerUtils.error("IntentsHelper.builUrl()", e);
    		}
    	}
    	
		return url;
    }
    
    public void startActionViewIntent(final String url) {
        try {
        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	activity.startActivity(intent);
        } catch (Exception e) {
			LoggerUtils.error("IntentsHelper.startActionViewIntent()", e);
			showInfoToast(Locale.getMessage(R.string.Unexpected_error));
		}
    }

    public void startNewestLandmarkIntent(double[] currentLocation, String[] excluded, int maxDays) {
        Intent src = new Intent();
        src.putExtra("excluded", excluded);
        src.putExtra("maxDays", maxDays);
        startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.NEWEST, currentLocation);
    }

    public void startFriendsCheckinsIntent(double[] currentLocation) {
        startLandmarkListActivity(INTENT_MULTILANDMARK, null, LandmarkListActivity.SOURCE.FRIENDS_CHECKINS, currentLocation);
    }

    public void startDealsOfTheDayIntent(double[] currentLocation, String[] excluded) {
        Intent src = new Intent();
        src.putExtra("excluded", excluded);
        startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.DOD, currentLocation);
    }

    public void startRecentLandmarksIntent(double[] currentLocation) {
        startLandmarkListActivity(INTENT_MULTILANDMARK, null, LandmarkListActivity.SOURCE.RECENT, currentLocation);
    }

    public void startMultiLandmarkIntent(double[] currentLocation) {
        startLandmarkListActivity(INTENT_MULTILANDMARK, null, LandmarkListActivity.SOURCE.MULTI_LANDMARK, currentLocation);
    }

    public void startMyLandmarksIntent(double[] currentLocation) {
        startLandmarkListActivity(INTENT_MYLANDMARKS, null, LandmarkListActivity.SOURCE.MY_LANDMARKS, currentLocation);
    }

    public void startWifiSettingsActivity() {
        activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    public void startLocationSettingsActivity() {
        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }
    
    public void startAutoCheckinBroadcast() {
    	if (ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN)) {
    		Intent intent = new Intent();
    		intent.setAction("com.jstakun.gms.android.autocheckinbroadcast");
    		activity.sendBroadcast(intent);
    	} else if (ConfigurationManager.getInstance().isOff(ConfigurationManager.AUTO_CHECKIN)) {
    		stopAutoCheckinService();
    	}
    }
    
    private void stopAutoCheckinService() {
    	AlarmManager service = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(activity, AutoCheckinStartServiceReceiver.class);
		PendingIntent pending = PendingIntent.getBroadcast(activity, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        service.cancel(pending);
    }

    private void startCommentActivity(String service, String venueid, String name) {
        if (venueid != null) {
            Bundle extras = new Bundle();
            extras.putString("service", service);
            extras.putString("placeId", venueid);
            if (StringUtils.isNotEmpty(name)) {
                extras.putString("name", name);
            }
            Intent intent = new Intent(activity, CommentActivity.class);
            intent.putExtras(extras);
            activity.startActivity(intent);
        } else {
            showInfoToast(Locale.getMessage(R.string.Unexpected_error));
        }
    }

    public void commentButtonPressedAction() {
        //FS, FB and GMS World only
        ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSeletedLandmarkUI();
        String selectedLayer = selectedLandmark.getLayer();
        if (selectedLayer.equals(Commons.FOURSQUARE_LAYER) || selectedLayer.equals(Commons.FOURSQUARE_MERCHANT_LAYER)) {
            String venueid = OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE).getKey(selectedLandmark.getUrl()); 
            startCommentActivity(Commons.FOURSQUARE, venueid, null);
        } else if (selectedLayer.equals(Commons.FACEBOOK_LAYER)) {
        	String venueid = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK).getKey(selectedLandmark.getUrl()); 
            startCommentActivity(Commons.FACEBOOK, venueid, selectedLandmark.getName());
        } else {
            String venueid = selectedLandmark.getUrl();
            startCommentActivity(Commons.GMS_WORLD, venueid, null);
        }
    }

    /*public void saveRouteAction() {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            asyncTaskManager.executeSaveRouteTask();
        } else if (ConfigurationManager.getInstance().isOff(ConfigurationManager.RECORDING_ROUTE)) {
            showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosStopped));
        }
    }*/

    public List<ResolveInfo> getSendIntentsList() {
        final Intent sendIntent = new Intent(android.content.Intent.ACTION_SEND, null);

        sendIntent.setType("text/plain");
        sendIntent.addCategory(Intent.CATEGORY_DEFAULT);

        PackageManager pm = activity.getPackageManager();
        activities = pm.queryIntentActivities(sendIntent, 0);

        if (!activities.isEmpty()) {
            Collections.sort(activities, new ResolveInfo.DisplayNameComparator(pm));
        }

        return Collections.unmodifiableList(activities);
    }

    public void startSendMessageIntent(int pos, ExtendedLandmark selectedLandmark) {
        ResolveInfo selectedIntent = activities.get(pos);

        if (selectedLandmark != null && selectedIntent != null) {
            String packageName = selectedIntent.activityInfo.packageName;
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setClassName(packageName, selectedIntent.activityInfo.name);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            String subject = Locale.getMessage(R.string.Landmark_see, selectedLandmark.getName());
            String message = "";
            String email;
            String url = buildUrl(selectedLandmark);

            if (StringUtils.indexOf(packageName, "facebook") > 0 || StringUtils.indexOf(packageName, "zxing") > 0) {
                message = url;
                intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            } else if (StringUtils.indexOf(packageName, "twitter") > 0 || StringUtils.indexOf(packageName, "friendstream") > 0) {
                message = Locale.getMessage(R.string.Landmark_see, url);
                intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            } else if (StringUtils.indexOf(packageName, "mms") > 0) {
                intent.putExtra("sms_body", Locale.getMessage(R.string.Landmark_see, url));
            } else {
                if (selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER)) {
                    String date = DateTimeUtils.getDefaultDateTimeString(selectedLandmark.getDescription(), ConfigurationManager.getInstance().getCurrentLocale());
                    message = Locale.getMessage(R.string.Last_update, date);
                } else {
                	//extract alt from img and replace img tag 
                	
                	String htmlBody = selectedLandmark.getDescription();
                	Pattern p = Pattern.compile("alt=\"(.*?)\"");
                    Matcher m = p.matcher(htmlBody);
                    List<String> alts = new ArrayList<String>();
                    while (m.find()) {
                    	alts.add(m.group(1));
                    }
                    
                    StringBuffer sb = new StringBuffer();
                    p = Pattern.compile("[<](/)?img[^>]*[>]"); 
                    m = p.matcher(htmlBody);
                    int i=0;
                    while (m.find()) {
                    	m.appendReplacement(sb, alts.get(i));
                    	i++;
                    }
                    m.appendTail(sb);
                    message = Html.fromHtml(sb.toString()).toString();
                }

                message += Locale.getMessage(R.string.mailMessageSuffix, url, Locale.getMessage(R.string.app_name));
                		
                email = ConfigurationManager.getUserManager().getUserEmail();
                if (StringUtils.isNotEmpty(email)) {
                    intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{email});
                }
                intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            }

            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

            activity.startActivity(intent);
        } else {
            showInfoToast(Locale.getMessage(R.string.Share_no_share_apps));
        }
    }

    public void shareLandmarkAction(DialogManager dialogManager) {

        if (activities == null) {
            getSendIntentsList();
        }

        if (!activities.isEmpty()) {
            IntentArrayAdapter arrayAdapter = new IntentArrayAdapter(activity, activities);
            dialogManager.showAlertDialog(AlertDialogBuilder.SHARE_INTENTS_DIALOG, arrayAdapter, null);
        } else {
            showInfoToast(Locale.getMessage(R.string.Share_no_share_apps));
        }
    }

    public void shareImageAction(Uri uri) {
    	final Intent shareIntent = new Intent(Intent.ACTION_SEND);
    	shareIntent.setType("image/jpg");
    	shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
    	shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Locale.getMessage(R.string.Screenshot_subject, Locale.getMessage(R.string.app_name)));
    	shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, Locale.getMessage(R.string.Screenshot_text, Locale.getMessage(R.string.app_name)) + "\n" + Locale.getMessage(R.string.mailMessageSuffix, ConfigurationManager.SERVER_URL, Locale.getMessage(R.string.app_name)));
    	activity.startActivity(Intent.createChooser(shareIntent, Locale.getMessage(R.string.shareScreenshot)));
    }
    
    public void shareFileAction(Uri uri, int type) {
    	final Intent shareIntent = new Intent(Intent.ACTION_SEND);
    	shareIntent.setType("plain/text");
    	shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
    	if (type == FilesActivity.ROUTES) {	
    		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Locale.getMessage(R.string.Routes_share_subject));
    		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, Locale.getMessage(R.string.Routes_share_text) + "\n" + Locale.getMessage(R.string.mailMessageSuffix, ConfigurationManager.SERVER_URL, Locale.getMessage(R.string.app_name)));
    		activity.startActivity(Intent.createChooser(shareIntent, Locale.getMessage(R.string.Routes_share_title)));
    	} else {
    		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Locale.getMessage(R.string.Poi_share_subject));
        	shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, Locale.getMessage(R.string.Poi_share_text) + "\n" + Locale.getMessage(R.string.mailMessageSuffix, ConfigurationManager.SERVER_URL, Locale.getMessage(R.string.app_name)));
        	activity.startActivity(Intent.createChooser(shareIntent, Locale.getMessage(R.string.Poi_share_title)));	
    	}
    }
    
    public void setupShortcut() {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(activity, activity.getClass().getName());
        shortcutIntent.putExtra("com.jstakun.gms.android.ui.LauncherShortcut", activity.getClass().getName());

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, Locale.getMessage(R.string.app_name));

        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(activity, R.drawable.globe64_new);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        activity.setResult(Activity.RESULT_OK, intent);
    }

    public void startCalendarActivity(double[] coords) {
        Intent intent = new Intent(activity, CalendarActivity.class);
        Bundle extras = new Bundle();
        extras.putDouble("lat", coords[0]);
        extras.putDouble("lng", coords[1]);
        intent.putExtras(extras);
        activity.startActivityForResult(intent, INTENT_CALENDAR);
    }
    
    public void startRouteLoadingTask(ExtendedLandmark selectedLandmark, Handler showRouteHandler) {
    	String navigationUri = "google.navigation:q=" + selectedLandmark.getQualifiedCoordinates().getLatitude() + "," + selectedLandmark.getQualifiedCoordinates().getLongitude();
    	int routeType = ConfigurationManager.getInstance().getInt(ConfigurationManager.ROUTE_TYPE);
    	if (routeType == ConfigurationManager.ROUTE_WALK) {
    		navigationUri += "&mode=w";
    	} else if (routeType == ConfigurationManager.ROUTE_BICYCLE) {
    		navigationUri += "&mode=b";
    	}
    	
    	Intent mapIntent = null;
    	try {
    		//API v4 workaround
    		mapIntent = IntentHelper.getNavigationIntent(navigationUri);
    	} catch (VerifyError e) {   			
    	}
    	
    	boolean silent = true;
    	
    	if (mapIntent != null && mapIntent.resolveActivity(activity.getPackageManager()) != null) {
    		activity.startActivity(mapIntent);
    	} else {
    		silent = false;
    	}
    	asyncTaskManager.executeRouteServerLoadingTask(showRouteHandler, silent, selectedLandmark);
    }
    
    public boolean startStreetViewActivity(ExtendedLandmark selectedLandmark) {
    	String streetViewUri = "google.streetview:cbll=" + selectedLandmark.getQualifiedCoordinates().getLatitude() + "," + selectedLandmark.getQualifiedCoordinates().getLongitude();
    	Intent mapIntent = null;
    	try {
    		//API v4 workaround
    		mapIntent = IntentHelper.getNavigationIntent(streetViewUri);
    	} catch (VerifyError e) {   			
    	}
    	
    	if (mapIntent != null && mapIntent.resolveActivity(activity.getPackageManager()) != null) {
    		activity.startActivity(mapIntent);
    		return true;
    	} else {
    		return false;
    	}
    }

    public void loadLayersAction(boolean loadExternal, String selectedLayer, boolean clear, boolean loadServerLayers, double latitude, double longitude, int zoomLevel, ProjectionInterface projection) {
        if (LayerLoader.getInstance().isLoading()) {
        	LayerLoader.getInstance().stopLoading();
        }
        if (clear) {
        	LandmarkManager.getInstance().clearLandmarkStore();
            ConfigurationManager.getInstance().removeObject("dod", ExtendedLandmark.class);
        }
        if (projection != null) {
           ConfigurationManager.getInstance().putObject("bbox", projection.getBoundingBox());
        }
        Display display = activity.getWindowManager().getDefaultDisplay();
        LayerLoader.getInstance().loadLayers(latitude, longitude, zoomLevel, display.getWidth(), display.getHeight(), loadExternal, selectedLayer, loadServerLayers);
    }
    
    public int[] showSelectedLandmark(int id, double[] currentLocation, View lvView, int zoomLevel, ProjectionInterface projection) {
    	int[] coordsE6 = null;
    	if (id >= 0) {
            ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getLandmarkToFocusQueueSelectedLandmark(id);
            if (selectedLandmark != null) {
            	LandmarkManager.getInstance().setSelectedLandmark(selectedLandmark);
            	LandmarkManager.getInstance().clearLandmarkOnFocusQueue();
                coordsE6 = showLandmarkDetailsAction(currentLocation, lvView, zoomLevel, projection);
            } else {
            	showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
            }
        } else {
        	showInfoToast(Locale.getMessage(R.string.Landmark_search_empty_result));
        }
    	return coordsE6;
    }
    
    public int[] showLandmarkDetailsAction(double[] currentLocation, View lvView, int zoomLevel, ProjectionInterface projection) {
        int[] animateTo = null;
    	ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getLandmarkOnFocus();
        if (selectedLandmark != null) {
            if (!selectedLandmark.getLayer().equals(Commons.MULTI_LANDMARK)) {
            	LandmarkManager.getInstance().setSeletedLandmarkUI();
            }

            if (selectedLandmark.getLayer().equals(Commons.MULTI_LANDMARK)) {
                startMultiLandmarkIntent(currentLocation);
            } else {
                UserTracker.getInstance().trackEvent("Clicks", activity.getLocalClassName() + ".ShowSelectedLandmarkView", selectedLandmark.getLayer(), 0);
                ActionBarHelper.hide(activity);
                showLandmarkDetailsView(selectedLandmark, lvView, currentLocation, true);
                
                CategoriesManager.getInstance().addSubCategoryStats(selectedLandmark.getCategoryId(), selectedLandmark.getSubCategoryId());
                
                if (selectedLandmark.getLayer().equals(Commons.LOCAL_LAYER)) {
                    loadLayersAction(true, null, false, true, 
                    		selectedLandmark.getQualifiedCoordinates().getLatitude(), 
                    		selectedLandmark.getQualifiedCoordinates().getLongitude(),
                            zoomLevel, projection);
                }
                animateTo = new int[]{selectedLandmark.getLatitudeE6(), selectedLandmark.getLongitudeE6()};
            }
        } else {
            LoggerUtils.error(Locale.getMessage(R.string.Landmark_opening_error));
            showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
        }
        return animateTo;
    }

    public void showLandmarkDetailsView(final ExtendedLandmark selectedLandmark, final View lvView, double[] currentLocation, boolean loadAd) {
        TextView name = (TextView) lvView.findViewById(R.id.lvname);
        TextView header = (TextView) lvView.findViewById(R.id.lvheader);
        ImageButton lvCheckinButton = (ImageButton) lvView.findViewById(R.id.lvCheckinButton);
        View lvOpenButton = lvView.findViewById(R.id.lvOpenButton);
        View lvCommentButton = lvView.findViewById(R.id.lvCommentButton);
        View lvCallButton = lvView.findViewById(R.id.lvCallButton);
        ImageButton lvRouteButton = (ImageButton) lvView.findViewById(R.id.lvRouteButton);
        TextView desc = (TextView) lvView.findViewById(R.id.lvdesc);
        desc.setMovementMethod(LinkMovementMethod.getInstance());

        if (loadAd) {
            AdsUtils.loadAd(activity);
        }

        name.setText(selectedLandmark.getName());
        desc.setText("");
        header.setText("");
        int visibleButtons = 5;

        lvCheckinButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvCheckinSeparator).setVisibility(View.VISIBLE);
        lvCommentButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.VISIBLE);
        lvOpenButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvOpenSeparator).setVisibility(View.VISIBLE);
        
        //show only if location is available
        if (LandmarkManager.getInstance().hasMyLocation()) {
        	lvRouteButton.setVisibility(View.VISIBLE);
        	lvView.findViewById(R.id.lvRouteSeparator).setVisibility(View.VISIBLE);  
        } else {
        	lvRouteButton.setVisibility(View.GONE);
        	lvView.findViewById(R.id.lvRouteSeparator).setVisibility(View.GONE);
        }
        //
        
        String phone = selectedLandmark.getPhone();
        if (phone != null && OsUtil.hasTelephony(activity)) { 
            lvCallButton.setVisibility(View.VISIBLE);
            lvView.findViewById(R.id.lvCallSeparator).setVisibility(View.VISIBLE);
        } else {
            lvCallButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCallSeparator).setVisibility(View.GONE);
            visibleButtons--;
        }

        float dist = DistanceUtils.distanceInKilometer(currentLocation[0], currentLocation[1], selectedLandmark.getQualifiedCoordinates().getLatitude(), selectedLandmark.getQualifiedCoordinates().getLongitude());
        String descr = "";
        if (dist >= 0.001f) {
            descr += Locale.getMessage(R.string.Landmark_distance, DistanceUtils.formatDistance(dist)) + "<br/>";
        }
        if (selectedLandmark.getDescription() != null) {
            descr += selectedLandmark.getDescription();
        }

        int targetWidth = (int)(16f * activity.getResources().getDisplayMetrics().density);
        int targetHeight = (int)(16f * activity.getResources().getDisplayMetrics().density);
        if (selectedLandmark.getCategoryId() != -1) {
            //int icon = LayerManager.getDealCategoryIcon(selectedLandmark.getCategoryId(), LayerManager.LAYER_ICON_SMALL);
            //name.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
        	int iconId = LayerManager.getDealCategoryIcon(selectedLandmark.getCategoryId(), LayerManager.LAYER_ICON_SMALL);
			Picasso.with(activity).load(iconId).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(name, PicassoTextViewTarget.Position.LEFT));	           
        } else {
            //BitmapDrawable icon = LayerManager.getLayerIcon(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_SMALL, activity.getResources().getDisplayMetrics(), null);
            //name.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        	int iconId = LayerManager.getLayerIcon(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_SMALL);
			if (iconId != R.drawable.image_missing16) {
				Picasso.with(activity).load(iconId).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(name, PicassoTextViewTarget.Position.LEFT));
			} else {
				String iconUri = LayerManager.getLayerIconUri(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_SMALL);
				if (iconUri != null && StringUtils.startsWith(iconUri, "http")) {
					Picasso.with(activity).load(iconUri).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(name, PicassoTextViewTarget.Position.LEFT));
				} else {
					File fc = PersistenceManagerFactory.getFileManager().getExternalDirectory(FileManager.getIconsFolderPath(), iconUri);
					Picasso.with(activity).load(fc).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(name, PicassoTextViewTarget.Position.LEFT));
				}
			}
        }

        ImageView thumbnail = (ImageView) lvView.findViewById(R.id.thumbnailButton);
        if (StringUtils.isNotEmpty(selectedLandmark.getThumbnail())) {
            /*Bitmap image = IconCache.getInstance().getThumbnailResource(selectedLandmark.getThumbnail(), selectedLandmark.getLayer(), activity.getResources().getDisplayMetrics(), new ThumbnailLoadedHandler(activity));
            int width = activity.getWindowManager().getDefaultDisplay().getWidth();            
            if (thumbnail != null) {
            	if (image != null && (width == 0 || image.getWidth() < width * 0.9)) { 
                	thumbnail.setImageBitmap(image);
                	thumbnail.setTag(null);
            	} else {
                	thumbnail.setImageResource(R.drawable.download48);
                	thumbnail.setTag(selectedLandmark);
            	}
            }*/
        	if (thumbnail != null) {
        		targetWidth = (int)(128f * activity.getResources().getDisplayMetrics().density);
                targetHeight = (int)(128f * activity.getResources().getDisplayMetrics().density);
                Picasso.with(activity).load(selectedLandmark.getThumbnail()).resize(targetWidth, targetHeight).centerInside().tag(selectedLandmark).error(R.drawable.image_missing48).placeholder(R.drawable.download48).into(thumbnail);
        		thumbnail.setVisibility(View.VISIBLE);
        	}
        	if (StringUtils.isNotEmpty(descr)) {
                desc.setText(Html.fromHtml(descr, imgGetter, null));
            }
        } else {
            if (thumbnail != null) {
                thumbnail.setVisibility(View.GONE);
            }
            if (StringUtils.isNotEmpty(descr)) {
                desc.setText(Html.fromHtml(descr, imgGetter, null));
                desc.setVisibility(View.VISIBLE);
            } else {
                desc.setVisibility(View.GONE);
            }
        }

        //try {
        lvCheckinButton.setImageResource(R.drawable.checkin);
        if (selectedLandmark.getLayer().equals(Commons.FOURSQUARE_LAYER)) {
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FS_AUTH_STATUS)) {
                lvCheckinButton.setImageResource(R.drawable.login);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons--;
            }
        } else if (selectedLandmark.getLayer().equals(Commons.FOURSQUARE_MERCHANT_LAYER)) {
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FS_AUTH_STATUS)) {
                //lvActionButton.setImageResource(R.drawable.login);
                lvCheckinButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCheckinSeparator).setVisibility(View.GONE);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons -= 2;
            }
        } else if (selectedLandmark.getLayer().equals(Commons.FACEBOOK_LAYER)) {
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FB_AUTH_STATUS)) {
                lvCheckinButton.setImageResource(R.drawable.login);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons--;
            }
        } else if (selectedLandmark.getLayer().equals(Commons.GOOGLE_PLACES_LAYER)) {
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons--;
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.GL_AUTH_STATUS)) {
                lvCheckinButton.setImageResource(R.drawable.login);
            }
        } else if (selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER)) {
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            if (!ConfigurationManager.getUserManager().isUserLoggedIn()) {
                lvCheckinButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCheckinSeparator).setVisibility(View.GONE);
            } else {
                lvCheckinButton.setImageResource(R.drawable.share);
            }
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons--;
            //String date = DateTimeUtils.getDefaultDateTimeString(selectedLandmark.getDescription(), ConfigurationManager.getInstance().getCurrentLocale());
            //desc.setText(Locale.getMessage(R.string.Last_update, date));
            desc.setText(selectedLandmark.getDescription());
            lvRouteButton.setVisibility(View.GONE);
        } else if (selectedLandmark.getLayer().equals(Commons.LOCAL_LAYER)) {
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            lvCheckinButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCheckinSeparator).setVisibility(View.GONE);
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons -= 2;
        } else if (LayerManager.getInstance().isLayerCheckinable(selectedLandmark.getLayer())) { //GMS World checkinable layers
            header.setText(LayerManager.getInstance().getLayerFormatted(selectedLandmark.getLayer()));
            if (!ConfigurationManager.getUserManager().isUserLoggedIn()) {
                lvCheckinButton.setImageResource(R.drawable.login);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons--;
            }
        } else {
            header.setText(selectedLandmark.getLayer());
            lvCheckinButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCheckinSeparator).setVisibility(View.GONE);
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons -= 2;
        }
        float dip = activity.getResources().getDisplayMetrics().density;
        //API 8 minimum to replace getWidth()
        if (visibleButtons == 5 && activity.getWindowManager().getDefaultDisplay().getWidth() < (360f * dip)) {
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
        }
        lvView.setVisibility(View.VISIBLE);
        
        LandmarkManager.getInstance().addRecentlyOpenedLandmark(selectedLandmark);
    }

    public void showInfoToast(String msg) {
    	synchronized (longMutex) {
    		if (longToast != null) {
    			longToast.cancel();
    		}
    		longToast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG);
    		longToast.show();
    	}
    }
    
    public void showShortToast(String msg) {
    	synchronized (shortMutex) {
    		if (shortToast != null) {
    			shortToast.cancel();
    		}
    		shortToast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT);
    		shortToast.show();
    	}
    }

    public boolean isClearLandmarksRequired(ProjectionInterface projection, int mapCenterLatE6,
            int mapCenterLngE6, int myLocLat, int myLocLng) {
        boolean clearLandmarks = false;
        int[] topLeft = projection.fromPixels(0, 0);
        double mapCenterLat = MathUtils.coordIntToDouble(mapCenterLatE6);
        double mapCenterLng = MathUtils.coordIntToDouble(mapCenterLngE6);
        float viewDist = DistanceUtils.distanceInKilometer(
                MathUtils.coordIntToDouble(topLeft[0]),
                MathUtils.coordIntToDouble(topLeft[1]),
                mapCenterLat, mapCenterLng);
        float dist = DistanceUtils.distanceInKilometer(
                MathUtils.coordIntToDouble(myLocLat),
                MathUtils.coordIntToDouble(myLocLng),
                mapCenterLat, mapCenterLng);

        //System.out.println(dist + " " + (viewDist * 10));

        if (dist > 10 * viewDist) {
            clearLandmarks = true;
        }
        return clearLandmarks;
    }

    /*public void setRepeatingAlarm(Class<?> clazz) {
        AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(activity, clazz);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), (60 * 1000), pendingIntent);
    }*/

    protected boolean isNewerVersionAvailable() {
        HttpUtils utils = new HttpUtils();
        boolean response = false;

        try {
        	List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair(ConfigurationManager.APP_ID, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID)));
			params.add(new BasicNameValuePair("type", "v")); 
            String url = ConfigurationManager.getInstance().getServerUrl() + "notifications";
            
            Location location = ConfigurationManager.getInstance().getLocation();
            if (location != null) {
            	params.add(new BasicNameValuePair("lat", Double.toString(location.getLatitude())));
            	params.add(new BasicNameValuePair("lng", Double.toString(location.getLongitude())));
            	params.add(new BasicNameValuePair("username", Commons.MY_POS_USER));
            }

            String resp = utils.sendPostRequest(url, params, true);
            if (utils.getResponseCode() == HttpStatus.SC_OK) {
                if (StringUtils.startsWith(resp, "{")) {
                    JSONObject json = new JSONObject(resp);
                    int version = Integer.valueOf(json.optString("value", "0"));
                    PackageInfo info = ConfigurationManager.getAppUtils().getPackageInfo();
                    if (info != null) {
                    	int versionCode = info.versionCode;
                    	if (version > versionCode) {
                    		response = true;
                    	}
                    	LoggerUtils.debug("Current version: " + versionCode + ", GMS World server version " + version);
                    }
                }
            }    
        } catch (Exception ex) {
            LoggerUtils.error("Intents.isNewerVersionAvailable() exception:", ex);
        } finally {
            try {
                if (utils != null) {
                    utils.close();
                }
            } catch (Exception e) {
            }
        }

        return response;
    }

    public void startSearchActivity(int cursorLatitude, int cursorLongitude, int radius, boolean isDeal) {
        Bundle appData = new Bundle();
        if (isDeal) {
            appData.putString("type", GMSSearchActivity.TYPE.DEALS.name());
        }
        appData.putInt("lat", cursorLatitude);
        appData.putInt("lng", cursorLongitude);
        if (radius < 0) {
            appData.putInt("radius", DistanceUtils.radiusInKilometer());
        } else {
            appData.putInt("radius", radius);
        }
        activity.startSearch(null, false, appData, false);
    }

    public void onAppVersionChanged() {

    	//This method is executed when new app version is installed
    	String message = null;
        int versionCode = -1;
        String versionName = null;

        try {
            PackageInfo info = ConfigurationManager.getAppUtils().getPackageInfo();
            if (info != null) {
            	versionCode = info.versionCode;
            	versionName = info.versionName;
            }
            LoggerUtils.debug("Version code: " + versionCode);
        } catch (Exception ex) {
            LoggerUtils.error("Intents.checkAppVersion() exception", ex);
        }
        int buildVersion = ConfigurationManager.getInstance().getInt(ConfigurationManager.BUILD_VERSION);
        
        //if (buildVersion != -1 && buildVersion < 1000) { //2.0.1
        //    PersistenceManagerFactory.getPersistenceManagerInstance().deleteTilesCache();
        //}
        
        if (buildVersion < versionCode) {
        
        	//info.versionName ends with m;
            if (StringUtils.endsWithIgnoreCase(versionName, "m")) {
        		startHelpActivity();
        	}
        
        	if ((versionCode >= 1086 && buildVersion < 1086 && buildVersion > 500) || (versionCode >= 86 && buildVersion < 86)) { //2.0.7
        		boolean notify = false;
        		//logout user due to user management changes
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GMS_AUTH_STATUS) && ConfigurationManager.getInstance().containsKey("username") && ConfigurationManager.getInstance().containsKey("password")) {
        			ConfigurationManager.getInstance().removeAll(new String[]{"username", "password"});
        			ConfigurationManager.getInstance().setOff(ConfigurationManager.GMS_AUTH_STATUS);
        			notify = true;
        		}
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
                    OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK).logout();
                    notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TWEET_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.TWITTER).logout();
        			notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.LN_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.LINKEDIN).logout();
        			notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GL_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.GOOGLE).logout();
        			notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE).logout();
        			notify = true;
                } 
        	    
        		if (notify) {
        			message = Locale.getMessage(R.string.Migation_1086_message);
        		}	
        	} else if ((versionCode >= 1102 && buildVersion < 1102 && buildVersion > 500)) { //2.0.8
        		boolean notify = false;
        		//logout user due to user management changes
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GMS_AUTH_STATUS)) {
        			GMSUtils.logout();
        			notify = true;
        		}
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
                    OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK).logout();
                    notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.TWEET_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.TWITTER).logout();
        			notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.LN_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.LINKEDIN).logout();
        			notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.GL_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.GOOGLE).logout();
        			notify = true;
                } 
        		
        		if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FS_AUTH_STATUS)) {
        			OAuthServiceFactory.getSocialUtils(Commons.FOURSQUARE).logout();
        			notify = true;
                } 
        	    
        		if (notify) {
        			message = Locale.getMessage(R.string.Migation_1086_message);
        		}	
        	}
        	
        	ConfigurationManager.getInstance().putInteger(ConfigurationManager.BUILD_VERSION, versionCode);
        	ConfigurationManager.getDatabaseManager().saveConfiguration(false);
        
        	if (StringUtils.isNotEmpty(message)) {
        		showInfoToast(message);
        	};
        }	
    }

    private void startLandmarkListActivity(int requestCode, Intent src, LandmarkListActivity.SOURCE source, double[] myLocation) { 
        Intent intent = new Intent(activity, LandmarkListActivity.class);
        intent.putExtra("lat", myLocation[0]);
        intent.putExtra("lng", myLocation[1]);
        intent.putExtra("requestCode", requestCode);
        if (src != null) {
            intent.putExtras(src);
        }
        intent.putExtra("source", source);
        activity.startActivityForResult(intent, requestCode);
    }

    public void processActivityResult(int requestCode, int resultCode, Intent intent, double[] myLocation, double[] mapCenter, Handler uiHandler, int zoomLevel, ProjectionInterface projection) {
        if (requestCode == INTENT_CATEGORIES) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                if (StringUtils.equals(action, "show")) {
                    Intent src = new Intent();
                    src.putExtras(intent);
                    startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.CATEGORY, myLocation);
                } else if (StringUtils.equals(action, "noshow")) { //DealCategoryListActivity
                    Intent result = new Intent();
                    result.putExtras(intent);
                    result.putExtra("action", "show");
                    activity.setResult(Activity.RESULT_OK, result);
                    activity.finish();
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (intent != null) {
                    String action = intent.getStringExtra("action");
                    if (StringUtils.equals(action, "forward")) {
                        activity.setResult(Activity.RESULT_CANCELED);
                        activity.finish();
                    }
                }
            }
        } else if (requestCode == INTENT_PREFS) {
            if (resultCode == Activity.RESULT_OK) {
                String[] names = intent.getStringArrayExtra("names");
                String[] codes = intent.getStringArrayExtra("codes");
                String isDeal = intent.getStringExtra("deals");
                if (StringUtils.isNotEmpty(isDeal)) {
                    if (names != null && codes != null) {
                    	CategoriesManager.getInstance().saveCategoriesAction(names, codes);
                    }
                } else {
                	LayerManager.getInstance().saveLayersAction(names, codes);
                }
                
                if (intent.getBooleanExtra("reindex", false)) {
                	//reindex dynamic layers
                	asyncTaskManager.executeReIndexDynamicLayersTask();                	
                }
            }
            
        } else if (requestCode == INTENT_ADD_LANDMARK) {
            if (resultCode == Activity.RESULT_OK) {
                String name = intent.getStringExtra("name");
                String desc = intent.getStringExtra("desc");
                String layer = intent.getStringExtra("layer");
                String fsCategory = intent.getStringExtra("fsCategory"); 
                boolean addVenue = intent.getBooleanExtra("addVenue", false);
                asyncTaskManager.executeAddLandmarkTask(name, desc, layer, activity.getString(R.string.addLandmark), mapCenter[0], mapCenter[1], addVenue, fsCategory);
            }
        } else if (requestCode == INTENT_BLOGEO_POST) {
            if (resultCode == Activity.RESULT_OK) {
                String name = intent.getStringExtra("name");
                String desc = intent.getStringExtra("desc");
                Long validityDate = intent.getLongExtra("validityTime", DateTimeUtils.ONE_HOUR);
                if (ConfigurationManager.getInstance().getLocation() != null) {
                    if (StringUtils.isEmpty(desc)) {
                        desc = name;
                    }
                    if (validityDate < DateTimeUtils.FIVE_MINUTES) {
                        validityDate = DateTimeUtils.FIVE_MINUTES;
                    }
                    asyncTaskManager.executeSendBlogeoPostTask(name, desc, Long.toString(validityDate), activity.getString(R.string.blogeo));
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_FILES) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                int type = intent.getIntExtra("type", -1);
                String filename = intent.getStringExtra("filename");
                if (action.equals("load")) {
                    if (type == FilesActivity.ROUTES) {
                        asyncTaskManager.executeRouteLoadingTask(filename, uiHandler);
                    } else if (type == FilesActivity.FILES) {
                        asyncTaskManager.executePoiFileLoadingTask(filename, uiHandler);
                    }
                }
            }
        } else if (requestCode == IntentsHelper.INTENT_QRCODECHECKIN) {
            if (resultCode == Activity.RESULT_OK) {
                String checkinLandmarkCode = intent.getStringExtra("SCAN_RESULT");
                String qrformat = intent.getStringExtra("SCAN_RESULT_FORMAT");
                asyncTaskManager.executeQrCodeCheckInTask(checkinLandmarkCode, qrformat, activity.getString(R.string.qrcheckin));
            }
        } else if (requestCode == IntentsHelper.INTENT_CHECKIN) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                String name = intent.getStringExtra("name");
                if (action.equals("load")) {
                    String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                    int id = Integer.parseInt(ids);
                    ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getLandmarkToFocusQueueSelectedLandmark(id);
                    if (selectedLandmark != null) {
                        String key = StringUtil.getKeyFromUrl(selectedLandmark.getUrl());
                        if (key != null) {
                            asyncTaskManager.executeLocationCheckInTask(-1, key, activity.getString(R.string.searchcheckin), name, false, null);
                        } else {
                            showInfoToast(Locale.getMessage(R.string.Social_checkin_failure, "landmark key is empty"));
                        }
                    } else {
                        showInfoToast(Locale.getMessage(R.string.Social_checkin_failure, "unknown error"));
                    }
                } 
            }
        } else if (requestCode == IntentsHelper.INTENT_LAYERS) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                if (StringUtils.equals(action, "load")) {
                    String layer = intent.getStringExtra("layer");
                    loadLayersAction(true, layer, false, false, mapCenter[0], mapCenter[1], zoomLevel, projection);
                } else if (StringUtils.equals(action, "refresh")) {
                    loadLayersAction(true, null, false, true, mapCenter[0], mapCenter[1], zoomLevel, projection);
                } else if (StringUtils.equals(action, "show")) {
                    Intent src = new Intent();
                    src.putExtras(intent);
                    startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.LAYER, myLocation);
                }
            }
        }
    }
    
    public void vibrateOnLocationUpdate() {
    	if (!ConfigurationManager.getInstance().containsObject("vibratedOnLocationUpdate", Object.class)) {
    		ConfigurationManager.getInstance().putObject("vibratedOnLocationUpdate", new Object());
    		Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
    		v.vibrate(300);
    	}
    }
    
    public void softClose(int zoomLevel, int latitudeE6, int longitudeE6) {
        ConfigurationManager.getInstance().putInteger(ConfigurationManager.ZOOM, zoomLevel);
        ConfigurationManager.getInstance().putDouble(ConfigurationManager.LATITUDE, MathUtils.coordIntToDouble(latitudeE6));
        ConfigurationManager.getInstance().putDouble(ConfigurationManager.LONGITUDE, MathUtils.coordIntToDouble(longitudeE6));
        ConfigurationManager.getDatabaseManager().saveConfiguration(false);
    }
    
    public void hardClose(Handler loadingHandler, Runnable gpsRunnable, int zoomLevel, int latitudeE6, int longitudeE6) {
    	LoggerUtils.debug("hardClose");
    	if (LayerLoader.getInstance().isLoading()) {
    		LayerLoader.getInstance().stopLoading();
        }
    	LayerLoader.getInstance().onAppClose();
    	
    	showShortToast(Locale.getMessage(R.string.closingText));
    	
        UserTracker.getInstance().trackEvent("Exit", activity.getLocalClassName() + ".hardClose", "", 0);
        
        if (gpsRunnable != null) {
        	loadingHandler.removeCallbacks(gpsRunnable);
        }
        
        LocationServicesManager.disableMyLocation();

        ConfigurationManager.getInstance().setOn(ConfigurationManager.SEND_MY_POS_AT_STARTUP);
        softClose(zoomLevel, latitudeE6, longitudeE6);

        //SuggestionProviderUtil.clearHistory();

        IconCache.getInstance().clearAll();
        LandmarkManager.getInstance().clearLandmarkStore();
        asyncTaskManager.cancelAll();
        
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            RouteRecorder.getInstance().onAppClose();
        }
        RoutesManager.getInstance().clearRoutesStore();

        PersistenceManagerFactory.getFileManager().clearImageCache();
        ConfigurationManager.getDatabaseManager().closeAllDatabases();
        ConfigurationManager.getInstance().clearObjectCache();
        
        HttpUtils.closeConnManager();
        
        showShortToast(Locale.getMessage(R.string.Close_app_bye));
        LoggerUtils.debug("Bye...");
    }
    
    public void addMyLocationLandmark(Location l) {
    	if (l != null) {
    		String provider = l.getProvider();
    		if (StringUtils.isEmpty(provider)) {
    			provider = "unknown";
    		}
    		String date = DateTimeUtils.getDefaultDateTimeString(System.currentTimeMillis(), ConfigurationManager.getInstance().getCurrentLocale());
    		LandmarkManager.getInstance().addLandmark(l.getLatitude(), l.getLongitude(), (float)l.getAltitude(), Locale.getMessage(R.string.Your_Location), Locale.getMessage(R.string.Your_Location_Desc, provider, l.getAccuracy(), date), Commons.MY_POSITION_LAYER, false);
        }
    }
    
    private boolean isGoogleApiAvailable() {
    	if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity) == ConnectionResult.SUCCESS) {
        	return true;
        } else {
        	return false;
        }
    }
    
    private class ConfigurationEntryToLandmarkParcelableFunction implements Function<Map.Entry<String, String>, LandmarkParcelable> {

    	private int pos = -1;
        
		@Override
		public LandmarkParcelable apply(Entry<String, String> config) {
			pos++;
			return LandmarkParcelableFactory.getLandmarkParcelable(pos, config.getKey(), config.getValue());
		}
    	
    }
    
    private class FavouritesDAOToLandmarkParcelableFunction implements Function<FavouritesDAO, LandmarkParcelable> {

    	private double lat, lng;
    	
    	public FavouritesDAOToLandmarkParcelableFunction(double lat, double lng) {
    		this.lat = lat;
    		this.lng = lng;
    	}
    	
		@Override
		public LandmarkParcelable apply(FavouritesDAO f) {
			return LandmarkParcelableFactory.getLandmarkParcelable(f, lat, lng);
		}
    	
    }
    
    private static class IntentHelper {
    	public static Intent getNavigationIntent(String navigationUri) {
    		Uri gmmIntentUri = Uri.parse(navigationUri);
    		Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
    		//since API v4
    		mapIntent.setPackage("com.google.android.apps.maps");
    		return mapIntent;
    	}
    }

}
