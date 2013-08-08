/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
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
import com.exina.android.calendar.CalendarActivity;
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
import com.jstakun.gms.android.service.AutoCheckinStartServiceReceiver;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.BCTools;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.ProjectionInterface;
import com.jstakun.gms.android.utils.StringUtil;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class Intents {

    public static final int INTENT_ADD_LANDMARK = 0;
    public static final int INTENT_BLOGEO_POST = 1;
    private static final int INTENT_PREFS = 2;
    public static final int INTENT_FILES = 3;
    public static final int INTENT_QRCODECHECKIN = 4;
    public static final int INTENT_CHECKIN = 5;
    public static final int INTENT_PICKLOCATION = 6;
    public static final int INTENT_MULTILANDMARK = 7;
    public static final int INTENT_MYLANDMARKS = 8;
    public static final int INTENT_LAYERS = 9;
    private static final int INTENT_CATEGORIES = 10;
    public static final int INTENT_AUTO_CHECKIN = 11;
    public static final int INTENT_CALENDAR = 12;
    private static final String SCAN_INTENT = "com.google.zxing.client.android.SCAN";
    private Activity activity;
    private LandmarkManager landmarkManager;
    private AsyncTaskManager asyncTaskManager;
    private List<ResolveInfo> activities;
    private final Handler thumbnailLoadedHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            String url = message.getData().getString("url");
            //System.out.println("Refreshing thumbnail icon " + url + " -----------------------------------------------------");

            try {
                View lvView = activity.findViewById(R.id.lvView);
                ImageView thumbnail = (ImageView) lvView.findViewById(R.id.thumbnailButton);
                TextView desc = (TextView) lvView.findViewById(R.id.lvdesc);
                ExtendedLandmark selectedLandmark = (ExtendedLandmark) thumbnail.getTag();

                if (selectedLandmark != null && StringUtils.equals(url, selectedLandmark.getThumbnail())) {
                    Bitmap image = IconCache.getInstance().getThumbnailResource(url, activity.getResources().getDisplayMetrics(), null);
                    if (image != null && image.getWidth() < lvView.getWidth() * 0.5) {
                        thumbnail.setImageBitmap(image);
                        String descr = "";
                        double lat = ConfigurationManager.getInstance().getDouble(ConfigurationManager.LATITUDE);
                        double lng = ConfigurationManager.getInstance().getDouble(ConfigurationManager.LONGITUDE);
                        float dist = DistanceUtils.distanceInKilometer(lat, lng, selectedLandmark.getQualifiedCoordinates().getLatitude(), selectedLandmark.getQualifiedCoordinates().getLongitude());
                        if (dist >= 0.001f) {
                            descr += Locale.getMessage(R.string.Landmark_distance, DistanceUtils.formatDistance(dist)) + "<br/>";
                        }
                        if (selectedLandmark.getDescription() != null) {
                            descr += selectedLandmark.getDescription();
                        }
                        if (StringUtils.isNotEmpty(descr)) {
                            FlowTextHelper.tryFlowText(descr, thumbnail, desc, activity.getWindowManager().getDefaultDisplay(), 3, imgGetter);
                        }
                    }
                }
            } catch (Exception e) {
                LoggerUtils.error("Intents.thumbnailLoadedHandler exception:", e);
            }
        }
    };
    private final ImageGetter imgGetter = new Html.ImageGetter() {
        @Override
        public Drawable getDrawable(String source) {
            Drawable drawable = null;
            int resId = activity.getResources().getIdentifier(source, "drawable", activity.getPackageName());
            if (resId > 0) {
                drawable = activity.getResources().getDrawable(resId);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }
            return drawable;
        }
    };

    public Intents(Activity activity, LandmarkManager landmarkManager, AsyncTaskManager asyncTaskManager) {
        this.activity = activity;
        this.landmarkManager = landmarkManager;
        this.asyncTaskManager = asyncTaskManager;
    }

    public void startPickLocationActivity() {
        activity.startActivityForResult(new Intent(activity, PickLocationActivity.class), INTENT_PICKLOCATION);
    }

    public void startSettingsActivity(Class<?> settingsActivityClass) {
        Intent intent = new Intent(activity, settingsActivityClass);
        Bundle extras = landmarkManager.getLayerManager().loadLayersGroup();
        //CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        //Bundle cextras = cm.loadCategoriesGroup();
        //extras.putBundle("categories", cextras);
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

    public boolean startRouteLoadingActivity() {
        ArrayList<LandmarkParcelable> routes = PersistenceManagerFactory.getFileManager().readFolderAsLandmarkParcelable(FileManager.getRoutesFolderPath(), FilenameFilterFactory.getFilenameFilter("kml"), Commons.ROUTES_LAYER);

        if (!routes.isEmpty()) {
            Bundle extras = new Bundle();
            extras.putParcelableArrayList("files", routes);
            extras.putInt("type", FilesActivity.ROUTES);
            extras.putInt("sort", AbstractLandmarkList.ORDER_BY_DATE_DESC);
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
            extras.putInt("sort", AbstractLandmarkList.ORDER_BY_NAME);
            Intent intent = new Intent(activity, FilesActivity.class);
            intent.putExtras(extras);
            activity.startActivityForResult(intent, INTENT_FILES);
        }

        return files.isEmpty();
    }

    public void startSocialListActivity() {
        activity.startActivity(new Intent(activity, SocialListActivity.class));
    }

    public void startLayersListActivity() {
        activity.startActivityForResult(new Intent(activity, LayerListActivity.class), INTENT_LAYERS);
    }

    public void startCategoryListActivity(int latitudeSpan, int longitudeSpan, int cursorLatitude, int cursorLongitude, int parent, int radius, Class<?> dealCategoryListClass) {
        Intent intent = new Intent(activity, dealCategoryListClass);
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
        activity.startActivity(intent);
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
        } else if (landmarkManager.getLayerManager().isLayerCheckinable(selectedLayer)
                && !ConfigurationManager.getInstance().isUserLoggedIn()) {
            startLoginActivity();
            return false;
        } else if (!ConfigurationManager.getInstance().isUserLoggedIn()) {
            return false;
        }

        return true;
    }


    public void startLoginActivity(int type) {
        List<String> items = ConfigurationManager.getInstance().getLoginItems(true);
        String[] selected = items.get(type).split(";");
        if (selected[0].startsWith(ConfigurationManager.GMS_WORLD)) {
            startLoginActivity();
        } else {
            startOAuthActivity(selected[1]);
        }
    }

    public boolean isIntentAvailable(String action) {
        final PackageManager packageManager = activity.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (list.size() > 0);
    }

    public void startQrCodeCheckinActivity(double[] currentLocation) {
        final boolean scanAvailable = isIntentAvailable(SCAN_INTENT);
        if (scanAvailable) {
            Intent intent = new Intent(SCAN_INTENT);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            activity.startActivityForResult(intent, INTENT_QRCODECHECKIN);
        } else {
            showInfoToast(Locale.getMessage(R.string.QRCode_scanner_missing_error));
        }
    }

    public void startLocationCheckinActivity(double[] currentLocation) {
        //List<LandmarkParcelable> checkinable = new ArrayList<LandmarkParcelable>();
        //landmarkManager.getCheckinableLandmarks(checkinable, currentLocation[0], currentLocation[1]);
        //startLandmarkListActivity(checkinable, INTENT_CHECKIN, -1, -1, R.string.Location_checkin_error);
        startLandmarkListActivity(INTENT_CHECKIN, null, LandmarkListActivity.SOURCE.CHECKIN, currentLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
    }

    public void showNearbyLandmarks(double[] currentLocation, ProjectionInterface projection, int sortOrder) {
        landmarkManager.findVisibleLandmarks(projection, true);
        startMultiLandmarkIntent(currentLocation, sortOrder);
    }

    public void showLandmarksInDay(double[] currentLocation, int year, int month, int day) {
        //if (landmarkManager.findLandmarksInDay(year, month, day)) {
        //    startMultiLandmarkIntent(currentLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
        //} else {
        //    showInfoToast(Locale.getMessage(R.string.Landmarks_day_empty));
        //}
        Intent src = new Intent();
        src.putExtra("year", year);
        src.putExtra("month", month);
        src.putExtra("day", day);
        startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.DAY_LANDMARKS, currentLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
    }

    public void startAutoCheckinListActivity(double[] currentLocation) {
        FavouritesDbDataSource fdb = (FavouritesDbDataSource) ConfigurationManager.getInstance().getObject("FAVOURITESDB", FavouritesDbDataSource.class);
        if (fdb != null) {
            List<FavouritesDAO> favourites = fdb.fetchAllLandmarks();
            ArrayList<LandmarkParcelable> dataList = new ArrayList<LandmarkParcelable>();

            if (!favourites.isEmpty()) {
                for (Iterator<FavouritesDAO> iter = favourites.iterator(); iter.hasNext();) {
                    FavouritesDAO f = iter.next();
                    dataList.add(LandmarkParcelableFactory.getLandmarkParcelable(f, currentLocation[0], currentLocation[1]));
                }
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
            if (landmark != null) {
                String number = "tel:" + landmark.getPhone();
                Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number));
                activity.startActivity(callIntent);
            } else {
                showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
            }
        } catch (Exception e) {
            LoggerUtils.error("Intents.startPhoneCallActivity exception", e);
            showInfoToast(Locale.getMessage(R.string.Call_not_supported_error));
        }
    }

    public void startLandmarkDetailsActivity(final String url, String title) {
        if (url != null) {
            final String[] layers = new String[]{"youtube", "panoramio", "8coupons", "flickr", "google"};

            if (StringUtils.indexOfAny(url, layers) > 0) {
                startActionViewIntent(url);
            } else {
                Intent intent = new Intent(activity, WebViewActivity.class);
                intent.putExtra("url", url);
                if (StringUtils.isNotEmpty(title)) {
                    intent.putExtra("title", title);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
        } else {
            showInfoToast(Locale.getMessage(R.string.Landmark_url_empty_error));
        }
    }

    public void startActionViewIntent(final String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public void startNewestLandmarkIntent(double[] currentLocation, String[] excluded, int sortOrder, int maxDays) {
        //List<LandmarkParcelable> newest = new ArrayList<LandmarkParcelable>();
        //landmarkManager.findNewestLandmarks(newest, maxDays, excluded, currentLocation[0], currentLocation[1]);
        //startLandmarkListActivity(newest, INTENT_MULTILANDMARK, sortOrder, -1, R.string.Landmark_search_empty_result);
        Intent src = new Intent();
        src.putExtra("excluded", excluded);
        src.putExtra("maxDays", maxDays);
        startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.NEWEST, currentLocation, sortOrder);
    }

    public void startFriendsCheckinsIntent(double[] currentLocation) {
        //List<LandmarkParcelable> landmarks = new ArrayList<LandmarkParcelable>();
        //landmarkManager.findFriendsCheckinLandmarks(landmarks, currentLocation[0], currentLocation[1]);
        //startLandmarkListActivity(landmarks, INTENT_MULTILANDMARK, sortOrder, -1, R.string.Landmark_search_empty_result);
        startLandmarkListActivity(INTENT_MULTILANDMARK, null, LandmarkListActivity.SOURCE.FRIENDS_CHECKINS, currentLocation, AbstractLandmarkList.ORDER_BY_DATE_DESC);
    }

    public void startDealsOfTheDayIntent(double[] currentLocation, String[] excluded) {
        //List<LandmarkParcelable> deals = new ArrayList<LandmarkParcelable>();
        //landmarkManager.findDealsOfTheDay(deals, excluded, currentLocation[0], currentLocation[1]);
        //startLandmarkListActivity(deals, INTENT_MULTILANDMARK, AbstractLandmarkList.ORDER_BY_CAT_STATS, -1, R.string.Landmark_search_empty_result);
        Intent src = new Intent();
        src.putExtra("excluded", excluded);
        startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.DOD, currentLocation, AbstractLandmarkList.ORDER_BY_CAT_STATS);
    }

    public void startRecentLandmarksIntent(double[] currentLocation) {
        //List<LandmarkParcelable> recentlySelected = new ArrayList<LandmarkParcelable>();
        //landmarkManager.getRecentlyOpenedLandmarks(recentlySelected, currentLocation[0], currentLocation[1]);
        //startLandmarkListActivity(recentlySelected, INTENT_MULTILANDMARK, sortOrder, -1, R.string.Landmark_search_empty_result);
        startLandmarkListActivity(INTENT_MULTILANDMARK, null, LandmarkListActivity.SOURCE.RECENT, currentLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
    }

    public void startMultiLandmarkIntent(double[] currentLocation, int sortOrder) {
        /*List<LandmarkParcelable> parcels = new ArrayList<LandmarkParcelable>();
         java.util.Locale locale = ConfigurationManager.getInstance().getCurrentLocale();
         List<ExtendedLandmark> landmarks = landmarkManager.getLandmarkToFocusQueue();
         for (int i = 0; i < landmarks.size(); i++) {
         parcels.add(LandmarkParcelableFactory.getLandmarkParcelable(landmarks.get(i), i, Integer.toString(i), currentLocation[0], currentLocation[1], locale));
         }
         startLandmarkListActivity(parcels, INTENT_MULTILANDMARK, sortOrder, -1, R.string.Landmark_search_empty_result);*/
        startLandmarkListActivity(INTENT_MULTILANDMARK, null, LandmarkListActivity.SOURCE.MULTI_LANDMARK, currentLocation, sortOrder);
    }

    public void startMyLandmarksIntent(double[] currentLocation) {
        /*List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
         landmarks.addAll(landmarkManager.getUnmodifableLayer(LayerManager.LOCAL_LAYER));
         List<ExtendedLandmark> mypos = landmarkManager.getUnmodifableLayer(LayerManager.MY_POSITION_LAYER);
         if (!mypos.isEmpty()) {
         landmarks.add(mypos.get(0));
         }

         List<LandmarkParcelable> parcels = new ArrayList<LandmarkParcelable>();
         java.util.Locale locale = ConfigurationManager.getInstance().getCurrentLocale();
         for (int i = 0; i < landmarks.size(); i++) {
         parcels.add(LandmarkParcelableFactory.getLandmarkParcelable(landmarks.get(i), i, Integer.toString(i), currentLocation[0], currentLocation[1], locale));
         }

         startLandmarkListActivity(parcels, INTENT_MYLANDMARKS, AbstractLandmarkList.ORDER_BY_DIST_ASC, -1, R.string.Landmark_NoLandmarks);*/
        startLandmarkListActivity(INTENT_MYLANDMARKS, null, LandmarkListActivity.SOURCE.MY_LANDMARKS, currentLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
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
    
    public void stopAutoCheckinService() {
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
        //only FS, FB and GMS World only
        ExtendedLandmark selectedLandmark = landmarkManager.getSeletedLandmarkUI();
        if (selectedLandmark.getLayer().equals(Commons.FOURSQUARE_LAYER)) {
            String venueid = selectedLandmark.getUrl();
            if (venueid != null) {
                String[] s = venueid.split("/");
                if (s.length > 0) {
                    venueid = s[s.length - 1];
                }
            }
            startCommentActivity(Commons.FOURSQUARE, venueid, null);
        } else if (selectedLandmark.getLayer().equals(Commons.FACEBOOK_LAYER)) {
            startCommentActivity(Commons.FACEBOOK, selectedLandmark.getUrl(), selectedLandmark.getName());
        } else {
            String venueid = selectedLandmark.getUrl();
            if (venueid != null) {
                String[] s = venueid.split("=");
                if (s.length > 0) {
                    venueid = s[s.length - 1];
                }
            }
            startCommentActivity("gms", venueid, null);
        }
    }

    public void saveRouteAction() {
        if (ConfigurationManager.getInstance().isOn(ConfigurationManager.RECORDING_ROUTE)) {
            asyncTaskManager.executeSaveRouteTask(activity.getString(R.string.saveRoute));
        } else if (ConfigurationManager.getInstance().isOff(ConfigurationManager.RECORDING_ROUTE)) {
            showInfoToast(Locale.getMessage(R.string.Routes_TrackMyPosStopped));
        }
    }

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
            String url = landmarkManager.getLandmarkURL(selectedLandmark);

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
                    message = Html.fromHtml(selectedLandmark.getDescription()).toString();
                }

                message += "\nLink: " + url;

                email = ConfigurationManager.getInstance().getString(ConfigurationManager.USER_EMAIL);
                if (StringUtils.isNotEmpty(email)) {
                    try {
                        email = new String(BCTools.decrypt(Base64.decode(email)));
                    } catch (Exception e) {
                        email = null;
                    }
                }
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

    public void setupShortcut() {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(activity, activity.getClass().getName());
        shortcutIntent.putExtra("com.jstakun.gms.android.ui.LauncherShortcut", activity.getClass().getName());

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, Locale.getMessage(R.string.app_name));

        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(activity, R.drawable.icon);
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

    public void loadLayersAction(boolean loadExternal, String selectedLayer, boolean clear, boolean loadServerLayers, LayerLoader layerLoader, double latitude, double longitude, int zoomLevel) {
        if (layerLoader != null) {
            if (layerLoader.isLoading()) {
                layerLoader.stopLoading();
            }
            if (clear) {
                landmarkManager.clearLandmarkStore();
                ConfigurationManager.getInstance().removeObject("dod", ExtendedLandmark.class);
            }
            Display display = activity.getWindowManager().getDefaultDisplay();
            layerLoader.loadLayers(latitude, longitude, zoomLevel, display.getWidth(), display.getHeight(), loadExternal, selectedLayer, loadServerLayers);
        }
    }

    public void showLandmarkDetailsView(final ExtendedLandmark selectedLandmark, final View lvView, double[] currentLocation, boolean loadAd) {
        TextView name = (TextView) lvView.findViewById(R.id.lvname);
        TextView header = (TextView) lvView.findViewById(R.id.lvheader);
        ImageView layerImage = (ImageView) lvView.findViewById(R.id.lvLayerImage);
        ImageButton lvActionButton = (ImageButton) lvView.findViewById(R.id.lvActionButton);
        ImageView thumbnail = (ImageView) lvView.findViewById(R.id.thumbnailButton);
        View lvOpenButton = lvView.findViewById(R.id.lvOpenButton);
        View lvCommentButton = lvView.findViewById(R.id.lvCommentButton);
        View lvCallButton = lvView.findViewById(R.id.lvCallButton);
        ImageButton lvRouteButton = (ImageButton) lvView.findViewById(R.id.lvCarRouteButton);
        TextView desc = (TextView) lvView.findViewById(R.id.lvdesc);
        desc.setMovementMethod(LinkMovementMethod.getInstance());

        if (loadAd) {
            AdsUtils.loadAd(activity);
        }

        name.setText(selectedLandmark.getName());
        desc.setText("");
        header.setText("");
        int visibleButtons = 5;

        lvActionButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvActionSeparator).setVisibility(View.VISIBLE);
        lvCommentButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.VISIBLE);
        lvOpenButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvOpenSeparator).setVisibility(View.VISIBLE);
        lvRouteButton.setVisibility(View.VISIBLE);
        lvView.findViewById(R.id.lvCarRouteSeparator).setVisibility(View.VISIBLE);
        int routeType = ConfigurationManager.getInstance().getInt(ConfigurationManager.ROUTE_TYPE);
        if (routeType == 2) {
            lvRouteButton.setImageResource(R.drawable.walk48);
        } else {
            lvRouteButton.setImageResource(R.drawable.route48);
        }
        String phone = selectedLandmark.getPhone();
        if (phone != null) {
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

        if (selectedLandmark.getCategoryId() != -1) {
            int icon = LayerManager.getDealCategoryIcon(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_SMALL, activity.getResources().getDisplayMetrics(), selectedLandmark.getCategoryId());
            layerImage.setImageResource(icon);
        } else {
            Bitmap icon = LayerManager.getLayerIcon(selectedLandmark.getLayer(), LayerManager.LAYER_ICON_SMALL, activity.getResources().getDisplayMetrics(), null);
            layerImage.setImageBitmap(icon);
        }

        if (selectedLandmark.getThumbnail() != null) {

            Bitmap image = IconCache.getInstance().getThumbnailResource(selectedLandmark.getThumbnail(), activity.getResources().getDisplayMetrics(), thumbnailLoadedHandler);
            if (image != null && image.getWidth() < lvView.getWidth() * 0.5) {
                thumbnail.setImageBitmap(image);
                thumbnail.setTag(null);
            } else {
                thumbnail.setImageResource(R.drawable.download48);
                thumbnail.setTag(selectedLandmark);
            }
            if (StringUtils.isNotEmpty(descr)) {
                FlowTextHelper.tryFlowText(descr, thumbnail, desc, activity.getWindowManager().getDefaultDisplay(), 3, imgGetter);
            }
            if (thumbnail != null) {
                thumbnail.setVisibility(View.VISIBLE);
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
        lvActionButton.setImageResource(R.drawable.checkin);
        if (selectedLandmark.getLayer().equals(Commons.FOURSQUARE_LAYER)) {
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FS_AUTH_STATUS)) {
                lvActionButton.setImageResource(R.drawable.login);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons--;
            }
        } else if (selectedLandmark.getLayer().equals(Commons.FOURSQUARE_MERCHANT_LAYER)) {
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FS_AUTH_STATUS)) {
                //lvActionButton.setImageResource(R.drawable.login);
                lvActionButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvActionSeparator).setVisibility(View.GONE);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons -= 2;
            }
        } else if (selectedLandmark.getLayer().equals(Commons.FACEBOOK_LAYER)) {
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FB_AUTH_STATUS)) {
                lvActionButton.setImageResource(R.drawable.login);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons--;
            }
        } else if (selectedLandmark.getLayer().equals(Commons.GOOGLE_PLACES_LAYER)) {
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons--;
            if (ConfigurationManager.getInstance().isOff(ConfigurationManager.GL_AUTH_STATUS)) {
                lvActionButton.setImageResource(R.drawable.login);
            }
        } else if (selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER)) {
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            if (!ConfigurationManager.getInstance().isUserLoggedIn()) {
                lvActionButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvActionSeparator).setVisibility(View.GONE);
            } else {
                lvActionButton.setImageResource(R.drawable.share);
            }
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons--;
            String date = DateTimeUtils.getDefaultDateTimeString(selectedLandmark.getDescription(), ConfigurationManager.getInstance().getCurrentLocale());
            desc.setText(Locale.getMessage(R.string.Last_update, date));
            lvRouteButton.setVisibility(View.GONE);
        } else if (selectedLandmark.getLayer().equals(Commons.LOCAL_LAYER)) {
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            lvActionButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvActionSeparator).setVisibility(View.GONE);
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons -= 2;
        } else if (landmarkManager.getLayerManager().isLayerCheckinable(selectedLandmark.getLayer())) { //GMS World checkinabe layers
            header.setText(landmarkManager.getLayerManager().getLayerFormatted(selectedLandmark.getLayer()));
            if (!ConfigurationManager.getInstance().isUserLoggedIn()) {
                lvActionButton.setImageResource(R.drawable.login);
                lvCommentButton.setVisibility(View.GONE);
                lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
                visibleButtons--;
            }
        } else {
            header.setText(selectedLandmark.getLayer());
            lvActionButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvActionSeparator).setVisibility(View.GONE);
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
            visibleButtons -= 2;
        }
        float dip = activity.getResources().getDisplayMetrics().density;
        if (visibleButtons == 5 && activity.getWindowManager().getDefaultDisplay().getWidth() < (360f * dip)) {
            lvCommentButton.setVisibility(View.GONE);
            lvView.findViewById(R.id.lvCommentSeparator).setVisibility(View.GONE);
        }
        lvView.setVisibility(View.VISIBLE);

        landmarkManager.addRecentlyOpenedLandmark(selectedLandmark);
    }

    public void showInfoToast(String msg) {
        Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG).show();
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

    public void setRepeatingAlarm(Class<?> clazz) {
        AlarmManager am = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(activity, clazz);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(activity, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), (60 * 1000), pendingIntent);
    }

    public boolean isNewerVersionAvailable() {
        HttpUtils utils = new HttpUtils();
        boolean response = false;

        try {
            String appId = ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID);
            String url = ConfigurationManager.getInstance().getServicesUrl() + "notifications?type=v&appId=" + appId;

            byte[] res = utils.loadHttpFile(url, true, "json");
            if (res != null) {
                String resp = new String(res);
                if (resp.startsWith("{")) {
                    JSONObject json = new JSONObject(resp);
                    int version = Integer.valueOf(json.optString("value", "0"));
                    PackageInfo info = activity.getPackageManager().getPackageInfo(ConfigurationManager.getInstance().getString(ConfigurationManager.PACKAGE_NAME), 0);
                    int versionCode = info.versionCode;
                    if (version > versionCode) {
                        response = true;
                    }
                    LoggerUtils.debug("Current version: " + versionCode + ", server version " + version);
                }
            }
        } catch (Exception ex) {
            LoggerUtils.error("Intents.isNewerVersionAvailable exception", ex);
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

    public void startSearchActivity(int latitudeSpan, int longitudeSpan, int cursorLatitude, int cursorLongitude, int radius, boolean isDeal) {
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

    public void checkAppVersion() {

        //whats new helpactivity call
        int versionCode = -1;
        String versionName = null;

        try {
            PackageInfo info = activity.getPackageManager().getPackageInfo(ConfigurationManager.getInstance().getString(ConfigurationManager.PACKAGE_NAME), 0);
            versionCode = info.versionCode;
            versionName = info.versionName;
            //info.versionName ends with m;
            LoggerUtils.debug("Version code: " + versionCode);
        } catch (Exception ex) {
            LoggerUtils.error("Intents.checkAppVersion() exception", ex);
        }
        int buildVersion = ConfigurationManager.getInstance().getInt(ConfigurationManager.BUILD_VERSION);
        if (buildVersion == -1 || (buildVersion < versionCode && StringUtils.endsWithIgnoreCase(versionName, "m"))) {
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.BUILD_VERSION, versionCode);
            startHelpActivity();
            //if (buildVersion != -1 && buildVersion < 1000) { //GMS 2.0.1
            //    PersistenceManager pm = PersistenceManagerFactory.getPersistenceManagerInstance();
            //    pm.deleteTilesCache();
            //}
        }
    }

    /*private void startLandmarkListActivity(List<LandmarkParcelable> landmarks, int activityCode, int sortOrder, int requestCode, int emptyMessage) {
     if (!landmarks.isEmpty()) {
     ConfigurationManager.getInstance().putObject(LandmarkListActivity.LANDMARK_PARCELABLE_LIST, landmarks);
     Intent intent = new Intent(activity, LandmarkListActivity.class);
     intent.putExtra("sort", sortOrder);
     intent.putExtra("requestCode", requestCode);
     activity.startActivityForResult(intent, activityCode);
     } else {
     showInfoToast(Locale.getMessage(emptyMessage));
     }
     }*/
    private void startLandmarkListActivity(int requestCode, Intent src, LandmarkListActivity.SOURCE source, double[] myLocation, int sortOrder) { //int sortOrder, String layer, double[] myLocation, int cat, int subCat) {
        Intent intent = new Intent(activity, LandmarkListActivity.class);
        intent.putExtra("sort", sortOrder);
        intent.putExtra("lat", myLocation[0]);
        intent.putExtra("lng", myLocation[1]);
        intent.putExtra("requestCode", requestCode);
        if (src != null) {
            intent.putExtras(src);
        }
        intent.putExtra("source", source);
        activity.startActivityForResult(intent, requestCode);
    }

    public byte[] takeScreenshot() {
        byte[] scr = null;
        View v = activity.findViewById(android.R.id.content);
        v.setDrawingCacheEnabled(true);
        try {
            Bitmap screenShot = v.getDrawingCache();
            if (screenShot != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                screenShot.compress(Bitmap.CompressFormat.JPEG, 50, out);
                screenShot.recycle();
                scr = out.toByteArray();
            }
        } catch (Throwable ex) {
            LoggerUtils.error("Intents.takeScreenshot() exception", ex);
        } finally {
            v.setDrawingCacheEnabled(false);
        }
        return scr;
    }

    public void processActivityResult(int requestCode, int resultCode, Intent intent, double[] myLocation, double[] mapCenter, Handler showRouteHandler, int zoomLevel, LayerLoader layerLoader) {
        if (requestCode == INTENT_CATEGORIES) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                if (StringUtils.equals(action, "show")) {
                    //startMultiLandmarkIntent(myLocation, AbstractLandmarkList.ORDER_BY_DATE_DESC);
                    Intent src = new Intent();
                    src.putExtras(intent);
                    //String category = intent.getStringExtra("layer");
                    //int cat = intent.getIntExtra("catId", -1);
                    //int subCat = intent.getIntExtra("subCatId", -1);
                    startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.CATEGORY, myLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
                } else if (StringUtils.equals(action, "noshow")) { //DealCategoryListActivity
                    //int category = intent.getIntExtra("category", -1);
                    //int subcategory = intent.getIntExtra("subcategory", -1);
                    Intent result = new Intent();
                    //result.putExtra("subcategory", subcategory);
                    //result.putExtra("category", category);
                    result.putExtras(intent);
                    //if (StringUtils.equals(action, "noshow")) {
                    result.putExtra("action", "show");
                    //} //else {
                    //    result.putExtra("action", action);
                    //}
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
                if (intent.getStringExtra("deals") != null) {
                    CategoriesManager cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
                    if (cm != null) {
                        cm.saveCategoriesAction(names, codes);
                    }
                } else {
                    landmarkManager.getLayerManager().saveLayersAction(names, codes);
                }
            }
        } else if (requestCode == INTENT_ADD_LANDMARK) {
            if (resultCode == Activity.RESULT_OK) {
                String name = intent.getStringExtra("name");
                String desc = intent.getStringExtra("desc");
                String layer = intent.getStringExtra("layer");
                String fsCategory = null;
                if (intent.hasExtra("fsCategory")) {
                    fsCategory = intent.getStringExtra("fsCategory");
                }
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
        } else if (requestCode == Intents.INTENT_FILES) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                int type = intent.getIntExtra("type", -1);
                String filename = intent.getStringExtra("filename");
                if (action.equals("load")) {
                    if (type == FilesActivity.ROUTES) {
                        asyncTaskManager.executeRouteLoadingTask(filename, showRouteHandler);
                    } else if (type == FilesActivity.FILES) {
                        asyncTaskManager.executePoiFileLoadingTask(filename);
                    }
                }
            }
        } else if (requestCode == Intents.INTENT_QRCODECHECKIN) {
            if (resultCode == Activity.RESULT_OK) {
                String checkinLandmarkCode = intent.getStringExtra("SCAN_RESULT");
                String qrformat = intent.getStringExtra("SCAN_RESULT_FORMAT");
                asyncTaskManager.executeQrCodeCheckInTask(checkinLandmarkCode, qrformat, activity.getString(R.string.qrcheckin));
            }
        } else if (requestCode == Intents.INTENT_CHECKIN) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                String name = intent.getStringExtra("name");
                if (action.equals("load")) {
                    String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);
                    int id = Integer.parseInt(ids);
                    ExtendedLandmark selectedLandmark = landmarkManager.getLandmarkToFocusQueueSelectedLandmark(id);
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
                } //else if (action.equals("delete")) {
                //delete landmark
                //intents.showInfoToast(Locale.getMessage(R.string.Landmark_delete_not_allowed_error));
                //}
            }
        } else if (requestCode == Intents.INTENT_LAYERS) {
            if (resultCode == Activity.RESULT_OK) {
                String action = intent.getStringExtra("action");
                //if (StringUtils.equals(action, "start")) {
                //    startMultiLandmarkIntent(myLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
                //} else 
                if (StringUtils.equals(action, "load")) {
                    String layer = intent.getStringExtra("layer");
                    loadLayersAction(true, layer, false, false, layerLoader,
                            mapCenter[0], mapCenter[1], zoomLevel);
                } else if (StringUtils.equals(action, "refresh")) {
                    loadLayersAction(true, null, false, true, layerLoader,
                            mapCenter[0], mapCenter[1], zoomLevel);
                } else if (StringUtils.equals(action, "show")) {
                    //String layer = intent.getStringExtra("layer");
                    Intent src = new Intent();
                    src.putExtras(intent);
                    startLandmarkListActivity(INTENT_MULTILANDMARK, src, LandmarkListActivity.SOURCE.LAYER, myLocation, AbstractLandmarkList.ORDER_BY_DIST_ASC);
                }
            }
        }
    }
}
