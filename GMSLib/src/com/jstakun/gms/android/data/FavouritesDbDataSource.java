/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jstakun.gms.android.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MercatorUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class FavouritesDbDataSource {

    private static SQLiteDatabase database;
    private static FavouritesDbSQLiteOpenHelper dbHelper;
    private String[] allColumns = {FavouritesDbSQLiteOpenHelper.COLUMN_ID,
        FavouritesDbSQLiteOpenHelper.COLUMN_NAME,
        LandmarkDbSQLiteOpenHelper.COLUMN_LATITUDE,
        LandmarkDbSQLiteOpenHelper.COLUMN_LONGITUDE,
        FavouritesDbSQLiteOpenHelper.COLUMN_LAYER,
        FavouritesDbSQLiteOpenHelper.COLUMN_MAX_DISTANCE,
        FavouritesDbSQLiteOpenHelper.COLUMN_CHECKIN_DATE,
        FavouritesDbSQLiteOpenHelper.COLUMN_KEY};
    private SQLiteStatement countStatement;

    public FavouritesDbDataSource(Context context) {
        dbHelper = new FavouritesDbSQLiteOpenHelper(context);
    }

    private SQLiteDatabase getDatabase() throws SQLException {
        if (database == null) {
            database = dbHelper.getWritableDatabase();
        }
        return database;
    }

    public void close() {
        LoggerUtils.debug("Closing favouritesdb");
        if (database != null && database.isOpen()) {
            database.close();
        }
        database = null;
        dbHelper.close();
        dbHelper = null;
    }

    public FavouritesDAO addLandmark(ExtendedLandmark landmark, String key) {
        ContentValues values = new ContentValues();
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_ID, landmark.hashCode());
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_NAME, landmark.getName());
        double latitude = MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLatitude());
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_LATITUDE, latitude);
        double longitude = MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLongitude());
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_LONGITUDE, longitude);
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_LAYER, landmark.getLayer());
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_MAX_DISTANCE, 0);
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_CHECKIN_DATE, System.currentTimeMillis());
        values.put(FavouritesDbSQLiteOpenHelper.COLUMN_KEY, key);
        long insertId = getDatabase().insert(FavouritesDbSQLiteOpenHelper.TABLE_NAME, null, values);
        LoggerUtils.debug("Landmark added to favourites database with id: " + insertId);
        return new FavouritesDAO(landmark.hashCode(), landmark.getName(), latitude, longitude, landmark.getLayer(), 0, System.currentTimeMillis(), key);
    }

    public boolean hasLandmark(ExtendedLandmark landmark) {
        if (landmark != null) {
            if (countStatement == null) {
                String countSql = "SELECT COUNT(*) FROM " + FavouritesDbSQLiteOpenHelper.TABLE_NAME
                        + " where " + FavouritesDbSQLiteOpenHelper.COLUMN_ID + "=?";
                countStatement = getDatabase().compileStatement(countSql);
            }
            countStatement.bindLong(1, landmark.hashCode());
            long count = countStatement.simpleQueryForLong();
            return (count > 0);
        } else {
            return false;
        }
    }

    public int updateMaxDist(long newMaxDist, long hashcode) {
        ContentValues val = new ContentValues();
        val.put(FavouritesDbSQLiteOpenHelper.COLUMN_MAX_DISTANCE, newMaxDist);
        return getDatabase().update(FavouritesDbSQLiteOpenHelper.TABLE_NAME, val,
                FavouritesDbSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Long.toString(hashcode)});
    }

    public int updateOnCheckin(String key) {
        ContentValues val = new ContentValues();
        val.put(FavouritesDbSQLiteOpenHelper.COLUMN_MAX_DISTANCE, 0);
        val.put(FavouritesDbSQLiteOpenHelper.COLUMN_CHECKIN_DATE, System.currentTimeMillis());
        return getDatabase().update(FavouritesDbSQLiteOpenHelper.TABLE_NAME, val,
                FavouritesDbSQLiteOpenHelper.COLUMN_KEY + "=?", new String[]{key});
    }

    public int deleteLandmark(long hashcode) {
        LoggerUtils.debug("Favourite landmark deleted with id: " + hashcode);
        return getDatabase().delete(FavouritesDbSQLiteOpenHelper.TABLE_NAME, FavouritesDbSQLiteOpenHelper.COLUMN_ID
                + "=?", new String[]{Long.toString(hashcode)});
    }

    public FavouritesDAO getLandmark(long hashcode) {
        Cursor cursor = null;
        FavouritesDAO favourite = null;

        try {
            cursor = getDatabase().query(FavouritesDbSQLiteOpenHelper.TABLE_NAME,
                    allColumns, FavouritesDbSQLiteOpenHelper.COLUMN_ID + "=?", new String[]{Long.toString(hashcode)}, null, null, null);
            cursor.moveToFirst();
            favourite = cursorToLandmark(cursor);
        } catch (Exception e) {
            LoggerUtils.error("FavouritesDbDataSource.fetchAllLandmarks() exception:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return favourite;
    }

    public List<FavouritesDAO> fetchAllLandmarks() {
        List<FavouritesDAO> favourites = new ArrayList<FavouritesDAO>();

        Cursor cursor = null;

        try {
            cursor = getDatabase().query(FavouritesDbSQLiteOpenHelper.TABLE_NAME,
                    allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                FavouritesDAO favourite = cursorToLandmark(cursor);
                favourites.add(favourite);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            LoggerUtils.error("FavouritesDbDataSource.fetchAllLandmarks() exception:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return favourites;
    }

    private FavouritesDAO cursorToLandmark(Cursor cursor) {
        long id = cursor.getLong(0);
        String name = cursor.getString(1);
        double latitude = cursor.getDouble(2);
        double longitude = cursor.getDouble(3);
        String layer = cursor.getString(4);
        long maxDistance = cursor.getLong(5);
        long lastCheckinDate = cursor.getLong(6);
        String key = cursor.getString(7);
        return new FavouritesDAO(id, name, latitude, longitude, layer, maxDistance, lastCheckinDate, key);
    }
}
