package com.jstakun.gms.android.ui.deals;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.ui.ActionBarHelper;
import com.jstakun.gms.android.ui.ListPreferenceMultiSelect;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

/**
 *
 * @author jstakun
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private String[] layerNames, categoryNames;
    private CheckBoxPreference showDod;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
            
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            //UserTracker.getInstance().startSession(this);
            UserTracker.getInstance().trackActivity(getClass().getName());

            showDod = (CheckBoxPreference) findPreference("showDealOfTheDay");
            showDod.setChecked(ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY));

            layerNames = extras.getStringArray("names");

            ListPreferenceMultiSelect layers = (ListPreferenceMultiSelect) findPreference(ConfigurationManager.LAYERS);
            layers.setEntries(layerNames);
            layers.setSelectedEntries(extras.getBooleanArray("enabled"));
            layers.setEntryValues(extras.getStringArray("codes"));

            Bundle categoriesBundle = CategoriesManager.getInstance().loadCategoriesGroup();

            categoryNames = categoriesBundle.getStringArray("names");

            ListPreferenceMultiSelect categories = (ListPreferenceMultiSelect) findPreference(ConfigurationManager.DEAL_CATEGORIES);
            categories.setEntries(categoryNames);
            categories.setSelectedEntries(categoriesBundle.getBooleanArray("enabled"));
            categories.setEntryValues(categoriesBundle.getStringArray("codes"));
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

        editor.putString(ConfigurationManager.LOG_LEVEL, ConfigurationManager.getInstance().getString(ConfigurationManager.LOG_LEVEL));
        setListPreference(ConfigurationManager.LOG_LEVEL, R.array.logLevel);

        editor.putString(ConfigurationManager.UNIT_OF_LENGHT, ConfigurationManager.getInstance().getString(ConfigurationManager.UNIT_OF_LENGHT));
        setListPreference(ConfigurationManager.UNIT_OF_LENGHT, R.array.unitOfLength);

        editor.putString(ConfigurationManager.GOOGLE_MAPS_TYPE, ConfigurationManager.getInstance().getString(ConfigurationManager.GOOGLE_MAPS_TYPE));
        setListPreference(ConfigurationManager.GOOGLE_MAPS_TYPE, R.array.googleMaps);

        editor.putString(ConfigurationManager.ROUTE_TYPE, ConfigurationManager.getInstance().getString(ConfigurationManager.ROUTE_TYPE));
        setListPreference(ConfigurationManager.ROUTE_TYPE, R.array.routeType);

        editor.putInt(ConfigurationManager.DEAL_LIMIT, ConfigurationManager.getInstance().getInt(ConfigurationManager.DEAL_LIMIT));
        //setPreference(ConfigurationManager.DEAL_LIMIT, R.array.dealLimit);

        editor.putInt(ConfigurationManager.SEARCH_RADIUS, ConfigurationManager.getInstance().getInt(ConfigurationManager.SEARCH_RADIUS));
        //setPreference(ConfigurationManager.SEARCH_RADIUS, R.array.radius);

        editor.putBoolean(ConfigurationManager.SHOW_DEAL_OF_THE_DAY, ConfigurationManager.getInstance().isOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY));

        editor.putBoolean(ConfigurationManager.TRACK_USER, ConfigurationManager.getInstance().isOn(ConfigurationManager.TRACK_USER));

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

    //use only in target API version > 10
    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }*/

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
            result.putExtra("names", layerNames);
            result.putExtra("codes", selected);
            setResult(RESULT_OK, result);
            finish();
        } else if (key.equals(ConfigurationManager.DEAL_CATEGORIES)) {
            String rawval = sharedPreferences.getString(ConfigurationManager.DEAL_CATEGORIES, "");
            String[] selected = ListPreferenceMultiSelect.parseStoredValue(rawval);
            Intent result = new Intent();
            result.putExtra("names", categoryNames);
            result.putExtra("codes", selected);
            result.putExtra("deals", "1");
            setResult(RESULT_OK, result);
            finish();
        } else if (key.equals(ConfigurationManager.LOG_LEVEL)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.LOG_LEVEL, 3);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.LOG_LEVEL, tmp);
            setListPreference(ConfigurationManager.LOG_LEVEL, R.array.logLevel);
        } else if (key.equals(ConfigurationManager.GOOGLE_MAPS_TYPE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.GOOGLE_MAPS_TYPE, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.GOOGLE_MAPS_TYPE, tmp);
            setListPreference(ConfigurationManager.GOOGLE_MAPS_TYPE, R.array.googleMaps);
        } else if (key.equals(ConfigurationManager.ROUTE_TYPE)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.ROUTE_TYPE, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.ROUTE_TYPE, tmp);
            setListPreference(ConfigurationManager.ROUTE_TYPE, R.array.routeType);
        } else if (key.equals(ConfigurationManager.DEAL_LIMIT)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.DEAL_LIMIT, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.DEAL_LIMIT, tmp);
            //setPreference(ConfigurationManager.DEAL_LIMIT, R.array.dealLimit);
        } else if (key.equals(ConfigurationManager.SEARCH_RADIUS)) {
            tmp = getPreferenceAsInt(sharedPreferences, ConfigurationManager.SEARCH_RADIUS, 0);
            ConfigurationManager.getInstance().putInteger(ConfigurationManager.SEARCH_RADIUS, tmp);
            //setPreference(ConfigurationManager.SEARCH_RADIUS, R.array.radius);
        } else if (key.equals(ConfigurationManager.SHOW_DEAL_OF_THE_DAY)) {
            boolean value = sharedPreferences.getBoolean(ConfigurationManager.SHOW_DEAL_OF_THE_DAY, true);
            if (value) {
                ConfigurationManager.getInstance().setOn(ConfigurationManager.SHOW_DEAL_OF_THE_DAY);
            } else {
                ConfigurationManager.getInstance().setOff(ConfigurationManager.SHOW_DEAL_OF_THE_DAY);
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
}
