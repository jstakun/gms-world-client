package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CalendarView;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class CalendarActivity extends Activity {
	
	CalendarView calendarView;
    private IntentsHelper intents;
    private double lat = 0.0d, lng = 0.0d;
    private static boolean isSearching = false;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);
        
        calendarView = (CalendarView) findViewById(R.id.calendar);
        
        calendarView.setOnDateChangeListener( new CalendarView.OnDateChangeListener() {
        	@Override
        	public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
			    if (!isSearching) {
			    	java.util.Locale currentLocale = ConfigurationManager.getInstance().getCurrentLocale();
			    	String date = DateTimeUtils.getShortDateString(view.getDate(), currentLocale);
			    	intents.showShortToast(Locale.getMessage(R.string.Searching_calendar_message, date));
			    	new GetLandmarksTask().execute(year, month, dayOfMonth);
			    }
		    }
		});
        
        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());
        
        intents = new IntentsHelper(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            lat = extras.getDouble("lat");
            lng = extras.getDouble("lng");
        }
        
        //Calendar cal = Calendar.getInstance();
        //cal.add(Calendar.YEAR, 1);
        //calendarView.setMaxDate(cal.getTimeInMillis());
        
        //cal.add(Calendar.YEAR, -2);
        //calendarView.setMinDate(cal.getTimeInMillis());
        
        //final ListView mListView = (ListView) findViewById(android.R.id.list);
        //adapter = (BaseAdapter) mListView.getAdapter();
        
        /*mListView.setClickable(true);
        mListView.setAddStatesFromChildren(true);
        mListView.setOnTouchListener(new CalendarTouchListener());  	
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
            	System.out.println("Data set changed ....................................");
            }
        });  
        mListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				System.out.println("OnItemClick " + position + "...");
				
			}
		});*/
        AdsUtils.loadAd(this);
    }    
	
	@Override
    protected void onPause() {
        super.onPause();
        isSearching = true;
    }
	
	@Override
    protected void onResume() {
        super.onResume();
        isSearching = false;
    }
	
	@Override
    protected void onStop() {
        super.onStop();
        //UserTracker.getInstance().stopSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == IntentsHelper.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getLandmarkToFocusQueueSelectedLandmark(id);
                    if (selectedLandmark != null) {
                        Intent result = new Intent();
                        result.putExtra(LandmarkListActivity.LANDMARK, ids);
                        result.putExtra("action", action);
                        setResult(RESULT_OK, result);
                        finish();
                    } else {
                        intents.showInfoToast(Locale.getMessage(R.string.Landmark_opening_error));
                    }
                }
            }
        }
    }
    
    /*private class CalendarTouchListener implements View.OnTouchListener {
    
    	//private final GestureDetector mGestureDetector;
    	
    	//public CalendarTouchListener() {
    	//	mGestureDetector = new GestureDetector(ConfigurationManager.getInstance().getContext(), new CalendarGestureListener());
    	//}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
        	//if (mGestureDetector.onTouchEvent(event)) {
        		//System.out.println("onSingleTapUp");
        		//intents.showInfoToast("Searching for landmarks for the day...");
        	//} else {
        		//System.out.println("onTouch");
        	//}
        	if (!isSearching) {
        		adapter.notifyDataSetChanged();
        	}
        	return false;
		}
    	
		//private class CalendarGestureListener extends GestureDetector.SimpleOnGestureListener {
        //    @Override
        //    public boolean onSingleTapUp(MotionEvent event) {
        //    	return true;
        //    }
        //}
    }*/
    
    private class GetLandmarksTask extends GMSAsyncTask<Integer, Void, Void> {

    	public GetLandmarksTask() {
            super(1, GetLandmarksTask.class.getName());
        }
    	
    	@Override
        protected void onPreExecute() {
    		isSearching = true;
	    	//System.out.println("Searching for landmarks for the day...");
    	}
    	
		@Override
		protected Void doInBackground(Integer... params) {
			UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CalendarCellAction", "", 0);
	    	intents.showLandmarksInDay(new double[]{lat, lng}, params[0], params[1], params[2]);
	    	return null;
		}
    	
		@Override
	    protected void onPostExecute(Void errorMessage) {
			//System.out.println("Finished searching for landmarks for the day...");
	    	isSearching = false; 
		}
    }
}
