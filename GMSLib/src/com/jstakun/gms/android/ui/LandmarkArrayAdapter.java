package com.jstakun.gms.android.ui;

import java.lang.ref.WeakReference;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.jstakun.gms.android.utils.LoggerUtils;
import com.squareup.picasso.Picasso;

/**
 *
 * @author jstakun
 */
public class LandmarkArrayAdapter extends ArrayAdapter<LandmarkParcelable> {

    private final Activity parentListActivity;
    private static double maxDistance = ConfigurationManager.getInstance().getLong(ConfigurationManager.MAX_CURRENT_DISTANCE) / 1000d;
    
    private static final ImageGetter imgGetter = new Html.ImageGetter() {
        @Override
        public Drawable getDrawable(String source) {
            Drawable drawable = null;
            Context context = ConfigurationManager.getInstance().getContext();
            int resId = context.getResources().getIdentifier(source, "drawable", context.getPackageName());
            if (resId > 0) {
            	drawable = context.getResources().getDrawable(resId);
            	drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            }        
            return drawable;
        }
    };

    public LandmarkArrayAdapter(Activity parentListActivity, List<LandmarkParcelable> landmarks) {
        super(parentListActivity, R.layout.landmarkrow, landmarks);
        this.parentListActivity = parentListActivity;
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
            holder.landmarkDescText = (TextView) rowView.findViewById(R.id.landmarkDescText);
            holder.landmarkDescText.setMovementMethod(null);
            holder.thumbnailImage = (ImageView) rowView.findViewById(R.id.landmarkThumbnail);
            rowView.setTag(holder);
        } else {
            rowView = convertView;
            holder = (ViewHolder) rowView.getTag();
        }
        
        buildView(getItem(position), holder, rowView, parentListActivity);

