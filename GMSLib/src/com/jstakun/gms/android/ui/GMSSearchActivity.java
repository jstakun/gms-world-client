package com.jstakun.gms.android.ui;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.social.ISocialUtils;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MathUtils;
import com.jstakun.gms.android.utils.OsUtil;
import com.jstakun.gms.android.utils.StringUtil;
import com.jstakun.gms.android.utils.SuggestionProviderUtil;
import com.jstakun.gms.android.utils.UserTracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

/**
 * 
 * @author jstakun
 */
public class GMSSearchActivity extends AbstractLandmarkList {

	public enum TYPE {

		DEALS, LANDMARKS
	};

	// query suffixes
	// /a search for address only
	// /c like /a but don't append country
	// /l show save as layer dialog
	// /p exact phrase search
	// /w phrase words search
	// /f fuzzy search
	private String query = null;
    private int layerCodeIndex;
	private int radius = 3, lat, lng;
	private TYPE type;
	private SearchTask searchTask;
	private boolean local;
	private ProgressDialog progressDialog;
	private DialogInterface.OnClickListener addLayerListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
			ConfigurationManager.getInstance().removeObject(
					AlertDialogBuilder.OPEN_DIALOG, Integer.class);
			dialog.cancel();
			if (query != null) {
				String name = query.substring(0, query.length()-2);
				boolean containsLayer = LayerManager.getInstance().addDynamicLayer(name);
				if (containsLayer) {
					IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Layer_exists_error));
	            } else {
	            	AsyncTaskManager.getInstance().executeIndexDynamicLayer(name, new String[]{name});
	            	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.layerCreated));
	            }	
			}
			callSearchTask();
		}
	};
	private DialogInterface.OnClickListener cancelAddLayerListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
			ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
			dialog.cancel();
			callSearchTask();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//UserTracker.getInstance().startSession(this);
		UserTracker.getInstance().trackActivity(getClass().getName());

		setTitle(R.string.listSelection);
		searchButton.setVisibility(View.GONE);

		local = false;

		Object retained = getLastNonConfigurationInstance();
		if (retained instanceof SearchTask) {
			searchTask = (SearchTask) retained;
			searchTask.setActivity(this);
			query = searchTask.getQuery();
			list.setVisibility(View.GONE);
			loading.setVisibility(View.VISIBLE);
			setTitle(R.string.Please_Wait);
		} else {
			Intent queryIntent = getIntent();
			String queryAction = null;
			if (queryIntent != null) {
				queryAction = queryIntent.getAction();
			}

			if (StringUtils.equals(Intent.ACTION_SEARCH, queryAction) || StringUtils.equals(Intent.ACTION_VIEW, queryAction)) {
				query = queryIntent.getStringExtra(SearchManager.QUERY);

				layerCodeIndex = query.lastIndexOf("(");
				if (layerCodeIndex > 0) {
					query = query.substring(0, layerCodeIndex - 1);
				}

				type = TYPE.LANDMARKS;

				Bundle appData = queryIntent.getBundleExtra(SearchManager.APP_DATA);
				if (appData != null) {
					if (appData.containsKey("type")) {
						type = TYPE.valueOf(appData.getString("type"));
					}

					if (appData.containsKey("lat")) {
						lat = appData.getInt("lat");
					}

					if (appData.containsKey("lng")) {
						lng = appData.getInt("lng");
					}

					if (appData.containsKey("radius")) {
						radius = appData.getInt("radius");
					}

					if (appData.containsKey("local")) {
						local = true;
					}
				}

				// hide keyboard
				// InputMethodManager imm = (InputMethodManager)
				// getSystemService(Context.INPUT_METHOD_SERVICE);
				// boolean res =
				// imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),
				// 0); // InputMethodManager.HIDE_IMPLICIT_ONLY);
				
				if (StringUtils.endsWith(query, "/l") && query.length() > 2) {
					AlertDialog addLayerDialog = AlertDialogBuilder.getInstance().getAlertDialog(this, AlertDialogBuilder.ADD_LAYER_DIALOG, null, addLayerListener, cancelAddLayerListener);
					addLayerDialog.setTitle(Locale.getMessage(R.string.Layer_add_message_short, query.substring(0, query.length()-2)));
					//addLayerDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Layer_add_message_long, query.substring(0, query.length()-2))));
					addLayerDialog.setMessage(Locale.getMessage(R.string.Layer_add_message_long_plain, query.substring(0, query.length()-2)));	
					addLayerDialog.show();
				} else {
					callSearchTask();
				}
			}
		}
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (searchTask != null) {
			searchTask.setActivity(null);
		}
		return searchTask;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_PROGRESS) {
			Dialog dialog = super.onCreateDialog(id);
			if (dialog instanceof ProgressDialog) {
				progressDialog = (ProgressDialog) dialog;
				if (query != null) {
					String q;
					if (query.length() > 1 && query.lastIndexOf("/") == query.length()-2) {
						q = query.substring(0, query.length()-2);
					} else {
						q = query;
					}
					//progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Searching_dialog_message, q)));
					progressDialog.setMessage(Locale.getMessage(R.string.Searching_dialog_message_plain, q));
				}
			}
			return dialog;
		} else {
			return super.onCreateDialog(id);
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		if (id == ID_DIALOG_PROGRESS) {
			// System.out.println("onPrepareDialog --- ID_DIALOG_PROGRESS --------------");
			if (dialog instanceof ProgressDialog) {
				progressDialog = (ProgressDialog) dialog;
				if (query != null) {
					String q;
					if (query.length() > 1 && query.lastIndexOf("/") == query.length()-2) {
						q = query.substring(0, query.length()-2);
					} else {
						q = query;
					}
					//progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Searching_dialog_message, q)));
					progressDialog.setMessage(Locale.getMessage(R.string.Searching_dialog_message_plain, q));
				}
			}
		} // else {
			// System.out.println("onPrepareDialog -----------------");
			// }
	}

	@Override
	protected void onStop() {
		super.onStop();
		//UserTracker.getInstance().stopSession(this);
	}

	@Override
	protected void cancelProgressDialog() {
		if (searchTask != null
				&& searchTask.getStatus() == GMSAsyncTask.Status.RUNNING) {
			searchTask.cancel(true);
		}
		finish();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		LandmarkParcelable lm = searchTask.getLandmarks().get(position);

		UserTracker.getInstance().trackEvent("Clicks",
				getLocalClassName() + ".SearchLandmarkSelected", lm.getLayer(),
				0);

		ConfigurationManager.getInstance().putObject(
				ConfigurationManager.SEARCH_QUERY_RESULT,
				Integer.valueOf(lm.getKey()));

		finish();
	}

	private void callSearchTask() {
		searchTask = new SearchTask(this, query);
		searchTask.execute();
	}

	private void onTaskCompleted() {
		List<LandmarkParcelable> landmarkList = searchTask.getLandmarks();
		if (landmarkList.isEmpty()) {
			ConfigurationManager.getInstance().putObject(ConfigurationManager.SEARCH_QUERY_RESULT, -1);
			finish();
		} else {
			showSearchResults(landmarkList);
			// dismissDialog
			try {
				dismissDialog(ID_DIALOG_PROGRESS);
			} catch (Exception e) {
				// ignore error
			}
		}
	}

	private void showSearchResults(List<LandmarkParcelable> landmarkList) {

		setListAdapter(new LandmarkArrayAdapter(this, landmarkList));

		sort(ORDER_TYPE.ORDER_BY_REV, ORDER.DESC, false);

		list.setVisibility(View.VISIBLE);
		loading.setVisibility(View.GONE);
		setTitle(R.string.listSelection);
	}

	private void checkIfRepeatedQuery() {
		String q;
		if (query.length() > 2 && query.lastIndexOf("/") == query.length()-2) {
			q = query.substring(0, query.length()-2);
		} else {
			q = query;
		}
		
		List<String> recentQueries = (List<String>) ConfigurationManager.getInstance().getObject("RECENT_SEARCH_QUERIES", List.class);
		if (recentQueries == null) {
			if (!local) {
				recentQueries = new ArrayList<String>();
				recentQueries.add(q);
				ConfigurationManager.getInstance().putObject(
						"RECENT_SEARCH_QUERIES", recentQueries);
			}
		} else {
			for (Iterator<String> iter = recentQueries.iterator(); iter
					.hasNext();) {
				if (StringUtils.equalsIgnoreCase(iter.next(), q)) {
					local = true;
					break;
				}
			}
			if (!local) {
				recentQueries.add(query);
				ConfigurationManager.getInstance().putObject(
						"RECENT_SEARCH_QUERIES", recentQueries);
			}
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.search).setVisible(false);
		return true;
	}

	private class SearchTask extends GMSAsyncTask<Void, Integer, String> {

		private List<LandmarkParcelable> landmarkList = new ArrayList<LandmarkParcelable>();
		private GMSSearchActivity caller;
		private boolean completed;
		private String query;
	
		public SearchTask(GMSSearchActivity caller, String query) {
			super(1, SearchTask.class.getName());
			this.caller = caller;
			this.query = query;
		}

		private List<LandmarkParcelable> getLandmarks() {
			return landmarkList;
		}

		@Override
		protected void onPreExecute() {
			list.setVisibility(View.GONE);
			loading.setVisibility(View.VISIBLE);
			setTitle(R.string.Please_Wait);
			showDialog(ID_DIALOG_PROGRESS);
		}

		@Override
		protected String doInBackground(Void... arg0) {
			// don't save landmark names
			if (layerCodeIndex == -1) {
				SuggestionProviderUtil.saveRecentQuery(query, null);
			}

			String errorMessage = null;

			checkIfRepeatedQuery();

			if (!local) {
				errorMessage = searchServerAction(this);
			}

			if (!isCancelled()) {
				if (query != null) {
					
					int searchType = -1;
					if (StringUtils.endsWith(query, "/p")) {
						searchType = ConfigurationManager.PHRASE_SEARCH;
					} else if (StringUtils.endsWith(query, "/w") ){
						searchType = ConfigurationManager.WORDS_SEARCH;
					} else if (StringUtils.endsWith(query, "/f")) {
						searchType = ConfigurationManager.FUZZY_SEARCH;
					}
					
					//String q;
					//if (query.length() > 2 && query.lastIndexOf("/") == query.length()-2) {
					//	q = query.substring(0, query.length()-2);
					//} else {
					//	q = query;
					//} 
					
					if (type == TYPE.DEALS) {
						LandmarkManager.getInstance().searchDeals(landmarkList, query, null, MathUtils.coordIntToDouble(lat),
								MathUtils.coordIntToDouble(lng), searchType);
					} else if (type == TYPE.LANDMARKS) {
						LandmarkManager.getInstance().searchLandmarks(landmarkList, query, null, MathUtils.coordIntToDouble(lat),
								MathUtils.coordIntToDouble(lng), searchType);
					}
				}
			}

			return errorMessage;
		}

		@Override
		protected void onPostExecute(String errorMessage) {
			if (errorMessage != null) {
				LoggerUtils.debug("SearchTask error message: " + errorMessage);
			}
			completed = true;
			String message = null;
			if (landmarkList != null) {
				if (type == TYPE.DEALS) {
					message = Locale.getQuantityMessage(R.plurals.foundDeals, landmarkList.size());
				} else if (type == TYPE.LANDMARKS) {
					message = Locale.getQuantityMessage(R.plurals.foundLandmarks, landmarkList.size());
				}
				IntentsHelper.getInstance().showInfoToast(message);
			}
			notifyActivityTaskCompleted();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (values.length >= 2) {
				if (caller.progressDialog != null) {
					caller.progressDialog.setMessage(Locale.getMessage(R.string.Processing_results, values[0], values[1]));
				}
			}
		}

		private void setActivity(GMSSearchActivity activity) {
			this.caller = activity;
			if (completed) {
				notifyActivityTaskCompleted();
			}
		}
		
		private String getQuery() {
			return query;
		}

		private void notifyActivityTaskCompleted() {
			if (null != caller) {
				caller.onTaskCompleted();
			}
		}

		private String searchServerAction(GMSAsyncTask<?, ? ,?> task) {
			HttpUtils utils = new HttpUtils();
			String errorMessage = null;

			try {
				Map<String, String> params = new HashMap<String, String>();
				params.put("lat",StringUtil.formatCoordE6(MathUtils.coordIntToDouble(lat)));
				params.put("lng", StringUtil.formatCoordE6(MathUtils.coordIntToDouble(lng)));
				params.put("radius", Integer.toString(radius));				
				PackageInfo info = ConfigurationManager.getAppUtils().getPackageInfo();
				if (info != null) {
					params.put("version", Integer.toString(info.versionCode));
				}
				String q;
				if (query.length() > 2 && query.lastIndexOf("/") == query.length()-2) {
					q = query.substring(0, query.length()-2);
				} else {
					q = query;
				}
				params.put("query", URLEncoder.encode(q, "UTF-8"));
				if (query.endsWith("/a")) {
					params.put("geocode","1");
				} else if (query.endsWith("/c")) {
					params.put("geocode","2");
				}
				params.put("display",OsUtil.getDisplayType());
				if (type == TYPE.DEALS) {
					params.put("deals","1");
				} else if (type == TYPE.LANDMARKS) {
					if (ConfigurationManager.getInstance().isOn(ConfigurationManager.FB_AUTH_STATUS)) {
						ISocialUtils fbUtils = OAuthServiceFactory.getSocialUtils(Commons.FACEBOOK);
						String ftoken = fbUtils.getAccessToken().getToken();
						params.put("ftoken",URLEncoder.encode(ftoken, "UTF-8"));
					}
				}

				params.put("format", "bin");
				String url = ConfigurationManager.getInstance().getSecuredServerUrl() + "search";
				List<ExtendedLandmark> received = utils.loadLandmarkList(url, null, params, true, new String[]{"deflate", "application/x-java-serialized-object"});
				//call backup service if above fails
				if (utils.getResponseCode(url) >= 400 && received.isEmpty()) {
					url = ConfigurationManager.getInstance().getSecuredRHCloudUrl() + "search";
					received = utils.loadLandmarkList(url, null, params, true, new String[]{"deflate", "application/x-java-serialized-object"});
				}
				
				errorMessage = utils.getResponseErrorMessage(url);
				
				//process received landmark list
				int landmarkCount = received.size();
				if (landmarkCount > 0) {
					int i = 0; 	
					for (ExtendedLandmark landmark : received) {
						i++;
						publishProgress(i, landmarkCount); 
						String layer = landmark.getLayer();
						landmark.setSearchTerm(query);
						LandmarkManager.getInstance().getLandmarkStoreLayer(layer).add(landmark);
						LandmarkManager.getInstance().addLandmarkToDynamicLayer(landmark);
					}	 
				}
				//
				
			} catch (Exception ex) {
				LoggerUtils.error("GMSSearchActivity.SearchTask.searchServerAction() exception", ex);
				errorMessage = Locale.getMessage(R.string.Http_error, ex.getMessage());
			} finally {
				try {
					if (utils != null) {
						utils.close();
					}
				} catch (Exception e) {
				}
			}

			return errorMessage;
		}
	}
}
