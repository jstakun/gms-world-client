/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;


/**
 *
 * @author jstakun
 */
public class SMSSender {

    private static final String SMS_NUMBER = "+48123456789"; //TODO provide real phone number and enable in menu
    private String message;

    public SMSSender(String text)
    {
        message = text;
    }

    public void sendSMS()
    {
       //TODO send message to SMS_NUMBER   
    }
}
