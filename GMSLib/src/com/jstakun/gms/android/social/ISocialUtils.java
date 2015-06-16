/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.social;

//import org.json.JSONArray;
import org.json.JSONObject;

import com.jstakun.gms.android.utils.Token;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;

/**
 *
 * @author jstakun
 */
public interface ISocialUtils {
    public boolean initOnTokenPresent(JSONObject json);
    public void storeAccessToken(Token accessToken);
    public Token getAccessToken();
    public void logout();
    public String sendPost(ExtendedLandmark landmark, int type);
    public String checkin(String placeId, String name, Double lat, Double lng);
    public String sendComment(String placeId, String message, String name);
    public String addPlace(String name, String desc, String category, double lat, double lng);
    public String getKey(String url);
}
