/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.deals;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class Category {

    private int categoryID;
    private String category;
    private int subcategoryID;
    private String subcategory;
    private String key;
    private int stats;
    private int icon;
    private int iconLarge;
    private boolean custom = false;
    private int count;

    public Category(int categoryID, String category, int subcategoryID, String subcategory, int icon, int iconLarge) {
        this.category = category;
        this.subcategoryID = subcategoryID;
        this.categoryID = categoryID;
        this.subcategory = subcategory;

        this.stats = 0;
        this.count = 0;

        this.icon = icon;
        this.iconLarge = iconLarge;
        
        this.key = category + "_status";

        if (!ConfigurationManager.getInstance().containsKey(key)) {
            ConfigurationManager.getInstance().setOn(key);
        }
    }

    /**
     * @return the categoryID
     */
    public int getCategoryID() {
        return categoryID;
    }

    /**
     * @param categoryID the categoryID to set
     */
    public void setCategoryID(int categoryID) {
        this.categoryID = categoryID;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * @return the subcategoryID
     */
    public int getSubcategoryID() {
        return subcategoryID;
    }

    /**
     * @param subcategoryID the subcategoryID to set
     */
    public void setSubcategoryID(int subcategoryID) {
        this.subcategoryID = subcategoryID;
    }

    /**
     * @return the subcategory
     */
    public String getSubcategory() {
        return subcategory;
    }

    /**
     * @param subcategory the subcategory to set
     */
    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        if (ConfigurationManager.getInstance().isOn(key)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            ConfigurationManager.getInstance().setOn(key);
        } else {
            ConfigurationManager.getInstance().setOff(key);
        }
    }

    /**
     * @return the stats
     */
    public int getStats() {
        return stats;
    }

    /**
     * @param stats the stats to set
     */
    public void setStats(int stats) {
        this.stats = stats;
    }

    public int getIcon() {
        return icon;
    }

    public int getLargeIcon() {
        return iconLarge;
    }

    /**
     * @return the custom
     */
    public boolean isCustom() {
        return custom;
    }

    /**
     * @param custom the custom to set
     */
    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    /**
     * @return the count
     */
    public int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public void setCount(int count) {
        this.count = count;
    }
}
