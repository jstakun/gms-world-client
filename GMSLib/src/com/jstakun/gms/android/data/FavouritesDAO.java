/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.data;

/**
 *
 * @author jstakun
 */
public class FavouritesDAO {
    private long id;
    private String name;
    private double latitude;
    private double longitude;
    private String layer;
    private long maxDistance;
    private long lastCheckinDate;
    private String key;

    public FavouritesDAO(long id, String name, double latitude, double longitude, String layer, long maxDistance, long lastCheckinDate, String key) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.layer = layer;
        this.maxDistance = maxDistance;
        this.lastCheckinDate = lastCheckinDate;
        this.key = key;
    }
    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * @param latitude the latitude to set
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * @return the layer
     */
    public String getLayer() {
        return layer;
    }

    /**
     * @param layer the layer to set
     */
    public void setLayer(String layer) {
        this.layer = layer;
    }

    /**
     * @return the maxDistance
     */
    public long getMaxDistance() {
        return maxDistance;
    }

    /**
     * @param maxDistance the maxDistance to set
     */
    public void setMaxDistance(long maxDistance) {
        this.maxDistance = maxDistance;
    }

    /**
     * @return the lastCheckinDate
     */
    public long getLastCheckinDate() {
        return lastCheckinDate;
    }

    /**
     * @param lastCheckinDate the lastCheckinDate to set
     */
    public void setLastCheckinDate(long lastCheckinDate) {
        this.lastCheckinDate = lastCheckinDate;
    }
    
    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }
}
