package com.jstakun.gms.android.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public class LandmarkListActivity extends AbstractLandmarkList {

    private AlertDialog deleteLandmarkDialog;
    private int currentPos = -1, requestCode = -1;
    public static final String LANDMARK = "landmark";
    private double lat, lng;
    private GetLandmarksTask getLandmarksTask;
    private View progress;
    private SOURCE source;

    public enum SOURCE {
        CATEGORY, LAYER, FRIENDS_CHECKINS, NEWEST, RECENT, DOD, CHECKIN, DAY_LANDMARKS, MY_LANDMARKS, MULTI_LANDMARK
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        progress = findViewById(R.id.listLoadingProgress);

        Intent intent = getIntent();
        requestCode = intent.getIntExtra("requestCode", -1);
        lat = intent.getDoubleExtra("lat", 0.0);
        lng = intent.getDoubleExtra("lng", 0.0);

        registerForContextMenu(getListView());

        createDeleteLandmarkAlertDialog();

        Object retained = getLastNonConfigurationInstance();
        if (retained instanceof GetLandmarksTask) {
            getLandmarksTask = (GetLandmarksTask) retained;
            getLandmarksTask.setActivity(this);
        } else {
            source = (SOURCE) intent.getSerializableExtra("source");
            if (source != null) {
                getLandmarksTask = new GetLandmarksTask(this);
                getLandmarksTask.execute();
                //AsyncTaskExecutor.execute(getLandmarksTask, this);
            } else {
            	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_search_empty_result));
                finish();
            }
        }

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (getLandmarksTask != null) {
            getLandmarksTask.setActivity(null);
        }
        return getLandmarksTask;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ConfigurationManager.getInstance().containsObject(ConfigurationManager.SEARCH_QUERY_RESULT, Integer.class)) {
            finish();
        }
    }

    @Override
    public boolean onSearchRequested() {
        Bundle appData = new Bundle();
        appData.putBoolean("local", true);
        startSearch(null, false, appData, false);
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        close(position, "load");
    }

    private void close(int position, String action) {
        Intent result = new Intent();
        UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".SelectedLandmark", getLandmarksTask.getLandmarks().get(position).getLayer(), 0);
        result.putExtra(LANDMARK, getLandmarksTask.getLandmarks().get(position).getKey());
        result.putExtra("action", action);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            currentPos = info.position;
            LandmarkParcelable landmark = getLandmarksTask.getLandmarks().get(info.position);
            menu.setHeaderTitle(landmark.getName());
            menu.setHeaderIcon(R.drawable.ic_dialog_menu_generic);
            String[] menuItems = getResources().getStringArray(R.array.landmarkContextMenu);
            boolean authStatus = IntentsHelper.getInstance().checkAuthStatus(landmark.getLayer());
            for (int i = 0; i < menuItems.length; i++) { //open, checkin, delete	
                if (i != 1 || authStatus) {
                	menu.add(Menu.NONE, i, Menu.NONE, menuItems[i]);
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();
            
        if (menuItemIndex == 0) {
            close(currentPos, "load");
        } else if (menuItemIndex == 1) {
        	int hashCode = getLandmarksTask.getLandmarks().get(currentPos).hashCode();
        	ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().findLandmarkById(hashCode);
        	if (selectedLandmark != null) {
        		boolean addToFavourites = ConfigurationManager.getInstance().isOn(ConfigurationManager.AUTO_CHECKIN) && !selectedLandmark.getLayer().equals(Commons.MY_POSITION_LAYER);
        		CheckinManager.getInstance().checkinAction(addToFavourites, false, selectedLandmark);
        	} else {
        		IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Unexpected_error));
        	}
        } else if (menuItemIndex == 2) {
        	deleteLandmarkDialog.setTitle(Locale.getMessage(R.string.Landmark_delete_prompt, getLandmarksTask.getLandmarks().get(currentPos).getName()));
            deleteLandmarkDialog.show();
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //System.out.println("Key pressed in activity: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getLandmarksTask != null && getLandmarksTask.isRunning()) {
                getLandmarksTask.cancel(true);
            }
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void createDeleteLandmarkAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                remove(currentPos);
            }
        }).setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        deleteLandmarkDialog = builder.create();
    }

    private void remove(int position) {
    	String ids = getLandmarksTask.getLandmarks().get(position).getKey();
        int id = Integer.parseInt(ids);
        String message = Locale.getMessage(R.string.Landmark_opening_error);
        ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getLandmarkToFocusQueueSelectedLandmark(id);
        if (selectedLandmark != null) {
            //System.out.println("Deleting landmark: " + selectedLandmark.getName());
        	int count = LandmarkManager.getInstance().removeLandmark(selectedLandmark);
            if (count >= 1 || count == -1) {
               ((ArrayAdapter<LandmarkParcelable>) getListAdapter()).remove(getLandmarksTask.getLandmarks().remove(position));
               message = Locale.getMessage(R.string.Landmark_deleted);
            } else {
               message = Locale.getMessage(R.string.Landmark_deleted_error);
            }
        }
        IntentsHelper.getInstance().showInfoToast(message);
    }

    private void onTaskPreExecute() {
        progress.setVisibility(View.VISIBLE);
        setTitle(Locale.getMessage(R.string.Please_Wait));
    }

    private void onTaskPostExecute() {
        //System.out.println("onPostExecute() start");
        if (getLandmarksTask.getLandmarks().isEmpty()) {
        	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Landmark_search_empty_result));
            finish();
        } else {
        	IntentsHelper.getInstance().showInfoToast(Locale.getQuantityMessage(R.plurals.foundLandmarks, getLandmarksTask.getLandmarks().size()));
            
        	if (requestCode == IntentsHelper.INTENT_MYLANDMARKS) {
                setTitle(R.string.listLocations);
            } else {
                setTitle(R.string.listSelection);
            }
            
            setListAdapter(new LandmarkArrayAdapter(this, getLandmarksTask.getLandmarks()));
        
            if (source == SOURCE.DOD) {
            	sort(ORDER_TYPE.ORDER_BY_CAT_STATS, ORDER.DESC, false);	
            } else if (source == SOURCE.FRIENDS_CHECKINS || source == SOURCE.NEWEST) {
            	sort(ORDER_TYPE.ORDER_BY_DATE, ORDER.DESC, false);	
            } else if (source == SOURCE.MY_LANDMARKS) {
            	sort(ORDER_TYPE.ORDER_BY_DIST, ORDER.ASC, false);
            } else if (StringUtils.equalsIgnoreCase(ConfigurationManager.getInstance().getString(ConfigurationManager.DEFAULT_SORT), "CD")) {
            	sort(ORDER_TYPE.ORDER_BY_CAT_STATS, ORDER.DESC, false);	
            } else {
            	//default sort by distance asc
            	sort(ORDER_TYPE.ORDER_BY_DIST, ORDER.ASC, false);
            }
            progress.setVisibility(View.GONE);
        }
    }

    private class GetLandmarksTask extends GMSAsyncTask<Void, Void, Void> {

        private List<LandmarkParcelable> landmarks = new ArrayList<LandmarkParcelable>();
        private LandmarkListActivity caller;

        public GetLandmarksTask(LandmarkListActivity caller) {
            super(1, GetLandmarksTask.class.getName());
            this.caller = caller;
        }

        private List<LandmarkParcelable> getLandmarks() {
            return landmarks;
        }

        private void setActivity(LandmarkListActivity activity) {
            this.caller = activity;
            if (caller != null) {
                if (isRunning()) {
                    caller.onTaskPreExecute();
                } else if (isFinished()) {
                    caller.onTaskPostExecute();
                }
            }
        }
        
        private boolean isRunning() {
            return getStatus() == GMSAsyncTask.Status.RUNNING;
        }
        
        private boolean isFinished() {
            return getStatus() == GMSAsyncTask.Status.FINISHED;
        }

        @Override
        protected void onPreExecute() {
            //System.out.println("onPreExecute() start");
            if (caller != null) {
                caller.onTaskPreExecute();
            }
            //System.out.println("onPreExecute() finish");
        }

        @Override
        protected Void doInBackground(Void... params) {
            Intent intent = getIntent();
            SOURCE source = (SOURCE) intent.getSerializableExtra("source");

            if (source == SOURCE.LAYER) {
                    String layer = intent.getStringExtra("layer");
                    LandmarkManager.getInstance().setLayerOnFocus(landmarks, layer, false, lat, lng);
            } else if (source == SOURCE.CATEGORY) {
                    String layer = intent.getStringExtra("layer");
                    if (layer != null) {
                    	LandmarkManager.getInstance().setLayerOnFocus(landmarks, layer, false, lat, lng);
                    } else {
                        int cat = intent.getIntExtra("category", -1);
                        if (cat != -1) {
                            int subCat = intent.getIntExtra("subcategory", -1);
                            LandmarkManager.getInstance().selectCategoryLandmarks(landmarks, cat, subCat, lat, lng);
                        }
                    }
            } else if (source == SOURCE.FRIENDS_CHECKINS) {
                	LandmarkManager.getInstance().findFriendsCheckinLandmarks(landmarks, lat, lng);
            } else if (source == SOURCE.NEWEST) {
                    String[] excluded = intent.getStringArrayExtra("excluded");
                    int maxDays = intent.getIntExtra("maxDays", 31);
                    LandmarkManager.getInstance().findNewLandmarks(landmarks, maxDays, excluded, lat, lng);
            } else if (source == SOURCE.RECENT) {
                	LandmarkManager.getInstance().getRecentlyOpenedLandmarks(landmarks, lat, lng);
            } else if (source == SOURCE.DOD) {
                    String[] excluded = intent.getStringArrayExtra("excluded");
                    LandmarkManager.getInstance().findDealsOfTheDay(landmarks, excluded, lat, lng);
            } else if (source == SOURCE.CHECKIN) {
                	LandmarkManager.getInstance().getCheckinableLandmarks(landmarks, lat, lng);
            } else if (source == SOURCE.DAY_LANDMARKS) {
                    int year = intent.getIntExtra("year", 0);
                    int month = intent.getIntExtra("month", 0);
                    int day = intent.getIntExtra("day", 0);
                    LandmarkManager.getInstance().findLandmarksInDay(landmarks, year, month, day, lat, lng);
            } else if (source == SOURCE.MY_LANDMARKS) {
                    LandmarkManager.getInstance().getMyLandmarks(landmarks, lat, lng);
                } else if (source == SOURCE.MULTI_LANDMARK) {
                	LandmarkManager.getInstance().getMultiLandmarks(landmarks, lat, lng);
            }
            
            return null;
        }

        @Override
        protected void onPostExecute(Void errorMessage) {
            //System.out.println("onPostExecute() start");
            if (caller != null) {
                caller.onTaskPostExecute();
            }
            //System.out.println("onPostExecute() finish");
        }
    }
}
