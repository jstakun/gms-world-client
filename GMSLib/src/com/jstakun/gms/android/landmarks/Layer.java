/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.landmarks;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.data.FileManager;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class Layer {

    private String name;
    //private boolean extensible; //czy mozna dodac wlasne landmarki
    private boolean manageable; //czy moza samemu wlaczyc widok warstwy
    private boolean checkinable; //is checkin allowed
    private boolean searchable;
    private List<LayerReader> layerReader;
    private String smallIconPath;
    private int smallIconResource;
    private String largeIconPath;
    private int largeIconResource;
    private int type;
    private String key;
    private String desc;
    private String formatted;
    private String[] keywords = null;
    private int count;
    private FileManager.ClearPolicy clearPolicy;
    private int image;

    protected Layer(String name, boolean extensible, boolean manageable, boolean enabled, boolean checkinable, boolean searchable, List<LayerReader> layerReader, String smallIconPath, int smallIconResource, String largeIconPath, int largeIconResource, int type, String desc, String formatted, FileManager.ClearPolicy clearPolicy, int image) {
        this.name = name;
        //this.extensible = extensible;
        this.manageable = manageable;
        this.checkinable = checkinable;
        this.searchable = searchable;
        this.layerReader = layerReader;
        this.smallIconPath = smallIconPath;
        this.smallIconResource = smallIconResource;
        this.largeIconPath = largeIconPath;
        this.largeIconResource = largeIconResource;
        this.type = type;
        this.desc = desc;
        this.formatted = formatted;
        this.image = image;
        
        this.count = 0;
        

        this.key = name + "_status";

        if (ConfigurationManager.getInstance().containsKey(key)) {
            if (ConfigurationManager.getInstance().isOn(key)) {
                ConfigurationManager.getInstance().setOn(key);
            } else {
                ConfigurationManager.getInstance().setOff(key);
            }
        } else {
            if (enabled) {
                ConfigurationManager.getInstance().setOn(key);
            } else {
                ConfigurationManager.getInstance().setOff(key);
            }
        }
        
        this.setClearPolicy(clearPolicy);
    }

    public boolean isEnabled() {
        if (ConfigurationManager.getInstance().isOn(key)) {
            return true;
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            ConfigurationManager.getInstance().setOn(key);
        } else {
            ConfigurationManager.getInstance().setOff(key);
        }
    }

    public boolean isManageable() {
        return manageable;
    }

    public List<LayerReader> getLayerReader() {
        if (layerReader != null) {
            return Collections.unmodifiableList(layerReader);
        } else {
            return Collections.<LayerReader>emptyList();
        }
    }

    public String getSmallIconPath() {
        return smallIconPath;
    }

    public int getSmallIconResource() {
        return smallIconResource;
    }

    public String getLargeIconPath() {
        return largeIconPath;
    }

    public int getLargeIconResource() {
        return largeIconResource;
    }

    public int getType() {
        return type;
    }

    public boolean isCheckinable() {
        return checkinable;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public String getDesc() {
        return desc;
    }

    public String getFormatted() {
        return formatted;
    }

    /**
     * @return the keywords
     */
    public String[] getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    /**
     * @return the count
     */
    public synchronized int getCount() {
        return count;
    }

    /**
     * @param count the count to set
     */
    public synchronized void setCount(int count) {
        this.count = count;
    }

    public synchronized void increaseCount() {
        count++;
    }
    
    public synchronized void decreaseCount() {
    	count--;
    }
    
    public synchronized void increaseCount(int inc) {
        count+=inc;
    }

	public FileManager.ClearPolicy getClearPolicy() {
		return clearPolicy;
	}

	public void setClearPolicy(FileManager.ClearPolicy clearPolicy) {
		this.clearPolicy = clearPolicy;
	}
	
	public int getImage() {
		return image;
	}
}
