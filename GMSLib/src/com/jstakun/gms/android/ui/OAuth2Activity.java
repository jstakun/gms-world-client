package com.jstakun.gms.android.ui;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.Token;
import com.jstakun.gms.android.utils.UserTracker;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.gms_oauth_webview);

		ActionBarHelper.setDisplayHomeAsUpEnabled(this);

		UserTracker.getInstance().trackActivity(getClass().getName());

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
						//progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Oauth_progress_message, serviceStr)));
						progressDialog.setMessage(Locale.getMessage(R.string.Oauth_progress_message_plain, serviceStr));
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
		IntentsHelper.getInstance().showInfoToast(message);
	}
	
	private void processJSon(String jsonString) {
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
		if (url.contains("google.com")) {
			//workaround for https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html
			webView.getSettings().setUserAgentString("Mozilla/5.0 (X11; Linux x86_64; rv:45.0) Gecko/20100101 Firefox/45.0");
		} else {
			webView.getSettings().setUserAgentString(ConfigurationManager.getAppUtils().getAboutMessage());
		}
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
			if (url.startsWith(ConfigurationManager.SERVER_URL) || url.startsWith(ConfigurationManager.SSL_SERVER_URL)) {
				if (ConfigurationManager.getUserManager().isTokenPresent()) {
					headers.put(Commons.TOKEN_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_TOKEN));
					headers.put(Commons.SCOPE_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.GMS_SCOPE));
					headers.put(Commons.APP_HEADER, ConfigurationManager.getInstance().getString(ConfigurationManager.APP_ID));
					headers.put(Commons.APP_VERSION_HEADER, Integer.toString(ConfigurationManager.getAppUtils().getVersionCode()));	            
				}
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
			UrlLoaderHelperInternal.loadUrl(url, view);
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
			LoggerUtils.debug("onPageFinished: " + url + " with redirect " + redirect);
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
				String prefix = OAuthServiceFactory.getOAuthCallback(serviceName);
				LoggerUtils.debug("Checking if url starts with " + prefix);
				if (url.startsWith(prefix)) {
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
				view.setVisibility(View.VISIBLE);
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
