/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.Token;
import com.jstakun.gms.android.utils.UserTracker;

/**
 * 
 * @author jstakun
 */
public class OAuth2Activity extends Activity implements OnDismissListener {

	private WebView webView = null;
	private ISocialUtils socialUtils = null;
	private View rl = null;
	private static final int ID_DIALOG_PROGRESS = 0;
	private boolean isDialogVisible = false;
	private String serviceName = null;
	private IntentsHelper intents;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.gms_oauth_webview);

		ActionBarHelper.setDisplayHomeAsUpEnabled(this);

		UserTracker.getInstance().trackActivity(getClass().getName());

		intents = new IntentsHelper(this, ConfigurationManager.getInstance().getLandmarkManager(), null);

		webView = (WebView) findViewById(R.id.webview);
		rl = findViewById(R.id.loadingWebView);

		rl.setVisibility(View.VISIBLE);
		webView.setVisibility(View.GONE);

		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setSupportZoom(true);
		webView.getSettings().setBuiltInZoomControls(true);
		
		webView.setWebViewClient(new MyWebViewClient());

		webView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_UP:
					if (!v.hasFocus()) {
						v.requestFocus();
					}
					break;
				}
				return false;
			}
		});

		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("service")) {
			serviceName = extras.getString("service");
			socialUtils = OAuthServiceFactory.getSocialUtils(serviceName);
			loadUrl(webView, OAuthServiceFactory.getOAuthString(serviceName));
		} else {
			finishWithToast(Locale.getMessage(R.string.OAuth_service_missing), null);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_PROGRESS) {
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(Locale.getMessage(R.string.Please_Wait));
			progressDialog.setIndeterminate(true);
			progressDialog.setCancelable(true);
			progressDialog.setOnDismissListener(this);
			progressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					cancelProgressDialog();
				}
			});
			progressDialog.getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			return progressDialog;
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		if (id == ID_DIALOG_PROGRESS) {
			isDialogVisible = true;
			if (dialog instanceof ProgressDialog) {
				ProgressDialog progressDialog = (ProgressDialog) dialog;
				if (serviceName != null) {
					String serviceStr = OAuthServiceFactory.getServiceName(serviceName);
					if (serviceStr != null) {
						progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Oauth_progress_message,serviceStr)));
					}
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		try {
			if (isDialogVisible) {
				dismissDialog(ID_DIALOG_PROGRESS);
			}
		} catch (Exception e) {
			// ignore error
		}
		super.onSaveInstanceState(outState);
	}

	public void onDismiss(DialogInterface arg0) {
		isDialogVisible = false;
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		if (webView != null) {
			webView.removeAllViews();
		}
		super.onDestroy();
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

	private void cancelProgressDialog() {
		finish();
	}

	private void finishWithToast(String message, String layer) {
		if (layer != null) {
			Intent result = new Intent();
			result.putExtra("action", "load");
        	result.putExtra("layer", layer);
        	setResult(RESULT_OK, result);
		}
		finish();
		intents.showInfoToast(message);
	}
	
	private void processJSon(String jsonString) {
		//System.out.println("json: " + jsonString);
		if (StringUtils.startsWith(jsonString, "{")) {
			String token = null;
			JSONObject json = null;
			try {
				json = new JSONObject(jsonString);
				token = json.getString("token");
			} catch (JSONException ex) {
				LoggerUtils.error("GMSJavaScriptInterface.processJson exception", ex);
			}
			if (token != null && json != null) {
				String secret = json.optString("secret");
				socialUtils.storeAccessToken(new Token(token, secret));
				if (socialUtils.initOnTokenPresent(json)) {
					if (serviceName.equals(Commons.FACEBOOK)) {
						finishWithToast(Locale.getMessage(R.string.Authn_success), Commons.FACEBOOK_LAYER);
			        } else if (serviceName.equals(Commons.FOURSQUARE)) {
			        	finishWithToast(Locale.getMessage(R.string.Authn_success), Commons.FOURSQUARE_LAYER);
			        } else if (serviceName.equals(Commons.TWITTER)) {
			        	finishWithToast(Locale.getMessage(R.string.Authn_success), Commons.TWITTER_LAYER);
			        } else {
			        	finishWithToast(Locale.getMessage(R.string.Authn_success), null);
			        }
				} else {
					finishWithToast(Locale.getMessage(R.string.Authz_error), null);
				}
			} else {
				finishWithToast(Locale.getMessage(R.string.Authz_error), null);
			}
		} else {
			finishWithToast(Locale.getMessage(R.string.Authz_error), null);
		}
	}

	private static void loadUrl(WebView webView, String url) {
		try {
			UrlLoaderHelperInternal.loadUrl(url, webView);
		} catch (Throwable e) {
			webView.loadUrl(url);
		}
	}
	
	private static class UrlLoaderHelperInternal {
		private static void loadUrl(String url, WebView webView) {
			//API version 8+
			Map<String, String> headers = new HashMap<String, String>();
			if (ConfigurationManager.getUserManager().isTokenPresent()) {
	    		headers.put(Commons.TOKEN_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_TOKEN));
	    		headers.put(Commons.SCOPE_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_SCOPE));
			}
			webView.loadUrl(url, headers);
		}
	}
	
	private class MyWebViewClient extends WebViewClient {

		private boolean loadingFinished;
		private boolean redirect;

		private MyWebViewClient() {
			loadingFinished = true;
			redirect = false;
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			LoggerUtils.debug("shouldOverrideUrlLoading: " + url);
			if (!loadingFinished) {
				redirect = true;
			}
			loadingFinished = false;
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			loadingFinished = false;
			if (!isDialogVisible) {
				try {
					showDialog(ID_DIALOG_PROGRESS);
				} catch (Exception e) {
					// ignore error
				}
			}

			LoggerUtils.debug("onPageStarted url: " + url);
			
			if (url.startsWith(OAuthServiceFactory.CALLBACK_ERROR_URL)) {
				finishWithToast(Locale.getMessage(R.string.Authz_error), null);
			}
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			LoggerUtils.debug("onPageFinished: " + url);
			if (!redirect) {
				loadingFinished = true;
			}
			if (loadingFinished && !redirect) {
				if (isDialogVisible) {
					try {
						dismissDialog(ID_DIALOG_PROGRESS);
					} catch (Exception e) {
						// ignore error
					}
				}

				if (url.startsWith(OAuthServiceFactory.getOAuthCallback(serviceName))) {
					//process page title
					processJSon(view.getTitle());
	    		} else if (StringUtils.startsWith(view.getTitle(), "401")) {
	    			finishWithToast(Locale.getMessage(R.string.Authz_error), null);		
	    		} else if (StringUtils.startsWith(view.getTitle(), "403")) {
	    			finishWithToast(Locale.getMessage(R.string.Forbidden_connection_error), null);
	    		} else if (StringUtils.startsWith(view.getTitle(), "500")) {
	    			finishWithToast(Locale.getMessage(R.string.Unexpected_error), null);
	    		}

				rl.setVisibility(View.GONE);
				webView.setVisibility(View.VISIBLE);
			} else {
				redirect = false;
			}
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			LoggerUtils.error("Received error " + errorCode + ": " + description);
		}
	}
	
	
}
