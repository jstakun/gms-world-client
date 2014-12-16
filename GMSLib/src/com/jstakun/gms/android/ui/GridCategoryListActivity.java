package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class GridCategoryListActivity extends Activity {

	private int parent = -1, radius = 3, currentPos = -1, lat, lng;
    private LandmarkManager landmarkManager = null;
    private CategoriesManager cm = null;
    private IntentsHelper intents;
    private GridView gridView;
    private List<String> names = null;
    private List<Category> categories = null;
    private AlertDialog deleteLayerDialog;
    
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	
        setContentView(R.layout.gms_grid_list);
        
        setTitle(Locale.getMessage(R.string.searchDeals));
        
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
        
        AdsUtils.loadAd(this);
        
        UserTracker.getInstance().trackActivity(getClass().getName());
        
        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        
        intents = new IntentsHelper(this, landmarkManager, null);
        
        gridView = (GridView) findViewById(R.id.layers_grid_view);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            parent = extras.getInt("parent");
            radius = extras.getInt("radius");
            lat = extras.getInt("lat");
            lng = extras.getInt("lng");
        }

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);
        
        createDeleteLayerAlertDialog();

        registerForContextMenu(gridView);
	}
	
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
            Collections.sort(categories, new CategoryCountComparator());
        }

        names = new ArrayList<String>();

        for (int i = 0; i < size; i++) {
            if (parent != -1) {
                names.add(categories.get(i).getSubcategory());
            } else {
                names.add(StringUtils.capitalize(categories.get(i).getCategory()));
            }
        }

        if (parent != -1 && cm != null) {
            Category parentCat = cm.getCategory(parent);
            setTitle(Locale.getMessage(R.string.dealsString, parentCat.getCategory()));
        }

        gridView.setAdapter(new GridCategoryArrayAdapter(this, names, new PositionClickListener()));	
	}   
	
	@Override
    public boolean onSearchRequested() {
        intents.startSearchActivity(-1, -1, lat, lng, radius, true);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.deal_menu, menu);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	MenuItem addLayers = menu.findItem(R.id.addLayer);
    	if (ConfigurationManager.getInstance().getInt(ConfigurationManager.APP_ID) != ConfigurationManager.DA) {
    		addLayers.setVisible(false);
    	}
    	
        return true;
    }	

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemid = item.getItemId();

        if (itemid == R.id.mapMode) {
            cancelActivity(true);
        } else if (itemid == R.id.search) {
            onSearchRequested();
        } else if (itemid == android.R.id.home) {
            cancelActivity(false);
        } else if (itemid == R.id.addLayer) {
            startActivity(new Intent(this, AddLayerActivity.class));
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        intents.processActivityResult(requestCode, resultCode, intent, new double[]{lat, lng}, null, null, -1, null, null);
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
            menu.setHeaderTitle(categories.get(info.position).getCategory());
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
            deleteLayerDialog.setTitle(Locale.getMessage(R.string.Layer_delete_prompt, categories.get(currentPos).getCategory()));
            deleteLayerDialog.show();
        }

        return true;
    }
    
    private void onClickAction(int position, String action) {
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
        Category c = categories.get(currentPos);
        if (!c.isCustom()) {
            intents.showInfoToast(Locale.getMessage(R.string.Layer_operation_unsupported));
        } else {
            landmarkManager.deleteLayer(c.getCategory());
            ((ArrayAdapter<String>) gridView.getAdapter()).remove(names.remove(currentPos));
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
    
    private boolean hasSubcategory(int position) {
        if (parent == -1) {
            int categoryId = categories.get(position).getCategoryID();
            return cm.hasSubcategory(categoryId);
        }
        return false;
    }
	
	private class PositionClickListener implements View.OnClickListener {

        public void onClick(View v) {
            //show landmarks in category
            if (hasSubcategory(v.getId())) {
                onClickAction(v.getId(), "drill");
            } else {
                onClickAction(v.getId(), "show");
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
