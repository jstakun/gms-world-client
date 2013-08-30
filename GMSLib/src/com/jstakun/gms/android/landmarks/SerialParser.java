package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.util.List;

import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;

public class SerialParser {

	private HttpUtils utils;
    
	public SerialParser() {
		utils = new HttpUtils();
	}
	
	public void close() {
        try {
            if (utils != null) {
                utils.close();
            }
        } catch (IOException ex) {
            LoggerUtils.debug("SerialParser.close() exception: ", ex);
        }
    }
	
    public String parse(String url, List<ExtendedLandmark> landmarks, GMSAsyncTask<?,?,?> task, boolean close) {
        
        String errorMessage = null;
        boolean hasJsonError = false;
        
        try {
            //System.out.println("Loading file " + url);
            Object reply = utils.loadObject(url, true, "x-java-serialized-object");
            if (reply instanceof List && !task.isCancelled()) {
            	landmarks.addAll((List<ExtendedLandmark>)reply);
            } 
        } catch (Exception ex) {
            LoggerUtils.error("SerialParser.parse() exception: ", ex);
        } finally {
            if (!hasJsonError) {
                errorMessage = utils.getResponseCodeErrorMessage();
            }
            if (close) {
                close();
            }
        }
        
        return errorMessage;
    }  
}   