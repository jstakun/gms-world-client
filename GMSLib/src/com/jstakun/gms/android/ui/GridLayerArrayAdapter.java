package com.jstakun.gms.android.ui;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.Layer;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.ui.lib.R;

public class GridLayerArrayAdapter extends ArrayAdapter<String> {

	    private final Activity parentActivity;
	    private final LandmarkManager landmarkManager;
	    private final View.OnClickListener positionClickListener;

	    public GridLayerArrayAdapter(Activity context, List<String> names, View.OnClickListener positionClickListener) {
	        super(context, R.layout.layerrow, names);
	        this.parentActivity = context;
	        this.landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
	        this.positionClickListener = positionClickListener;
	    }

	    @Override
	    public View getView(final int position, View convertView, ViewGroup parent) {

	        final ViewHolder holder;
	        View rowView = convertView;
	        if (rowView == null) {
	            LayoutInflater inflater = parentActivity.getLayoutInflater();
	            rowView = inflater.inflate(R.layout.dynamiclayerrow, null, true);
	            holder = new ViewHolder();
	            holder.headerText = (TextView) rowView.findViewById(R.id.layerNameHeader);
	            holder.detailText = (TextView) rowView.findViewById(R.id.layerDetailsHeader);
	            holder.layerThumbnail = (ImageView) rowView.findViewById(R.id.layerThumbnail);
	            rowView.setTag(holder);
	        } else {
	            holder = (ViewHolder) rowView.getTag();
	        }

	        rowView.setOnClickListener(positionClickListener);
	        rowView.setId(position);
	        
	        String[] layerStr = getItem(position).split(";");
	        final String layerKey = layerStr[0];
	        final String layerName = layerStr[1];

	        Layer layer = landmarkManager.getLayerManager().getLayer(layerKey);

	        holder.headerText.setText(layerName);
	        
	        if (landmarkManager.getLayerType(layerKey) != LayerManager.LAYER_DYNAMIC && !layer.isEnabled()) {
	        	holder.headerText.setTextColor(Color.RED);
	        } else {
	        	holder.headerText.setTextColor(Color.BLACK);
	        }

	        BitmapDrawable image = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_SMALL,
	                        getContext().getResources().getDisplayMetrics(), new LayerImageLoadingHandler(holder, parentActivity, layerKey));
	        holder.headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
	  
	        int count = landmarkManager.getLayerSize(layerKey);
	        
	        int layerThumbnail = layer.getImage();
	        if (layerThumbnail > 0) {
	        	holder.detailText.setText("" + count);
		        holder.layerThumbnail.setImageResource(layerThumbnail);
	        } else if (count > 0) {
	        	holder.detailText.setText("" + count);
	        	holder.layerThumbnail.setImageResource(R.drawable.getin);
	        } else {
	        	holder.detailText.setText("");
	        	holder.layerThumbnail.setImageResource(R.drawable.image_missing128);
	        }

	        rowView.setOnCreateContextMenuListener(parentActivity);

	        return rowView;
	    }

	    private static class ViewHolder {
	        protected TextView headerText;
	        protected TextView detailText;
	        protected ImageView layerThumbnail;
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
	    		if (parentActivity != null && parentActivity.get() != null && !parentActivity.get().isFinishing() && viewHolder != null && viewHolder.get() != null) {
	    			BitmapDrawable image = LayerManager.getLayerIcon(layerName.get(), LayerManager.LAYER_ICON_SMALL, parentActivity.get().getResources().getDisplayMetrics(), null);
	                viewHolder.get().headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
	    		}
	        }
	    }
	}

