/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.DateUtils;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.deals.Deal;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.DateTimeUtils;
import com.jstakun.gms.android.utils.GMSAsyncTask;
import com.jstakun.gms.android.utils.HttpUtils;
import com.jstakun.gms.android.utils.Locale;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.StringUtil;
import com.openlapi.AddressInfo;
import com.openlapi.QualifiedCoordinates;

/**
 *
 * @author jstakun
 */
public class JSONParser {
    
    private HttpUtils utils;
    private long creationDate = 0, endDate = 0;
    private int numberOfReviews = 0;
    private double rating = -1.0d;
    private NumberFormat nf, pf;
    private java.util.Locale locale;
    private boolean hasCheckinOrPhoto = false;
    private String thumbnail;
    private LandmarkManager landmarkManager;
    
    public JSONParser() {
        utils = new HttpUtils();
        locale = ConfigurationManager.getInstance().getCurrentLocale();
        landmarkManager = ConfigurationManager.getInstance().getLandmarkManager();
        
        if (locale != null) {
            nf = NumberFormat.getNumberInstance(locale);
            nf.setMaximumFractionDigits(2);
            pf = NumberFormat.getPercentInstance(locale);
        } else {
            nf = NumberFormat.getNumberInstance();
            pf = NumberFormat.getPercentInstance();
        }
    }
    