        return rowView;
    }

	private static void buildView(final LandmarkParcelable landmark, final ViewHolder holder, final View rowView, Activity parentListActivity) {
		if (StringUtils.isNotEmpty(landmark.getLayer())) {
            if (landmark.getCategoryid() != -1) {
                int image = LayerManager.getDealCategoryIcon(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL, parentListActivity.getResources().getDisplayMetrics(), landmark.getCategoryid());
                holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, 0, 0, 0);
            } else {
                BitmapDrawable image = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL, parentListActivity.getResources().getDisplayMetrics(), new LayerImageLoadingHandler(holder, parentListActivity, landmark.getLayer()));
                holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
            }
        } else {
            String filename = landmark.getName();
            final String layerName = filename.substring(0, filename.lastIndexOf('.'));
            final String iconPath = layerName + ".png";
            BitmapDrawable image = IconCache.getInstance().getLayerImageResource(layerName, "_small", iconPath, -1, null, LayerManager.LAYER_FILESYSTEM, parentListActivity.getResources().getDisplayMetrics(), null);
            holder.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
        }

        holder.landmarkNameText.setText(landmark.getName());

        String desc = landmark.getDesc();
        if (landmark.getDistance() >= 0.001) {
            String distanceStatus = "#FF0000";
            if (landmark.getDistance() <= maxDistance) {
                distanceStatus = "#339933";
            }
            String dist = "<font color=\"" + distanceStatus + "\">" + DistanceUtils.formatDistance(landmark.getDistance()) + "</font>";
            desc = Locale.getMessage(R.string.Landmark_distance, dist)
                    + "<br/>" + desc;
        }
        if (StringUtils.isNotEmpty(landmark.getThunbnail())) {
            /*Bitmap image = IconCache.getInstance().getThumbnailResource(landmark.getThunbnail(), landmark.getLayer(), parentListActivity.getResources().getDisplayMetrics(), new LandmarkThumbnailLoadingHandler(rowView, parentListActivity, landmark));
            int width = parentListActivity.getWindowManager().getDefaultDisplay().getWidth();            
            if (image != null && (width == 0 || image.getWidth() < width * 0.9)) {
                //System.out.println(image.getWidth() + " " + rowView.getWidth() + " " + parentListActivity.getWindowManager().getDefaultDisplay().getWidth());
            	holder.thumbnailImage.setImageBitmap(image);
            	//holder.landmarkDescText.setCompoundDrawablesWithIntrinsicBounds(null, image, null, null);
            } else {
                holder.thumbnailImage.setImageResource(R.drawable.download48);
            }
            holder.thumbnailImage.setVisibility(View.VISIBLE);*/
            holder.thumbnailImage.setVisibility(View.VISIBLE);      
            int targetWidth = (int)(128f * parentListActivity.getResources().getDisplayMetrics().density);
            int targetHeight = (int)(128f * parentListActivity.getResources().getDisplayMetrics().density);
            Picasso.with(parentListActivity).load(landmark.getThunbnail()).resize(targetWidth, targetHeight).centerInside().placeholder(R.drawable.download48).error(R.drawable.image_missing48).into(holder.thumbnailImage);
        	holder.landmarkDescText.setText(Html.fromHtml(desc, imgGetter, null));
        } else {
        	holder.thumbnailImage.setVisibility(View.GONE);
        	holder.landmarkDescText.setText(Html.fromHtml(desc, imgGetter, null));
        }
	}

    private static class ViewHolder {
        protected TextView landmarkNameText;
        protected TextView landmarkDescText;
        protected ImageView thumbnailImage;
    }
    
    private static class LandmarkThumbnailLoadingHandler extends Handler {
    	
    	private WeakReference<View> view;
    	private WeakReference<Activity> parentActivity;
    	private WeakReference<LandmarkParcelable> landmark;
    	
    	public LandmarkThumbnailLoadingHandler(View view, Activity parentActivity, LandmarkParcelable landmark) {
    	    this.view = new WeakReference<View>(view);
    	    this.parentActivity = new WeakReference<Activity>(parentActivity);  	    
    	    this.landmark = new WeakReference<LandmarkParcelable>(landmark);
    	}
    	
    	@Override
        public void handleMessage(Message message) {
    		Activity a = null;
    		if (parentActivity != null) {
    			a = parentActivity.get();
    		}
    		LandmarkParcelable l = null;
    		if (landmark != null) {
    			l = landmark.get();
    		}
    		if (a != null && !a.isFinishing() && l != null) {
    			Bitmap image = IconCache.getInstance().getThumbnailResource(l.getThunbnail(), l.getLayer(), a.getResources().getDisplayMetrics(), null);
                int width = a.getWindowManager().getDefaultDisplay().getWidth();            
                View v = view.get();
                if (image != null && v != null  && (width == 0 || image.getWidth() < width * 0.5)) {
                	ViewHolder holder = (ViewHolder) v.getTag();
                	buildView(l, holder, v, a);
                } else {
                	LoggerUtils.debug(l.getThunbnail() + " size is too big or image is empty!");
                }
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
    		Activity a = null;
    		if (parentActivity != null) {
    			a = parentActivity.get();
    		}
    		if (a != null && !a.isFinishing()) {
    			BitmapDrawable image = LayerManager.getLayerIcon(layerName.get(), LayerManager.LAYER_ICON_SMALL, a.getResources().getDisplayMetrics(), null);
    			if (image != null && viewHolder != null) {
    				ViewHolder v = viewHolder.get();
    				if (v != null) {
    					v.landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
    				}
    			}
    		}
        }
    	
    	/*@Override
        public void handleMessage(Message message) {
    		if (parentActivity != null && parentActivity.get() != null && !parentActivity.get().isFinishing()) {
    			BitmapDrawable image = LayerManager.getLayerIcon(layerName.get(), LayerManager.LAYER_ICON_SMALL, parentActivity.get().getResources().getDisplayMetrics(), null);
    			if (image != null) {
    				viewHolder.get().landmarkNameText.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
    			}
    		}
        }*/
    }
}
