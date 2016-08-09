/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.content.SearchRecentSuggestionsProvider;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.SuggestionProviderUtil;

/**
 *
 * @author jstakun
 */
public class LandmarksSuggestionProvider extends SearchRecentSuggestionsProvider {

    private final static String AUTHORITY = "com.jstakun.gms.android.landmarks";
    private final static Integer MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;

    public LandmarksSuggestionProvider() {
        super();

        LoggerUtils.debug("Running LandmarksSuggestionProvider() ...");
        
        SuggestionProviderUtil.seAuthority(AUTHORITY);
        SuggestionProviderUtil.setMode(MODE);
        
        setupSuggestions(AUTHORITY, MODE);
    }
}
