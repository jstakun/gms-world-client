/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
public class WebViewActivity extends Activity {

    private WebView webView = null;
    private View rl, backButton;
    private LinearLayout webviewHolder;
    private String url = null, title = null;
    private Intents intents = null;
    private static final String WEBVIEW_STATE_PRESENT = "webview_state_present";
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
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            url = extras.getString("url");
            title = extras.getString("title");
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.newwebview);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        intents = new Intents(this, null, null);

        setProgressBarIndeterminateVisibility(true);
        setProgressBarVisibility(true);

        webView = (WebView) findViewById(R.id.webview);
        rl = findViewById(R.id.loadingWebView);
        webviewHolder = (LinearLayout) findViewById(R.id.webviewHolder); 

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(backListener);
        View doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(doneListener);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);	    													 //if ROM supports Multi-Touch
        settings.setBuiltInZoomControls(true);
        settings.setAllowFileAccess(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        try {
            HelperInternal.setCacheSettings(getApplicationContext(), settings);
        } catch (VerifyError e) {
        }

        webView.setWebViewClient(new MyWebViewClient());

        //System.out.println("Loading url: " + url);

        final Activity MyActivity = this;

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                //Make the bar disappear after URL is loaded, and changes string to Loading...
                MyActivity.setTitle(Locale.getMessage(R.string.Please_Wait));
                setProgressBarIndeterminateVisibility(true);
                setProgressBarVisibility(true);
                MyActivity.setProgress(progress * 100); //Make the bar disappear after URL is loaded

                // Return the app name after finish loading
                if (progress == 100) {
                    if (title != null) {
                        MyActivity.setTitle(title);
                    } else {
                        MyActivity.setTitle(R.string.app_name);
                    }
                    setProgressBarIndeterminateVisibility(false);
                    setProgressBarVisibility(false);
                }
            }
        });

        if (icicle == null && url != null) {
            //System.out.println("Icicle == null ------------------------------------------");
            webView.loadUrl(url);
        } else if ((icicle != null && !icicle.getBoolean(WEBVIEW_STATE_PRESENT, false)) && url != null) {
            //System.out.println("Icicle != null and web view status absent -------------------------------------");
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        backButton.setEnabled(webView.canGoBack());
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
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        if (url != null) {
            //System.out.println("WebView saving instance state -------------------------");
            state.putBoolean(WEBVIEW_STATE_PRESENT, true);
            webView.saveState(state);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //System.out.println("WebView restoring instance state -------------------------");
        webView.restoreState(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //System.out.println("WebView onConfigurationChanged -------------------------");
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //System.out.println("shouldOverrideUrlLoading Loading url: " + url);
            LoggerUtils.debug("Loading url: " + url);
            if (url.startsWith("market://")) {
                intents.startActionViewIntent(url);
            } else {
                view.loadUrl(url);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //System.out.println("onPageFinished Loading url: " + url);
            rl.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            backButton.setEnabled(view.canGoBack());
        }
    }

    private static class HelperInternal {

        private static void setCacheSettings(Context context, WebSettings settings) {
            settings.setAppCacheMaxSize(1024 * 1024); // 1MB
            settings.setAppCachePath(context.getCacheDir().getAbsolutePath());
            settings.setAppCacheEnabled(true);
        }
    }
}
