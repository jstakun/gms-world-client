/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import android.graphics.drawable.Drawable;

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.ui.lib.R;
import com.jstakun.gms.android.utils.Locale;
import com.openlapi.Landmark;
import com.openlapi.QualifiedCoordinates;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class LandmarkPaintManager {

    //recently opened landmarks
    private List<ExtendedLandmark> recentlyOpenedLandmarks = new ArrayList<ExtendedLandmark>();
    //landmarks on focus
    private List<ExtendedLandmark> landmarkOnFocus = new ArrayList<ExtendedLandmark>();
    //landmark selected by user on the screen
    private ExtendedLandmark selectedLandmark;
    //dist between selected landmark & screen center
    private double distanceFromCenter;
    //last bounding box
    //private BoundingBox lastBBox;
    //landmark drawables
    private List<Drawable> landmarkDrawables = new ArrayList<Drawable>();
    //selected landmark drawable
    private Drawable selectedLandmarkDrawable = null;

    protected void addLandmarkToFocusQueue(ExtendedLandmark landmark) {
        landmarkOnFocus.add(landmark);
    }

    protected List<ExtendedLandmark> getLandmarkOnFocusQueue() {
        return Collections.unmodifiableList(landmarkOnFocus);
    }

    protected ExtendedLandmark getLandmarkOnFocusQueueSelectedLandmark(int id) {
        if (id >= 0 && id < landmarkOnFocus.size()) {
            return landmarkOnFocus.get(id);
        }
        return null;
    }

    protected ExtendedLandmark getLandmarkOnFocus() {
        int size = landmarkOnFocus.size();
        //System.out.println("Landmark on focus queue size: " + size);
        if (size < 2) {
            return getSelectedLandmark();
        } else if (size > 1) {
            return getMultiLandmark(getSelectedLandmark().getQualifiedCoordinates());
        } else {
            return null;
        }
    }

    protected void clearLandmarkOnFocusQueue() {
        landmarkOnFocus.clear();
    }

    protected void setLandmarkOnFocusQueue(List<ExtendedLandmark> newFocusQueue) {
        landmarkOnFocus.clear();
        landmarkOnFocus.addAll(newFocusQueue);
    }

    protected void setSelectedLandmark(ExtendedLandmark landmark, double distance) {
        if (distance == -1.0 || distanceFromCenter == -1 || distance <= distanceFromCenter) {
            selectedLandmark = landmark;
            distanceFromCenter = distance;
        }
    }

    protected boolean hasSelectedLandmark() {
        return (selectedLandmark != null);
    }

    protected ExtendedLandmark getSelectedLandmark() {
        return selectedLandmark;
    }

    private ExtendedLandmark getMultiLandmark(QualifiedCoordinates qc) {
        Landmark l = landmarkOnFocus.get(0);
        selectedLandmark = LandmarkFactory.getLandmark(Locale.getMessage(R.string.Landmark_MultiLandmark, l.getName()), null, qc, Commons.MULTI_LANDMARK, System.currentTimeMillis());
        return selectedLandmark;
    }

    protected void addAllLandmarkDrawable(List<Drawable> d) {
        landmarkDrawables.addAll(d);
    }

    protected void addLandmarkDrawable(Drawable d) {
        landmarkDrawables.add(d);
    }

    protected void setSelectedLandmarkDrawable(Drawable d) {
        selectedLandmarkDrawable = d;
    }

    protected List<Drawable> getLandmarkDrawables() {
        return landmarkDrawables;
    }

    protected void clearLandmarkDrawables() {
        landmarkDrawables.clear();
    }

    /**
     * @return the selectedLandmarkDrawable
     */
    protected Drawable getSelectedLandmarkDrawable() {
        return selectedLandmarkDrawable;
    }

    /**
     * @return the recentlyOpenedLandmarks
     */
    protected List<ExtendedLandmark> getRecentlyOpenedLandmarks() {
        return Collections.unmodifiableList(recentlyOpenedLandmarks);
    }

    protected void addRecentlyOpenedLandmark(ExtendedLandmark landmark) {
        if (!recentlyOpenedLandmarks.contains(landmark)) {
            recentlyOpenedLandmarks.add(landmark);
        }
    }
}
