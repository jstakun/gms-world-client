package com.jstakun.gms.android.data;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author jstakun
 */
public class PNGFilenameFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        return (name.endsWith(PersistenceManager.FORMAT_PNG));
    }
}
