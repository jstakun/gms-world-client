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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class GridLayerListActivity extends Activity {
	
	protected static final int ACTION_OPEN = 0;
    protected static final int ACTION_REFRESH = 1;
    protected static final int ACTION_CLEAR = 2;
    protected static final int ACTION_DELETE = 3;
    private List<String> names = null;
    private LandmarkManager landmarkManager;
    private RoutesManager routesManager;
    private IntentsHelper intents;
    private GridView gridView;
    private int mode = ConfigurationManager.ALL_LAYERS_MODE;
    private AlertDialog enableAllLayersDialog, disableAllLayersDialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.layers);

        setContentView(R.layout.gms_grid_list);
        
        AdsUtils.loadAd(this);
        
        UserTracker.getInstance().trackActivity(getClass().getName());
        
        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        routesManager = ConfigurationManager.getInstance().getRoutesManager();
        
        intents = new IntentsHelper(this, landmarkManager, null);
        
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("mode")) {
        	mode = intent.getIntExtra("mode", ConfigurationManager.ALL_LAYERS_MODE);
        } else {
        	mode = ConfigurationManager.ALL_LAYERS_MODE;
        }
        
        gridView = (GridView) findViewById(R.id.layers_grid_view);
        
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
        
        createEnableAllLayersAlertDialog();
        createDisableAllLayersAlertDialog();
	}
	
	public void onResume() {
        super.onResume();

        names = new ArrayList<String>();

        if (landmarkManager != null) {
            List<String> layers = null; 
            
            if (mode == ConfigurationManager.DYNAMIC_LAYERS_MODE) {
            	layers = landmarkManager.getLayerManager().getDynamicLayers();
            } else {
            	layers = landmarkManager.getLayerManager().getLayers();
            }
            
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

        gridView.setAdapter(new DynamicLayerArrayAdapter(this, names, new PositionClickListener()));
    }
	
	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	MenuItem enableLayers = menu.findItem(R.id.enableLayers);
    	MenuItem refreshLayers = menu.findItem(R.id.refreshLayers);
    	if (mode == ConfigurationManager.ALL_LAYERS_MODE) {
    		enableLayers.setVisible(true);
    		refreshLayers.setVisible(true);
    		if (landmarkManager != null && landmarkManager.getLayerManager().isAllLayersEnabled()) {
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
        	if (landmarkManager != null && landmarkManager.getLayerManager().isAllLayersEnabled()) {
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
	
	private void layerAction(String action, String layer) {
        Intent result = new Intent();
        result.putExtra("action", action);
        result.putExtra("layer", layer);
        setResult(RESULT_OK, result);
        finish();
    }
	
	protected void layerAction(int type, int position) {
        String[] layerStr = names.get(position).split(";");
        String layerKey = layerStr[0];
        //layerName = layerStr[1];

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
        }     
	}
	
	private void createEnableAllLayersAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setTitle(Locale.getMessage(R.string.Layer_enableLayers_prompt)).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (landmarkManager != null) {
                	landmarkManager.getLayerManager().enableAllLayers();
                	//((ArrayAdapter<?>) getListAdapter()).notifyDataSetChanged();
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
    
    private void createDisableAllLayersAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setTitle(Locale.getMessage(R.string.Layer_disableLayers_prompt)).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (landmarkManager != null) {
                	landmarkManager.getLayerManager().disableAllLayers();
                	//((ArrayAdapter<?>) getListAdapter()).notifyDataSetChanged();
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
	
	private class PositionClickListener implements View.OnClickListener {
        public void onClick(View v) {
        	int position = v.getId();
        	layerAction(ACTION_OPEN, position);
        }
    }
}
