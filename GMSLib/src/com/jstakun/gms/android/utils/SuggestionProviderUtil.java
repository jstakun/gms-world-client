/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import com.jstakun.gms.android.config.ConfigurationManager;

import android.provider.SearchRecentSuggestions;

/**
 *
 * @author jstakun
 */
public class SuggestionProviderUtil {

    private static SearchRecentSuggestions suggestions = null;

    private static SearchRecentSuggestions getSuggestions() {
        if (suggestions == null) {
            String authority = (String) ConfigurationManager.getInstance().getObject("SuggestionsProviderAuthority", String.class);
            int mode = (Integer) ConfigurationManager.getInstance().getObject("SuggestionsProviderMode", Integer.class);
            suggestions = new SearchRecentSuggestions(ConfigurationManager.getInstance().getContext(), authority, mode);
        }
        return suggestions;
    }

    public static void saveRecentQuery(String query, String line2) {
        try {
            getSuggestions().saveRecentQuery(query, line2);
        } catch (Exception e) {
            LoggerUtils.error("SuggestionProviderUtil.saveRecentQuery exception", e);
        }
    }

    public static void clearHistory() {
        try {
            getSuggestions().clearHistory();
        } catch (Exception e) {
            LoggerUtils.error("SuggestionProviderUtil.clearHistory exception", e);
        }
    }
}
