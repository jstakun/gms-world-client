/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.deals;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jstakun
 */
public final class Deal implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double price;
    private double discount;
    private double save;
    private String dealType;
    private boolean isDealOfTheDay;
    private String currencyCode;
    private long endDate = -1;

    public Deal(double price, double discount, double save, String dealType, String currencyCode) {
        this.price = price;
        this.discount = discount;
        this.save = save;
        this.dealType = dealType;
        this.currencyCode = currencyCode;
        isDealOfTheDay = StringUtils.equals(dealType, "Deals-of-the-Day");
    }

    /**
     * @return the price
     */
    public double getPrice() {
        return price;
    }

    /**
     * @param price the price to set
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * @return the discount
     */
    public double getDiscount() {
        return discount;
    }

    /**
     * @param discount the discount to set
     */
    public void setDiscount(double discount) {
        this.discount = discount;
        if (save <= 0.0) {
            double fullPrice = (price * 1E2) / (1E2 - (discount * 100));
            save = fullPrice - price;
        }
    }

    /**
     * @return the save
     */
    public double getSave() {
        return save;
    }

    /**
     * @param save the save to set
     */
    public void setSave(double save) {
        this.save = save;
    }

    /**
     * @return the dealType
     */
    public String getDealType() {
        return dealType;
    }

    /**
     * @param dealType the dealType to set
     */
    public void setDealType(String dealType) {
        this.dealType = dealType;
        isDealOfTheDay = StringUtils.equals(dealType, "Deals-of-the-Day");
    }

    /**
     * @return the isDealOfTheDay
     */
    public boolean isIsDealOfTheDay() {
        return isDealOfTheDay;
    }

    /**
     * @param isDealOfTheDay the isDealOfTheDay to set
     */
    public void setIsDealOfTheDay(boolean isDealOfTheDay) {
        this.isDealOfTheDay = isDealOfTheDay;
    }

    /**
     * @return the currencyCode
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * @param currencyCode the currencyCode to set
     */
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * @return the endDate
     */
    public long getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }
}
