package com.jstakun.gms.android.ui;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.Layer;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.routes.RoutesManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.squareup.picasso.Picasso;

/**
 *
 * @author jstakun
 */
public class LayerArrayAdapter extends ArrayAdapter<String> {

    private final Activity parentActivity;
    private final View.OnClickListener positionClickListener;

    public LayerArrayAdapter(Activity context, List<String> names, View.OnClickListener positionClickListener) {
        super(context, R.layout.layerrow, names);
        this.parentActivity = context;
        this.positionClickListener = positionClickListener;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.layerrow, null, true);
            holder = new ViewHolder();
            holder.headerText = (TextView) rowView.findViewById(R.id.layerNameHeader);
            holder.layerCheckbox = (CheckBox) rowView.findViewById(R.id.layerStatusCheckbox);
            holder.detailText = (TextView) rowView.findViewById(R.id.layerDetailsHeader);
            holder.layerThumbnail = (ImageView) rowView.findViewById(R.id.layerThumbnail);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        String[] layerStr = getItem(position).split(";");
        final String layerKey = layerStr[0];
        final String layerName = layerStr[1];

        rowView.setOnClickListener(positionClickListener);
        rowView.setId(position);

        holder.headerText.setText(layerName);

        //BitmapDrawable image = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_SMALL,
        //                getContext().getResources().getDisplayMetrics(), new LayerImageLoadingHandler(holder, parentActivity, layerKey));
        //holder.headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
  
        int targetWidth = (int)(16f * parentActivity.getResources().getDisplayMetrics().density);
        int targetHeight = (int)(16f * parentActivity.getResources().getDisplayMetrics().density);
        int iconId = LayerManager.getLayerIcon(layerKey, LayerManager.LAYER_ICON_SMALL);
		if (iconId != R.drawable.image_missing16) {
			Picasso.with(parentActivity).load(iconId).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.headerText, PicassoTextViewTarget.Position.LEFT));
		} else {
			String iconUri = LayerManager.getLayerIconUri(layerKey, LayerManager.LAYER_ICON_SMALL);
			if (iconUri != null && StringUtils.startsWith(iconUri, "http")) {
				Picasso.with(parentActivity).load(iconUri).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.headerText, PicassoTextViewTarget.Position.LEFT));
			} else {
				File fc = PersistenceManagerFactory.getFileManager().getExternalDirectory(FileManager.getIconsFolderPath(), iconUri);
				Picasso.with(parentActivity).load(fc).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.headerText, PicassoTextViewTarget.Position.LEFT));
			}
		}
        
        holder.layerCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //System.out.println("Setting " + names[position] + " checked " + buttonView.isChecked());
            	LayerManager.getInstance().setLayerEnabled(getItem(position).split(";")[0], buttonView.isChecked());
                notifyDataSetChanged();
            }
        });

        if (LandmarkManager.getInstance().getLayerType(layerKey) == LayerManager.LAYER_DYNAMIC) {
            holder.layerCheckbox.setVisibility(View.GONE);
        } else if (LayerManager.getInstance().isLayerEnabled(layerKey)) {
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
        Layer layer = LayerManager.getInstance().getLayer(layerKey);

        if (layer.getType() == LayerManager.LAYER_DYNAMIC) {
            String[] keywords = layer.getKeywords();
            if (keywords != null) {
                desc = Locale.getMessage(R.string.keywordDesc, StringUtils.join(keywords, ", "));
            }
        } else {
            desc = layer.getDesc();
        }

        if (StringUtils.isNotEmpty(desc)) {
            message += desc + ".\n";
        }
        if (layerKey.equals(Commons.ROUTES_LAYER)) {
            message += Locale.getMessage(R.string.Routes_in_layer_count, RoutesManager.getInstance().getCount());
        } else {
            message += Locale.getMessage(R.string.Landmark_in_layer_count, LandmarkManager.getInstance().getLayerSize(layerKey));
        }
        holder.detailText.setText(message);
        
        int layerThumbnail = layer.getImage();
        if (layerThumbnail > 0) {
        	holder.layerThumbnail.setVisibility(View.VISIBLE);
        	holder.layerThumbnail.setImageResource(layerThumbnail);
        } else {
        	holder.layerThumbnail.setVisibility(View.GONE);
        }

        rowView.setOnCreateContextMenuListener(parentActivity);

        return rowView;
    }

    private static class ViewHolder {
        protected CheckBox layerCheckbox;
        protected TextView headerText;
        protected TextView detailText;
        protected ImageView layerThumbnail;
    }

    /*private static class LayerImageLoadingHandler extends Handler {
    	
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
    		Activity a = null;
    		if (parentActivity != null) {
    			a = parentActivity.get();
    		}
    		if (a != null && !a.isFinishing() && viewHolder != null) {
    			BitmapDrawable image = LayerManager.getLayerIcon(layerName.get(), LayerManager.LAYER_ICON_SMALL, a.getResources().getDisplayMetrics(), null);
    			ViewHolder v = viewHolder.get();
    			if (v != null) {
    				v.headerText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
    			}
    		}
        }
    }*/
}
