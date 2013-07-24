/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.jstakun.gms.android.utils.LoggerUtils;

/**
 *
 * @author jstakun
 */
public class ConfigDbSQLiteOpenHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "config";

    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";

    private static final String DATABASE_NAME = "config.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "create table " + TABLE_NAME + "( "
            + COLUMN_KEY + " text primary key, "
            + COLUMN_VALUE + " text not null "
            + ");";

    public ConfigDbSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase configdb) {
        configdb.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase configdb, int oldVersion, int newVersion) {
        LoggerUtils.debug("Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        configdb.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
        onCreate(configdb);
    }
}
