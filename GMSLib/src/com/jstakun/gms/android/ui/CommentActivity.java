/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.AdsUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

/**
 *
 * @author jstakun
 */
public class CommentActivity extends Activity implements OnClickListener {

    private View commentButton, cancelButton;
    private EditText commentText;
    private String service, placeId, name;
    private Intents intents;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setTitle(R.string.commentTitle);
        setContentView(R.layout.comment);

        ActionBarHelper.setDisplayHomeAsUpEnabled(this);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            UserTracker.getInstance().startSession(this);
            UserTracker.getInstance().trackActivity(getClass().getName());

            intents = new Intents(this, null, null);

            service = extras.getString("service");
            placeId = extras.getString("placeId");
            name = extras.getString("name");

            initComponents();
        } else {
            finish();
        }
    }

    private void initComponents() {

        commentButton = findViewById(R.id.sendButton);
        cancelButton = findViewById(R.id.cancelButton);
        commentText = (EditText) findViewById(R.id.commentText);

        commentButton.setOnClickListener(CommentActivity.this);
        cancelButton.setOnClickListener(CommentActivity.this);

        AdsUtils.loadAd(this);
    }

    public void onClick(View v) {
        if (v == commentButton) {
            if (commentText.getText().length() >= 10) {
                AsyncTaskManager asyncTaskManager = ConfigurationManager.getInstance().getTaskManager();
                asyncTaskManager.executeSendCommentTask(service, placeId, commentText.getText().toString(), name);
                finish();
            } else {
                intents.showInfoToast(Locale.getMessage(R.string.Comment_empty_error));
            }
        } else if (v == cancelButton) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AdsUtils.destroyAdView(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        UserTracker.getInstance().stopSession();
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
}
