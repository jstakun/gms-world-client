package com.jstakun.gms.android.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.ads.AdsUtils;
import com.jstakun.gms.android.landmarks.LayerManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

/**
 *
 * @author jstakun
 */
public class AddLayerActivity extends Activity implements OnClickListener {

    private View addButton, cancelButton;
    private EditText nameText, keywordsText;
    private String name;
    private static final String NAME = "name";
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.addLayer);
        setContentView(R.layout.addlayer);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        if (savedInstanceState != null) {
        	name = savedInstanceState.getString(NAME);
        } 

        initComponents();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putString(NAME, name);
    }	

    private void initComponents() {
        addButton = findViewById(R.id.addButton);
        cancelButton = findViewById(R.id.cancelButton);
        keywordsText = (EditText) findViewById(R.id.keywordsText);
        nameText = (EditText) findViewById(R.id.nameText);

        AdsUtils.loadAd(this);

        addButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    //    UserTracker.getInstance().stopSession(this);
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

    /*private void onTaskCompleted() {
        try {
            dismissDialog(ID_DIALOG_PROGRESS);
        } catch (Exception e) {
        }
        finish();
    }*/

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

            boolean containsLayer = false;
            containsLayer = LayerManager.getInstance().addDynamicLayer(keywordsJoin);
          
            if (containsLayer) {
            	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Layer_exists_error));
            } else {
            	AsyncTaskManager.getInstance().executeIndexDynamicLayer(name, keywordsList.toArray(new String[keywordsList.size()]));
            	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.layerCreated));
                finish();
            }
        } else {
        	IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Layer_name_empty_error));
        }
    }
}
