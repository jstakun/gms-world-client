/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.osm.maps;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import android.content.Context;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class OsmMapsTypeSelector {

    public static void selectMapType(final MapView osmMapsView, Context context) {
        int osmMapsType = ConfigurationManager.getInstance().getInt(ConfigurationManager.OSM_MAPS_TYPE);

        switch (osmMapsType) {
            case 0:
                osmMapsView.setTileSource(TileSourceFactory.MAPNIK);
                break;
            //case 1:
            //    osmMapsView.setTileSource(TileSourceFactory.OSMARENDER);
            //    break;
            case 1:
                osmMapsView.setTileSource(TileSourceFactory.CYCLEMAP);
                break;
            case 2:
                osmMapsView.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT);
                break;
            //case 4:
            //    osmMapsView.setTileSource(TileSourceFactory.BASE);
            //    break;
            //case 5:
            //    osmMapsView.setTileSource(TileSourceFactory.TOPO);
            //    break;
            //case 6:
            //    osmMapsView.setTileSource(TileSourceFactory.HILLS);
            //    break;
            //case 7:
            //    CloudmadeUtil.retrieveCloudmadeKey(context);
            //    osmMapsView.setTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);
            //    break;
            //case 8:
            //    CloudmadeUtil.retrieveCloudmadeKey(context);
            //    osmMapsView.setTileSource(TileSourceFactory.CLOUDMADESMALLTILES);
            //    break;
            //case 3:
            //    osmMapsView.setTileSource(TileSourceFactory.MAPQUESTOSM);
            //    break;
            //case 4:
            //    osmMapsView.setTileSource(TileSourceFactory.MAPQUESTAERIAL);
            //    break;
            default:
                osmMapsView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                break;
        }
    }
}
