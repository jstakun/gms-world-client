/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
public final class HelpActivity extends Activity {

    // Use this key and one of the values below when launching this activity via intent. If not
    // present, the default page will be loaded.
    public static final String REQUESTED_PAGE_KEY = "requested_page_key";
    public static final String DEFAULT_PAGE = "index.html";
    public static final String WHATS_NEW_PAGE = "whatsnew.html";
    public static final String HELP_ACTIVITY_SHOWN = "HELP_ACTIVITY_SHOWN";
    private static final String BASE_URL = "file:///android_asset/html/";
    private static final String WEBVIEW_STATE_PRESENT = "webview_state_present";
    private static boolean initialized = false;
    private WebView webView;
    private LinearLayout webviewHolder;
    private View backButton;
    private IntentsHelper intents = null;
    private final Button.OnClickListener backListener = new Button.OnClickListener() {

        public void onClick(View view) {
            webView.goBack();
        }
    };
    private final Button.OnClickListener doneListener = new Button.OnClickListener() {

        public void onClick(View view) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.help);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        webView = (WebView) findViewById(R.id.help_contents);
        webView.setWebViewClient(new HelpClient());
        
        webviewHolder = (LinearLayout) findViewById(R.id.webviewHolder);

        intents = new IntentsHelper(this, null, null);
        // Froyo has a bug with calling onCreate() twice in a row, which causes the What's New page
        // that's auto-loaded on first run to appear blank. As a workaround we only call restoreState()
        // if a valid URL was loaded at the time the previous activity was torn down.
        Intent intent = getIntent();
        if (icicle != null && icicle.getBoolean(WEBVIEW_STATE_PRESENT, false)) {
            webView.restoreState(icicle);
        } else if (intent != null) {
            String page = intent.getStringExtra(REQUESTED_PAGE_KEY);
            if (page != null && page.length() > 0) {
                webView.loadUrl(BASE_URL + page);
            } else {
                webView.loadUrl(BASE_URL + DEFAULT_PAGE);
            }
        } else {
            webView.loadUrl(BASE_URL + DEFAULT_PAGE);
        }

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(backListener);
        View doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(doneListener);

        ConfigurationManager.getInstance().putObject(HELP_ACTIVITY_SHOWN, "true");

        if (!initialized) {
            initialized = true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        String url = webView.getUrl();
        if (url != null && url.length() > 0) {
            webView.saveState(state);
            state.putBoolean(WEBVIEW_STATE_PRESENT, true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //UserTracker.getInstance().stopSession(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        webviewHolder.removeView(webView);
        //webView.setFocusable(true);
        //webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private final class HelpClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            setTitle(view.getTitle());
            backButton.setEnabled(view.canGoBack());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("file")) {
                // Keep local assets in this WebView.
                return false;
            } else {
                // Open external URLs in Browser.
                intents.startActionViewIntent(url);
                return true;
            }
        }
    }
}
