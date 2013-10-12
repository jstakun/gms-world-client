package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.jstakun.gms.android.social.OAuthServiceFactory;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.LoggerUtils;

public class SerialParser {

	private HttpUtils utils;
	protected SerialParser() {
		utils = new HttpUtils();
	}
	
	protected void close() {
        try {
            if (utils != null) {
                utils.close();
            }
        } catch (IOException ex) {
            LoggerUtils.debug("SerialParser.close() exception: ", ex);
        }
    }
	
    protected String parse(String url, List<ExtendedLandmark> landmarks, GMSAsyncTask<?,?,?> task, boolean close, String socialService) { 	
        String errorMessage = null;
        
        try {
        	URI uri = new URI(url);
        	List<ExtendedLandmark> received = utils.loadLandmarkList(uri, true, "deflate");
        	//TODO
        	//add lm to dynamic layers
            //if (landmarkManager != null) {
            //    landmarkManager.addLandmarkToDynamicLayer(lm);
            //}
        	if (landmarks.isEmpty()) {
        		landmarks.addAll(received);
        	} else {
        		landmarks.addAll(Collections2.filter(received, new ExistsPredicate(landmarks)));
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
    
    /*private class NotNullPredicate implements Predicate<ExtendedLandmark> {

    	public boolean apply(ExtendedLandmark landmark) {
            return (landmark != null);
        }
    }*/
}   