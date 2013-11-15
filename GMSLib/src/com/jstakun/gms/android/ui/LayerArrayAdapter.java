/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.Layer;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

import java.lang.ref.WeakReference;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class LayerArrayAdapter extends ArrayAdapter<String> {

    private final LayerListActivity parentActivity;
    private final LandmarkManager landmarkManager;
    private final RoutesManager routesManager;

    public LayerArrayAdapter(LayerListActivity context, List<String> names) {
        super(context, R.layout.layerrow, names);
        this.parentActivity = context;
        this.landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        this.routesManager = ConfigurationManager.getInstance().getRoutesManager();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.layerrow, null, true);
            holder = new ViewHolder();
            holder.headerText = (TextView) rowView.findViewById(R.id.layerStatusHeader);
            holder.layerCheckbox = (CheckBox) rowView.findViewById(R.id.layerStatusCheckbox);
            holder.detailText = (TextView) rowView.findViewById(R.id.layerDetailsHeader);

            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        String[] layerStr = getItem(position).split(";");
        final String layerKey = layerStr[0];
        final String layerName = layerStr[1];

        rowView.setOnClickListener(new PositionClickListener(position));

        holder.headerText.setText(layerName);

        /*final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                BitmapDrawable image = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_SMALL,
                        getContext().getResources().getDisplayMetrics(), null);
                holder.headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
            }
        };*/
        
        BitmapDrawable image = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_SMALL,
                        getContext().getResources().getDisplayMetrics(), new LayerImageLoadingHandler(holder, parentActivity, layerKey));
        holder.headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
  
        holder.layerCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //System.out.println("Setting " + names[position] + " checked " + buttonView.isChecked());
                landmarkManager.getLayerManager().setLayerEnabled(getItem(position).split(";")[0], buttonView.isChecked());
                notifyDataSetChanged();
            }
        });

        if (landmarkManager.getLayerType(layerKey) == LayerManager.LAYER_DYNAMIC) {
            holder.layerCheckbox.setVisibility(View.GONE);
        } else if (landmarkManager.getLayerManager().isLayerEnabled(layerKey)) {
            holder.layerCheckbox.setVisibility(View.VISIBLE);
            holder.layerCheckbox.setChecked(true);
            holder.layerCheckbox.setText(Locale.getMessage(R.string.Layer_enabled));
        } else {
            holder.layerCheckbox.setVisibility(View.VISIBLE);
            holder.layerCheckbox.setChecked(false);
            holder.layerCheckbox.setText(Locale.getMessage(R.string.Layer_disabled));
        }

        String message = "";
        String desc = null;
        Layer layer = landmarkManager.getLayerManager().getLayer(layerKey);

        if (layer.getType() == LayerManager.LAYER_DYNAMIC) {
            String[] keywords = layer.getKeywords();
            if (keywords != null) {
                desc = "Keywords: " + StringUtils.join(keywords, ", ");
            }
        } else {
            desc = layer.getDesc();
        }

        if (StringUtils.isNotEmpty(desc)) {
            message += desc + ".\n";
        }
        if (layerKey.equals(Commons.ROUTES_LAYER)) {
            message += Locale.getMessage(R.string.Routes_in_layer_count, routesManager.getCount());
        } else {
            message += Locale.getMessage(R.string.Landmark_in_layer_count, landmarkManager.getLayerSize(layerKey));
        }
        holder.detailText.setText(message);

        rowView.setOnCreateContextMenuListener(parentActivity);

        return rowView;
    }

    private static class ViewHolder {
        protected CheckBox layerCheckbox;
        protected TextView headerText;
        protected TextView detailText;
    }

    private class PositionClickListener implements View.OnClickListener {

        private int position;

        public PositionClickListener(int pos) {
            this.position = pos;
        }

        public void onClick(View v) {
            parentActivity.layerAction(LayerListActivity.ACTION_OPEN, position);
        }
    }
    
    private static class LayerImageLoadingHandler extends Handler {
    	
    	private WeakReference<ViewHolder> viewHolder;
    	private WeakReference<Activity> parentActivity;
    	private WeakReference<String> layerName;
    	
    	public LayerImageLoadingHandler(ViewHolder viewHolder, Activity parentActivity, String layerName) {
    	    this.viewHolder = new WeakReference<ViewHolder>(viewHolder);
    	    this.parentActivity = new WeakReference<Activity>(parentActivity);  	    
    	    this.layerName = new WeakReference<String>(layerName);
    	}
    	
    	@Override
        public void handleMessage(Message message) {
    		if (parentActivity != null && parentActivity.get() != null && !parentActivity.get().isFinishing()) {
    			BitmapDrawable image = LayerManager.getLayerIcon(layerName.get(), LayerManager.LAYER_ICON_SMALL, parentActivity.get().getResources().getDisplayMetrics(), null);
                viewHolder.get().headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
    		}
        }
    }
}
