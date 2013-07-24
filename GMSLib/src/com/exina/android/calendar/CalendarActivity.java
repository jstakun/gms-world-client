package com.exina.android.calendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.ActionBarHelper;
import com.jstakun.gms.android.ui.Intents;
import com.jstakun.gms.android.ui.LandmarkListActivity;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

public class CalendarActivity extends Activity implements CalendarView.OnCellTouchListener, OnClickListener {

    private CalendarView mView = null;
    private TextView currentMonth;
    private Handler mHandler = new Handler();
    private View prevMonth, nextMonth;
    private LandmarkManager landmarkManager = null;
    private Intents intents;
    private double lat = 0.0d, lng = 0.0d;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        mView = (CalendarView) findViewById(R.id.calendar);
        mView.setOnCellTouchListener(this);

        prevMonth = findViewById(R.id.leftArrow);
        prevMonth.setOnClickListener(this);

        nextMonth = findViewById(R.id.rightArrow);
        nextMonth.setOnClickListener(this);

        currentMonth = (TextView) findViewById(R.id.currentMonth);
        //currentMonth.setText(DateUtils.getMonthString(mView.getMonth(), DateUtils.LENGTH_LONG) + " " + mView.getYear());
        currentMonth.setText(DateTimeUtils.getYearMonth(mView.getYear(), mView.getMonth()));  
        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();

        intents = new Intents(this, landmarkManager, null);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            lat = extras.getDouble("lat");
            lng = extras.getDouble("lng");
        }

        AdsUtils.loadAd(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    public void onTouch(Cell cell) {
        UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".CalendarCellAction", "", 0);
        if (!(cell instanceof CalendarView.GrayCell)) {
            intents.showLandmarksInDay(new double[]{lat, lng}, mView.getYear(), mView.getMonth(), cell.getDayOfMonth());
        }

        if (cell instanceof CalendarView.GrayCell) {

            int day = cell.getDayOfMonth();

            if (day > 15) {
                mView.previousMonth();
            } else if (day < 15) {
                mView.nextMonth();
            } else {
                return;
            }

            mHandler.post(new Runnable() {

                public void run() {
                    //currentMonth.setText(DateUtils.getMonthString(mView.getMonth(), DateUtils.LENGTH_LONG) + " " + mView.getYear());
                	currentMonth.setText(DateTimeUtils.getYearMonth(mView.getYear(), mView.getMonth()));  
                }
            });
        }
    }

    public void onClick(View v) {
        if (v == prevMonth) {
            mView.previousMonth();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".PreviousMonthAction", "", 0);
            //currentMonth.setText(DateUtils.getMonthString(mView.getMonth(), DateUtils.LENGTH_LONG) + " " + mView.getYear());
            currentMonth.setText(DateTimeUtils.getYearMonth(mView.getYear(), mView.getMonth()));  
        } else if (v == nextMonth) {
            mView.nextMonth();
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".NextMonthAction", "", 0);
            //currentMonth.setText(DateUtils.getMonthString(mView.getMonth(), DateUtils.LENGTH_LONG) + " " + mView.getYear());
            currentMonth.setText(DateTimeUtils.getYearMonth(mView.getYear(), mView.getMonth()));  
        }
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
        if (requestCode == Intents.INTENT_MULTILANDMARK) {
            if (resultCode == RESULT_OK) {
                String action = intent.getStringExtra("action");
                String ids = intent.getStringExtra(LandmarkListActivity.LANDMARK);

                if (action.equals("load")) {
                    int id = Integer.parseInt(ids);
                    ExtendedLandmark selectedLandmark = landmarkManager.getLandmarkToFocusQueueSelectedLandmark(id);
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
}
