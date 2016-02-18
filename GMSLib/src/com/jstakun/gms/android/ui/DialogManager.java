package com.jstakun.gms.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.Spannable;
import android.widget.ArrayAdapter;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;


/**
 *
 * @author jstakun
 */
public class DialogManager {

    private Activity activity;
    private AlertDialogBuilder dialogBuilder;
    private IntentsHelper intents;
    private AsyncTaskManager asyncTaskManager;
    private LandmarkManager landmarkManager;
    private CheckinManager checkinManager;
    private Handler loadingHandler;

    private DialogInterface.OnClickListener exitListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            ConfigurationManager.getInstance().putObject(ConfigurationManager.APP_CLOSING, new Object());
            activity.finish();
        }
    };
    private DialogInterface.OnClickListener trackMyPosListener;
    private DialogInterface.OnClickListener loginListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int item) {
            intents.startLoginActivity(item);
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
        }
    };
    private DialogInterface.OnClickListener saveRouteListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            asyncTaskManager.executeSaveRouteTask(activity.getString(R.string.saveRoute));
        }
    };
    private DialogInterface.OnClickListener packetDataListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            HttpUtils.clearCounter();
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            intents.showInfoToast(Locale.getMessage(R.string.Counter_cleared));
        }
    };
    private DialogInterface.OnClickListener networkErrorListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            intents.startWifiSettingsActivity();
        }
    };
    private DialogInterface.OnClickListener locationErrorListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            intents.startLocationSettingsActivity();
        }
    };
    private DialogInterface.OnClickListener sendIntentListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            ExtendedLandmark selectedLandmark = landmarkManager.getSelectedLandmark();
            intents.startSendMessageIntent(id, selectedLandmark);
        }
    };
    private DialogInterface.OnClickListener checkinAutoListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            //add to favourites database
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            checkinManager.checkinAction(true, false, landmarkManager.getSeletedLandmarkUI());
        }
    };
    private DialogInterface.OnClickListener checkinManualListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            //don't add to favourites database
        	ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            checkinManager.checkinAction(false, false, landmarkManager.getSeletedLandmarkUI());
        }
    };
    private DialogInterface.OnClickListener rateUsListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            intents.startActionViewIntent(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL));
            ConfigurationManager.getInstance().setOn(ConfigurationManager.APP_RATED);
        }
    };
    private DialogInterface.OnClickListener newVersionListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            intents.startActionViewIntent(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL));
        }
    };
    private DialogInterface.OnClickListener resetListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            ConfigurationManager.getAppUtils().reset();
            intents.showInfoToast(Locale.getMessage(R.string.Reset_confirmation));
        }
    };
   
    public DialogManager(Activity activity, IntentsHelper intents, AsyncTaskManager asyncTaskManager,
            LandmarkManager landmarkManager, CheckinManager checkinManager, DialogInterface.OnClickListener trackMyPosListener) {
        this.activity = activity;
        this.intents = intents;
        this.asyncTaskManager = asyncTaskManager;
        this.landmarkManager = landmarkManager;
        this.checkinManager = checkinManager;
        this.trackMyPosListener = trackMyPosListener;
        dialogBuilder = new AlertDialogBuilder(activity);
    }
    
    public DialogManager(Activity activity, IntentsHelper intents, AsyncTaskManager asyncTaskManager,
            LandmarkManager landmarkManager, CheckinManager checkinManager, Handler loadingHandler, DialogInterface.OnClickListener trackMyPosListener) {
        this(activity, intents, asyncTaskManager, landmarkManager, checkinManager, trackMyPosListener);
        this.loadingHandler = loadingHandler;
    }

    public void showAlertDialog(int type, ArrayAdapter<?> arrayAdapter, Spannable message) {
        AlertDialog alertDialog = null;

        if (!activity.isFinishing()) {
            switch (type) {
                case AlertDialogBuilder.EXIT_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.EXIT_DIALOG, null, exitListener);
                    break;
                case AlertDialogBuilder.INFO_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.INFO_DIALOG, null);
                    break;
                case AlertDialogBuilder.TRACK_MYPOS_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.TRACK_MYPOS_DIALOG, null, trackMyPosListener);
                    break;
                case AlertDialogBuilder.SAVE_ROUTE_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.SAVE_ROUTE_DIALOG, null, saveRouteListener);
                    break;
                case AlertDialogBuilder.PACKET_DATA_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.PACKET_DATA_DIALOG, null, packetDataListener);
                    break;
                case AlertDialogBuilder.NETWORK_ERROR_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.NETWORK_ERROR_DIALOG, null, networkErrorListener);
                    break;
                case AlertDialogBuilder.LOGIN_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.LOGIN_DIALOG, arrayAdapter, loginListener);
                    break;
                //case AlertDialogBuilder.CHECKIN_DIALOG:
                //    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.CHECKIN_DIALOG, null, checkinListener);
                //    break;
                case AlertDialogBuilder.SHARE_INTENTS_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.SHARE_INTENTS_DIALOG, arrayAdapter, sendIntentListener);
                    break;
                case AlertDialogBuilder.LOCATION_ERROR_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.LOCATION_ERROR_DIALOG, null, locationErrorListener);
                    break;
                case AlertDialogBuilder.RATE_US_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.RATE_US_DIALOG, null, rateUsListener);
                    break;
                case AlertDialogBuilder.NEW_VERSION_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.NEW_VERSION_DIALOG, null, newVersionListener);
                    break;
                case AlertDialogBuilder.AUTO_CHECKIN_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.AUTO_CHECKIN_DIALOG, null, checkinAutoListener, checkinManualListener);
                    break;
                case AlertDialogBuilder.RESET_DIALOG:
                    alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.RESET_DIALOG, null, resetListener);
                    break;  
                case AlertDialogBuilder.ROUTE_DIALOG:
                	alertDialog = dialogBuilder.getAlertDialog(AlertDialogBuilder.ROUTE_DIALOG, null, new DialogInterface.OnClickListener() {
    			        public void onClick(DialogInterface dialog, int id) {
    			        	ConfigurationManager.getInstance().putInteger(ConfigurationManager.ROUTE_TYPE, id);
    			        	intents.startRouteLoadingTask(landmarkManager.getSeletedLandmarkUI(), loadingHandler);
    			        }
    			    });
                	break;
                default:
                    break;
            }
        }

        if (alertDialog != null) {
            if (message != null) {
                alertDialog.setMessage(message);
            }
            alertDialog.show();
        }
    }

    public int dismissDialog() {
        if (dialogBuilder != null) {
            return dialogBuilder.dismissDialog();
        } else {
            return -1;
        }
    }
}
