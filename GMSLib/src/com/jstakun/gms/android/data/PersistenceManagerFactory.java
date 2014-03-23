/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import com.jstakun.gms.android.config.ConfigurationManager;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class PersistenceManagerFactory {

    private static PersistenceManager pm = null;
    private static FileManager fm = null;

    public static PersistenceManager getPersistenceManagerInstance() {
        if (pm == null) {
            int presistenceManager = ConfigurationManager.getInstance().getInt(ConfigurationManager.PERSISTENCE_MANAGER, 1);
            if (presistenceManager == 0) {
                pm = new AndroidPersistenceManager();
            } else if (presistenceManager == 1) {
                pm = getFileManager();
            } else if (presistenceManager == 2) {
                pm = new MemoryPersistenceManager();
            }
        }

        return pm;
    }

    public static FileManager getFileManager() {
        if (fm == null) {
        	try {
        		String packageName = ConfigurationManager.getAppUtils().getPackageInfo().packageName;
        		fm = new FileManager(packageName);
        	} catch (Exception e) {
        		LoggerUtils.error("PersistenceManagerfactory.getFileManager() exception:", e);
        	}
        }
        return fm;
    }
}
