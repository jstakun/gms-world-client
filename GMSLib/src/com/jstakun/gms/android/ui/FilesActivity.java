package com.jstakun.gms.android.ui;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.jstakun.gms.android.data.FileManager;
import com.jstakun.gms.android.data.PersistenceManagerFactory;
import com.jstakun.gms.android.landmarks.LandmarkParcelable;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.UserTracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 *
 * @author jstakun
 */
public class FilesActivity extends AbstractLandmarkList {

    private List<LandmarkParcelable> files;
    private int type = -1;
    private AlertDialog deleteFileDialog;
    private int currentPos = -1;
    private IntentsHelper intents;
    public static final int FILES = 0;
    public static final int ROUTES = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        searchButton.setVisibility(View.GONE);
        findViewById(R.id.searchButtonSeparator).setVisibility(View.GONE);
        ratingButton.setVisibility(View.GONE);
        findViewById(R.id.sortRatingSeparator).setVisibility(View.GONE);

        //UserTracker.getInstance().startSession(this);
        UserTracker.getInstance().trackActivity(getClass().getName());

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            files = extras.getParcelableArrayList("files");
            type = extras.getInt("type");

            intents = new IntentsHelper(this, null, null);

            String directory = PersistenceManagerFactory.getFileManager().getExternalDirectory(null, null).getAbsolutePath();
            if (directory != null) {

                if (type == ROUTES) {
                    directory += "/" + FileManager.getRoutesFolderPath();
                } else if (type == FILES) {
                    directory += "/" + FileManager.getFileFolderPath();
                }

                TextView topTextView = (TextView) findViewById(R.id.topTextView);
                topTextView.setVisibility(View.VISIBLE);
                topTextView.setText(directory);
                findViewById(R.id.topTextViewSeparator).setVisibility(View.VISIBLE);
            }

            if (type == ROUTES) {
                setTitle(Locale.getMessage(R.string.Routes_title));
            } else if (type == FILES) {
                setTitle(Locale.getMessage(R.string.Files_title));
            }

            registerForContextMenu(getListView());

            setListAdapter(new LandmarkArrayAdapter(this, files));

            sort(ORDER_TYPE.ORDER_BY_DATE, ORDER.DESC, false);

            createDeleteFileAlertDialog();
        } else {
            finish();
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        close(position, "load");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            currentPos = info.position;
            menu.setHeaderTitle(files.get(info.position).getName());
            menu.setHeaderIcon(R.drawable.ic_dialog_menu_generic);
            String[] menuItems = getResources().getStringArray(R.array.filesContextMenu);
            for (int i = 0; i < menuItems.length; i++) {
                menu.add(Menu.NONE, i, i, menuItems[i]);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int menuItemIndex = item.getItemId();

        if (menuItemIndex == 0) { //open
            close(currentPos, "load");
        } else if (menuItemIndex == 1) { //share
        	Uri uri = null;
        	if (type == ROUTES) {
        		uri = Uri.fromFile(PersistenceManagerFactory.getFileManager().getRouteFile(files.get(currentPos).getName()));
        	} else if (type == FILES) {
        		uri = Uri.fromFile(PersistenceManagerFactory.getFileManager().getPoiFile(files.get(currentPos).getName()));
        	}
        	if (uri != null) {
        		intents.shareFileAction(uri, type);
        	} else {
        		intents.showInfoToast(Locale.getMessage(R.string.Unexpected_error));
        	}
        } else if (menuItemIndex == 2) { //rename
        	String currentName = files.get(currentPos).getName();
        	showRenameFileDialog(currentName.substring(0, currentName.length()-4), currentPos);
        } else if (menuItemIndex == 3) { //delete
            deleteFileDialog.setTitle(Locale.getMessage(R.string.Files_delete_prompt, files.get(currentPos).getName()));
            deleteFileDialog.show();
        }

        return true;
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
    
    private void showRenameFileDialog(final String oldName, final int filePos) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View promptView = LayoutInflater.from(this).inflate(R.layout.routename, null);
        final EditText input =  (EditText) promptView.findViewById(R.id.dialogRouteName);
        input.setText(oldName);
        //TODO translate
        String message = "Enter new file name:";
        String title = "Rename file";
        builder.setTitle(title).setMessage(message).setView(promptView).setCancelable(true).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	dialog.cancel();
                    	String newName = input.getText().toString();
                    	if (StringUtils.isNotEmpty(newName)) { 
                    		newName += ".kml";
                    		//TODO implement file rename
                    		if (type == ROUTES) {
                    			PersistenceManagerFactory.getFileManager().renameRouteFile(oldName + ".kml", newName);
                    		} else if (type == FILES) {
                    			PersistenceManagerFactory.getFileManager().renamePoiFile(oldName + ".kml", newName);
                    		}
                    		//refresh files list
                    		((ArrayAdapter<LandmarkParcelable>) getListAdapter()).getItem(filePos).setName(newName);
                    		//TODO translate
                    		intents.showInfoToast("File renamed");
                    	} else {
                    		//TODO translate
                    		intents.showInfoToast("File name can't be empty!");
                    	}
                    }
                }).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    private void close(int position, String action) {
        Intent result = new Intent();
        result.putExtra("filename", files.get(position).getName());
        result.putExtra("type", type);
        result.putExtra("action", action);
        setResult(RESULT_OK, result);
        finish();
    }

    private void remove(int position) {
        FileManager fm = PersistenceManagerFactory.getFileManager();
        String filename = files.get(position).getName();
        ((ArrayAdapter<LandmarkParcelable>) getListAdapter()).remove(files.remove(position));

        if (type == ROUTES) {
            //delete route file
            fm.deleteRouteFile(filename);
        } else if (type == FILES) {
            //delete poi file
            fm.deletePoiFile(filename);
        }
        
        intents.showInfoToast(Locale.getMessage(R.string.Files_deleted));
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
