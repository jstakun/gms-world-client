package com.jstakun.gms.android.ui;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.IconCache;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DistanceUtils;
import com.jstakun.gms.android.utils.Locale;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
            rowView = parentListActivity.getLayoutInflater().inflate(R.layout.landmarkrow, parent, false);
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
			int targetWidth = (int)(16f * parentListActivity.getResources().getDisplayMetrics().density);
            int targetHeight = (int)(16f * parentListActivity.getResources().getDisplayMetrics().density);
            if (landmark.getCategoryid() != -1) {
                int iconId = LayerManager.getDealCategoryIcon(landmark.getCategoryid(), LayerManager.LAYER_ICON_SMALL);
				Picasso.with(parentListActivity).load(iconId).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.landmarkNameText, PicassoTextViewTarget.Position.LEFT));	           
            } else {
                int iconId = LayerManager.getLayerIcon(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL);
    			if (iconId != R.drawable.image_missing16) {
    				Picasso.with(parentListActivity).load(iconId).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.landmarkNameText, PicassoTextViewTarget.Position.LEFT));
    			} else {
    				String iconUri = LayerManager.getLayerIconUri(landmark.getLayer(), LayerManager.LAYER_ICON_SMALL);
    				if (iconUri != null && StringUtils.startsWith(iconUri, "http")) {
    					Picasso.with(parentListActivity).load(iconUri).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.landmarkNameText, PicassoTextViewTarget.Position.LEFT));
    				} else {
    					File fc = FileManager.getInstance().getExternalDirectory(FileManager.getInstance().getIconsFolderPath(), iconUri);
    					Picasso.with(parentListActivity).load(fc).resize(targetWidth, targetHeight).error(R.drawable.image_missing16).centerInside().into(new PicassoTextViewTarget(holder.landmarkNameText, PicassoTextViewTarget.Position.LEFT));
    				}
    			}
            }
        } else {
        	//FilesActivity
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
            holder.thumbnailImage.setVisibility(View.VISIBLE);      
            int targetWidth = (int)(128f * parentListActivity.getResources().getDisplayMetrics().density);
            int targetHeight = (int)(128f * parentListActivity.getResources().getDisplayMetrics().density);
            int missingIconPlaceholder = LayerManager.getLayerImage(landmark.getLayer());
            if (missingIconPlaceholder <= 0) {
            	missingIconPlaceholder = R.drawable.image_missing48;
            }
            Picasso.with(parentListActivity).load(landmark.getThunbnail()).resize(targetWidth, targetHeight).centerInside().placeholder(R.drawable.download48).error(missingIconPlaceholder).into(holder.thumbnailImage);
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
}
