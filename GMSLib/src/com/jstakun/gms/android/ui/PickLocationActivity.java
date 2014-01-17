/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

/**
 *
 * @author jstakun
 */
public class PickLocationActivity extends Activity implements OnClickListener {

    private EditText locationAddressText;
    private View pickButton, cancelButton, loading, form;
    private Spinner locationCountrySpinner;
    private String country, lat, lng, name, message;
    private static final int ID_DIALOG_PROGRESS = 0;
    private static final String DEFAULT_NAME = "unknown";
    private static final String NAME = "name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.pickMyPos);
        setContentView(R.layout.picklocation);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        if (savedInstanceState != null) {
        	name = savedInstanceState.getString(NAME);
        } 
        
        if (name == null) {
        	name = DEFAULT_NAME;
        }
        
        initComponents();
    }

    private void initComponents() {
        pickButton = findViewById(R.id.locationPickButton);
        cancelButton = findViewById(R.id.locationCancelButton);
        locationAddressText = (EditText) findViewById(R.id.locationAddressText);

        loading = findViewById(R.id.mapCanvasWidgetL);
        form = findViewById(R.id.pickLocationForm);

        locationCountrySpinner = (Spinner) findViewById(R.id.locationCountrySpinner);
        ArrayAdapter<?> adapter = ArrayAdapter.createFromResource(this, R.array.countries, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationCountrySpinner.setAdapter(adapter);
        locationCountrySpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());
        
        String iso3Country = ConfigurationManager.getInstance().getString(ConfigurationManager.ISO3COUNTRY);
        if (StringUtils.isEmpty(iso3Country)) {
            iso3Country = "USA";
        }
        String[] iso3countryCodes = getResources().getStringArray(R.array.iso3countryCodes);
        int selection = 0;

        for (int i = 0; i < iso3countryCodes.length; i++) {
            if (iso3countryCodes[i].equals(iso3Country)) {
                selection = i;
                break;
            }
        }

        locationCountrySpinner.setSelection(selection);
        country = (String)adapter.getItem(selection); 
        AdsUtils.loadAd(this);

        pickButton.setOnClickListener(PickLocationActivity.this);
        cancelButton.setOnClickListener(PickLocationActivity.this);

        locationAddressText.setOnEditorActionListener(new OnEditorActionListener() {

            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    String address = locationAddressText.getText().toString();
                    name = country;
                    if (StringUtils.isNotEmpty(address)) {
                        name += "," + StringUtils.trimToEmpty(address);
                    }
                    new PickLocationTask().execute();
                    return true;
                }
                return false;
            }
        });
    }

    public void onClick(View v) {
        if (v == pickButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".PickLocationAction", "", 0);
            loading.setVisibility(View.VISIBLE);
            form.setVisibility(View.GONE);
            //hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(locationAddressText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

            String address = locationAddressText.getText().toString();
            name = country;
            if (StringUtils.isNotEmpty(address)) {
                name += "," + StringUtils.trimToEmpty(address);
            }
            new PickLocationTask().execute();
        } else if (v == cancelButton) {
            cancelActivity();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //System.out.println("Key pressed in activity: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            cancelActivity();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_PROGRESS) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            if (name != null) {
                progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Searching_dialog_message, name)));
            } else {
                progressDialog.setMessage(Locale.getMessage(R.string.Please_Wait));
            }
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setOnCancelListener(new OnCancelListener() {

                public void onCancel(DialogInterface arg0) {
                    cancelActivity();
                }
            });
            progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            return progressDialog;
        } else {
            return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == ID_DIALOG_PROGRESS) {
            if (dialog instanceof ProgressDialog) {
                ProgressDialog progressDialog = (ProgressDialog) dialog;
                if (name != null) {
                    progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Searching_dialog_message, name)));
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putString(NAME, name);
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
            cancelActivity();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void cancelActivity() {
        Intent result = new Intent();
        if (StringUtils.equals(name, PickLocationActivity.DEFAULT_NAME)) {
        	result.putExtra("message", Locale.getMessage(R.string.Action_canceled));
        } else {
        	result.putExtra("message", Locale.getMessage(R.string.Pick_location_failed_error, name, message));
        }
        setResult(RESULT_CANCELED, result);
        finish();
    }

    private class PickLocationTask extends GMSAsyncTask<Void, Void, Void> {

        public PickLocationTask() {
            super(1);
        }
        
        @Override
        protected void onPreExecute() {
        	try {
        		showDialog(ID_DIALOG_PROGRESS);
        	} catch (Exception e) {
        		LoggerUtils.error("PickLocationTask.onPreExecute() exception", e);
            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            pickLocationAction();
            return null;
        }

        @Override
        protected void onPostExecute(Void res) {
            try {
                dismissDialog(ID_DIALOG_PROGRESS);
            } catch (Exception e) {
                //ignore error
            }
            Intent result = new Intent();
            if (lat != null && lng != null) {
                result.putExtra("lat", lat);
                result.putExtra("lng", lng);
                result.putExtra("name", name);
                setResult(RESULT_OK, result);
            } else {
                result.putExtra("message", message);
                result.putExtra("name", name);
                setResult(RESULT_CANCELED, result);
            }
            finish();
        }
    }

    private void pickLocationAction() {

        HttpUtils utils = new HttpUtils();
        
        try {
        	List<NameValuePair> params = new ArrayList<NameValuePair>();
        	String address = locationAddressText.getText().toString();
            name = country;
            if (StringUtils.isNotEmpty(address)) {
                name += "," + StringUtils.trimToEmpty(address);
            }

            params.add(new BasicNameValuePair("address", name));
            
            String email = ConfigurationManager.getUserManager().getUserEmail();
            if (StringUtils.isNotEmpty(email)) {
              	params.add(new BasicNameValuePair("email", email));
            }

            String url = ConfigurationManager.getInstance().getServicesUrl() + "geocode";

            utils.sendPostRequest(url, params, true);
            
            String response = utils.getPostResponse();
			if (StringUtils.startsWith(response, "{")) {
               JSONObject json = new JSONObject(response);
               LoggerUtils.debug("Geocode response: " + response);

               if (json.getString("status").equals("OK")) {
                  lat = json.getString("lat");
                  lng = json.getString("lng");
                  String type = json.getString("type");
                  if (type.equals("l")) {
                     name = address;
                  }
               } else {
                  message = Locale.getMessage(R.string.Http_error, json.getString("message"));
               }              
            } else {
                message = utils.getResponseCodeErrorMessage();
            }
        } catch (Exception ex) {
            LoggerUtils.error("PickLocationActivity.pickLocationAction() exception", ex);
            message = Locale.getMessage(R.string.Http_error, ex.getMessage());
        } finally {
            try {
                if (utils != null) {
                    utils.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private class MyOnItemSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            country = parent.getItemAtPosition(pos).toString();
            //System.out.println("You have selected: " + parent.getItemAtPosition(pos).toString());
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
}
