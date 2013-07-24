/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.landmarks.LayerLoader;

/**
 *
 * @author jstakun
 */
public class LayersMessageCondition extends MessageCondition {

    @Override
    public boolean isLoading(int type) {
        if (type == LAYER_LOADING) {
            LayerLoader layerLoader = (LayerLoader) ConfigurationManager.getInstance().getObject("layerLoader", LayerLoader.class);
            if (layerLoader != null) {
                return layerLoader.isLoading();
            }
        }
        return false;
    }
}
