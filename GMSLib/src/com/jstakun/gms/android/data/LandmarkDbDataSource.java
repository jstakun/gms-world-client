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

import com.jstakun.gms.android.config.Commons;
import com.jstakun.gms.android.landmarks.ExtendedLandmark;
import com.jstakun.gms.android.landmarks.LandmarkFactory;
import com.jstakun.gms.android.utils.LoggerUtils;
import com.jstakun.gms.android.utils.MercatorUtils;
import com.openlapi.QualifiedCoordinates;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author jstakun
 */
public class LandmarkDbDataSource {

    private static SQLiteDatabase database;
    private static LandmarkDbSQLiteOpenHelper dbHelper;
    private String[] allColumns = {LandmarkDbSQLiteOpenHelper.COLUMN_ID,
        LandmarkDbSQLiteOpenHelper.COLUMN_NAME,
        LandmarkDbSQLiteOpenHelper.COLUMN_LATITUDE,
        LandmarkDbSQLiteOpenHelper.COLUMN_LONGITUDE,
        LandmarkDbSQLiteOpenHelper.COLUMN_DESCRIPTION,
        LandmarkDbSQLiteOpenHelper.COLUMN_CREATION_DATE
    };

    public LandmarkDbDataSource(Context context) {
        dbHelper = new LandmarkDbSQLiteOpenHelper(context);
    }

    private SQLiteDatabase getDatabase() throws SQLException {
        if (database == null) {
            database = dbHelper.getWritableDatabase();
        }
        return database;
    }

    public void close() {
        LoggerUtils.debug("Closing landmarkdb");
        if (database != null && database.isOpen()) {
            database.close();
        }
        database = null;
        dbHelper.close();
        dbHelper = null;
    }

    public void addAll(List<ExtendedLandmark> landmarks) {
        for (Iterator<ExtendedLandmark> iter = landmarks.iterator(); iter.hasNext();) {
            ExtendedLandmark landmark = iter.next();
            addLandmark(landmark);
        }
    }

    public long addLandmark(ExtendedLandmark landmark) {
        ContentValues values = new ContentValues();
        values.put(LandmarkDbSQLiteOpenHelper.COLUMN_ID, landmark.hashCode());
        values.put(LandmarkDbSQLiteOpenHelper.COLUMN_NAME, landmark.getName());
        values.put(LandmarkDbSQLiteOpenHelper.COLUMN_LATITUDE, MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLatitude()));
        values.put(LandmarkDbSQLiteOpenHelper.COLUMN_LONGITUDE, MercatorUtils.normalizeE6(landmark.getQualifiedCoordinates().getLongitude()));
        values.put(LandmarkDbSQLiteOpenHelper.COLUMN_DESCRIPTION, landmark.getDescription());
        values.put(LandmarkDbSQLiteOpenHelper.COLUMN_CREATION_DATE, landmark.getCreationDate());
        long insertId = getDatabase().insert(LandmarkDbSQLiteOpenHelper.TABLE_NAME, null, values);
        return insertId;
    }

    public int deleteLandmark(ExtendedLandmark landmark) {
        long id = landmark.hashCode();
        LoggerUtils.debug("Landmark deleted with id: " + id);

        //String whereClause = LandmarkDbSQLiteOpenHelper.COLUMN_ID  + "=?";
        //return getDatabase().delete(LandmarkDbSQLiteOpenHelper.TABLE_NAME, whereClause, new String[]{Long.toString(id)});

        String whereClause = LandmarkDbSQLiteOpenHelper.COLUMN_NAME + "=? AND "
                + LandmarkDbSQLiteOpenHelper.COLUMN_LATITUDE + "=? AND "
                + LandmarkDbSQLiteOpenHelper.COLUMN_LONGITUDE + "=?";

        String[] selectionArgs = new String[]{landmark.getName(),
            Double.toString(landmark.getQualifiedCoordinates().getLatitude()),
            Double.toString(landmark.getQualifiedCoordinates().getLongitude())};

        Cursor cursor = null;

        int count = 0;

        try {
            cursor = getDatabase().query(LandmarkDbSQLiteOpenHelper.TABLE_NAME, null, whereClause, selectionArgs, null, null, null);
            count = cursor.getCount();
        } catch (Exception e) {
            LoggerUtils.error("LandmarkDbDataSource.deleteLandmark() exception:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (count > 0) {
            return getDatabase().delete(LandmarkDbSQLiteOpenHelper.TABLE_NAME, whereClause, selectionArgs);
        } else {
            return -1;
        }
    }

    public List<ExtendedLandmark> fetchAllLandmarks() {
        List<ExtendedLandmark> landmarks = new ArrayList<ExtendedLandmark>();
        Cursor cursor = null;

        try {
            cursor = getDatabase().query(LandmarkDbSQLiteOpenHelper.TABLE_NAME,
                    allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ExtendedLandmark landmark = cursorToLandmark(cursor);
                landmarks.add(landmark);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            LoggerUtils.error("LandmarkDbDataSource.fetchAllLandmarks() exception:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return landmarks;
    }

    private ExtendedLandmark cursorToLandmark(Cursor cursor) {
        long id = cursor.getLong(0);
        String name = cursor.getString(1);
        double latitude = cursor.getDouble(2);
        double longitude = cursor.getDouble(3);
        String details = cursor.getString(4);
        long creationDate = cursor.getLong(5);
        QualifiedCoordinates qc = new QualifiedCoordinates(latitude, longitude, 0f, Float.NaN, Float.NaN);
        return LandmarkFactory.getLandmark(name, details, qc, Commons.LOCAL_LAYER, creationDate);
    }
}
