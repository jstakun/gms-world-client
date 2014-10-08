package com.jstakun.gms.android.utils;

import java.io.IOException;

import org.acra.ACRA;

import android.util.Log;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class LoggerUtils {

    private static String TAG = "GMS World";

    public static void setTag(String tag) {
        TAG = tag;
    }
    
    public static String getTag() {
    	return TAG;
    }

    public static void debug(String msg) {
        //System.out.println(msg);
        if (isDebug()) {
            Log.d(TAG, msg);
        }
    }

    public static void error(String msg) {
        //System.out.println(msg);
        if (isError()) {
            Log.e(TAG, msg);
        }
    }

    public static void debug(String msg, Throwable t) {
        //System.out.println(msg);
        //t.printStackTrace();
        if (isDebug()) {
            Log.d(TAG, msg, t);
        }
    }

    public static void error(String msg, Throwable t) {
        //System.out.println(msg);
        //t.printStackTrace();
        if (isError()) {
            Log.e(TAG, msg, t);
        }
        //ifDebug() send crash report
        if (isDebug()) {
            ACRA.getErrorReporter().handleSilentException(t);
        }
    }
    
    private static boolean isError()
    {
        return (ConfigurationManager.getInstance().getInt(ConfigurationManager.LOG_LEVEL, 3) <= 3);
    }

    private static boolean isDebug()
    {
        return (ConfigurationManager.getInstance().getInt(ConfigurationManager.LOG_LEVEL, 3) == 0);
    }
    
    public static void saveLogcat(String filePath) {
    	if (isDebug()) {
        	try {
            	LoggerUtils.debug("Saving logcat to file " + filePath);
            	Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-f", filePath, "-v", "time"}); //, LoggerUtils.getTag() + ":V", "*:S"});
			} catch (IOException e) {
				error("LoggerUtils.saveLogcat() exception:", e);
			}
    	}	
    }
}
