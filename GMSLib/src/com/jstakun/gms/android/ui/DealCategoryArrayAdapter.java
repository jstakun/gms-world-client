package com.jstakun.gms.android.ui;

import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.jstakun.gms.android.deals.Category;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class DealCategoryArrayAdapter extends ArrayAdapter<String> {

    private final DealCategoryListActivity context;
    
    public DealCategoryArrayAdapter(DealCategoryListActivity context, List<String> names) {
        super(context, R.layout.categoryrow, names);
        this.context = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        View rowView = convertView;

        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.categoryrow, null, true);
            holder = new ViewHolder();
            holder.headerText = (TextView) rowView.findViewById(R.id.categoryStatusHeader);
            holder.detailText = (TextView) rowView.findViewById(R.id.categoryDetailsHeader);
            holder.listButton = rowView.findViewById(R.id.categoryListButton);
            holder.separator = rowView.findViewById(R.id.categorySeparator);
            //holder.categoryImage = (ImageView) rowView.findViewById(R.id.categoryIcon);

            rowView.setTag(holder);
        } else {
            holder = (ViewHolder) rowView.getTag();
        }

        rowView.setOnClickListener(new PositionClickListener(position, PositionClickListener.LEFT));

        try {
            String category = getItem(position);

            holder.headerText.setText(category);

            if (context.hasSubcategory(position)) {
                holder.listButton.setVisibility(View.VISIBLE);
                holder.separator.setVisibility(View.VISIBLE);
                holder.listButton.setOnClickListener(new PositionClickListener(position, PositionClickListener.RIGHT));
            } else {
                holder.listButton.setVisibility(View.GONE);
                holder.separator.setVisibility(View.GONE);
            }
            holder.detailText.setText(Locale.getMessage(R.string.Landmark_deals_in_category, context.countLandmarks(position)));

            Category c = context.getCategory(position);
            if (c != null) {
                if (c.getSubcategoryID() != -1) {
                    c = context.getParentCategory(c.getCategoryID());
                }
                //holder.categoryImage.setImageResource(c.getIcon());
                holder.headerText.setCompoundDrawablesWithIntrinsicBounds(c.getIcon(), 0, 0, 0);
            }
        } catch (Exception e) {
            LoggerUtils.error("DealCategoryArrayAdapter.getView error", e);
        }

        rowView.setOnCreateContextMenuListener(context);

        return rowView;
    }

    private static class ViewHolder {

        protected View listButton;
        protected TextView headerText;
        protected TextView detailText;
        //protected ImageView categoryImage;
        protected View separator;
    }

    private class PositionClickListener implements View.OnClickListener {

        private int position;
        private int type;
        private static final int LEFT = 0;
        private static final int RIGHT = 1;

        public PositionClickListener(int pos, int type) {
            this.position = pos;
            this.type = type;
        }

        public void onClick(View v) {
            //LEFT
            //if has subcats show subcats else RIGHT

            //RIGHT
            //show landmarks in category
            if (type == RIGHT) {
                context.onClickAction(position, "show");
            } else if (type == LEFT) {
                if (context.hasSubcategory(position)) {
                    context.onClickAction(position, "drill");
                } else {
                    context.onClickAction(position, "show");
                }
            }
        }
    }
}
