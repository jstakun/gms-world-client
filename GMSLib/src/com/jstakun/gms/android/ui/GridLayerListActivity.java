package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.GridView;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.Layer;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class GridLayerListActivity extends Activity {
	
	private static final int ACTION_OPEN = 0;
    private static final int ACTION_REFRESH = 1;
    private static final int ACTION_CLEAR = 2;
    private static final int ACTION_ENABLE = 3;
    private static final int ACTION_DISABLE = 4;
    private static final int ACTION_DELETE = 5;
    private List<String> names = null;
    private LandmarkManager landmarkManager;
    private IntentsHelper intents;
    private GridView gridView;
    private int mode = ConfigurationManager.ALL_LAYERS_MODE, currentPos = -1;
    private AlertDialog deleteLayerDialog, enableAllLayersDialog, disableAllLayersDialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.layers);

        setContentView(R.layout.gms_grid_list);
        
        AdsUtils.loadAd(this);
        
        UserTracker.getInstance().trackActivity(getClass().getName());
        
        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        
        intents = new IntentsHelper(this, landmarkManager, null);
        
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("mode")) {
        	mode = intent.getIntExtra("mode", ConfigurationManager.ALL_LAYERS_MODE);
        } else {
        	mode = ConfigurationManager.ALL_LAYERS_MODE;
        }
        
        gridView = (GridView) findViewById(R.id.layers_grid_view);
        
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
        
        registerForContextMenu(gridView);
        
        createDeleteLayerAlertDialog();
        createEnableAllLayersAlertDialog();
        createDisableAllLayersAlertDialog();
	}
	
	public void onResume() {
        super.onResume();

        names = new ArrayList<String>();

        if (landmarkManager != null) {
            List<String> layers = null; 
            
            if (mode == ConfigurationManager.DYNAMIC_LAYERS_MODE) {
            	layers = LayerManager.getInstance().getDynamicLayers();
            } else {
            	layers = LayerManager.getInstance().getLayers();
            }
            
            Collections.sort(layers, new LayerSizeComparator());
            for (String key : layers) {
                if (!key.equals(Commons.MY_POSITION_LAYER)) {
                	Layer layer = LayerManager.getInstance().getLayer(key);
                	if (layer.getType() == LayerManager.LAYER_DYNAMIC || layer.getCount() > 0 || layer.getImage() > 0 || landmarkManager.getLayerSize(key) > 0) {
                		String formatted = layer.getFormatted();
                		if (formatted == null) {
                			formatted = key;
                		}
                		names.add(key + ";" + formatted);
                	}
                }
            }
        }

        gridView.setAdapter(new GridLayerArrayAdapter(this, names, new PositionClickListener()));
    }
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	MenuItem enableLayers = menu.findItem(R.id.enableLayers);
    	MenuItem refreshLayers = menu.findItem(R.id.refreshLayers);
    	if (mode == ConfigurationManager.ALL_LAYERS_MODE) {
    		enableLayers.setVisible(true);
    		refreshLayers.setVisible(true);
    		if (LayerManager.getInstance().isAllLayersEnabled()) {
    			enableLayers.setTitle(R.string.disableLayers);
    		} else {
    			enableLayers.setTitle(R.string.enableLayers);
    		}
    	} else {
    		enableLayers.setVisible(false);
    		refreshLayers.setVisible(false);
    	}
    	return true;
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
        	if (LayerManager.getInstance().isAllLayersEnabled()) {
        		disableAllLayersDialog.show();
        	} else {
        		enableAllLayersDialog.show();
        	}
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
        if (v.getId() == gridView.getId()) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            currentPos = info.position;
            String[] layerStr = names.get(currentPos).split(";");
            String layerKey = layerStr[0];
            String layerName = layerStr[1];
            menu.setHeaderTitle(layerName);
            menu.setHeaderIcon(R.drawable.ic_dialog_menu_generic);
            String[] menuItems = getResources().getStringArray(R.array.layersContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
          
            if (LayerManager.getInstance().isLayerEnabled(layerKey)) {
            	menu.getItem(ACTION_ENABLE).setVisible(false);
            } else {
            	menu.getItem(ACTION_DISABLE).setVisible(false);
            }
            
            int layerType = landmarkManager.getLayerType(layerKey);

            if (layerKey.equals(Commons.ROUTES_LAYER)) {
                menu.getItem(ACTION_OPEN).setVisible(false);
                menu.getItem(ACTION_REFRESH).setVisible(false);
            } else if (layerType == LayerManager.LAYER_DYNAMIC) {
                menu.getItem(ACTION_REFRESH).setVisible(false);
                menu.getItem(ACTION_CLEAR).setVisible(false);
                menu.getItem(ACTION_ENABLE).setVisible(false);
                menu.getItem(ACTION_DISABLE).setVisible(false);
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

	
	private void layerAction(String action, String layer) {
        Intent result = new Intent();
        result.putExtra("action", action);
        result.putExtra("layer", layer);
        setResult(RESULT_OK, result);
        finish();
    }
	
	@SuppressWarnings("unchecked")
	protected void layerAction(int type, int position) {
        String[] layerStr = names.get(position).split(";");
        String layerKey = layerStr[0];
        String layerName = layerStr[1];

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
            	RoutesManager.getInstance().clearRoutesStore();
                ((ArrayAdapter<?>) gridView.getAdapter()).notifyDataSetChanged();
                intents.showInfoToast(Locale.getMessage(R.string.Layer_cleared, layerName));
            } else if (landmarkManager.getLayerType(layerKey) == LayerManager.LAYER_DYNAMIC) {
                intents.showInfoToast(Locale.getMessage(R.string.Layer_operation_unsupported));
            } else {
                landmarkManager.clearLayer(layerKey);
                ((ArrayAdapter<?>) gridView.getAdapter()).notifyDataSetChanged();
                intents.showInfoToast(Locale.getMessage(R.string.Layer_cleared, layerName));
            }
        } else if (type == ACTION_DELETE) {
            //DELETE
            landmarkManager.deleteLayer(layerKey);
            ((ArrayAdapter<String>) gridView.getAdapter()).remove(names.remove(position));

            if (layerKey.equals(Commons.ROUTES_LAYER)) {
            	RoutesManager.getInstance().clearRoutesStore();
                ConfigurationManager.getInstance().setOff(ConfigurationManager.FOLLOW_MY_POSITION);
            }    
            
            intents.showInfoToast(Locale.getMessage(R.string.Layer_deleted, layerName));
        } else if (type == ACTION_ENABLE) {
        	LayerManager.getInstance().setLayerEnabled(layerKey, true);
        	((ArrayAdapter<?>) gridView.getAdapter()).notifyDataSetChanged();
        	intents.showInfoToast(Locale.getMessage(R.string.Layer_enabled));
        } else if (type == ACTION_DISABLE) {
        	LayerManager.getInstance().setLayerEnabled(layerKey, false);
        	((ArrayAdapter<?>) gridView.getAdapter()).notifyDataSetChanged();
        	intents.showInfoToast(Locale.getMessage(R.string.Layer_disabled));      	
        }
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
                setIcon(android.R.drawable.ic_dialog_alert).
                setTitle(Locale.getMessage(R.string.Layer_enableLayers_prompt)).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                LayerManager.getInstance().enableAllLayers();
                ((ArrayAdapter<?>) gridView.getAdapter()).notifyDataSetChanged();
                intents.showInfoToast(Locale.getMessage(R.string.Layer_all_enabled));
            }
        }).setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        enableAllLayersDialog = builder.create();
    }
    
    private void createDisableAllLayersAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setTitle(Locale.getMessage(R.string.Layer_disableLayers_prompt)).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (landmarkManager != null) {
                	LayerManager.getInstance().disableAllLayers();
                	((ArrayAdapter<?>) gridView.getAdapter()).notifyDataSetChanged();
                	intents.showInfoToast(Locale.getMessage(R.string.Layer_all_disabled));
                }
            }
        }).setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        disableAllLayersDialog = builder.create();
    }
	
	private class LayerSizeComparator implements Comparator<String> {

        public int compare(String lhs, String rhs) {
            int lhsCount = 0;
            if (lhs.equals(Commons.ROUTES_LAYER)) {
            	lhsCount = RoutesManager.getInstance().getCount();
            } else {
                lhsCount = landmarkManager.getLayerSize(lhs);
            }

            int rhsCount = 0;
            if (rhs.equals(Commons.ROUTES_LAYER)) {
            	rhsCount = RoutesManager.getInstance().getCount();
            } else {
                rhsCount = landmarkManager.getLayerSize(rhs);
            }

            if (lhsCount > rhsCount) {
                return -1;
            } else if (lhsCount < rhsCount) {
                return 1;
            } else {
            	int lhsType = landmarkManager.getLayerType(lhs);
            	int rhsType = landmarkManager.getLayerType(rhs);
            	if (lhsType > rhsType) {
                    return 1;
                } else if (lhsType < rhsType) {
                    return -1;
                } else {
                	return 0;
                }
            }
        }
    }
	
	private class PositionClickListener implements View.OnClickListener {
        public void onClick(View v) {
        	int position = v.getId();
        	layerAction(ACTION_OPEN, position);
        }
    }
}
