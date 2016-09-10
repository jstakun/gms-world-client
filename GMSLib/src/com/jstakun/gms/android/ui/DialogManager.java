package com.jstakun.gms.android.ui;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkManager;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.widget.ArrayAdapter;


/**
 *
 * @author jstakun
 */
public class DialogManager {

    private static DialogManager instance = new DialogManager();
    
    private DialogManager() {
    	
    }
    
    public static DialogManager getInstance() {
    	return instance;
    }
    	
	private DialogInterface.OnClickListener loginListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int item) {
            IntentsHelper.getInstance().startLoginActivity(item);
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
        }
    };
    
    private DialogInterface.OnClickListener packetDataListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
        	ConfigurationManager.getAppUtils().clearCounter();
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Counter_cleared));
        }
    };
    private DialogInterface.OnClickListener networkErrorListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            IntentsHelper.getInstance().startWifiSettingsActivity();
        }
    };
    private DialogInterface.OnClickListener locationErrorListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            IntentsHelper.getInstance().startLocationSettingsActivity();
        }
    };
    private DialogInterface.OnClickListener sendIntentListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            ExtendedLandmark selectedLandmark = LandmarkManager.getInstance().getSelectedLandmark();
            IntentsHelper.getInstance().startSendMessageIntent(id, selectedLandmark);
        }
    };
    private DialogInterface.OnClickListener checkinAutoListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            //add to favorites database
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            CheckinManager.getInstance().checkinAction(true, false, LandmarkManager.getInstance().getSeletedLandmarkUI());
        }
    };
    private DialogInterface.OnClickListener checkinManualListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            //don't add to favorites database
        	ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
        	CheckinManager.getInstance().checkinAction(false, false, LandmarkManager.getInstance().getSeletedLandmarkUI());
        }
    };
    private DialogInterface.OnClickListener rateUsListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            IntentsHelper.getInstance().startActionViewIntent(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL));
            ConfigurationManager.getInstance().setOn(ConfigurationManager.APP_RATED);
        }
    };
    private DialogInterface.OnClickListener newVersionListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            IntentsHelper.getInstance().startActionViewIntent(ConfigurationManager.getInstance().getString(ConfigurationManager.APP_URL));
        }
    };
    private DialogInterface.OnClickListener resetListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            dialog.cancel();
            ConfigurationManager.getAppUtils().reset();
            IntentsHelper.getInstance().showInfoToast(Locale.getMessage(R.string.Reset_confirmation));
        }
    };
   
    public void showAlertDialog(Activity activity, int type, ArrayAdapter<?> arrayAdapter, Spannable message) {
        AlertDialog alertDialog = null;

        if (activity != null && !activity.isFinishing()) {
            switch (type) {
            	case AlertDialogBuilder.INFO_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.INFO_DIALOG, null);
                    break;
                case AlertDialogBuilder.SAVE_ROUTE_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, new DialogHandler());
                    break;
                case AlertDialogBuilder.PACKET_DATA_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.PACKET_DATA_DIALOG, null, packetDataListener);
                    break;
                case AlertDialogBuilder.NETWORK_ERROR_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.NETWORK_ERROR_DIALOG, null, networkErrorListener);
                    break;
                case AlertDialogBuilder.LOGIN_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.LOGIN_DIALOG, arrayAdapter, loginListener);
                    break;
                case AlertDialogBuilder.SHARE_INTENTS_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.SHARE_INTENTS_DIALOG, arrayAdapter, sendIntentListener);
                    break;
                case AlertDialogBuilder.LOCATION_ERROR_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.LOCATION_ERROR_DIALOG, null, locationErrorListener);
                    break;
                case AlertDialogBuilder.RATE_US_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.RATE_US_DIALOG, null, rateUsListener);
                    break;
                case AlertDialogBuilder.NEW_VERSION_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.NEW_VERSION_DIALOG, null, newVersionListener);
                    break;
                case AlertDialogBuilder.AUTO_CHECKIN_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.AUTO_CHECKIN_DIALOG, null, checkinAutoListener, checkinManualListener);
                    break;
                case AlertDialogBuilder.RESET_DIALOG:
                    alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.RESET_DIALOG, null, resetListener);
                    break;  
                default:
                    break;
            }
        }

        if (alertDialog != null) {
            alertDialog.show();
        }
    }
    
    public void showTrackMyPosAlertDialog(Activity activity, DialogInterface.OnClickListener trackMyPosListener) {
    	if (activity != null && !activity.isFinishing()) {
            AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.TRACK_MYPOS_DIALOG, null, trackMyPosListener).show();
    	}
    }
    
    public void showRouteAlertDialog(Activity activity, Spannable message, final  Handler loadingHandler) {
    	if (activity != null && !activity.isFinishing()) {
    		ExtendedLandmark l = null;
    		if (message != null && message.toString().equals("dod") && ConfigurationManager.getInstance().containsObject("dod", ExtendedLandmark.class)) {
    			l = (ExtendedLandmark) ConfigurationManager.getInstance().getObject("dod", ExtendedLandmark.class); 
    		} else {
    			l = LandmarkManager.getInstance().getSeletedLandmarkUI();
    		}
    		final ExtendedLandmark landmark = l;
    		AlertDialog alertDialog = AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.ROUTE_DIALOG, null, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int id) {
    				dialog.cancel();
    				ConfigurationManager.getInstance().putInteger(ConfigurationManager.ROUTE_TYPE, id);
    				IntentsHelper.getInstance().startRouteLoadingTask(landmark, loadingHandler);
    			}
    		});
    		alertDialog.show();
    	}
    }
    
    public void showExitAlertDialog(final Activity activity) {
    	if (activity != null && !activity.isFinishing()) {
    		AlertDialogBuilder.getInstance().getAlertDialog(activity, AlertDialogBuilder.EXIT_DIALOG, null, new DialogInterface.OnClickListener() {

    	        public void onClick(DialogInterface dialog, int id) {
    	            ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
    	            dialog.cancel();
    	            ConfigurationManager.getInstance().putObject(ConfigurationManager.APP_CLOSING, new Object());
    	            activity.finish();
    	        }
    	    }).show();
    	}
    }

    public int dismissDialog(Activity activity) {
        return AlertDialogBuilder.getInstance().dismissDialog(activity);
    }
    
    private static class DialogHandler extends Handler {
    	
    	@Override
        public void handleMessage(Message msg) {
    		if (msg.what == AlertDialogBuilder.SAVE_ROUTE_DIALOG) {
    			AsyncTaskManager.getInstance().executeSaveRouteTask((String)msg.obj);
    		}
    	}
    }
}
