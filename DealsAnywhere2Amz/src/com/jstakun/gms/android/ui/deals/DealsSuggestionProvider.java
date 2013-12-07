/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.ui.deals;

import android.content.SearchRecentSuggestionsProvider;

import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.SuggestionProviderUtil;
/**
 *
 * @author jstakun
 */
public class DealsSuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.jstakun.gms.android.deals";
    public final static Integer MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;

    public DealsSuggestionProvider() {
        super();

        LoggerUtils.debug("Running DealsSuggestionProvider() ...");
        
        SuggestionProviderUtil.seAuthority(AUTHORITY);
        SuggestionProviderUtil.setMode(MODE);
        
        setupSuggestions(AUTHORITY, MODE);
    }
}
