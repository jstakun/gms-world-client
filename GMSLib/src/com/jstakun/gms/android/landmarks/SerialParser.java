package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.jstakun.gms.android.social.OAuthServiceFactory;
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
	
    public String parse(String url, List<ExtendedLandmark> landmarks, GMSAsyncTask<?,?,?> task, boolean close, String socialService) {
        
        String errorMessage = null;
        
        try {
            //System.out.println("Loading file " + url);
            Object reply = utils.loadObject(url, true, "x-java-serialized-object");
            if (reply instanceof List && !task.isCancelled()) {
            	//deduplicate
            	List<ExtendedLandmark> received = (List<ExtendedLandmark>)reply;
            	if (landmarks.isEmpty()) {
            		landmarks.addAll(Collections2.filter(received, new NotNullPredicate()));
            	} else {
            		landmarks.addAll(Collections2.filter(received, new ExistsPredicate(landmarks)));
            	}
            } 
        } catch (Exception ex) {
            LoggerUtils.error("SerialParser.parse() exception: ", ex);
        } finally {
        	int responseCode = utils.getResponseCode();
        	if (responseCode == 401 && socialService != null) {
        		OAuthServiceFactory.getSocialUtils(socialService).logout();
        	}
            errorMessage = utils.getResponseCodeErrorMessage();
            if (close) {
                close();
            }
        }
        
        return errorMessage;
    }  
    
    private class ExistsPredicate implements Predicate<ExtendedLandmark> {

    	private List<ExtendedLandmark> source;
    	
    	public ExistsPredicate(List<ExtendedLandmark> source) {
    		this.source = source;
    	}
    	
        public boolean apply(ExtendedLandmark landmark) {
            return (landmark != null && !source.contains(landmark));
        }
    }
    
    private class NotNullPredicate implements Predicate<ExtendedLandmark> {

    	public boolean apply(ExtendedLandmark landmark) {
            return (landmark != null);
        }
    }
}   