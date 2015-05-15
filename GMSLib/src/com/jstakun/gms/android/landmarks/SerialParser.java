package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.jstakun.gms.android.config.ConfigurationManager;
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
	
    protected String parse(String[] urls, int urlIndex, List<NameValuePair> params, List<ExtendedLandmark> landmarks, GMSAsyncTask<?,?,?> task, boolean close, String socialService, boolean removeIfExists) { 	
        String errorMessage = null;
        LandmarkManager landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        
        try {
        	URI uri = new URI(urls[urlIndex]);
        	if (!task.isCancelled()) {
        		List<ExtendedLandmark> received = utils.loadLandmarkList(uri, params, true, "deflate");
        		if (!received.isEmpty()) {
        			if (landmarks.isEmpty()) {
        				if (landmarkManager != null) {
        					//System.out.println("1. Indexing " + urls[0] + " " + received.size());
            				landmarkManager.addLandmarkListToDynamicLayer(received);
        				}
        				landmarks.addAll(received);
        			} else {
        				synchronized (landmarks) {
        					Collection<ExtendedLandmark> filtered = Collections2.filter(received, new ExistsPredicate(landmarks, removeIfExists, landmarkManager)); 
        					//System.out.println("2. Indexing " + urls[0] + " " + filtered.size());         				
        					if (landmarkManager != null) {
        						landmarkManager.addLandmarkListToDynamicLayer(filtered);
        					}
        					landmarks.addAll(filtered);
        				}
        			}
        		}
        	}
        } catch (Exception ex) {
            LoggerUtils.error("SerialParser.parse() exception: ", ex);
        } finally {
        	int responseCode = utils.getResponseCode();
        	if (responseCode == HttpStatus.SC_UNAUTHORIZED && socialService != null) {
        		ISocialUtils service = OAuthServiceFactory.getSocialUtils(socialService);
        		if (service != null) {
        			service.logout();
        		}
        	} else if (responseCode >= 500 && urlIndex+1 < urls.length) {
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
    	private LandmarkManager landmarkManager;
    	
    	public ExistsPredicate(List<ExtendedLandmark> source, boolean removeIfExists, LandmarkManager landmarkManager) {
    		this.source = source;
    		this.removeIfExists = removeIfExists;
    		this.landmarkManager = landmarkManager;
    	}
    	
        public boolean apply(ExtendedLandmark landmark) {
        	boolean decision = false;
        	if (landmark != null) {
        		if (removeIfExists) {
        			source.remove(landmark);
        			decision = true;
        			if (landmarkManager != null) {
        				landmarkManager.removeLandmarkFromDynamicLayer(landmark);
        			}
        		} else {
        			decision = !source.contains(landmark);
        		}
        	}
        	return decision;
        }
    }
}   