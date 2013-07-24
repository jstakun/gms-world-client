/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.utils;

/**
 *
 * @author jstakun
 */
public abstract class MessageCondition {

     public static final int MAP_LOADING = 0;
     public static final int LAYER_LOADING = 1;

     public abstract boolean isLoading(int type);
}
