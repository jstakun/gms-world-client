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
public class FavouritesDbSQLiteOpenHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "favourites";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_LAYER = "layer";
    public static final String COLUMN_MAX_DISTANCE = "maxDistance";
    public static final String COLUMN_CHECKIN_DATE = "checkinDate";

    private static final String DATABASE_NAME = "favourites.db";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "create table " + TABLE_NAME + "( "
            + COLUMN_ID + " integer primary key, "
            + COLUMN_NAME + " text not null, "
            + COLUMN_LATITUDE + " real not null, "
            + COLUMN_LONGITUDE + " real not null, "
            + COLUMN_LAYER + " text not null, "
            + COLUMN_MAX_DISTANCE + " integer, "
            + COLUMN_CHECKIN_DATE + " integer"
            + ");";

    public FavouritesDbSQLiteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase landmarkdb) {
        landmarkdb.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase landmarkdb, int oldVersion, int newVersion) {
        LoggerUtils.debug("Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        landmarkdb.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
        onCreate(landmarkdb);
    }

}
