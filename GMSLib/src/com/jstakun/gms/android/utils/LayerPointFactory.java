/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.utils;

import com.devahead.util.objectpool.PoolObject;
import com.devahead.util.objectpool.PoolObjectFactory;

/**
 *
 * @author jstakun
 */
public class LayerPointFactory implements PoolObjectFactory
{

    public PoolObject createPoolObject() {
        return new LayerPoint();
    }
    
}
