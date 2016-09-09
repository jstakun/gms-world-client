package com.jstakun.gms.android.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FavouritesDbDataSource;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class AutoCheckinListActivity extends AbstractLandmarkList {

    private List<LandmarkParcelable> favourites;
    private int currentPos = -1;
    
    private AlertDialog deleteFileDialog;
    
    private IntentsHelper intents = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        favourites = getIntent().getParcelableArrayListExtra("favourites");

        intents = new IntentsHelper(this, null);

        if (favourites != null) {

            UserTracker.getInstance().trackActivity(getClass().getName());

            setListAdapter(new LandmarkArrayAdapter(this, favourites));

            sort(ORDER_TYPE.ORDER_BY_DIST, ORDER.ASC, false);

            registerForContextMenu(getListView());

            searchButton.setVisibility(View.GONE);
            ratingButton.setVisibility(View.GONE);

            createDeleteFileAlertDialog();
            
            intents.showInfoToast(Locale.getQuantityMessage(R.plurals.foundLandmarks, favourites.size()));
        } else {
        	intents.showInfoToast(Locale.getMessage(R.string.autoCheckinListEmpty));
            finish();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        close(position);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            currentPos = info.position;
            menu.setHeaderTitle(favourites.get(info.position).getName());
            menu.setHeaderIcon(R.drawable.ic_dialog_menu_generic);
            String[] menuItems = getResources().getStringArray(R.array.filesContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
            menu.getItem(ACTION_SHARE).setVisible(false);
            menu.getItem(ACTION_RENAME).setVisible(false);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();

        if (menuItemIndex == ACTION_OPEN) {
            close(currentPos);
        } else if (menuItemIndex == ACTION_DELETE) {
            deleteFileDialog.setTitle(Locale.getMessage(R.string.Landmark_delete_prompt, favourites.get(currentPos).getName()));
            deleteFileDialog.show();
        }

        return true;
    }

    private void close(int position) {
        Intent result = new Intent();
        result.putExtra("favourite", favourites.get(position).hashCode());
        setResult(RESULT_OK, result);
        finish();
    }

    private void createDeleteFileAlertDialog() {
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
        deleteFileDialog = builder.create();
    }

    private void remove(int position) {
        //delete from db
        int id = favourites.get(position).hashCode();
        FavouritesDbDataSource fdb = ConfigurationManager.getDatabaseManager().getFavouritesDatabase();
        int response = fdb.deleteLandmark(id);

        if (response > 0) {
            ((ArrayAdapter<LandmarkParcelable>) getListAdapter()).remove(favourites.remove(position));
            intents.showInfoToast(Locale.getMessage(R.string.Landmark_deleted));
        } else {
            intents.showInfoToast(Locale.getMessage(R.string.Landmark_deleted_error));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //UserTracker.getInstance().stopSession(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.search).setVisible(false);
        menu.findItem(R.id.sortRating).setVisible(false);
        return true;
    }
}