    public String parse(String url, List<ExtendedLandmark> landmarks, String layer, String[] urlPrefix, int defaultCategory, int defaultSubcategory, GMSAsyncTask<?,?,?> task, boolean close, int cacheLimit) {
        
        String errorMessage = null;
        boolean hasJsonError = false;
        
        try {
            //load
            //long start = System.currentTimeMillis();
            //System.out.println("Loading file " + url);
            byte[] resp = utils.loadHttpFile(url, true, "json");
            //long end = System.currentTimeMillis();
            //System.out.println("File " + url + " loaded in " + (end - start) + " millis at " + end);
            if (resp != null && resp.length > 0 && !task.isCancelled()) {
                //long start = System.currentTimeMillis();
                String jsonResp = new String(resp, "UTF-8");
                //System.out.println("Json: " + jsonResp);
                
                if (jsonResp.startsWith("{")) {
                    
                    JSONObject jsonRoot = new JSONObject(jsonResp);
                    
                    JSONArray resultSet = jsonRoot.optJSONArray("ResultSet");
                    if (resultSet != null && resultSet.length() > 0) {
                        //extract
                        //1. task - must be called in UI thread
                        //JSonParserAsyncTask extractTask = new JSonParserAsyncTask(resultSet, landmarks, layer, urlPrefix, defaultCategory, defaultSubcategory, task, cacheLimit);
                        //extractTask.execute();
                        //2. thread 
                        Thread parser = new Thread(new JSonParserTask(resultSet, landmarks, layer, urlPrefix, defaultCategory, defaultSubcategory, task, cacheLimit));
                        parser.start();
                        //3. method
                        //parseJSonArray(resultSet, landmarks, layer, urlPrefix, defaultCategory, defaultSubcategory, task, cacheLimit, null);
                    } else if (jsonRoot.has("error")) {
                        JSONObject error = jsonRoot.getJSONObject("error");
                        errorMessage = error.optString("message");
                        if (errorMessage != null) {
                            hasJsonError = true;
                        }
                    }
                } else {
                    LoggerUtils.error("Wrong json content: " + jsonResp);
                }
                //System.out.println("Processed layer " + layer + " containing " + landmarks.size() + " in " + (System.currentTimeMillis() - start) + " millis.");
            }
        } catch (Exception ex) {
            LoggerUtils.error("JSonParser.parse exception: ", ex);
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
    
    public void close() {
        try {
            if (utils != null) {
                utils.close();
            }
        } catch (IOException ex) {
            LoggerUtils.debug("JSonParser error: ", ex);
        }
    }
    
    public void parseJSonArray(JSONArray jsonLandmarks, List<ExtendedLandmark> landmarks, String layer, String[] urlPrefix, int defaultCategory, int defaultSubcategory, GMSAsyncTask<?,?,?> task, int cacheLimit, String searchTerm) throws JSONException {
        List<ExtendedLandmark> localCache = new ArrayList<ExtendedLandmark>(cacheLimit);
        List<ExtendedLandmark> origLandmarks = new ArrayList<ExtendedLandmark>(landmarks);

        //long start = System.currentTimeMillis();
        //System.out.println("Starting processing landmarks from layer " + layer + ", size " + jsonLandmarks.length());

        QualifiedCoordinates tmp = new QualifiedCoordinates(Double.NaN, Double.NaN, Float.NaN, Float.NaN, Float.NaN);
        int length = jsonLandmarks.length();
        for (int i = 0; i < length; i++) {
            //System.out.println("Processing landmark (" + i + "/" + jsonLandmarks.length() + ") from layer " + layer + "...");
            if (task != null && task.isCancelled()) {
                break;
            } else {
                if (!jsonLandmarks.isNull(i)) {
                    JSONObject landmark = (JSONObject) jsonLandmarks.get(i);
                    String name = landmark.optString("name", Locale.getMessage(R.string.label_empty));
                    double lat = landmark.getDouble("lat");
                    double lng = landmark.getDouble("lng");
                    tmp.setLatitude(lat);
                    tmp.setLongitude(lng);
                    
                    if (origLandmarks.isEmpty() || !origLandmarks.contains(LandmarkFactory.getLandmark(StringUtils.trimToEmpty(name), null, tmp, layer, -1))) {
                        String desc = "";
                        int categoryId = defaultCategory;
                        int subCategoryId = defaultSubcategory;
                        creationDate = 0;
                        endDate = 0;
                        rating = -1.0d;
                        numberOfReviews = 0;
                        hasCheckinOrPhoto = false;
                        thumbnail = null;
                        AddressInfo address = new AddressInfo();
                        Deal deal = new Deal(-1, -1, -1, null, null);
                        String checkins = null;

                        //api v2
                        String url = landmark.optString("url");
                        if (url != null) {
                            //String urlS = landmark.getString("url");
                            int urlType = landmark.optInt("urlType", 0);
                            if (urlPrefix != null && urlPrefix.length > urlType) {
                                url = urlPrefix[urlType] + url;
                            }
                            if (StringUtils.isNotEmpty(url)) {
                                address.setField(AddressInfo.URL, url);
                            }
                            
                            subCategoryId = landmark.optInt("subcategoryID", defaultSubcategory);
                            
                            categoryId = landmark.optInt("categoryID", defaultCategory);

                            //Google Maps reference
                            String reference = landmark.optString("reference");
                            if (reference != null) {
                                address.setField(AddressInfo.EXTENSION, reference);
                            }

                            //checkins FB, FS
                            JSONObject checkinsJson = landmark.optJSONObject("checkins");
                            if (checkinsJson != null) {
                                checkins = "";
                                for (Iterator<String> iter = checkinsJson.keys(); iter.hasNext();) {
                                    String checkinUser = iter.next();
                                    long checkinDate = checkinsJson.getLong(checkinUser);
                                    if (checkins.length() > 0) {
                                        checkins += ", ";
                                    }
                                    checkins += Locale.getMessage(R.string.checkinUser, checkinUser,
                                            DateUtils.getRelativeTimeSpanString(checkinDate, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
                                }
                                hasCheckinOrPhoto = true;
                            }
                            
                            if (landmark.has("desc")) {
                                Object descr = landmark.get("desc");
                                if (descr instanceof JSONObject) {
                                    JSONObject description = (JSONObject) descr;
                                    //System.out.println("parseJSonObject landmark: " + layer);
                                    List<String> res = parseJSonObject(address, toMap(description), deal, categoryId, checkins, layer);
                                    if (!res.isEmpty()) {
                                        desc = StringUtil.join(res, "<br/>");
                                    }
                                } else if (descr instanceof String) {
                                    String descs = (String) descr;
                                    if (descs.length() > 0) {
                                        desc = String.format(Locale.getMessage(R.string.description, descs));
                                    }
                                }
                            }
                        } else { //api v1
                            desc = landmark.getString("desc");
                        }
                        
                        ExtendedLandmark lm = LandmarkFactory.getLandmark(StringUtils.trimToEmpty(name), StringUtils.trimToEmpty(desc),
                                new QualifiedCoordinates(lat, lng, Float.NaN, Float.NaN, Float.NaN), layer, address, creationDate, searchTerm);
                        
                        if (categoryId != -1) {
                            lm.setCategoryId(categoryId);
                        }
                        if (subCategoryId != -1) {
                            lm.setSubCategoryId(subCategoryId);
                        }
                        
                        lm.setHasCheckinsOrPhotos(hasCheckinOrPhoto);
                        
                        if (deal.getPrice() > 0 || StringUtils.isNotEmpty(deal.getDealType()) || categoryId > 0) {
                            lm.setDeal(deal);
                        }
                        
                        if (rating >= 0) {
                            lm.setRating(rating);
                        }
                        
                        if (numberOfReviews > 0) {
                            lm.setNumberOfReviews(numberOfReviews);
                        }
                        
                        if (thumbnail != null) {
                            lm.setThumbnail(thumbnail);
                        }
                        
                        localCache.add(lm);
                        if (localCache.size() == cacheLimit) {
                            landmarks.addAll(localCache);
                            localCache.clear();
                        }

                        //add lm to dynamic layers
                        if (landmarkManager != null) {
                            landmarkManager.addLandmarkToDynamicLayer(lm);
                        }
                        //landmarks.add(lm);
                    }
                }
            }
        }

        //System.out.println("Processed landmarks from layer " + layer + " in " + (System.currentTimeMillis() - start) + " millis.");

        if (!localCache.isEmpty()) {
            landmarks.addAll(localCache);
        }

        //if (! task.isCancelled()) {
        //AsyncTaskExecutor.execute(new SaveSuggestionsTask(layer, localCache, task));
        //}
    }
    
    private static String formatAddress(AddressInfo address) {
        List<String> tokens = new ArrayList<String>();
        
        if (address.getField(AddressInfo.STREET) != null) {
            tokens.add(address.getField(AddressInfo.STREET));
        }
        
        String line = "";
        if (address.getField(AddressInfo.POSTAL_CODE) != null) {
            line += address.getField(AddressInfo.POSTAL_CODE);
        }
        if (address.getField(AddressInfo.CITY) != null) {
            if (line.length() > 0) {
                line += " ";
            }
            line += address.getField(AddressInfo.CITY);
        }
        
        if (line.length() > 0) {
            tokens.add(line);
        }
        
        if (address.getField(AddressInfo.STATE) != null) {
            tokens.add(address.getField(AddressInfo.STATE));
        }
        if (address.getField(AddressInfo.COUNTRY) != null) {
            tokens.add(address.getField(AddressInfo.COUNTRY));
        }
        
        if (!tokens.isEmpty()) {
            return StringUtil.join(tokens, ", ");
        } else {
            return "";
        }
    }
    
    private String formatDeal(Deal deal) {
        String result = "";
        String currencyCode = deal.getCurrencyCode();
        
        if (deal.getPrice() > 0 || (deal.getPrice() == 0 && deal.getDiscount() > 0)) {
            String dealFormatted = nf.format(deal.getPrice());
            if (currencyCode != null && (currencyCode.equals("$") || currencyCode.equals("USD"))) {
                dealFormatted = "<font color=\"green\">$" + dealFormatted + "</font>";
            } else if (currencyCode != null && currencyCode.equals("C$")) {
                dealFormatted = "<font color=\"green\">C$" + dealFormatted + "</font>";
            } else {
                dealFormatted = "<font color=\"green\">" + dealFormatted + " " + deal.getCurrencyCode() + "</font>";
            }
            result = Locale.getMessage(R.string.price, dealFormatted);
        }
        
        if (deal.getDiscount() > 0) {
            String saveFormatted = nf.format(deal.getSave());
            if (currencyCode.equals("$") || currencyCode.equals("USD")) {
                saveFormatted = "<font color=\"red\">$" + saveFormatted + "</font>";
            } else if (currencyCode.equals("C$")) {
                saveFormatted = "<font color=\"red\">C$" + saveFormatted + "</font>";
            } else {
                saveFormatted = "<font color=\"red\">" + saveFormatted + " " + deal.getCurrencyCode() + "</font>";
            }
            
            String discountFormatted = "<font color=\"red\">" + pf.format(deal.getDiscount()) + "</font>";
            
            result += Locale.getMessage(R.string.discount, discountFormatted, saveFormatted);
        }
        
        if (deal.isIsDealOfTheDay()) {
            if (result.length() > 0) {
                result += "<br/>";
            }
            result += "<font color=\"red\">" + Locale.getMessage(R.string.dealOfTheDay) + "</font>";
        } else if (StringUtils.isNotEmpty(deal.getDealType())) {
            if (result.length() > 0) {
                result += "<br/>";
            }
            result += Locale.getMessage(R.string.dealType, deal.getDealType());
        }
        
        return result;
    }
    
    private static String setDate(int resource, String value, java.util.Locale l, boolean shortDate) {
        String date = DateTimeUtils.getDefaultDateTimeString(value, l);
        if (shortDate) {
            date = DateTimeUtils.getShortDateTimeString(value, l);
        }
        return Locale.getMessage(resource, date);
    }
    
    private static String getStars(int num) {
        String stars = "";
        
        for (int i = 0; i < num; i++) {
            //stars += "*";
            stars += "<img src=\"star_blue\"/> ";
        }
        return stars;
    }
    
    private static String getLink(String url, String name) {
        if (StringUtils.startsWith(url, "http")) {
            return "<a href=\"" + url + "\">" + name + "</a>";
        } else {
            return "<a href=\"http://" + url + "\">" + name + "</a>";
        }
    }

    //v2
    private static void putOptValue(List<String> vector, int resource, String property, Map<String, String> tokens, java.util.Locale l, boolean isHtml, boolean isDate) {
        String value = tokens.remove(property);
        if (StringUtils.isNotEmpty(value)) {
            if (isHtml) {
                value = Locale.getMessage(resource, value);//Html.fromHtml(value).toString());
            } else if (isDate) {
                value = setDate(resource, value, l, true);
            } else {
                value = Locale.getMessage(resource, value);
            }
            vector.add(StringUtils.trimToEmpty(value));
        }
    }
    
    private List<String> parseJSonObject(AddressInfo address, Map<String, String> tokens, Deal deal, int categoryId, String checkins, String layer) {
        List<String> result = new ArrayList<String>();
        List<String> otherNamed = new ArrayList<String>();
        String start_date = null;

        //long start = System.currentTimeMillis();

        putOptValue(result, R.string.category, "category", tokens, locale, false, false);
        putOptValue(result, R.string.merchant, "merchant", tokens, locale, false, false);
        putOptValue(result, R.string.artist, "artist", tokens, locale, false, false);
        putOptValue(result, R.string.venue, "venue", tokens, locale, false, false);
        putOptValue(result, R.string.description, "description", tokens, locale, true, false);
        
        String val = tokens.remove("star_rating");
        if (val != null) {
            int star_rating = (int) StringUtil.parseDouble(val, 0.0);
            if (star_rating > 0) {
                result.add(getStars(star_rating));
            }
        }
        
        boolean isDeal = buildDeal(tokens, deal);
        //System.out.println("P: " + deal.getPrice() + ", D: " + deal.getDiscount() + ", S: " + deal.getSave());
        if (isDeal) {
            String priceFormatted = formatDeal(deal);
            if (StringUtils.isNotEmpty(priceFormatted)) {
                result.add(priceFormatted);
            }
        }
        
        buildAddress(address, tokens);
        String locality = formatAddress(address);
        if (StringUtils.isNotEmpty(locality)) {
            result.add(Locale.getMessage(R.string.address, locality));
        }
        if (StringUtils.isNotEmpty(address.getField(AddressInfo.PHONE_NUMBER))) {
            otherNamed.add(Locale.getMessage(R.string.phone, address.getField(AddressInfo.PHONE_NUMBER)));
        }

        //thumbnail

        val = tokens.remove("icon");
        if (val != null) {
            thumbnail = val;
        }

        //dates

        val = tokens.remove("upload_date");
        if (val != null) {
            otherNamed.add(Locale.getMessage(R.string.upload_date, val));
            //TODO convert panoramio date to long
            //creationDate = val;
        }
        
        val = tokens.remove("start_date");
        if (val != null) {
            start_date = setDate(R.string.start_date, val, locale, true);
            creationDate = StringUtil.parseLong(val, 0);
        }
        
        val = tokens.remove("creationDate");
        if (val != null) {
            if (!StringUtils.equals(layer, Commons.LOCAL_LAYER)) {
                otherNamed.add(setDate(R.string.creation_date, val, locale, true));
            }
            creationDate = StringUtil.parseLong(val, 0);
        }
        
        val = tokens.remove("taken_date");
        if (val != null) {
            otherNamed.add(setDate(R.string.taken_date, val, locale, true));
            creationDate = StringUtil.parseLong(val, 0);
        }
        
        val = tokens.remove("end_date");
        if (val != null) {
            otherNamed.add(setDate(R.string.end_date, val, locale, true));
            endDate = StringUtil.parseLong(val, 0);
        }
        
        val = tokens.remove("expiration_date");
        if (val != null) {
            otherNamed.add(setDate(R.string.expiration_date, val, locale, true));
            endDate = StringUtil.parseLong(val, 0);
        }

        //rating

        String ratingStr = "";
        val = tokens.remove("rating");
        int maxRating = StringUtil.parseInteger(tokens.remove("maxRating"), 5);
        double r = StringUtil.parseDouble(val, -1);
        if (r >= 0) {
            rating = r / maxRating;
            if (layer.equals(Commons.YELP_LAYER) || layer.equals(Commons.COUPONS_LAYER)) {
                if (r >= maxRating * 0.9) {
                    ratingStr = "<img src=\"stars_5\"/>";
                } else if (r >= maxRating * 0.8 && r < maxRating * 0.9) {
                    ratingStr = "<img src=\"stars_4_half\"/>";
                } else if (r >= maxRating * 0.7 && r < maxRating * 0.8) {
                    ratingStr = "<img src=\"stars_4\"/>";
                } else if (r >= maxRating * 0.6 && r < maxRating * 0.7) {
                    ratingStr = "<img src=\"stars_3_half\"/>";
                } else if (r >= maxRating * 0.5 && r < maxRating * 0.6) {
                    ratingStr = "<img src=\"stars_3\"/>";
                } else if (r >= maxRating * 0.4 && r < maxRating * 0.5) {
                    ratingStr = "<img src=\"stars_2_half\"/>";
                } else if (r >= maxRating * 0.3 && r < maxRating * 0.4) {
                    ratingStr = "<img src=\"stars_2\"/>";
                } else if (r >= maxRating * 0.2 && r < maxRating * 0.3) {
                    ratingStr = "<img src=\"stars_1_half\"/>";
                } else if (r >= maxRating * 0.1 && r < maxRating * 0.2) {
                    ratingStr = "<img src=\"stars_1\"/>";
                } else {
                    ratingStr = "<img src=\"stars_0\"/>";
                }
            } else {
                if (r >= maxRating * 0.9) {
                    ratingStr = "<img src=\"star_5\"/>";
                } else if (r >= maxRating * 0.8 && r < maxRating * 0.9) {
                    ratingStr = "<img src=\"star_5\"/>";
                } else if (r >= maxRating * 0.7 && r < maxRating * 0.8) {
                    ratingStr = "<img src=\"star_4\"/>";
                } else if (r >= maxRating * 0.6 && r < maxRating * 0.7) {
                    ratingStr = "<img src=\"star_4\"/>";
                } else if (r >= maxRating * 0.5 && r < maxRating * 0.6) {
                    ratingStr = "<img src=\"star_3\"/>";
                } else if (r >= maxRating * 0.4 && r < maxRating * 0.5) {
                    ratingStr = "<img src=\"star_3\"/>";
                } else if (r >= maxRating * 0.3 && r < maxRating * 0.4) {
                    ratingStr = "<img src=\"star_2\"/>";
                } else if (r >= maxRating * 0.2 && r < maxRating * 0.3) {
                    ratingStr = "<img src=\"star_2\"/>";
                } else if (r >= maxRating * 0.1 && r < maxRating * 0.2) {
                    ratingStr = "<img src=\"star_1\"/>";
                } else {
                    ratingStr = "<img src=\"star_0\"/>";
                }
            }
        }
        
        val = tokens.remove("numberOfReviews");
        if (val != null) {
            if (ratingStr.length() > 0) {
                if (!layer.equals(Commons.YELP_LAYER)) {
                    ratingStr += ",";
                }
                ratingStr += " ";
            }
            numberOfReviews = Integer.parseInt(val);
            if (numberOfReviews == 1) {
                ratingStr += Locale.getMessage(R.string.numberOfReviews_single);
            } else {
                ratingStr += Locale.getMessage(R.string.numberOfReviews_multiple, val);
            }
        }
        
        if (ratingStr.length() > 0) {
            otherNamed.add(ratingStr);
        }

        //checkins

        if (checkins != null) {
            result.add("<font color=\"red\">" + checkins + "</font>");
        }

        //photo

        String photoUser = tokens.remove("photoUser");
        if (photoUser != null) {
            String caption = tokens.remove("caption");
            String link = tokens.remove("link");
            
            String message = null;
            if (StringUtils.startsWith(link, "http")) {
                message = Locale.getMessage(R.string.photo_url, new Object[]{photoUser, DateUtils.getRelativeTimeSpanString(creationDate, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS), link, caption});
            } else if (StringUtils.startsWith(caption, "http")) {
                message = Locale.getMessage(R.string.photo_url_noname, new Object[]{photoUser, DateUtils.getRelativeTimeSpanString(creationDate, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS), caption});
            }
            
            if (message != null) {
                result.add(message);
                hasCheckinOrPhoto = true;
            }
        }
        
        val = tokens.remove("isTrending");
        if (val != null) {
            result.add(Locale.getMessage(R.string.trending, val));
        }
        //start_date

        if (StringUtils.isNotEmpty(start_date)) {
            result.add(start_date);
        }

        //other

        if (!otherNamed.isEmpty()) {
            result.add(StringUtil.join(otherNamed, ",<br/>"));
        }
        
        List<String> others = new ArrayList<String>();
        for (Iterator<Map.Entry<String, String>> i = tokens.entrySet().iterator(); i.hasNext();) {
            Map.Entry<String, String> entry = i.next();
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (key.equals("homepage")) {
                String[] homepageTokens = StringUtils.split(value, ' ');
                if (homepageTokens.length > 0) {
                    others.add(getLink(homepageTokens[0], Locale.getMessage(R.string.homepage)));
                }
            } else if (key.equals("source")) {
                others.add(Locale.getMessage(R.string.source, getLink(value, value)));
            } else if (key.equals("twitter")) {
                others.add(Locale.getMessage(R.string.twitter, getLink("http://mobile.twitter.com/" + value, "@" + value)));
            } else if (key.equals("facebook")) {
                others.add(getLink(FbPlacesReader.FBPLACES_PREFIX[0] + value, "Facebook"));
            } else if (key.equals("menu")) {
                others.add(getLink(value, Locale.getMessage(R.string.menu)));
            } else if (key.equals("photo")) {
                others.add(getLink(value, Locale.getMessage(R.string.photo)));
            } else {
                others.add(key + ": " + value);
            }
            
        }
        
        if (!others.isEmpty()) {
            result.add(StringUtil.join(others, ", "));
        }
        
        if (isDeal || categoryId > 0) {
            deal.setEndDate(endDate);
        }

        //System.out.println("parseJsonObject in " + (System.currentTimeMillis() - start) + " millis");

        return result;
    }
    
    private static boolean buildDeal(Map<String, String> tokens, Deal deal) {
        
        boolean isDeal = false;
        
        String price = tokens.remove("price");
        if (price == null) {
            price = tokens.remove("average_price");
        }
        
        if (price != null) {
            isDeal = true;
            String currencyCode = null;
            if (price.startsWith("$")) {
                price = price.substring(1);
                currencyCode = "$";
            } else if (price.startsWith("C$")) {
                price = price.substring(2);
                currencyCode = "C$";
            } else {
                String[] amountSplit = price.split(" ");
                if (amountSplit.length == 2) {
                    price = amountSplit[0];
                    currencyCode = amountSplit[1];
                }
            }
            
            if (currencyCode != null) {
                deal.setCurrencyCode(currencyCode);
                deal.setPrice(StringUtil.parseDouble(price, 0));
                
                String save = tokens.remove("save");
                if (save != null && save.startsWith("$")) {
                    save = save.substring(1);
                    deal.setSave(StringUtil.parseDouble(save, 0));
                } else if (save != null && save.startsWith("C$")) {
                    save = save.substring(2);
                    deal.setSave(StringUtil.parseDouble(save, 0));
                }
                
                String discount = tokens.remove("discount");
                if (discount != null && discount.endsWith("%")) {
                    discount = discount.substring(0, discount.length() - 1);
                    deal.setDiscount(StringUtil.parseDouble(discount, 0) / 100);
                }
            }
        }
        
        String dealType = tokens.remove("dealType");
        if (dealType != null) {
            isDeal = true;
            deal.setDealType(dealType);
        }
        
        return isDeal;
    }
    
    private static void buildAddress(AddressInfo address, Map<String, String> tokens) {
        String val = tokens.remove("phone");
        if (val != null) {
            address.setField(AddressInfo.PHONE_NUMBER, val);
        }
        
        val = tokens.remove("country");
        if (val != null) {
            address.setField(AddressInfo.COUNTRY, val);
        }
        
        val = tokens.remove("zip");
        if (val != null) {
            address.setField(AddressInfo.POSTAL_CODE, val);
        } else {
            val = tokens.remove("postalCode");
            if (val != null) {
            	address.setField(AddressInfo.POSTAL_CODE, val);
            }
        }
        
        val = tokens.remove("city");
        if (val != null) {
        	address.setField(AddressInfo.CITY, val);
        } else {
            val = tokens.remove("locality");
            if (val != null) {
            	address.setField(AddressInfo.CITY, val);
            }
        }
        
        val = tokens.remove("street");
        if (val != null) {
        	address.setField(AddressInfo.STREET, val);
        } else {
            val = tokens.remove("address");
            if (val != null) {
            	address.setField(AddressInfo.STREET, val);
            } else {
                val = tokens.remove("location");
                if (val != null) {
                	address.setField(AddressInfo.STREET, val);
                }
            }
        }
        
        val = tokens.remove("state");
        if (val != null) {
        	address.setField(AddressInfo.STATE, val);
        } else {
            val = tokens.remove("region");
            if (val != null) {
            	address.setField(AddressInfo.STATE, val);
            }
        }
        
        val = tokens.remove("crossStreet");
        if (val != null) {
        	address.setField(AddressInfo.CROSSING1, val);
        }
    }
    
    private Map<String, String> toMap(JSONObject description) throws JSONException {
        Map<String, String> tokens = new HashMap<String, String>();
        for (Iterator<String> iter = description.keys(); iter.hasNext();) {
            String key = iter.next();
            tokens.put(key, description.getString(key));
        }
        return tokens;
    }
    
    private class JSonParserTask implements Runnable {
        
        private JSONArray resultSet;
        private List<ExtendedLandmark> landmarks;
        private String layer;
        private String[] urlPrefix;
        private int defaultCategory, defaultSubcategory, cacheLimit;
        private GMSAsyncTask<?,?,?> task;
        
        public JSonParserTask(JSONArray resultSet, List<ExtendedLandmark> landmarks, String layer, String[] urlPrefix, int defaultCategory, int defaultSubcategory, GMSAsyncTask<?,?,?> task, int cacheLimit) {
            this.resultSet = resultSet;
            this.defaultCategory = defaultCategory;
            this.defaultSubcategory = defaultSubcategory;
            this.landmarks = landmarks;
            this.layer = layer;
            this.task = task;
            this.urlPrefix = urlPrefix;
            this.cacheLimit = cacheLimit;
        }
        
        public void run() {
            try {
                parseJSonArray(resultSet, landmarks, layer, urlPrefix, defaultCategory, defaultSubcategory, task, cacheLimit, null);
            } catch (Exception ex) {
                LoggerUtils.error("JSonParserTask exception: ", ex);
            }
        }
    }
    
    /*private class JSonParserAsyncTask extends GMSAsyncTask<String, Void, String> {
        
        private JSONArray resultSet;
        private List<ExtendedLandmark> landmarks;
        private String layer;
        private String[] urlPrefix;
        private int defaultCategory, defaultSubcategory, cacheLimit;
        private GMSAsyncTask<?,?,?> task;
        
        public JSonParserAsyncTask(JSONArray resultSet, List<ExtendedLandmark> landmarks, String layer, String[] urlPrefix, int defaultCategory, int defaultSubcategory, GMSAsyncTask<?,?,?> task, int cacheLimit) {
            super(1);
            this.resultSet = resultSet;
            this.defaultCategory = defaultCategory;
            this.defaultSubcategory = defaultSubcategory;
            this.landmarks = landmarks;
            this.layer = layer;
            this.task = task;
            this.urlPrefix = urlPrefix;
            this.cacheLimit = cacheLimit;
        }
        
        @Override
        protected String doInBackground(String... fileData) {
            try {
                parseJSonArray(resultSet, landmarks, layer, urlPrefix, defaultCategory, defaultSubcategory, task, cacheLimit, null);
            } catch (Exception ex) {
                LoggerUtils.error("JSonParserTask exception: ", ex);
            }
            return null;
        }
    }
    
    private class SaveSuggestionsTask extends GMSAsyncTask<Void, Void, Void> {
        
        private String layer;
        private List<ExtendedLandmark> localCache;
        private GMSAsyncTask<?,?,?> parent;
        private static final int MAX_SAVED_LANDMARKS = 10;
        
        public SaveSuggestionsTask(String layer, List<ExtendedLandmark> localCache, GMSAsyncTask<?,?,?> parent) {
            super(10);
            this.layer = layer;
            this.localCache = localCache;
            this.parent = parent;
        }
        
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                int size = localCache.size();
                int count = (MAX_SAVED_LANDMARKS > size) ? size : MAX_SAVED_LANDMARKS;
                
                for (int i = 0; i < count; i++) {
                    if (parent.isCancelled()) {
                        break;
                    } else {
                        SuggestionProviderUtil.saveRecentQuery(localCache.get(i).getName() + " (" + layer + ")", null);
                    }
                }
            } catch (Exception e) {
                LoggerUtils.error("JSONParser.parseJSonArray error:", e);
            }
            return null;
        }
    }*/
}
