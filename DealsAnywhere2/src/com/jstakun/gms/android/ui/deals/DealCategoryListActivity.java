/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui.deals;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.ActionBarHelper;
import com.jstakun.gms.android.ui.AddLayerActivity;
import com.jstakun.gms.android.ui.Intents;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class DealCategoryListActivity extends ListActivity implements View.OnClickListener {

    private List<Category> categories = null;
    private int parent = -1, radius = 3, currentPos = -1, lat, lng;
    private LandmarkManager landmarkManager = null;
    private CategoriesManager cm = null;
    private Intents intents;
    private View searchButton, mapViewButton;
    private AlertDialog deleteLayerDialog;
    private List<String> names = null;
    private String categoryName;

    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);

        setTitle(Locale.getMessage(R.string.Category_title));

        setContentView(R.layout.categorylist);
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            parent = extras.getInt("parent");
            radius = extras.getInt("radius");
            lat = extras.getInt("lat");
            lng = extras.getInt("lng");
        }

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);

        intents = new Intents(this, landmarkManager, null);

        AdsUtils.loadAd(this);

        searchButton = findViewById(R.id.searchDealsButton);
        mapViewButton = findViewById(R.id.mapViewButton);

        searchButton.setOnClickListener(this);
        mapViewButton.setOnClickListener(this);

        if (OsUtil.isHoneycomb2OrHigher()) {
            findViewById(R.id.topPanel).setVisibility(View.GONE);
            findViewById(R.id.topPanelSeparator).setVisibility(View.GONE);
        }

        createDeleteLayerAlertDialog();

        Object retained = getLastNonConfigurationInstance();
        if (retained instanceof String) {
            categoryName = (String) retained;
        }

        registerForContextMenu(getListView());
    }

    protected boolean hasSubcategory(int position) {
        if (parent == -1) {
            int categoryId = categories.get(position).getCategoryID();
            return cm.hasSubcategory(categoryId);
        }
        return false;
    }

    protected Category getCategory(int position) {
        return categories.get(position);
    }

    protected Category getParentCategory(int categoryId) {
        return cm.getCategory(categoryId);
    }

    protected int countLandmarks(int position) {
        Category c = categories.get(position);
        return landmarkManager.countLandmarks(c);
    }

    protected void onClickAction(int position, String action) {
        if (action.equals("drill")) {
            intents.startCategoryListActivity(-1, -1, lat, lng, categories.get(position).getCategoryID(), radius, DealCategoryListActivity.class);
        } else if (action.equals("cancel")) {
            cancelActivity(false);
        } else {
            Category c = categories.get(position);
            if (!landmarkManager.isLayerEmpty(c)) {
                Intent result = new Intent();
                result.putExtra("action", "show");
                if (c.isCustom()) {
                    result.putExtra("layer", c.getCategory());
                } else {
                    if (parent != -1) {
                        result.putExtra("action", "noshow");
                    }
                    result.putExtra("category", c.getCategoryID());
                    result.putExtra("subcategory", c.getSubcategoryID());
                }
                setResult(RESULT_OK, result);
                finish();
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Landmark_search_empty_result));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class)) {
            Intent result = new Intent();
            setResult(RESULT_OK, result);
            finish();
        }

        if (cm != null) {
            if (parent != -1) {
                categories = cm.getSubCategories(parent);
            } else if (landmarkManager != null) {
                categories = cm.getEnabledCategories(landmarkManager.getLayerManager());
            }
        }

        int size = 0;
        if (categories != null) {
            size = categories.size();
            Collections.sort(categories, new CategoryComparator());
            
            //DEBUG comment out
            /*FileManager.copyDatabaseToSdCard("com.jstakun.gms.android.ui.deals", "config.db");
            
            String desc = "";
            for (Category c : categories) {
                desc += "Name: " + c.getCategory() + ", " +
                        "stats: " + c.getStats() + ", " +
                        "count: " + landmarkManager.selectCategoryLandmarksCount(c.getCategoryID(), c.getSubcategoryID()) + "\n";
            }
            intents.showInfoToast(desc);*/
            //
        }
        
        names = new ArrayList<String>();

        for (int i = 0; i < size; i++) {
            if (parent != -1) {
                names.add(categories.get(i).getSubcategory());
            } else {
                names.add(StringUtils.capitalize(categories.get(i).getCategory()));
            }
        }

        if (parent != -1) {
            Category parentCat = cm.getCategory(parent);
            setTitle(Locale.getMessage(R.string.dealsString, parentCat.getCategory()));
        }

        setListAdapter(new DealCategoryArrayAdapter(this, names));
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return categoryName;
    }

    @Override
    public boolean onSearchRequested() {
        intents.startSearchActivity(-1, -1, lat, lng, radius, true);
        return true;
    }

    public void onClick(View v) {
        if (v == searchButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".SearchAction", "", 0);
            onSearchRequested();
        } else if (v == mapViewButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".ShowMapAction", "", 0);
            cancelActivity(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //System.out.println("Key pressed in activity: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelActivity(false);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        intents.processActivityResult(requestCode, resultCode, intent, new double[]{lat, lng}, null, null, -1, null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.deal_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapMode:
                cancelActivity(true);
                break;
            case R.id.search:
                onSearchRequested();
                break;
            case android.R.id.home:
                cancelActivity(false);
                break;
            case R.id.addLayer:
                startActivity(new Intent(this, AddLayerActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list && parent == -1) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            currentPos = info.position;
            menu.setHeaderTitle(getCategory(info.position).getCategory());
            menu.setHeaderIcon(R.drawable.ic_dialog_menu_generic);
            String[] menuItems = getResources().getStringArray(R.array.filesContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();

        if (menuItemIndex == 0) {
            onClickAction(currentPos, "show");
        } else if (menuItemIndex == 1) {
            deleteLayerDialog.setTitle(Locale.getMessage(R.string.Layer_delete_prompt, getCategory(currentPos).getCategory()));
            deleteLayerDialog.show();
        }

        return true;
    }

    private void createDeleteLayerAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                layerDeleteAction();
            }
        }).setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        deleteLayerDialog = builder.create();
    }

    private void layerDeleteAction() {
        Category c = getCategory(currentPos);
        if (!c.isCustom()) {
            intents.showInfoToast(Locale.getMessage(R.string.Layer_operation_unsupported));
        } else {
            landmarkManager.deleteLayer(c.getCategory());
            ((ArrayAdapter) getListAdapter()).remove(names.remove(currentPos));
            categories.remove(c);
            intents.showInfoToast(Locale.getMessage(R.string.Layer_deleted, c.getCategory()));
        }
    }

    private void cancelActivity(boolean forward) {
        Intent result = new Intent();
        if (forward && parent != -1) {
            result.putExtra("action", "forward");
        }
        setResult(RESULT_CANCELED, result);
        finish();
    }

    private class CategoryComparator implements Comparator<Category> {

        private final CategoryCountComparator categoryCountComparator =
                new CategoryCountComparator();

        public int compare(Category cat0, Category cat1) {
            int res = cat1.getStats() - cat0.getStats();
            if (res != 0) {
                return res;
            } else {
                return categoryCountComparator.compare(cat0, cat1); //desc
            }
        }
    }

    private class CategoryCountComparator implements Comparator<Category> {

        private Map<String, Integer> categoryStats = new HashMap<String, Integer>();

        public int compare(Category cat0, Category cat1) {

            int count1;
            String key = cat1.getCategoryID() + "," + cat1.getSubcategoryID();
            if (categoryStats.containsKey(key)) {
                count1 = categoryStats.get(key);
            } else {
                count1 = landmarkManager.selectCategoryLandmarksCount(cat1.getCategoryID(), cat1.getSubcategoryID());
                categoryStats.put(key, count1);
            }

            int count0;
            key = cat0.getCategoryID() + "," + cat0.getSubcategoryID();
            if (categoryStats.containsKey(key)) {
                count0 = categoryStats.get(key);
            } else {
                count0 = landmarkManager.selectCategoryLandmarksCount(cat0.getCategoryID(), cat0.getSubcategoryID());
                categoryStats.put(key, count0);
            }

            return (count1 - count0); //desc
        }
    }
}
