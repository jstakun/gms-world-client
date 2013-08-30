/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import java.io.Serializable;

import com.google.common.base.Objects;
import com.jstakun.gms.android.deals.Deal;
import com.jstakun.gms.android.utils.MathUtils;
import com.openlapi.AddressInfo;
import com.openlapi.Landmark;
import com.openlapi.QualifiedCoordinates;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public final class ExtendedLandmark extends Landmark implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String layer;
    private int categoryId = -1;
    private int subCategoryId = -1;
    private long creationDate = 0;
    private int latitudeE6 = -1;
    private int longitudeE6 = -1;
    private boolean hasCheckins = false;
    private double rating = -1.0;
    private int numberOfReviews = 0;
    private String thumbnail = null;
    private int revelance = 0;
    private String searchTerm;
    private String serverKey = null;
    private Deal deal;
    
    protected ExtendedLandmark(String name, String desc, QualifiedCoordinates qc, String layer, AddressInfo address, long creationDate, String searchTerm) {
        super(name, desc, qc, address);
        this.layer = layer;
        this.creationDate = creationDate;
        this.searchTerm = searchTerm;
    }

    protected ExtendedLandmark(String name, String desc, QualifiedCoordinates qc, String layer, long creationDate) {
        super(name, desc, qc, new AddressInfo());
        this.layer = layer;
        this.creationDate = creationDate;
    }

    public String getLayer() {
        return layer;
    }

    /**
     * @return the categoryId
     */
    public int getCategoryId() {
        return categoryId;
    }

    /**
     * @param categoryId the categoryId to set
     */
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * @return the subCategoryId
     */
    public int getSubCategoryId() {
        return subCategoryId;
    }

    /**
     * @param subCategoryId the subCategoryId to set
     */
    public void setSubCategoryId(int subCategoryId) {
        this.subCategoryId = subCategoryId;
    }

    public String getUrl() {
        return getAddressInfo().getField(AddressInfo.URL);
    }
    
    public void setUrl(String url) {
    	getAddressInfo().setField(AddressInfo.URL, url);
    }

    public String getPhone() {
    	return getAddressInfo().getField(AddressInfo.PHONE_NUMBER);
    }

    /**
     * @return the creationDate
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * @return the latitudeE6
     */
    public int getLatitudeE6() {
        if (latitudeE6 == -1) {
            latitudeE6 = MathUtils.coordDoubleToInt(getQualifiedCoordinates().getLatitude());
        }
        return latitudeE6;
    }

    /**
     * @param latitudeE6 the latitudeE6 to set
     */
    public void setLatitudeE6(int latitudeE6) {
        this.latitudeE6 = latitudeE6;
    }

    /**
     * @return the longitudeE6
     */
    public int getLongitudeE6() {
        if (longitudeE6 == -1) {
           longitudeE6 = MathUtils.coordDoubleToInt(getQualifiedCoordinates().getLongitude());
        }
        return longitudeE6;
    }

    /**
     * @param longitudeE6 the longitudeE6 to set
     */
    public void setLongitudeE6(int longitudeE6) {
        this.longitudeE6 = longitudeE6;
    }

    /**
     * @return the deal
     */
    public Deal getDeal() {
        return deal;
    }

    /**
     * @param deal the deal to set
     */
    public void setDeal(Deal deal) {
        this.deal = deal;
    }

    public boolean isDeal() {
        return categoryId > 0 || (deal != null && (deal.getPrice() >= 0 || StringUtils.isNotEmpty(deal.getDealType())));
    }

    @Override
    public boolean equals(Object landmark) {
        if (landmark == this) {
            return true;
        } else if (landmark instanceof ExtendedLandmark) {
            ExtendedLandmark l = (ExtendedLandmark)landmark;
            
            return Objects.equal(l.getName(), getName()) && 
               Objects.equal(l.getLatitudeE6(), getLatitudeE6()) &&
               Objects.equal(l.getLongitudeE6(), getLongitudeE6()) &&
               Objects.equal(l.getLayer(), layer);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName(), getLatitudeE6(), getLongitudeE6(), layer);
    }

    /**
     * @return the hasCheckins
     */
    public boolean hasCheckinsOrPhotos() {
        return hasCheckins;
    }

    /**
     * @param hasCheckins the hasCheckins to set
     */
    public void setHasCheckinsOrPhotos(boolean hasCheckins) {
        this.hasCheckins = hasCheckins;
    }

    /**
     * @return the rating
     */
    public double getRating() {
        return rating;
    }

    /**
     * @param rating the rating to set
     */
    public void setRating(double rating) {
        this.rating = rating;
    }

    /**
     * @return the numberOfReviews
     */
    public int getNumberOfReviews() {
        return numberOfReviews;
    }

    /**
     * @param numberOfReviews the numberOfReviews to set
     */
    public void setNumberOfReviews(int numberOfReviews) {
        this.numberOfReviews = numberOfReviews;
    }

    /**
     * @return the thunbnail
     */
    public String getThumbnail() {
        return thumbnail;
    }

    /**
     * @param thunbnail the thunbnail to set
     */
    public void setThumbnail(String thunbnail) {
        this.thumbnail = thunbnail;
    }

    /**
     * @return the revelance
     */
    public int getRevelance() {
        return revelance;
    }

    /**
     * @param revelance the revelance to set
     */
    public void setRevelance(int revelance) {
        this.revelance = revelance;
    }

    /**
     * @return the searchTerm
     */
    public String getSearchTerm() {
        return searchTerm;
    }

	public String getServerKey() {
		return serverKey;
	}

	public void setServerKey(String serverKey) {
		this.serverKey = serverKey;
	}
}
