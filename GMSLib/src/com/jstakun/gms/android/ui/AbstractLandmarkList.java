/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.CategoriesManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.OsUtil;

/**
 *
 * @author jstakun
 */
public abstract class AbstractLandmarkList extends ListActivity implements View.OnClickListener {

    protected enum ORDER {ASC, DESC};
    protected enum ORDER_TYPE {ORDER_BY_NAME, ORDER_BY_DIST, ORDER_BY_DATE,
    	ORDER_BY_CAT_STATS, ORDER_BY_RATING, ORDER_BY_REV};
    protected static final int ID_DIALOG_PROGRESS = 0;
    private View sortButton, distanceButton, dateButton, selectedView;
    protected View list, loading, searchButton, ratingButton;
    private TextView sortingText;
    private ImageView sortingImage;
    private ORDER order_name = ORDER.ASC;
    private ORDER order_dist = ORDER.ASC;
    private ORDER order_date = ORDER.ASC;
    private ORDER order_rating = ORDER.DESC;
    private CategoriesManager cm;
    private Comparator<LandmarkParcelable> distanceComparator = new DistanceComparator();
    private Comparator<LandmarkParcelable> dateComparator = new CreationDateComparator();
    
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
        } if (v == sortButton) {
            sort(ORDER_TYPE.ORDER_BY_NAME, order_name, true);
        } else if (v == distanceButton) {
            sort(ORDER_TYPE.ORDER_BY_DIST, order_dist, true);
        } else if (v == dateButton) {
            sort(ORDER_TYPE.ORDER_BY_DATE, order_date, true);
        } else if (v == ratingButton) {
            sort(ORDER_TYPE.ORDER_BY_RATING, order_rating, true);
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
            sort(ORDER_TYPE.ORDER_BY_RATING, order_rating, true);
            return true;
        } else if (itemId == R.id.sortDate) {
        	sort(ORDER_TYPE.ORDER_BY_DATE, order_date, true);
            return true;
        } else if (itemId == R.id.sortDistance) {
        	sort(ORDER_TYPE.ORDER_BY_DIST, order_dist, true);
            return true;
        } else if (itemId == R.id.sortAlphabetically) {
        	sort(ORDER_TYPE.ORDER_BY_NAME, order_name, true);
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

    protected void sort(ORDER_TYPE order_type, ORDER order, boolean changeOrder) {
    	
    	LoggerUtils.debug("Sorting landmarks by " + order_type.name() + ", " + order.name() + " and changing order: " + changeOrder);	
    	
    	Comparator<LandmarkParcelable> comparator = null;
        if (order_type == ORDER_TYPE.ORDER_BY_NAME) {
            comparator = new NameComparator();
            setSelectedButton(sortButton);
        } else if (order_type == ORDER_TYPE.ORDER_BY_DIST) {
            comparator = distanceComparator;
            setSelectedButton(distanceButton);
        } else if (order_type == ORDER_TYPE.ORDER_BY_DATE) {
            comparator = dateComparator;
            setSelectedButton(dateButton);
        } else if (order_type == ORDER_TYPE.ORDER_BY_CAT_STATS) {
            comparator = new CategoryComparator();
        } else if (order_type == ORDER_TYPE.ORDER_BY_RATING) {
            comparator = new RatingComparator();
            setSelectedButton(ratingButton);
        } else if (order_type == ORDER_TYPE.ORDER_BY_REV) {
            comparator = new RevelanceComparator();
        }
        setStatusBar(order_type, order);
        
        if (!changeOrder) {
        	if (order_type == ORDER_TYPE.ORDER_BY_NAME) {
        		order_name = order;
        	} else if (order_type == ORDER_TYPE.ORDER_BY_DIST) {
        		order_dist = order;                
        	} else if (order_type == ORDER_TYPE.ORDER_BY_DATE) {
        		order_date = order;
        	} else if (order_type == ORDER_TYPE.ORDER_BY_RATING) {
        		order_rating = order;
        	} 
        	changeOrder = true;
        }
        
        try {
            ArrayAdapter<LandmarkParcelable> arrayAdapter = (ArrayAdapter<LandmarkParcelable>) getListAdapter();
            if (comparator != null && arrayAdapter != null) {
            	arrayAdapter.sort(comparator);
            }	
        } catch (Exception e) {
            LoggerUtils.error("AbstractLandmarkList.sort() exception", e);
        }
            
        if (changeOrder) {
        	reverseOrder(order_type);
        }
    }
    
    private void reverseOrder(ORDER_TYPE order_type) {
    	if (order_type == ORDER_TYPE.ORDER_BY_NAME) {
    		if (order_name == ORDER.ASC) {
            	order_name = ORDER.DESC;
            } else if (order_name == ORDER.DESC) {
            	order_name = ORDER.ASC;
            }
    	} else if (order_type == ORDER_TYPE.ORDER_BY_DIST) {
    		if (order_dist == ORDER.ASC) {
            	order_dist = ORDER.DESC;
            } else if (order_dist == ORDER.DESC) {
            	order_dist = ORDER.ASC;
            }
    	} else if (order_type == ORDER_TYPE.ORDER_BY_DATE) {
    		if (order_date == ORDER.ASC) {
            	order_date = ORDER.DESC;
            } else if (order_date == ORDER.DESC) {
            	order_date = ORDER.ASC;
            }
    	} else if (order_type == ORDER_TYPE.ORDER_BY_RATING) {
    		if (order_rating == ORDER.ASC) {
            	order_rating = ORDER.DESC;
            } else if (order_rating == ORDER.DESC) {
            	order_rating = ORDER.ASC;
            }
    	} 
    }

    private void setStatusBar(ORDER_TYPE order_type, ORDER order) {
        String orderText = "";

        if (order_type == ORDER_TYPE.ORDER_BY_NAME) {
            orderText = Locale.getMessage(R.string.list_sorted_name);
        } else if (order_type == ORDER_TYPE.ORDER_BY_DIST) {
            orderText = Locale.getMessage(R.string.list_sorted_distance);
        } else if (order_type == ORDER_TYPE.ORDER_BY_DATE) {
            orderText = Locale.getMessage(R.string.list_sorted_date);
        } else if (order_type == ORDER_TYPE.ORDER_BY_CAT_STATS) {
            orderText = Locale.getMessage(R.string.list_sorted_category);
        } else if (order_type == ORDER_TYPE.ORDER_BY_RATING) {
            orderText = Locale.getMessage(R.string.list_sorted_rating);
        } else if (order_type == ORDER_TYPE.ORDER_BY_REV) {
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
