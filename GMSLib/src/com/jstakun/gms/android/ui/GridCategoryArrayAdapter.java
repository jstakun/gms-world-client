package com.jstakun.gms.android.ui;

import java.util.List;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GridCategoryArrayAdapter extends ArrayAdapter<String> {

	private final Activity parentActivity;
    private final LandmarkManager landmarkManager;
    private final View.OnClickListener positionClickListener;
	
	public GridCategoryArrayAdapter(Activity context, List<String> names, View.OnClickListener positionClickListener) {
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
            holder.categoryThumbnail = (ImageView) rowView.findViewById(R.id.layerThumbnail);
            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }
        
        rowView.setOnClickListener(positionClickListener);
        rowView.setId(position);
        
        String category = getItem(position);

        holder.headerText.setText(category);
        
        int count = 0; //TODO context.countLandmarks(position);
        
        holder.detailText.setText(Locale.getMessage(R.string.Landmark_deals_in_category, count));
        
        //TODO fix
        /*Category c = context.getCategory(position);
        if (c != null) {
            if (c.getSubcategoryID() != -1) {
                c = context.getParentCategory(c.getCategoryID());
            }
            holder.headerText.setCompoundDrawablesWithIntrinsicBounds(c.getIcon(), 0, 0, 0);
        }*/
        
        if (count > 0) {
        	holder.detailText.setText("" + count);
        	holder.categoryThumbnail.setImageResource(R.drawable.getin);
        } else {
        	holder.detailText.setText("");
        	holder.categoryThumbnail.setImageResource(R.drawable.image_missing128);
        }
        
        rowView.setOnCreateContextMenuListener(parentActivity);
        
        return rowView;
	}   
	
	private static class ViewHolder {
        protected TextView headerText;
        protected TextView detailText;
        protected ImageView categoryThumbnail;
    }
}
