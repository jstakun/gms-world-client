/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import com.jstakun.gms.android.config.ConfigurationManager;

import android.content.res.Resources;

/**
 *
 * @author jstakun
 */
public class Locale {

    private static Resources res;

    static {
        res = ConfigurationManager.getInstance().getContext().getResources();
        String localeISO3Code = "USA";
        try {
        	localeISO3Code = ConfigurationManager.getInstance().getCurrentLocale().getISO3Country();
        } catch (Exception e) {
        }
        ConfigurationManager.getInstance().putString(ConfigurationManager.ISO3COUNTRY, localeISO3Code);
        LoggerUtils.debug("System ISO3 country code is: " + localeISO3Code);
    }

    public static String getMessage(int key) {
        return res.getString(key);
    }

    public static String getMessage(int key, Object... args) {
        return String.format(res.getString(key), args);
    }
    
    public static String getQuantityMessage(int key, int quantity) {
    	return res.getQuantityString(key, quantity, quantity);
    }
}
