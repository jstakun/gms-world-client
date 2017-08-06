package com.jstakun.gms.android.ui;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.UserTracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.view.KeyEvent;

/**
 *
 * @author jstakun
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private ListPreferenceMultiSelect layers = null;
    private String[] names = null;
    private Preference googleMapsType, osmMapsType, mapProvider;
    private PreferenceCategory settings;
    private boolean reindex = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            names = extras.getStringArray("names");
            String[] codes = extras.getStringArray("codes");
            boolean[] enabled = extras.getBooleanArray("enabled");

            //UserTracker.getInstance().startSession(this);
            UserTracker.getInstance().trackActivity(getClass().getName());
            //clear();

            layers = (ListPreferenceMultiSelect) findPreference(ConfigurationManager.LAYERS);
            layers.setEntries(names);
            layers.setSelectedEntries(enabled);
            layers.setEntryValues(codes);

            googleMapsType = findPreference("googleMapsType");
            osmMapsType = findPreference("osmMapsType");
            mapProvider = findPreference("mapProvider");
            settings = (PreferenceCategory) findPreference("settings");

            if (ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.OSM_MAPS) {
                settings.addPreference(osmMapsType);
                settings.removePreference(googleMapsType);
            } else if (ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.GOOGLE_MAPS) {
                settings.addPreference(googleMapsType);
                settings.removePreference(osmMapsType);
            }
            
            if (!OsUtil.hasSystemSharedLibraryInstalled(this, "com.google.android.maps") || !OsUtil.isDonutOrHigher()) {
                settings.removePreference(mapProvider);
                settings.removePreference(googleMapsType);
            }
        } else {
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        prefs.registerOnSharedPreferenceChangeListener(this);

        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(ConfigurationManager.MAP_PROVIDER, ConfigurationManager.getInstance().getString(ConfigurationManager.MAP_PROVIDER));
        setListPreference(ConfigurationManager.MAP_PROVIDER, R.array.mapProvider);

        editor.putString(ConfigurationManager.LOG_LEVEL, ConfigurationManager.getInstance().getString(ConfigurationManager.LOG_LEVEL));
        setListPreference(ConfigurationManager.LOG_LEVEL, R.array.logLevel);

        editor.putString(ConfigurationManager.PERSIST_METHOD, ConfigurationManager.getInstance().getString(ConfigurationManager.PERSIST_METHOD));
        setListPreference(ConfigurationManager.PERSIST_METHOD, R.array.landmarkPersist);

        editor.putString(ConfigurationManager.UNIT_OF_LENGHT, ConfigurationManager.getInstance().getString(ConfigurationManager.UNIT_OF_LENGHT));
        setListPreference(ConfigurationManager.UNIT_OF_LENGHT, R.array.unitOfLength);

        editor.putString(ConfigurationManager.GOOGLE_MAPS_TYPE, ConfigurationManager.getInstance().getString(ConfigurationManager.GOOGLE_MAPS_TYPE));
        setListPreference(ConfigurationManager.GOOGLE_MAPS_TYPE, R.array.googleMaps);

        editor.putString(ConfigurationManager.OSM_MAPS_TYPE, ConfigurationManager.getInstance().getString(ConfigurationManager.OSM_MAPS_TYPE));
        setListPreference(ConfigurationManager.OSM_MAPS_TYPE, R.array.osmMaps);

        editor.putString(ConfigurationManager.ROUTE_TYPE, ConfigurationManager.getInstance().getString(ConfigurationManager.ROUTE_TYPE));
        setListPreference(ConfigurationManager.ROUTE_TYPE, R.array.routeType);

        editor.putString(ConfigurationManager.SEARCH_TYPE, ConfigurationManager.getInstance().getString(ConfigurationManager.SEARCH_TYPE));
        setListPreference(ConfigurationManager.SEARCH_TYPE, R.array.searchType);

        editor.putInt(ConfigurationManager.LANDMARKS_PER_LAYER, ConfigurationManager.getInstance().getInt(ConfigurationManager.LANDMARKS_PER_LAYER));
        //setPreference(ConfigurationManager.LANDMARKS_PER_LAYER, R.array.landmarksPerLayer);

        editor.putInt(ConfigurationManager.SEARCH_RADIUS, ConfigurationManager.getInstance().getInt(ConfigurationManager.SEARCH_RADIUS));
        //setPreference(ConfigurationManager.SEARCH_RADIUS, R.array.radius);

        editor.putBoolean(ConfigurationManager.AUTO_CHECKIN, ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN));

        editor.putBoolean(ConfigurationManager.TRACK_USER, ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER));

        editor.putBoolean(ConfigurationManager.DEV_MODE, ConfigurationManager.getInstance().isOn(ConfigurationManager.DEV_MODE));

        editor.putString(ConfigurationManager.NETWORK_MODE, ConfigurationManager.getInstance().getString(ConfigurationManager.NETWORK_MODE));
        setListPreference(ConfigurationManager.NETWORK_MODE, R.array.imageLoading);
        
        editor.commit();
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        //UserTracker.getInstance().stopSession(this);
    }

    //use only in API version > 10
    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	Intent result = new Intent();
                result.putExtra("reindex", reindex);
                setResult(RESULT_OK, result);
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }*/
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		Intent result = new Intent();
            result.putExtra("reindex", reindex);
            setResult(RESULT_OK, result);
            finish();
            return true;
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //System.out.println("Preference " + key + " has changed");
        int tmp;

        if (key.equals(ConfigurationManager.UNIT_OF_LENGHT)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.UNIT_OF_LENGHT, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.UNIT_OF_LENGHT, tmp);
            setListPreference(ConfigurationManager.UNIT_OF_LENGHT, R.array.unitOfLength);
        } else if (key.equals(ConfigurationManager.LAYERS)) {
            String rawval = sharedPreferences.getString(ConfigurationManager.LAYERS, "");
            String[] selected = ListPreferenceMultiSelect.parseStoredValue(rawval);
            Intent result = new Intent();
            result.putExtra("reindex", reindex);
            result.putExtra("names", names);
            result.putExtra("codes", selected);
            setResult(RESULT_OK, result);
            finish();
        } else if (key.equals(ConfigurationManager.MAP_PROVIDER)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.MAP_PROVIDER, ConfigurationManager.OSM_TILES);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.MAP_PROVIDER, tmp);
            setListPreference(ConfigurationManager.MAP_PROVIDER, R.array.mapProvider);

            if (ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.OSM_MAPS) {
                settings.addPreference(osmMapsType);
                settings.removePreference(googleMapsType);
                setListPreference(ConfigurationManager.OSM_MAPS_TYPE, R.array.osmMaps);
            } else if (ConfigurationManager.getInstance().getInt(ConfigurationManager.MAP_PROVIDER) == ConfigurationManager.GOOGLE_MAPS) {
                settings.addPreference(googleMapsType);
                settings.removePreference(osmMapsType);
                setListPreference(ConfigurationManager.GOOGLE_MAPS_TYPE, R.array.googleMaps);
            }

        } else if (key.equals(ConfigurationManager.LOG_LEVEL)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.LOG_LEVEL, 3);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.LOG_LEVEL, tmp);
            setListPreference(ConfigurationManager.LOG_LEVEL, R.array.logLevel);
        } else if (key.equals(ConfigurationManager.PERSIST_METHOD)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.PERSIST_METHOD, ConfigurationManager.PERSIST_SERVER);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.PERSIST_METHOD, tmp);
            setListPreference(ConfigurationManager.PERSIST_METHOD, R.array.landmarkPersist);
        } else if (key.equals(ConfigurationManager.GOOGLE_MAPS_TYPE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.GOOGLE_MAPS_TYPE, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.GOOGLE_MAPS_TYPE, tmp);
            setListPreference(ConfigurationManager.GOOGLE_MAPS_TYPE, R.array.googleMaps);
        } else if (key.equals(ConfigurationManager.OSM_MAPS_TYPE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.OSM_MAPS_TYPE, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.OSM_MAPS_TYPE, tmp);
            setListPreference(ConfigurationManager.OSM_MAPS_TYPE, R.array.osmMaps);
        } else if (key.equals(ConfigurationManager.ROUTE_TYPE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.ROUTE_TYPE, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.ROUTE_TYPE, tmp);
            setListPreference(ConfigurationManager.ROUTE_TYPE, R.array.routeType);
        } else if (key.equals(ConfigurationManager.SEARCH_TYPE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.SEARCH_TYPE, 0);
            if (tmp != ConfigurationManager.getInstance().getInt(ConfigurationManager.SEARCH_TYPE)) {
            	ConfigurationManager.getInstance().putInteger(ConfigurationManager.SEARCH_TYPE, tmp);
                setListPreference(ConfigurationManager.SEARCH_TYPE, R.array.searchType);
                reindex = true;
            }
        } else if (key.equals(ConfigurationManager.LANDMARKS_PER_LAYER)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.LANDMARKS_PER_LAYER, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.LANDMARKS_PER_LAYER, tmp);
            //setPreference(ConfigurationManager.LANDMARKS_PER_LAYER, R.array.landmarksPerLayer);
        } else if (key.equals(ConfigurationManager.SEARCH_RADIUS)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.SEARCH_RADIUS, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.SEARCH_RADIUS, tmp);
            //setPreference(ConfigurationManager.SEARCH_RADIUS, R.array.radius);
        } else if (key.equals(ConfigurationManager.AUTO_CHECKIN)) {
            boolean value = sharedPreferences.getBoolean(ConfigurationManager.AUTO_CHECKIN, true);
            if (value) {
                ConfigurationManager.getInstance().setOn(ConfigurationManager.AUTO_CHECKIN);
            } else {
                ConfigurationManager.getInstance().setOff(ConfigurationManager.AUTO_CHECKIN);
            }
        } else if (key.equals(ConfigurationManager.TRACK_USER)) {
            boolean value = sharedPreferences.getBoolean(ConfigurationManager.TRACK_USER, true);
            if (value) {
                ConfigurationManager.getInstance().setOn(ConfigurationManager.TRACK_USER);
            } else {
                ConfigurationManager.getInstance().setOff(ConfigurationManager.TRACK_USER);
            }
        } else if (key.equals(ConfigurationManager.NETWORK_MODE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.NETWORK_MODE, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.NETWORK_MODE, tmp);
            setListPreference(ConfigurationManager.NETWORK_MODE, R.array.imageLoading);
        } else if (key.equals(ConfigurationManager.DEV_MODE)) {
            boolean value = sharedPreferences.getBoolean(ConfigurationManager.DEV_MODE, false);
            if (value) {
                ConfigurationManager.getInstance().setOn(ConfigurationManager.DEV_MODE);
            } else {
                ConfigurationManager.getInstance().setOff(ConfigurationManager.DEV_MODE);
            }
        } 
    }

    private void setListPreference(String preferenceName, int arrayId) {
    	ListPreference preference = (ListPreference)findPreference(preferenceName);
        if (preference != null) {
            String[] array = getResources().getStringArray(arrayId);
            int pos = ConfigurationManager.getInstance().getInt(preferenceName);
            if (pos < 0 || pos >= array.length) {
                pos = 0;
            }
            preference.setSummary(Locale.getMessage(R.string.Settings_Summary, array[pos]));
            preference.setValueIndex(pos);
        }
    }

    private int getPreferenceAsInt(SharedPreferences sharedPreferences, String key, int missing) {
        int result = missing;
        Object value = sharedPreferences.contains(key) ? sharedPreferences.getAll().get(key) : null;
        if (value instanceof Integer) {
            result = ((Integer) value).intValue();
        }
        if (value instanceof String) {
            try {
                result = Integer.parseInt((String) value);
            } finally {
            }
        }
        return result;
    }

    //private void clear() {
    //    SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
    //    SharedPreferences.Editor editor = prefs.edit();
    //    editor.clear();
    //    editor.commit();
    //}
}
