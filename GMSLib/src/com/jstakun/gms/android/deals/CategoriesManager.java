/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.deals;

import android.os.Bundle;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.Layer;
import com.jstakun.gms.android.landmarks.LayerManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class CategoriesManager {

    private List<Category> categories = new ArrayList<Category>();
    private List<Category> subcategories;
    private boolean initialized = false;
    public static final int CATEGORY_TRAVEL = 7;
    public static final int SUBCATEGORY_HOTEL = 129;
    private static final String KEY_FORMAT = "c_%d_%d_s";
    private int topSubCategoryStats = 0;
    private int topSubCategory = -1;
    private int topCategory = -1;

    public CategoriesManager() {
    }

    /**
     * @return the categories
     */
    public List<Category> getCategories() {
        return categories;
    }

    /**
     * @param categories the categories to set
     */
    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    /**
     * @return the subcategories
     */
    public List<Category> getSubcategories() {
        return subcategories;
    }

    /**
     * @param subcategories the subcategories to set
     */
    public void setSubcategories(List<Category> subcategories, boolean initStats) {
        this.subcategories = subcategories;
        if (initStats) {
            for (Category subcat : subcategories) {
                int stats = getSubCategoryStatsFromConf(subcat.getCategoryID(), subcat.getSubcategoryID());
                subcat.setStats(stats);
                //LoggerUtils.debug("Setting subcat stats: " + subcat.getCategoryID() + " " + subcat.getSubcategoryID() + " " + stats);
                Category cat = getCategory(subcat.getCategoryID());
                cat.setStats(cat.getStats() + stats);
                //LoggerUtils.debug("Setting cat stats: " + subcat.getCategoryID() + " " + (cat.getStats() + stats));
                if (stats > getTopSubCategoryStats()) {
                    topSubCategoryStats = stats;
                    topCategory = subcat.getCategoryID();
                    topSubCategory = subcat.getSubcategoryID();
                }
            }
        }
    }

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @param initialized the initialized to set
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public Category getCategory(String name) {
        for (Category c : categories) {
            if (c.getCategory().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public Category getCategory(int category) {
        if (categories != null) {
            for (Category c : categories) {
                if (c.getCategoryID() == category) {
                    return c;
                }
            }
        }
        return null;
    }

    public Category getSubCategory(int category, int subcategory) {
        if (subcategories != null) {
            for (Category c : subcategories) {
                if (c.getCategoryID() == category && c.getSubcategoryID() == subcategory) {
                    return c;
                }
            }
        }
        return null;
    }

    public List<Category> getSubCategories(int parent) {
        List<Category> cats = new ArrayList<Category>();
        for (Category c : subcategories) {
            if (c.getCategoryID() == parent) {
                cats.add(c);
            }
        }
        return cats;
    }

    public Bundle loadCategoriesGroup() {
        Bundle extras = new Bundle();
        List<String> names = new ArrayList<String>();
        List<Boolean> enabled = new ArrayList<Boolean>();

        for (Category c : categories) {
            names.add(c.getCategory());
            enabled.add(c.isEnabled());
        }

        int size = names.size();
        String[] codes = new String[size];
        String[] namesArray = new String[size];
        boolean[] enabledArray = new boolean[size];

        for (int i = 0; i < enabled.size(); i++) {
            enabledArray[i] = enabled.get(i);
            namesArray[i] = names.get(i);
            codes[i] = Integer.toString(i);
            //System.out.println(namesArray[i] + " " + enabledArray[i]);
        }

        extras.putStringArray("names", namesArray);
        extras.putBooleanArray("enabled", enabledArray);
        extras.putStringArray("codes", codes);

        return extras;
    }

    public void saveCategoriesAction(String[] names, String[] codes) {
        int maxj = codes.length;

        int j = 0;

        for (int i = 0; i < names.length; i++) {    //0 1 2 3 4
            //0   2   4
            if (j < maxj && Integer.parseInt(codes[j]) == i) {
                //System.out.println(names[i] + " selected");
                setCategoryEnabled(names[i], true);
                j++;

            } else {
                if (j >= maxj || Integer.parseInt(codes[j]) > i) {
                    //System.out.println(names[i] + " unselected");
                    setCategoryEnabled(names[i], false);
                }
            }
        }
    }

    private void setCategoryEnabled(String name, boolean enabled) {
        Category c = getCategory(name);
        if (c != null) {
            c.setEnabled(enabled);
        }
    }

    public boolean hasSubcategory(int categoryId) {
    	if (subcategories != null) {
    		for (Category cat : subcategories) {
    			if (cat.getCategoryID() == categoryId) {
    				return true;
    			}
    		}
    	}
        return false;
    }

    public List<Category> getEnabledCategories(LayerManager layerManager) {
        List<Category> enabled = new ArrayList<Category>();
        for (Category c : categories) {
            if (c.isEnabled()) {
                enabled.add(c);
            }
        }
        //only in da
        if (ConfigurationManager.getInstance().getInt(ConfigurationManager.APP_ID) == ConfigurationManager.DA) {
        	for (String key : layerManager.getDynamicLayers()) {
        		Layer layer = layerManager.getLayer(key);
        		Category category = new Category(-1, layer.getName(), -1, null, layer.getSmallIconResource(), -1);
        		category.setCustom(true);
        		enabled.add(category);
        	}
        }
        return enabled;
    }

    public boolean isCategoryEnabled(int category) {
        if (categories != null) {
            for (Category c : categories) {
                if (c.getCategoryID() == category) {
                    return c.isEnabled();
                }
            }
            return false;
        } else {
            return true;
        }

    }

    private List<Integer> getEnabledCategoriesIds() {
        List<Integer> ids = new ArrayList<Integer>();
        if (categories != null) {
            for (Category c : categories) {
                if (c.isEnabled()) {
                    ids.add(c.getCategoryID());
                }
            }
        }
        return ids;
    }

    public String getEnabledCategoriesString() {
        List<Integer> enabledCategories = getEnabledCategoriesIds();
        String categoryid = "";
        if (!enabledCategories.isEmpty()) {
            categoryid = StringUtils.join(enabledCategories, ",");
        }

        return categoryid;
    }

    private int getSubCategoryStatsFromConf(int category, int subcategory) {
        int value = 0;
        if (category > 0 && subcategory > 0) {
            final String key = String.format(Locale.US, KEY_FORMAT, category, subcategory);
            value = ConfigurationManager.getInstance().getInt(key, 0);
        }
        return value;
    }

    public void addSubCategoryStats(int category, int subcategory) {
        if (category > 0 && subcategory > 0) {
            final String key = String.format(Locale.US, KEY_FORMAT, category, subcategory);
            Category cat = getCategory(category);
            Category subcat = getSubCategory(category, subcategory);
            if (cat != null && subcat != null) {
                int catvalue = cat.getStats();
                int subcatvalue = subcat.getStats();
                catvalue++;
                subcatvalue++;
                ConfigurationManager.getInstance().putInteger(key, subcatvalue);
                cat.setStats(catvalue);
                subcat.setStats(subcatvalue);
            }
        }
    }

    public int getSubCategoryStats(int category, int subcategory) {
        if (category > 0 && subcategory > 0 && subcategories != null) {
            for (Category subcat : subcategories) {
                if (subcat.getCategoryID() == category && subcat.getSubcategoryID() == subcategory) {
                    return subcat.getStats();
                }
            }
        }

        return 0;
    }
    
    public void printCategoryStats() {
    	System.out.println("Deals categories stats: ");
    	for (Category c : getCategories()) {
    		System.out.println(c.getCategory() + ": " + getCategoryStats(c.getCategoryID()));
    		for (Category sub : getSubCategories(c.getCategoryID())) {
    			System.out.println(c.getCategory() + " -> " + sub.getSubcategory() + ": " + getSubCategoryStats(c.getCategoryID(), sub.getSubcategoryID()));
    		}
    	}
    }

    public int getCategoryStats(int category) {
        for (Category cat : categories) {
            if (cat.getCategoryID() == category) {
                return cat.getStats();
            }
        }

        return 0;
    }

    public void clearAllStats() {
        if (subcategories != null) {
            for (Category c : subcategories) {
                final String key = String.format(Locale.US, KEY_FORMAT, c.getCategoryID(), c.getSubcategoryID());
                if (ConfigurationManager.getInstance().containsKey(key)) {
                    ConfigurationManager.getInstance().putInteger(key, 0);
                }
                c.setStats(0);
            }
            for (Category cat : categories) {
                cat.setStats(0);
            }
        }
    }

    /**
     * @return the topSubCategory
     */
    public int getTopSubCategory() {
        return topSubCategory;
    }

    /**
     * @return the topCategory
     */
    public int getTopCategory() {
        return topCategory;
    }

    /**
     * @return the topSubCategoryStats
     */
    public int getTopSubCategoryStats() {
        return topSubCategoryStats;
    }
}
