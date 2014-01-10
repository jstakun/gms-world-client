/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.OsUtil;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jstakun
 */
public abstract class AbstractLandmarkList extends ListActivity implements View.OnClickListener {

    private enum ORDER {ASC, DESC};
    public static final int ORDER_BY_NAME = 0;
    private static final int ORDER_BY_DIST = 1;
    private static final int ORDER_BY_DATE = 2;
    public static final int ORDER_BY_DIST_ASC = 3;
    public static final int ORDER_BY_DATE_DESC = 4;
    public static final int ORDER_BY_CAT_STATS = 5;
    private static final int ORDER_BY_RATING = 6;
    public static final int ORDER_BY_REV_DESC = 7;
    protected static final int ID_DIALOG_PROGRESS = 0;
    private View sortButton, distanceButton, dateButton, selectedView;
    protected View list, loading, searchButton, ratingButton;
    private TextView sortingText;
    private ImageView sortingImage;
    private ORDER order_name = ORDER.ASC;
    private ORDER order_dist = ORDER.ASC;
    private ORDER order_date = ORDER.ASC;
    private ORDER order_rating = ORDER.DESC;
    protected int order_type;
    private CategoriesManager cm;
    private Comparator<LandmarkParcelable> distanceComparator = new DistanceComparator();
    private Comparator<LandmarkParcelable> nameComparator = new NameComparator();
    private Comparator<LandmarkParcelable> dateComparator = new CreationDateComparator();
    private Comparator<LandmarkParcelable> ratingComparator = new RatingComparator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.landmarklist);
            
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);
        
        loading = findViewById(R.id.loadingLandmarkList);
        list = findViewById(R.id.landmarkListLayout);

        loading.setVisibility(View.GONE);

        AdsUtils.loadAd(this);

        sortButton = findViewById(R.id.sortButton);
        distanceButton = findViewById(R.id.sortDistanceButton);
        dateButton = findViewById(R.id.dateButton);
        searchButton = findViewById(R.id.searchButton);
        ratingButton = findViewById(R.id.sortRatingButton);

        sortButton.setOnClickListener(this);
        distanceButton.setOnClickListener(this);
        dateButton.setOnClickListener(this);
        searchButton.setOnClickListener(this);
        ratingButton.setOnClickListener(this);

        sortingText = (TextView) findViewById(R.id.sortingText);
        sortingImage = (ImageView) findViewById(R.id.sortingImage);

        cm = (CategoriesManager) ConfigurationManager.getInstance().getObject(ConfigurationManager.DEAL_CATEGORIES, CategoriesManager.class);

        order_type = getIntent().getIntExtra("sort", ORDER_BY_DIST_ASC);
        
        if (OsUtil.isHoneycomb2OrHigher()) {
            findViewById(R.id.topPanel).setVisibility(View.GONE);
            findViewById(R.id.topPanelSeparator).setVisibility(View.GONE);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_PROGRESS) {
            //System.out.println("onCreateDialog------- ID_DIALOG_PROGRESS -------------------------");
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(Locale.getMessage(R.string.Please_Wait));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            progressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    cancelProgressDialog();
                }
            });
            return progressDialog;
        } else {
            //System.out.println("onCreateDialog--------------------------------");
            return super.onCreateDialog(id);
        }
    }

    public void onClick(View v) {
        if (v == searchButton) {
            onSearchRequested();
        } else {
            if (v == sortButton) {
                order_type = ORDER_BY_NAME;
            } else if (v == distanceButton) {
                order_type = ORDER_BY_DIST;
            } else if (v == dateButton) {
                order_type = ORDER_BY_DATE;
            } else if (v == ratingButton) {
                order_type = ORDER_BY_RATING;
            }
            if (getListAdapter() != null) {
                sort();
            } else {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.landmarks_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent result = new Intent();
            setResult(RESULT_CANCELED, result);
            finish();
            return true;
        } else if (itemId == R.id.sortRating) {
            order_type = ORDER_BY_RATING;
            if (getListAdapter() != null) {
                sort();
            }
            return true;
        } else if (itemId == R.id.sortDate) {
            order_type = ORDER_BY_DATE;
            if (getListAdapter() != null) {
                sort();
            }
            return true;
        } else if (itemId == R.id.sortDistance) {
            order_type = ORDER_BY_DIST;
            if (getListAdapter() != null) {
                sort();
            }
            return true;
        } else if (itemId == R.id.sortAlphabetically) {
            order_type = ORDER_BY_NAME;
            if (getListAdapter() != null) {
                sort();
            }
            return true;
        } else if (itemId == R.id.search) {
            onSearchRequested();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    protected void cancelProgressDialog() {
        finish();
    }

    protected void sort() {
        try {
            ORDER order = ORDER.ASC;
            ArrayAdapter<LandmarkParcelable> arrayAdapter = (ArrayAdapter<LandmarkParcelable>) getListAdapter();
			
            if (order_type == ORDER_BY_NAME) {
                arrayAdapter.sort(nameComparator);
                order = order_name;
                if (order_name == ORDER.ASC) {
                    order_name = ORDER.DESC;
                } else if (order_name == ORDER.DESC) {
                    order_name = ORDER.ASC;
                }
                setSelectedButton(sortButton);
            } else if (order_type == ORDER_BY_DIST) {
                arrayAdapter.sort(distanceComparator);
                order = order_dist;
                if (order_dist == ORDER.ASC) {
                    order_dist = ORDER.DESC;
                } else if (order_dist == ORDER.DESC) {
                    order_dist = ORDER.ASC;
                }
                setSelectedButton(distanceButton);
            } else if (order_type == ORDER_BY_DATE) {
                arrayAdapter.sort(dateComparator);
                order = order_date;
                if (order_date == ORDER.ASC) {
                    order_date = ORDER.DESC;
                } else if (order_date == ORDER.DESC) {
                    order_date = ORDER.ASC;
                }
                setSelectedButton(dateButton);
            } else if (order_type == ORDER_BY_DIST_ASC) {
                order_dist = ORDER.ASC;
                arrayAdapter.sort(distanceComparator);
                order = ORDER.ASC;
                order_dist = ORDER.DESC;
                setSelectedButton(distanceButton);
            } else if (order_type == ORDER_BY_DATE_DESC) {
                order_date = ORDER.DESC;
                arrayAdapter.sort(dateComparator);
                order = ORDER.DESC;
                order_date = ORDER.ASC;
                setSelectedButton(dateButton);
            } else if (order_type == ORDER_BY_CAT_STATS) {
                arrayAdapter.sort(new CategoryComparator());
                order = ORDER.DESC;
            } else if (order_type == ORDER_BY_RATING) {
                arrayAdapter.sort(ratingComparator);
                order = order_rating;
                if (order_rating == ORDER.ASC) {
                    order_rating = ORDER.DESC;
                } else if (order_rating == ORDER.DESC) {
                    order_rating = ORDER.ASC;
                }
                setSelectedButton(ratingButton);
            } else if (order_type == ORDER_BY_REV_DESC) {
                order = ORDER.DESC;
                arrayAdapter.sort(new RevelanceComparator());
            }

            setStatusBar(order);
        } catch (Exception e) {
            LoggerUtils.error("AbstractLandmarkList.sort exception", e);
        }
    }

    private void setStatusBar(ORDER order) {
        String orderText = "";

        if (order_type == ORDER_BY_NAME) {
            orderText = Locale.getMessage(R.string.list_sorted_name);
        } else if (order_type == ORDER_BY_DIST) {
            orderText = Locale.getMessage(R.string.list_sorted_distance);
        } else if (order_type == ORDER_BY_DATE) {
            orderText = Locale.getMessage(R.string.list_sorted_date);
        } else if (order_type == ORDER_BY_DIST_ASC) {
            orderText = Locale.getMessage(R.string.list_sorted_distance);
        } else if (order_type == ORDER_BY_DATE_DESC) {
            orderText = Locale.getMessage(R.string.list_sorted_date);
        } else if (order_type == ORDER_BY_CAT_STATS) {
            orderText = Locale.getMessage(R.string.list_sorted_category);
        } else if (order_type == ORDER_BY_RATING) {
            orderText = Locale.getMessage(R.string.list_sorted_rating);
        } else if (order_type == ORDER_BY_REV_DESC) {
            orderText = Locale.getMessage(R.string.list_sorted_relevance);
        }

        if (order == ORDER.ASC) {
            sortingImage.setImageResource(R.drawable.sort_ascending);
        } else if (order == ORDER.DESC) {
            sortingImage.setImageResource(R.drawable.sort_descending);
        }

        sortingText.setText(orderText);
    }

    private void setSelectedButton(View button) {
        Drawable d = getApplicationContext().getResources().getDrawable(R.drawable.defaultbg);
        Drawable r = getApplicationContext().getResources().getDrawable(R.drawable.redbg);

        if (selectedView != button) {
            if (selectedView != null) {
                selectedView.setBackgroundDrawable(d);
            }
            selectedView = button;
            button.setBackgroundDrawable(r);
        }
    }

    private boolean isLocalLayer(LandmarkParcelable l0) {
        return (l0.getLayer().equals(Commons.LOCAL_LAYER));
    }

    private class NameComparator implements Comparator<LandmarkParcelable> {

        public int compare(LandmarkParcelable s0, LandmarkParcelable s1) {
            if (order_name == ORDER.ASC) {
                return s0.getName().compareTo(s1.getName()); //asc
            } else {
                return s1.getName().compareTo(s0.getName()); //desc
            }
        }
    }

    private class DistanceComparator implements Comparator<LandmarkParcelable> {

        public int compare(LandmarkParcelable s0, LandmarkParcelable s1) {
            if (order_dist == ORDER.ASC) {
                return comp(s0, s1);
            } else {
                return comp(s1, s0);
            }
        }

        private int comp(LandmarkParcelable s0, LandmarkParcelable s1) {
            if (s0.getDistance() > s1.getDistance()) {
                return 1;
            } else if (s0.getDistance() == s1.getDistance()) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    private class CreationDateComparator implements Comparator<LandmarkParcelable> {

        public int compare(LandmarkParcelable arg0, LandmarkParcelable arg1) {
            if (order_date == ORDER.ASC) {
                if (arg0.getCreationDate() > arg1.getCreationDate()) {
                    return 1;
                } else if (arg0.getCreationDate() == arg1.getCreationDate()) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                if (arg0.getCreationDate() < arg1.getCreationDate()) {
                    return 1;
                } else if (arg0.getCreationDate() == arg1.getCreationDate()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }

    private class CategoryComparator implements Comparator<LandmarkParcelable> {

        private Map<String, Integer> statsCache;

        public CategoryComparator() {
            super();
            statsCache = new HashMap<String, Integer>();
        }

        public int compare(LandmarkParcelable s0, LandmarkParcelable s1) {
            int s0_stats = getSubCategoryStats(s0.getCategoryid(), s0.getSubcategoryid());
            int s1_stats = getSubCategoryStats(s1.getCategoryid(), s1.getSubcategoryid());

            int diff = s1_stats - s0_stats;
            if (diff != 0) {
                return diff; //desc
            } else {
                if (s0.getCategoryid() != s1.getCategoryid()) {
                    s0_stats = getCategoryStats(s0.getCategoryid());
                    s1_stats = getCategoryStats(s1.getCategoryid());
                    diff = s1_stats - s0_stats;
                    if (diff != 0) {
                        return diff; //desc
                    }
                }
                return dateComparator.compare(s0, s1);
            }
        }

        private int getSubCategoryStats(int cat, int subcat) {
            int value = 0;
            if (cat > 0 && subcat > 0) {
                final String key = cat + "_" + subcat;
                if (statsCache.containsKey(key)) {
                    value = statsCache.get(key);
                    //System.out.println("Subcat from cache: " + cat + " " + subcat + " " + value);
                } else {
                    int stats = cm.getSubCategoryStats(cat, subcat);
                    statsCache.put(key, stats);
                    value = stats;
                    //System.out.println("Subcat from cm: " + cat + " " + subcat + " " + value);
                }
            }

            return value;
        }
        
        private int getCategoryStats(int cat) {
            int value = 0;
            if (cat > 0) {
                final String key = cat + "_";
                if (statsCache.containsKey(key)) {
                    value = statsCache.get(key);
                    //System.out.println("Cat from cache: " + cat + " " + value);
                } else {
                    int stats = cm.getCategoryStats(cat);
                    statsCache.put(key, stats);
                    value = stats;
                    //System.out.println("Cat from cm: " + cat + " " + value);
                }
            }
            return value;
        }
    }

    private class RatingComparator implements Comparator<LandmarkParcelable> {

        public int compare(LandmarkParcelable s0, LandmarkParcelable s1) {
            if (order_rating == ORDER.ASC) {
               return comp(s0, s1);
            } else {
               return comp(s1, s0);
            }
        }

        private int comp(LandmarkParcelable s0, LandmarkParcelable s1) {
        	int r0 = s0.getRating();
        	int r1 = s1.getRating();
            if (r0 > r1) {
                return 1;
            } else if (r0 == r1) {
            	int n0 = s0.getNumberOfReviews();
            	int n1 = s1.getNumberOfReviews();
                if (n0 > n1) {
                    return 1;
                } else if (n0 == n1) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                return -1;
            }
        }
        
        /*private int comp(LandmarkParcelable s0, LandmarkParcelable s1) {
        	if (s0.getRating() > s1.getRating()) {
                return 1;
            } else if (s0.getRating() == s1.getRating()) {
                if (s0.getNumberOfReviews() > s1.getNumberOfReviews()) {
                    return 1;
                } else if (s1.getNumberOfReviews() > s0.getNumberOfReviews()) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }
        }*/
    }

    private class RevelanceComparator implements Comparator<LandmarkParcelable> {

        public int compare(LandmarkParcelable arg0, LandmarkParcelable arg1) {
            if (isLocalLayer(arg0)) {
                return -1;
            } else if (order_date == ORDER.DESC) {
                return comp(arg0, arg1);
            } else {
                return comp(arg1, arg0);
            }
        }

        private int comp(LandmarkParcelable arg0, LandmarkParcelable arg1) {

            if (arg0.getRevelance() > arg1.getRevelance()) {
                return 1;
            } else if (arg0.getRevelance() == arg1.getRevelance()) {
                //compare by distance
                return distanceComparator.compare(arg1, arg0);
            } else {
                return -1;
            }
        }
    }
}
