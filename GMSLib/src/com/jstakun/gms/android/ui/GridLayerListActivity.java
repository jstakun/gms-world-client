package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
    protected static final int ALL_LAYERS_MODE = 0;
    protected static final int DYNAMIC_LAYERS_MODE = 1;
    private List<String> names = null;
    private LandmarkManager landmarkManager;
    private RoutesManager routesManager;
    private IntentsHelper intents;
    private GridView gridView;
    private int mode = 0;
	
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
        	mode = intent.getIntExtra("mode", ALL_LAYERS_MODE);
        } else {
        	mode = ALL_LAYERS_MODE;
        }
        
        gridView = (GridView) findViewById(R.id.layers_grid_view);
        
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
	}
	
	public void onResume() {
        super.onResume();

        names = new ArrayList<String>();

        if (landmarkManager != null) {
            List<String> layers = null; 
            
            if (mode == DYNAMIC_LAYERS_MODE) {
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
