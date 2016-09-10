package com.jstakun.gms.android.ui;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.routes.RouteRecorder;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

/**
 *
 * @author jstakun
 */
public class AlertDialogBuilder {

    protected static final int EXIT_DIALOG = 0;
    public static final int INFO_DIALOG = 1;
    protected static final int TRACK_MYPOS_DIALOG = 2;
    public static final int SAVE_ROUTE_DIALOG = 3;
    public static final int PACKET_DATA_DIALOG = 4;
    public static final int NETWORK_ERROR_DIALOG = 5;
    public static final int LOGIN_DIALOG = 6;
    public static final int CHECKIN_DIALOG = 7;
    public static final int SHARE_INTENTS_DIALOG = 8;
    public static final int DEAL_OF_THE_DAY_DIALOG = 9;
    public static final int AUTO_CHECKIN_DIALOG = 10;
    public static final int LOCATION_ERROR_DIALOG = 11;
    public static final int ADD_LAYER_DIALOG = 12;
    public static final int RATE_US_DIALOG = 13;
    public static final int NEW_VERSION_DIALOG = 14;
    public static final int RESET_DIALOG = 15;
    protected static final int ROUTE_DIALOG = 16;
    public static final String OPEN_DIALOG = "openDialog";
    
    private static AlertDialogBuilder instance = new AlertDialogBuilder();
    
    private AlertDialogBuilder() {
    	
    }
    
    protected static AlertDialogBuilder getInstance() {
    	return instance;
    }
    
    private AlertDialog exitDialog, infoDialog, trackMyPosDialog, saveRouteDialog,
            packetDataDialog, networkErrorDialog, loginDialog, checkinDialog,
            shareIntentsDialog, autoCheckinDialog, locationErrorDialog, addLayerDialog,
            rateUsDialog, newVersionDialog, resetDialog, routeDialog;
    
