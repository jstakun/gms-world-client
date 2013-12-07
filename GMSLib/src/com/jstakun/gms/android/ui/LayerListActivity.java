/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class LayerListActivity extends ListActivity {

    protected static final int ACTION_OPEN = 0;
    protected static final int ACTION_REFRESH = 1;
    protected static final int ACTION_CLEAR = 2;
    protected static final int ACTION_DELETE = 3;
    private AlertDialog deleteLayerDialog, enableAllLayersDialog;
    private List<String> names = null;
    private Intents intents;
    private LandmarkManager landmarkManager;
    private RoutesManager routesManager;
    private int currentPos = -1;
    private String layerName;
    private static final String NAME = "layerName";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.layers);

        setContentView(R.layout.mylist);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        AdsUtils.loadAd(this);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        routesManager = ConfigurationManager.getInstance().getRoutesManager();

        intents = new Intents(this, landmarkManager, null);

        //footer = new TextView(this);
        //getListView().addFooterView(footer);

        createDeleteLayerAlertDialog();
        createEnableAllLayersAlertDialog();

        registerForContextMenu(getListView());

        //Object retained = getLastNonConfigurationInstance();
        //if (retained instanceof String) {
        //    layerName = (String) retained;
        //}
        
        if (savedInstanceState != null) {
        	layerName = savedInstanceState.getString(NAME);
        } 
    }

    @Override
    public void onResume() {
        super.onResume();

        names = new ArrayList<String>();

        //if (landmarkManager == null) {
        //    landmarkManager = new LandmarkManager();
        //    ConfigurationManager.getInstance().putObject("landmarkManager", landmarkManager);
        //}

        if (landmarkManager != null) {
            List<String> layers = landmarkManager.getLayerManager().getLayers();
            Collections.sort(layers, new LayerSizeComparator());
            for (String key : layers) {
                if (!key.equals(Commons.MY_POSITION_LAYER)) {
                    String formatted = landmarkManager.getLayerManager().getLayerFormatted(key);
                    if (formatted == null) {
                        formatted = key;
                    }
                    names.add(key + ";" + formatted);
                }
            }
        }

        setListAdapter(new LayerArrayAdapter(this, names));
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

    //@Override
    //public Object onRetainNonConfigurationInstance() {
    //    return layerName;
    //}
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putString(NAME, layerName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.layers_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserTracker.getInstance().trackEvent("MenuClicks", item.getTitle().toString(), "", 0);
        int itemId = item.getItemId();
        if (itemId == R.id.addLayer) {
            startActivity(new Intent(this, AddLayerActivity.class));
            return true;
        } else if (itemId == R.id.refreshLayers) {
            Intent result = new Intent();
            result.putExtra("action", "refresh");
            setResult(RESULT_OK, result);
            finish();
            return true;
        } else if (itemId == R.id.enableLayers) {
            enableAllLayersDialog.show();
            return true;
        } else if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            currentPos = info.position;
            String[] layerStr = names.get(currentPos).split(";");
            String layerKey = layerStr[0];
            layerName = layerStr[1];
            menu.setHeaderTitle(layerName);
            menu.setHeaderIcon(R.drawable.ic_dialog_menu_generic);
            String[] menuItems = getResources().getStringArray(R.array.layersContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }

            //if option n/a for layer hide item

            int layerType = landmarkManager.getLayerType(layerKey);

            if (layerKey.equals(Commons.ROUTES_LAYER)) {
                menu.getItem(ACTION_OPEN).setVisible(false);
                menu.getItem(ACTION_REFRESH).setVisible(false);
            } else if (layerType == LayerManager.LAYER_DYNAMIC) {
                menu.getItem(ACTION_REFRESH).setVisible(false);
                menu.getItem(ACTION_CLEAR).setVisible(false);
            } else if (layerType == LayerManager.LAYER_FILESYSTEM) {
                menu.getItem(ACTION_REFRESH).setVisible(false);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();

        if (menuItemIndex == ACTION_DELETE) {
            deleteLayerDialog.setTitle(Locale.getMessage(R.string.Layer_delete_prompt, names.get(currentPos).split(";")[1]));
            deleteLayerDialog.show();
        } else {
            layerAction(menuItemIndex, currentPos);
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
                layerAction(ACTION_DELETE, currentPos);
            }
        }).setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        deleteLayerDialog = builder.create();
    }

    private void createEnableAllLayersAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).setTitle(Locale.getMessage(R.string.Layer_enableLayers_prompt)).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (landmarkManager != null) {
                	landmarkManager.getLayerManager().enableAllLayers();
                	((ArrayAdapter<?>) getListAdapter()).notifyDataSetChanged();
                	intents.showInfoToast(Locale.getMessage(R.string.Layer_all_enabled));
                }
            }
        }).setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        enableAllLayersDialog = builder.create();
    }

    protected void layerAction(int type, int position) {
        String[] layerStr = names.get(position).split(";");
        String layerKey = layerStr[0];
        layerName = layerStr[1];

        if (type == ACTION_OPEN) {
            //OPEN
            if (layerKey.equals(Commons.ROUTES_LAYER)) {
                intents.showInfoToast(Locale.getMessage(R.string.Layer_operation_unsupported));
            } else {
                UserTracker.getInstance().trackEvent("Clicks", "LayersListActivity.ShowLayerAction", layerKey, 0);
                if (landmarkManager.getLayerSize(layerKey) > 0) {
                    layerAction("show", layerKey);
                } else {
                    intents.showInfoToast(Locale.getMessage(R.string.Landmark_search_empty_result));
                }
            }
        } else if (type == ACTION_REFRESH) {
            //REFRESH
            if (layerKey.equals(Commons.ROUTES_LAYER) || landmarkManager.getLayerType(layerKey) == LayerManager.LAYER_DYNAMIC
                    || landmarkManager.getLayerType(layerKey) == LayerManager.LAYER_FILESYSTEM) {
                intents.showInfoToast(Locale.getMessage(R.string.Layer_operation_unsupported));
            } else {
                layerAction("load", layerKey);
            }
        } else if (type == ACTION_CLEAR) {
            //CLEAR
            if (layerKey.equals(Commons.ROUTES_LAYER)) {
                routesManager.clearRoutesStore();
                intents.showInfoToast(Locale.getMessage(R.string.Layer_cleared, layerName));
            } else if (landmarkManager.getLayerType(layerKey) == LayerManager.LAYER_DYNAMIC) {
                intents.showInfoToast(Locale.getMessage(R.string.Layer_operation_unsupported));
            } else {
                landmarkManager.clearLayer(layerKey);
                ((ArrayAdapter<?>) getListAdapter()).notifyDataSetChanged();
                intents.showInfoToast(Locale.getMessage(R.string.Layer_cleared, layerName));
            }
        } else if (type == ACTION_DELETE) {
            //DELETE
            landmarkManager.deleteLayer(layerKey);
            ((ArrayAdapter<String>) getListAdapter()).remove(names.remove(position));

            intents.showInfoToast(Locale.getMessage(R.string.Layer_deleted, layerName));
        }
    }

    private void layerAction(String action, String layer) {
        Intent result = new Intent();
        result.putExtra("action", action);
        result.putExtra("layer", layer);
        setResult(RESULT_OK, result);
        finish();
    }

    private class LayerSizeComparator implements Comparator<String> {

        public int compare(String lhs, String rhs) {
            int lhsCount;
            if (lhs.equals(Commons.ROUTES_LAYER)) {
                lhsCount = routesManager.getCount();
            } else {
                lhsCount = landmarkManager.getLayerSize(lhs);
            }

            int rhsCount;
            if (rhs.equals(Commons.ROUTES_LAYER)) {
                rhsCount = routesManager.getCount();
            } else {
                rhsCount = landmarkManager.getLayerSize(rhs);
            }

            if (lhsCount > rhsCount) {
                return -1;
            } else if (lhsCount < rhsCount) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
