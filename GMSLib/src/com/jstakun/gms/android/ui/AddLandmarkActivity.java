/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class AddLandmarkActivity extends Activity implements OnClickListener {

    private View addButton, cancelButton;
    private EditText nameText, descText;
    private Spinner categories;
    private CheckBox addVenueCheckbox;
    private Map<String, String> extLayersMap;
    private static final int ID_DIALOG_CATEGORIES = 0;
    private Map<String, Map<String, String>> fsCatMap;
    private String[] keys, values;
    private String fsCategory, selectedLayer;
    private DialogInterface.OnCancelListener fsCatCancel;
    private Intents intents;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.addLandmark);
        setContentView(R.layout.addlandmark);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
        
        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        initComponents();
    }

    private void initComponents() {
        addButton = findViewById(R.id.addButton);
        cancelButton = findViewById(R.id.cancelButton);
        descText = (EditText) findViewById(R.id.descText);
        nameText = (EditText) findViewById(R.id.nameText);
        categories = (Spinner) findViewById(R.id.categorySpinner);
        addVenueCheckbox = (CheckBox) findViewById(R.id.fsAddVenueCheckbox);

        intents = new Intents(this, null, null);

        if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FS_AUTH_STATUS)) {
            addVenueCheckbox.setVisibility(View.GONE);
        } else {
            fsCatMap = (Map<String, Map<String, String>>) ConfigurationManager.getInstance().getObject("fsCatMap", Map.class);
            if (fsCatMap == null) {
                FileManager fm = PersistenceManagerFactory.getFileManager();
                String json = fm.readJsonFile(R.raw.fscategories, ConfigurationManager.getInstance().getContext());
                //AsyncTaskExecutor.executeTask(new ParseFoursquareCategoriesTask(), this, json);
                new ParseFoursquareCategoriesTask(1).execute(json);
            } else {
                List<String> keysList = (List<String>) ConfigurationManager.getInstance().getObject("fsKeys", List.class);
                if (keysList != null) {
                    keys = (String[]) keysList.toArray();
                }

                List<String> valuesList = (List<String>) ConfigurationManager.getInstance().getObject("fsValues", List.class);
                if (valuesList != null) {
                    values = (String[]) valuesList.toArray();
                }
            }
        }

        List<String> extLayers = new ArrayList<String>();
        extLayers.add("Public");

        LandmarkManager lm = ConfigurationManager.getInstance().getLandmarkManager();
        if (lm != null) {
            extLayersMap = lm.getLayerManager().getExternalLayers();
            if (extLayersMap != null) {
                extLayers.addAll(extLayersMap.values());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, extLayers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categories.setAdapter(adapter);

        AdsUtils.loadAd(this);

        addButton.setOnClickListener(AddLandmarkActivity.this);
        cancelButton.setOnClickListener(AddLandmarkActivity.this);

        fsCatCancel = new DialogInterface.OnCancelListener() {

            public void onCancel(DialogInterface dialog) {
                try {
                    removeDialog(ID_DIALOG_CATEGORIES);
                } catch (Exception e) {
                    //ignore error
                }
            }
        };
    }

    public void onClick(View v) {
        if (v == addButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".AddLandmarkAction", "", 0);
            addLandmarkAction();
        } else if (v == cancelButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".AddLandmarkCancelled", "", 0);
            ConfigurationManager.getInstance().removeObject("fsKeys", List.class);
            ConfigurationManager.getInstance().removeObject("fsValues", List.class);
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_CATEGORIES) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.pickCategory).
                    setIcon(R.drawable.ic_dialog_menu_generic).
                    setOnCancelListener(fsCatCancel).
                    setSingleChoiceItems(values, -1, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int item) {
                    fsCategory = keys[item];
                    finalizeActivity();
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        } else {
            return super.onCreateDialog(id);
        }
    }

    private void addLandmarkAction() {
        String name = nameText.getText().toString();
        if (StringUtils.isNotEmpty(name)) {
            boolean finalize = true;
            selectedLayer = (String) categories.getSelectedItem();
            if (extLayersMap != null) {
                for (Iterator<Map.Entry<String, String>> iter = extLayersMap.entrySet().iterator(); iter.hasNext();) {
                    Map.Entry<String, String> entry = iter.next();
                    if (entry.getValue().equals(selectedLayer)) {
                        selectedLayer = entry.getKey();
                        break;
                    }
                }
            }
            if (addVenueCheckbox.isChecked()) {
                if (fsCatMap != null && selectedLayer != null && fsCatMap.containsKey(selectedLayer)) {
                    Map<String, String> cats = fsCatMap.get(selectedLayer);
                    int size = cats.size();
                    if (size > 1) {
                        keys = new String[size];
                        values = new String[size];
                        int i = 0;
                        for (Iterator<Map.Entry<String, String>> iter = cats.entrySet().iterator(); iter.hasNext();) {
                            Map.Entry<String, String> entry = iter.next();
                            keys[i] = entry.getKey();
                            values[i] = entry.getValue();
                            i++;
                        }
                        ConfigurationManager.getInstance().putObject("fsKeys", Arrays.asList(keys));
                        ConfigurationManager.getInstance().putObject("fsValues", Arrays.asList(values));
                        finalize = false;
                        showDialog(ID_DIALOG_CATEGORIES);
                    } else if (size == 1) {
                        fsCategory = cats.keySet().iterator().next();
                    }
                }
            }
            if (finalize) {
                finalizeActivity();
            }
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.Landmark_name_empty_error));
        }
    }

    private void finalizeActivity() {

        try {
            dismissDialog(ID_DIALOG_CATEGORIES);
        } catch (Exception e) {
            //ignore error
        }

        Intent result = new Intent();
        result.putExtra("name", nameText.getText().toString());
        result.putExtra("desc", descText.getText().toString());
        result.putExtra("addVenue", addVenueCheckbox.isChecked());
        result.putExtra("layer", selectedLayer);
        if (fsCategory != null) {
            result.putExtra("fsCategory", fsCategory);
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".AddLandmarkAction", "FS " + fsCategory, 0);
        }
        ConfigurationManager.getInstance().removeObject("fsKeys", List.class);
        ConfigurationManager.getInstance().removeObject("fsValues", List.class);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private class ParseFoursquareCategoriesTask extends GMSAsyncTask<String, Void, Void> {

        public ParseFoursquareCategoriesTask(int priority) {
            super(priority);
        }
        
        @Override
        protected Void doInBackground(String... args) {
            String json = args[0];
            try {
                if (json != null && json.startsWith("[")) {
                    JSONArray array = new JSONArray(json);
                    fsCatMap = new HashMap<String, Map<String, String>>();
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject cat = array.getJSONObject(i);
                        String id = cat.getString("id");
                        JSONArray subcat = cat.getJSONArray("subcategories");
                        Map<String, String> subCatMap = new LinkedHashMap<String, String>();
                        for (int j = 0; j < subcat.length(); j++) {
                            JSONObject sub = subcat.getJSONObject(j);
                            subCatMap.put(sub.getString("id"), sub.getString("name"));
                        }
                        if (!subCatMap.isEmpty()) {
                            fsCatMap.put(id, subCatMap);
                            ConfigurationManager.getInstance().putObject("fsCatMap", fsCatMap);
                        }
                    }
                }
            } catch (JSONException ex) {
                LoggerUtils.error("CategoryJsonParser.parserCategoryJson error:", ex);
            }

            return null;
        }
    }
}
