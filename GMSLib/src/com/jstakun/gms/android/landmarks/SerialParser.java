package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.jstakun.gms.android.social.ISocialUtils;
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
	
    protected String parse(String[] urls, int urlIndex, Map<String, String> params, List<ExtendedLandmark> landmarks, GMSAsyncTask<?,?,?> task, boolean close, String socialService, boolean removeIfExists) { 	
        String errorMessage = null;
        
        try {
        	if (!task.isCancelled()) {
        		List<ExtendedLandmark> received = utils.loadLandmarkList(urls[urlIndex], params, true, new String[]{"deflate", "application/x-java-serialized-object"});
        		if (!received.isEmpty()) {
        			if (landmarks.isEmpty()) {
        				LandmarkManager.getInstance().addLandmarkListToDynamicLayer(received);
        				landmarks.addAll(received);
        			} else {
        				synchronized (landmarks) {
        					Collection<ExtendedLandmark> filtered = Collections2.filter(received, new ExistsPredicate(landmarks, removeIfExists)); 
        					//System.out.println("2. Indexing " + urls[0] + " " + filtered.size());         				
        					LandmarkManager.getInstance().addLandmarkListToDynamicLayer(filtered);
        					landmarks.addAll(filtered);
        				}
        			}
        		}
        	}
        } catch (Exception ex) {
            LoggerUtils.error("SerialParser.parse() exception: ", ex);
        } finally {
        	int responseCode = utils.getResponseCode();
        	if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && socialService != null) {
        		ISocialUtils service = OAuthServiceFactory.getSocialUtils(socialService);
        		if (service != null) {
        			service.logout();
        		}
        	} else if (responseCode >= 400 && urlIndex+1 < urls.length) {
        		return parse(urls, urlIndex+1, params, landmarks, task, close, socialService, removeIfExists);
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
    	private boolean removeIfExists = false;
    	
    	public ExistsPredicate(List<ExtendedLandmark> source, boolean removeIfExists) {
    		this.source = source;
    		this.removeIfExists = removeIfExists;
    	}
    	
        public boolean apply(ExtendedLandmark landmark) {
        	boolean decision = false;
        	if (landmark != null) {
        		if (removeIfExists) {
        			decision = true;
        			if (source.remove(landmark)) {
        				LandmarkManager.getInstance().removeLandmarkFromDynamicLayer(landmark);
        			}
        		} else {
        			decision = !source.contains(landmark);
        		}
        	}
        	return decision;
        }
    }
}   