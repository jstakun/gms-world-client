package com.jstakun.gms.android.ui;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GridCategoryArrayAdapter extends ArrayAdapter<String> {

	private final Activity parentActivity;
    private final View.OnClickListener positionClickListener;
    private final List<Category> categories;
    
	public GridCategoryArrayAdapter(Activity context, List<String> names, List<Category> categories, View.OnClickListener positionClickListener) {
		super(context, R.layout.layerrow, names);
        this.parentActivity = context;
        this.categories = categories;
        this.positionClickListener = positionClickListener;
	}
	
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            rowView = parentActivity.getLayoutInflater().inflate(R.layout.dynamiclayerrow, parent, false);
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
        
        Category c = categories.get(position);
        int count = LandmarkManager.getInstance().countLandmarks(c);
        
        holder.detailText.setText(Locale.getMessage(R.string.Landmark_deals_in_category, count));
        
        if (c != null) {
            if (c.getSubcategoryID() != -1) {
                c = CategoriesManager.getInstance().getCategory(c.getCategoryID());
            }
            holder.headerText.setCompoundDrawablesWithIntrinsicBounds(c.getIcon(), 0, 0, 0);
        }
        
        int categoryThumbnail = getCategoryImage(category);
        if (categoryThumbnail > 0) {
        	holder.detailText.setText("" + count);
	        holder.categoryThumbnail.setImageResource(categoryThumbnail);
        } else if (count > 0) {
        	holder.detailText.setText("" + count);
        	holder.categoryThumbnail.setImageResource(R.drawable.getin);
        } else {
        	holder.detailText.setText("");
        	holder.categoryThumbnail.setImageResource(R.drawable.image_missing128);
        }
        
        rowView.setOnCreateContextMenuListener(parentActivity);
        
        return rowView;
	}   
	
    private static int getCategoryImage(String category) {
        Context c = ConfigurationManager.getInstance().getContext();
        if (c != null) {
            String formattedName = StringUtils.replaceChars(category.toLowerCase(java.util.Locale.US), " &", "");
            return c.getResources().getIdentifier(formattedName + "_img", "drawable", c.getPackageName());
        }
        return 0;
    }
    
	private static class ViewHolder {
        protected TextView headerText;
        protected TextView detailText;
        protected ImageView categoryThumbnail;
    }
}
