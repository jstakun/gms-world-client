/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import android.os.Parcel;
import android.os.Parcelable;

/**
 *
 * @author jstakun
 */
public class LandmarkParcelable implements Parcelable {

    private long id;
    private String name;
    private String key;
    private String layer;
    private String desc;
    private float distance;
    private long creationDate;
    private int categoryid;
    private int subcategoryid;
    private int rating;
    private int numberOfReviews;
    private String thunbnail;
    private int revelance;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(getId());
        dest.writeString(getName());
        dest.writeString(getKey());
        dest.writeString(getLayer());
        dest.writeString(getDesc());
        dest.writeFloat(getDistance());
        dest.writeLong(getCreationDate());
        dest.writeInt(getCategoryid());
        dest.writeInt(getSubcategoryid());
        dest.writeInt(getRating());
        dest.writeInt(getNumberOfReviews());
        dest.writeString(getThunbnail());
        dest.writeInt(getRevelance());
    }

    public LandmarkParcelable(Parcel source) {
        id = source.readLong();
        name = source.readString();
        key = source.readString();
        layer = source.readString();
        desc = source.readString();
        distance = source.readFloat();
        creationDate = source.readLong();
        categoryid = source.readInt();
        subcategoryid = source.readInt();
        rating = source.readInt();
        numberOfReviews = source.readInt();
        thunbnail = source.readString();
        revelance = source.readInt();
    }

    protected LandmarkParcelable(long id, String name, String key, String layer, String desc, float distance, long creationDate, int categoryid, int subcategoryid, int rating, int numOfRev, String thunbnail, int revelance) {
        this.id = id;
        this.name = name;
        this.key = key;
        this.layer = layer;
        this.desc = desc;
        this.distance = distance;
        this.creationDate = creationDate;
        this.categoryid = categoryid;
        this.subcategoryid = subcategoryid;
        this.rating = rating;
        this.numberOfReviews = numOfRev;
        this.thunbnail = thunbnail;
        this.revelance = revelance;
    }

    public static final Parcelable.Creator<LandmarkParcelable> CREATOR = new Parcelable.Creator<LandmarkParcelable>() {

        public LandmarkParcelable createFromParcel(Parcel in) {
            return new LandmarkParcelable(in);
        }

        public LandmarkParcelable[] newArray(int size) {
            return new LandmarkParcelable[size];
        }
    };

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return the layer
     */
    public String getLayer() {
        return layer;
    }

    /**
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * @return the distance
     */
    public float getDistance() {
        return distance;
    }

    /**
     * @return the creationDate
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * @return the categoryid
     */
    public int getCategoryid() {
        return categoryid;
    }

    /**
     * @return the subcategoryid
     */
    public int getSubcategoryid() {
        return subcategoryid;
    }

    /**
     * @return the rating
     */
    public int getRating() {
        return rating;
    }

    /**
     * @return the numberOfReviews
     */
    public int getNumberOfReviews() {
        return numberOfReviews;
    }

    /**
     * @return the thunbnail
     */
    public String getThunbnail() {
        return thunbnail;
    }

    /**
     * @return the revelance
     */
    public int getRevelance() {
        return revelance;
    }
}
