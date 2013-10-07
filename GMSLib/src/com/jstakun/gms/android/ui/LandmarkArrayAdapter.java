/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;

import java.lang.ref.WeakReference;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class LandmarkArrayAdapter extends ArrayAdapter<LandmarkParcelable> {

    private final Activity parentListActivity;
    private double maxDistance;
    private final ImageGetter imgGetter = new Html.ImageGetter() {
        @Override
        public Drawable getDrawable(String source) {
            Drawable drawable = null;
            int resId = getContext().getResources().getIdentifier(source, "drawable", getContext().getPackageName());
            if (resId > 0) {
            	drawable = getContext().getResources().getDrawable(resId);
            	drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }        
            return drawable;
        }
    };

    public LandmarkArrayAdapter(Activity parentListActivity, List<LandmarkParcelable> landmarks) {
        super(parentListActivity, R.layout.landmarkrow, landmarks);
        this.parentListActivity = parentListActivity;
        maxDistance = ConfigurationManager.getInstance().getLong(ConfigurationManager.MAX_CURRENT_DISTANCE) / 1000d;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        final View rowView;
        if (convertView == null) {
            LayoutInflater inflater = parentListActivity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.landmarkrow, null, true);
            holder = new ViewHolder();
            holder.landmarkNameText = (TextView) rowView.findViewById(R.id.landmarkNameText);
            holder.layerIconImage = (ImageView) rowView.findViewById(R.id.landmarkIcon);
            holder.landmarkDescText = (TextView) rowView.findViewById(R.id.landmarkDescText);
            holder.landmarkDescText.setMovementMethod(null);
            holder.thunbnailImage = (ImageView) rowView.findViewById(R.id.landmarkThumbnail);
            rowView.setTag(holder);
        } else {
            rowView = convertView;
            holder = (ViewHolder) rowView.getTag();
        }
        
        final LandmarkParcelable landmark = getItem(position);

        if (StringUtils.isNotEmpty(landmark.getLayer())) {
            if (landmark.getCategoryid() != -1) {
                int image = LayerManager.getDealCategoryIcon(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL, getContext().getResources().getDisplayMetrics(), landmark.getCategoryid());
                holder.layerIconImage.setImageResource(image);
                //holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, 0, 0, 0);
            } else {
                /*final Handler handler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        BitmapDrawable image = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL, getContext().getResources().getDisplayMetrics(), null);
                        holder.layerIconImage.setImageDrawable(image);
                        //holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
                    }
                };*/
                BitmapDrawable image = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL, getContext().getResources().getDisplayMetrics(), new LayerImageLoadingHandler(holder, parentListActivity, landmark.getLayer()));
                holder.layerIconImage.setImageDrawable(image);
                //holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
            }
        } else {
            String filename = landmark.getName();
            final String layerName = filename.substring(0, filename.lastIndexOf('.'));
            final String iconPath = layerName + ".png";
            /*final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    BitmapDrawable image = IconCache.getInstance().getLayerImageResource(layerName, "_small", iconPath, -1, null, LayerManager.LAYER_FILESYSTEM, getContext().getResources().getDisplayMetrics(), null);
                    holder.layerIconImage.setImageDrawable(image);
                    //holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
                }
            };*/
            BitmapDrawable image = IconCache.getInstance().getLayerImageResource(layerName, "_small", iconPath, -1, null, LayerManager.LAYER_FILESYSTEM, getContext().getResources().getDisplayMetrics(), null);
            holder.layerIconImage.setImageDrawable(image);
            //holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
        }

        holder.landmarkNameText.setText(landmark.getName());

        String desc = landmark.getDesc();
        if (landmark.getDistance() >= 0.001) {
            String distanceStatus = "red";
            if (landmark.getDistance() <= maxDistance) {
                distanceStatus = "green";
            }
            String dist = "<font color=\"" + distanceStatus + "\">" + DistanceUtils.formatDistance(landmark.getDistance()) + "</font>";
            desc = Locale.getMessage(R.string.Landmark_distance, dist)
                    + "<br/>" + desc;
        }
        if (landmark.getThunbnail() != null) {
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            /*final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    //System.out.println("Data set has changed -----------------------------------");
                    notifyDataSetChanged();
                }
            };*/
            Bitmap image = IconCache.getInstance().getThumbnailResource(landmark.getThunbnail(), getContext().getResources().getDisplayMetrics(), new DataChangeHandler(this, parentListActivity));
            if (image != null && image.getWidth() < rowView.getWidth() * 0.5) {
                holder.thunbnailImage.setImageBitmap(image);
            } else {
                holder.thunbnailImage.setImageResource(R.drawable.download48);
            }
            holder.thunbnailImage.setVisibility(View.VISIBLE);
            Display display = wm.getDefaultDisplay();
            FlowTextHelper.tryFlowText(desc, holder.thunbnailImage, holder.landmarkDescText, display, 3, imgGetter);
        } else {
            holder.landmarkDescText.setText(Html.fromHtml(desc, imgGetter, null));
            holder.thunbnailImage.setVisibility(View.GONE);
        }

        return rowView;
    }

    private static class ViewHolder {
        protected ImageView layerIconImage;
        protected TextView landmarkNameText;
        protected TextView landmarkDescText;
        protected ImageView thunbnailImage;
    }
    
    private static class DataChangeHandler extends Handler {
    	
    	private WeakReference<LandmarkArrayAdapter> landmarkArrayAdapter;
    	private WeakReference<Activity> parentActivity;

    	public DataChangeHandler(LandmarkArrayAdapter landmarkArrayAdapter, Activity parentActivity) {
    	    this.landmarkArrayAdapter = new WeakReference<LandmarkArrayAdapter>(landmarkArrayAdapter);
    	    this.parentActivity = new WeakReference<Activity>(parentActivity);  	    
    	}
    	@Override
        public void handleMessage(Message message) {           
    	    if (parentActivity != null && parentActivity.get() != null && !parentActivity.get().isFinishing()) {
    	    	landmarkArrayAdapter.get().notifyDataSetChanged();
    	    } 
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
    		if (!parentActivity.get().isFinishing()) {
    			BitmapDrawable image = LayerManager.getLayerIcon(layerName.get(), LayerManager.LAYER_ICON_SMALL, parentActivity.get().getResources().getDisplayMetrics(), null);
    			viewHolder.get().layerIconImage.setImageDrawable(image);
    			//viewHolder.get().landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
    		}
        }
    }
}
