package com.jstakun.gms.android.data;

import com.jstakun.gms.android.config.ConfigurationManager;

/**
 *
 * @author jstakun
 */
public class PersistenceManagerFactory {

    private static PersistenceManager pm = null;
    
    public static PersistenceManager getPersistenceManagerInstance() {
        if (pm == null) {
            int presistenceManager = ConfigurationManager.getInstance().getInt(ConfigurationManager.PERSISTENCE_MANAGER, 1);
            if (presistenceManager == 0) {
                pm = new AndroidPersistenceManager();
            } else if (presistenceManager == 1) {
                pm = FileManager.getInstance();
            } else if (presistenceManager == 2) {
                pm = new MemoryPersistenceManager();
            }
        }
        return pm;
    }
}