    private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int id) {
            ConfigurationManager.getInstance().removeObject(OPEN_DIALOG, Integer.class);
            dialog.cancel();
        }
    };
    private DialogInterface.OnCancelListener dialogCancelListener = new DialogInterface.OnCancelListener() {

        public void onCancel(DialogInterface dialog) {
            ConfigurationManager.getInstance().removeObject(OPEN_DIALOG, Integer.class);
        }
    };

    private void createExitAlertDialog(Activity activity, DialogInterface.OnClickListener exitListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(Locale.getMessage(R.string.Close_app_prompt)).
                setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setPositiveButton(Locale.getMessage(R.string.okButton), exitListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        exitDialog = builder.create();
    }
    
    private void createRouteAlertDialog(Activity activity, DialogInterface.OnClickListener routeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(Locale.getMessage(R.string.Routes_title)).
                setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setItems(R.array.navigationType, routeListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        routeDialog = builder.create();
    }

    private void createInfoAlertDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final TextView text = new TextView(activity);
        text.setPadding(10, 5, 10, 5);
        String message = ConfigurationManager.getAppUtils().getAboutMessage();
        text.setText(message);
        Linkify.addLinks(text, Linkify.WEB_URLS);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setView(text).
                setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.about)
                .setPositiveButton(Locale.getMessage(R.string.okButton), dialogClickListener)
                .setOnCancelListener(dialogCancelListener);
        infoDialog = builder.create();
    }

    private void createTrackMyPosDialog(Activity activity, DialogInterface.OnClickListener trackMyPosListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String message;
        if (ConfigurationManager.getInstance().isOff(ConfigurationManager.FOLLOW_MY_POSITION)) {
            message = Locale.getMessage(R.string.Routes_TrackMyPosEnable);
        } else {
            message = Locale.getMessage(R.string.Routes_TrackMyPosDisable);
        }
        builder.setTitle(message).
                setIcon(android.R.drawable.ic_dialog_alert).
                setCancelable(true).
                setPositiveButton(Locale.getMessage(R.string.okButton), trackMyPosListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        trackMyPosDialog = builder.create();
    }

    private void createLoginDialog(Activity activity, ArrayAdapter<?> arrayAdapter, DialogInterface.OnClickListener loginListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
       
        builder.setTitle(R.string.login).
                setIcon(R.drawable.ic_dialog_menu_generic).
                setSingleChoiceItems(arrayAdapter, -1, loginListener).
                setCancelable(true).
                setOnCancelListener(dialogCancelListener);

        loginDialog = builder.create();
    }

    private void createSaveRouteDialog(Activity activity, final Handler saveRouteHandler) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View promptView = LayoutInflater.from(activity).inflate(R.layout.routename, null);
        final EditText input =  (EditText) promptView.findViewById(R.id.dialogRouteName);
        input.setHint(RouteRecorder.ROUTE_PREFIX);
        String message = Locale.getMessage(R.string.Routes_Recording_Save_Message);
        String title = Locale.getMessage(R.string.Routes_Recording_Save_Title);
        builder.setTitle(title).setMessage(message).setView(promptView).setCancelable(true).
                setPositiveButton(Locale.getMessage(R.string.okButton), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	ConfigurationManager.getInstance().removeObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
                        if (saveRouteHandler != null) {
                    		String filename = input.getText().toString();
                    		Message msg = new Message();
                    		msg.what = SAVE_ROUTE_DIALOG;
                    		msg.obj = filename;
                    		saveRouteHandler.sendMessage(msg);
                    	}
                        dialog.cancel();
                    }
                }).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        saveRouteDialog = builder.create();
    }

    private void createPacketDataAlertDialog(Activity activity, DialogInterface.OnClickListener packetDataListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String[] dd = ConfigurationManager.getAppUtils().formatCounter();
        String message = Locale.getMessage(R.string.Packet_data, dd[0], dd[1], dd[2]);
        builder.setMessage(message).
                setCancelable(true).
                setTitle(R.string.dataPacket).
                setIcon(android.R.drawable.ic_dialog_info).
                setPositiveButton(Locale.getMessage(R.string.Clear_Counter), packetDataListener).
                setNegativeButton(Locale.getMessage(R.string.okButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        packetDataDialog = builder.create();
    }

    private void createNetworkErrorDialog(Activity activity, DialogInterface.OnClickListener createNetworkListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(Locale.getMessage(R.string.Network_connection_error_message)).
                setTitle(Locale.getMessage(R.string.Network_connection_error_title)).
                setIcon(android.R.drawable.ic_dialog_alert).
                setCancelable(true).
                setPositiveButton(Locale.getMessage(R.string.okButton), createNetworkListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        networkErrorDialog = builder.create();
    }

    private void createLocationErrorDialog(Activity activity, DialogInterface.OnClickListener locationListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(Locale.getMessage(R.string.Location_connection_error_message)).
                setTitle(Locale.getMessage(R.string.Location_connection_error_title)).
                setIcon(android.R.drawable.ic_dialog_alert).
                setCancelable(true).
                setPositiveButton(Locale.getMessage(R.string.okButton), locationListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        locationErrorDialog = builder.create();
    }

    private void createCheckinDialog(Activity activity, DialogInterface.OnClickListener checkinListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.checkin)
                .setCancelable(true)
        .setIcon(R.drawable.ic_dialog_menu_generic)
        .setItems(R.array.checkin, checkinListener)
        .setOnCancelListener(dialogCancelListener);
        checkinDialog = builder.create();
    }

    private void createShareIntentsDialog(Activity activity, ArrayAdapter<?> arrayAdatper, DialogInterface.OnClickListener shareListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(Locale.getMessage(R.string.Landmark_share))
                .setIcon(R.drawable.ic_dialog_menu_generic)
                .setSingleChoiceItems(arrayAdatper, -1, shareListener)
                .setCancelable(true)
                .setOnCancelListener(dialogCancelListener);
        shareIntentsDialog = builder.create();
    }

    private void createAutoCheckinAlertDialog(Activity activity, DialogInterface.OnClickListener... onClickListeners) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(Locale.getMessage(R.string.autoCheckinTitle)).
                setIcon(android.R.drawable.ic_dialog_alert).
                setCancelable(false).
                setPositiveButton(Locale.getMessage(R.string.yesButton), onClickListeners[0]).
                setNegativeButton(Locale.getMessage(R.string.noButton), onClickListeners[1]);
        autoCheckinDialog = builder.create();
    }

    private void createAddLayerAlertDialog(Activity activity, DialogInterface.OnClickListener... addLayerListeners) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false).
                setIcon(android.R.drawable.ic_dialog_alert).
                setPositiveButton(Locale.getMessage(R.string.okButton), addLayerListeners[0]).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), addLayerListeners[1]);
        addLayerDialog = builder.create();
        addLayerDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
    }

    private void createRateUsAlertDialog(Activity activity, DialogInterface.OnClickListener rateListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        int useCount = ConfigurationManager.getInstance().getInt(ConfigurationManager.USE_COUNT);
        builder.setMessage(Locale.getMessage(R.string.rate_us_message, useCount)).
                setTitle(Locale.getMessage(R.string.rateUs)).
                setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_info).
                setPositiveButton(Locale.getMessage(R.string.okButton), rateListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        rateUsDialog = builder.create();
    }

    private void createNewVersionAlertDialog(Activity activity, DialogInterface.OnClickListener vListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(Locale.getMessage(R.string.New_version_long_message)).
                setTitle(Locale.getMessage(R.string.New_version_short_message)).
                setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_info).
                setPositiveButton(Locale.getMessage(R.string.okButton), vListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        newVersionDialog = builder.create();
    }
    
    private void createResetAlertDialog(Activity activity, DialogInterface.OnClickListener resetListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(Locale.getMessage(R.string.Reset_long_message)).
                setTitle(Locale.getMessage(R.string.Reset_short_message)).
                setCancelable(true).
                setIcon(android.R.drawable.ic_dialog_alert).
                setPositiveButton(Locale.getMessage(R.string.okButton), resetListener).
                setNegativeButton(Locale.getMessage(R.string.cancelButton), dialogClickListener).
                setOnCancelListener(dialogCancelListener);
        resetDialog = builder.create();
    }

    private AlertDialog getLoginDialog(Activity activity, ArrayAdapter<?> arrayAdapter, DialogInterface.OnClickListener loginListener) {
        createLoginDialog(activity, arrayAdapter, loginListener);
        return loginDialog;
    }

    private AlertDialog getExitAlertDialog(Activity activity, DialogInterface.OnClickListener exitListener) {
        if (exitDialog == null) {
            createExitAlertDialog(activity, exitListener);
        }
        return exitDialog;
    }
    
    private AlertDialog getRouteAlertDialog(Activity activity, DialogInterface.OnClickListener routeListener) {
        if (routeDialog == null) {
            createRouteAlertDialog(activity, routeListener);
        }
        return routeDialog;
    }

    private AlertDialog getTrackMyPosDialog(Activity activity, DialogInterface.OnClickListener trackMyPosListener) {
        createTrackMyPosDialog(activity, trackMyPosListener);
        return trackMyPosDialog;
    }

    private AlertDialog getInfoAlertDialog(Activity activity) {
        if (infoDialog == null) {
            createInfoAlertDialog(activity);
        }
        return infoDialog;
    }

    private AlertDialog getSaveRouteDialog(Activity activity, Handler saveRouteHandler) {
        if (saveRouteDialog == null) {
            createSaveRouteDialog(activity, saveRouteHandler);
        }
        return saveRouteDialog;
    }

    private AlertDialog getPacketDataAlertDialog(Activity activity, DialogInterface.OnClickListener packetDataListener) {
        createPacketDataAlertDialog(activity, packetDataListener);
        return packetDataDialog;
    }

    private AlertDialog getNetworkErrorDialog(Activity activity, DialogInterface.OnClickListener createNetworkListener) {
        if (networkErrorDialog == null) {
            createNetworkErrorDialog(activity, createNetworkListener);
        }
        return networkErrorDialog;
    }

    private AlertDialog getLocationErrorDialog(Activity activity, DialogInterface.OnClickListener locationListener) {
        if (locationErrorDialog == null) {
            createLocationErrorDialog(activity, locationListener);
        }
        return locationErrorDialog;
    }

    private AlertDialog getCheckinDialog(Activity activity, DialogInterface.OnClickListener checkinListener) {
        if (checkinDialog == null) {
            createCheckinDialog(activity, checkinListener);
        }
        return checkinDialog;
    }

    private AlertDialog getAutoCheckinDialog(Activity activity, DialogInterface.OnClickListener... checkinListeners) {
        if (autoCheckinDialog == null) {
            createAutoCheckinAlertDialog(activity, checkinListeners);
        }
        return autoCheckinDialog;
    }

    private AlertDialog getShareIntentsDialog(Activity activity, ArrayAdapter<?> arrayAdapter, DialogInterface.OnClickListener listener) {
        ConfigurationManager.getInstance().putObject(OPEN_DIALOG, SHARE_INTENTS_DIALOG);
        if (shareIntentsDialog == null) {
            createShareIntentsDialog(activity, arrayAdapter, listener);
        }
        return shareIntentsDialog;
    }

    private AlertDialog getAddLayerAlertDialog(Activity activity, DialogInterface.OnClickListener... addLayerListeners) {
        if (addLayerDialog == null) {
            createAddLayerAlertDialog(activity, addLayerListeners);
        }
        return addLayerDialog;
    }

    private AlertDialog getRateUsAlertDialog(Activity activity, DialogInterface.OnClickListener rateUsListener) {
        if (rateUsDialog == null) {
            createRateUsAlertDialog(activity, rateUsListener);
        }
        return rateUsDialog;
    }

    private AlertDialog getNewVersionAlertDialog(Activity activity, DialogInterface.OnClickListener vListener) {
        if (newVersionDialog == null) {
            createNewVersionAlertDialog(activity, vListener);
        }
        return newVersionDialog;
    }
    
    private AlertDialog getResetAlertDialog(Activity activity, DialogInterface.OnClickListener resetListener) {
        if (resetDialog == null) {
            createResetAlertDialog(activity, resetListener);
        }
        return resetDialog;
    }

    protected AlertDialog getAlertDialog(Activity activity, int type, ArrayAdapter<?> arrayAdapter, DialogInterface.OnClickListener... listeners) {
        AlertDialog alertDialog = null;
        ConfigurationManager.getInstance().putObject(OPEN_DIALOG, type);

        switch (type) {
            case EXIT_DIALOG:
                alertDialog = getExitAlertDialog(activity, listeners[0]);
                break;
            case INFO_DIALOG:
                alertDialog = getInfoAlertDialog(activity);
                break;
            case TRACK_MYPOS_DIALOG:
                alertDialog = getTrackMyPosDialog(activity, listeners[0]);
                break;
            case PACKET_DATA_DIALOG:
                alertDialog = getPacketDataAlertDialog(activity, listeners[0]);
                break;
            case NETWORK_ERROR_DIALOG:
                alertDialog = getNetworkErrorDialog(activity, listeners[0]);
                break;
            case LOCATION_ERROR_DIALOG:
                alertDialog = getLocationErrorDialog(activity, listeners[0]);
                break;
            case LOGIN_DIALOG:
                alertDialog = getLoginDialog(activity, arrayAdapter, listeners[0]);
                break;
            case CHECKIN_DIALOG:
                alertDialog = getCheckinDialog(activity, listeners[0]);
                break;
            case SHARE_INTENTS_DIALOG:
                alertDialog = getShareIntentsDialog(activity, arrayAdapter, listeners[0]);
                break;
            case AUTO_CHECKIN_DIALOG:
                alertDialog = getAutoCheckinDialog(activity, listeners);
                break;
            case ADD_LAYER_DIALOG:
                alertDialog = getAddLayerAlertDialog(activity, listeners);
                break;
            case RATE_US_DIALOG:
                alertDialog = getRateUsAlertDialog(activity, listeners[0]);
                break;
            case NEW_VERSION_DIALOG:
                alertDialog = getNewVersionAlertDialog(activity, listeners[0]);
                break;
            case RESET_DIALOG:
            	alertDialog = getResetAlertDialog(activity, listeners[0]);
                break;
            case ROUTE_DIALOG:
            	alertDialog = getRouteAlertDialog(activity, listeners[0]);
                break;
            default:
                break;
        }

        return alertDialog;
    }
    
    protected AlertDialog getAlertDialog(Activity activity, Handler saveRouteDialog) {
    	ConfigurationManager.getInstance().putObject(OPEN_DIALOG, SAVE_ROUTE_DIALOG);
    	return getSaveRouteDialog(activity, saveRouteDialog);	
    }

    protected int dismissDialog(Activity activity) {
        int type = -1;
        if (ConfigurationManager.getInstance().containsObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class)) {
            type = (Integer) ConfigurationManager.getInstance().getObject(AlertDialogBuilder.OPEN_DIALOG, Integer.class);
            if (type != DEAL_OF_THE_DAY_DIALOG && type != ADD_LAYER_DIALOG) {
                getAlertDialog(activity, type, null, dialogClickListener).dismiss();
            }
        }
        return type;
    }
}
