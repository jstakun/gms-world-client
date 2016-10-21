package com.jstakun.gms.android.landmarks;

import java.util.List;

import com.jstakun.gms.android.data.FileManager;

/**
 *
 * @author jstakun
 */
public class LayerFactory {
    protected static Layer getLayer(String name, boolean manageable, boolean enabled, boolean checkinable, List<LayerReader> layerReader, String smallIconPath, int smallIconResource, String largeIconPath, int largeIconResource, int type, String desc, String formatted, FileManager.ClearPolicy clearPolicy, int image){
        return new Layer(name, manageable, enabled, checkinable, layerReader, smallIconPath, smallIconResource, largeIconPath, largeIconResource, type, desc, formatted, clearPolicy, image);
    }
}
