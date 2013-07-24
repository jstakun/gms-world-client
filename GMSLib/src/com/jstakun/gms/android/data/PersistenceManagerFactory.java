/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import com.jstakun.gms.android.config.ConfigurationManager;

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
            String packageName = ConfigurationManager.getInstance().getString(ConfigurationManager.PACKAGE_NAME);
            fm = new FileManager(packageName);
        }
        return fm;
    }
}
