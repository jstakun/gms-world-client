/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.landmarks.Layer;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
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
public class AddLayerActivity extends Activity implements OnClickListener {

    private View addButton, cancelButton;
    private EditText nameText, keywordsText;
    private Intents intents = null;
    private String name;
    private static final int ID_DIALOG_PROGRESS = 0;
    private LandmarkManager landmarkManager;
    private static final String NAME = "name";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.addLayer);
        setContentView(R.layout.addlayer);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();

        //Object retained = getLastNonConfigurationInstance();
        //if (retained instanceof String) {
        //    name = (String) retained;
        //}
        
        if (savedInstanceState != null) {
        	name = savedInstanceState.getString(NAME);
        } 

        initComponents();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_PROGRESS) {
            //System.out.println("onCreateDialog------- ID_DIALOG_PROGRESS -------------------------");
            ProgressDialog progressDialog = new ProgressDialog(this);
            if (name != null) {
                progressDialog.setMessage(Html.fromHtml(Locale.getMessage(R.string.Layer_creating_dynamic, name)));
            } else {
                progressDialog.setMessage(Locale.getMessage(R.string.Please_Wait));
            }
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            return progressDialog;
        } else {
            //System.out.println("onCreateDialog--------------------------------");
            return super.onCreateDialog(id);
        }
    }

    //@Override
    //public Object onRetainNonConfigurationInstance() {
    //    return name;
    //}
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putString(NAME, name);
    }	

    private void initComponents() {
        addButton = findViewById(R.id.addButton);
        cancelButton = findViewById(R.id.cancelButton);
        keywordsText = (EditText) findViewById(R.id.keywordsText);
        nameText = (EditText) findViewById(R.id.nameText);

        intents = new Intents(this, null, null);

        AdsUtils.loadAd(this);

        addButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
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

    public void onClick(View v) {
        if (v == addButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".AddLayerAction", "", 0);
            addLayerAction();
        } else if (v == cancelButton) {
            UserTracker.getInstance().trackEvent("Clicks", getLocalClassName() + ".AddLayerCancelled", "", 0);
            finish();
        }
    }

    private void onTaskCompleted() {
        try {
            dismissDialog(ID_DIALOG_PROGRESS);
        } catch (Exception e) {
        }
        finish();
    }

    private void addLayerAction() {
        name = StringUtils.trimToNull(nameText.getText().toString());

        if (StringUtils.isNotEmpty(name)) {
            String keywords = keywordsText.getText().toString();

            //parse keywords to comma separated
            List<String> keywordsList = new ArrayList<String>();

            keywordsList.add(name);

            if (!StringUtils.isEmpty(keywords)) {
                String[] tokens = StringUtils.split(keywords, ",");
                for (int i = 0; i < tokens.length; i++) {
                    String token = StringUtils.trimToNull(tokens[i]);
                    if (token != null) {
                        keywordsList.add(token);
                    }
                }
            }

            String keywordsJoin = StringUtils.join(keywordsList, ",");

            boolean containsLayer = landmarkManager.getLayerManager().addDynamicLayer(keywordsJoin);

            if (containsLayer) {
                intents.showInfoToast(Locale.getMessage(R.string.Layer_exists_error));
            } else {
                //AsyncTaskExecutor.execute(new AddLayerTask(this, name, keywordsList.toArray(new String[keywordsList.size()])), this);
                new AddLayerTask(this, name, keywordsList.toArray(new String[keywordsList.size()])).execute();
            }
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.Layer_name_empty_error));
        }
    }

    private class AddLayerTask extends GMSAsyncTask<Void, Void, Void> {

        private AddLayerActivity activity;
        private String[] keywords;
        private String name;

        public AddLayerTask(AddLayerActivity caller, String name, String[] keywords) {
            super(1);
            this.activity = caller;
            this.name = name;
            this.keywords = keywords;
        }

        @Override
        protected void onPreExecute() {
            showDialog(ID_DIALOG_PROGRESS);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            List<LandmarkParcelable> results = new ArrayList<LandmarkParcelable>();
            landmarkManager.searchLandmarks(results, null, keywords, 0.0, 0.0, -1);
            Layer l = landmarkManager.getLayerManager().getLayer(name);
            if (l != null) {
                l.setCount(results.size());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void errorMessage) {
            intents.showInfoToast(Locale.getMessage(R.string.layerCreated));
            activity.onTaskCompleted();
        }
    }
}
