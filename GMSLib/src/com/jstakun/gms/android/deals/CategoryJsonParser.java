/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.deals;

import android.content.Context;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.LoggerUtils;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class CategoryJsonParser {

    public static List<Category> parserCategoryJson(String json, int categoryID, GMSAsyncTask<?,?,?> caller) {
        List<Category> categories = new ArrayList<Category>();

        try {
            if (json != null && json.startsWith("[")) {
                JSONArray array = new JSONArray(json);
                int length = array.length();
                for (int i = 0; i < length; i++) {
                    JSONObject cat = array.getJSONObject(i);

                    int catId = cat.getInt("categoryID");

                    if (categoryID == -1 || catId == categoryID) {

                        String catName = cat.getString("category");

                        int subcatId = -1;
                        if (cat.has("subcategoryID")) {
                            subcatId = cat.getInt("subcategoryID");
                        }

                        String subcatName = null;
                        if (cat.has("subcategory")) {
                            subcatName = cat.getString("subcategory");
                        }

                        Context c = ConfigurationManager.getInstance().getContext();

                        int icon = 0;
                        String iconStr = cat.optString("icon");
                        if (iconStr != null && c != null) {
                            //icon = cat.getString("icon");
                            try {
                                icon = c.getResources().getIdentifier(iconStr, "drawable", c.getPackageName());
                            } catch (Exception ex) {
                                LoggerUtils.error("CategoryJsonParser.parserCategoryJson error:", ex);
                            }
                        }

                        int iconLarge = 0;
                        String iconLargeStr = cat.optString("iconLarge");
                        if (iconLargeStr != null && c != null) {
                            try {
                                iconLarge = c.getResources().getIdentifier(iconLargeStr, "drawable", c.getPackageName());
                            } catch (Exception ex) {
                                LoggerUtils.error("CategoryJsonParser.parserCategoryJson error:", ex);
                            }
                        }

                        Category category = new Category(catId, catName, subcatId, subcatName, icon, iconLarge);
                        categories.add(category);
                        
                        if (caller.isCancelled()) {
                            break;
                        }
                    }
                }
            }
        } catch (JSONException ex) {
            LoggerUtils.error("CategoryJsonParser.parserCategoryJson error:", ex);
        }

        return categories;
    }
}
