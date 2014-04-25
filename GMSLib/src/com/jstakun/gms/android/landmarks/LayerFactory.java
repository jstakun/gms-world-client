/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.data.FileManager;

/**
 *
 * @author jstakun
 */
public class LayerFactory {
    public static Layer getLayer(String name, boolean extensible, boolean manageable, boolean enabled, boolean checkinable, boolean searchable, List<LayerReader> layerReader, String smallIconPath, int smallIconResource, String largeIconPath, int largeIconResource, int type, String desc, String formatted, FileManager.ClearPolicy clearPolicy){
        return new Layer(name, extensible, manageable, enabled, checkinable, searchable, layerReader, smallIconPath, smallIconResource, largeIconPath, largeIconResource, type, desc, formatted, clearPolicy);
    }
}
