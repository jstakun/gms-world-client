package com.jstakun.gms.android.utils;

import com.jstakun.gms.android.landmarks.LayerLoader;

/**
 *
 * @author jstakun
 */
public class LayersMessageCondition extends MessageCondition {

    @Override
    public boolean isLoading(int type) {
        if (type == LAYER_LOADING) {
            return LayerLoader.getInstance().isLoading();
        } else {
        	return false;
        }
    }
}
