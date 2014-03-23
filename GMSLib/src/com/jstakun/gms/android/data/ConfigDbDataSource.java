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
import com.jstakun.gms.android.utils.LoggerUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author jstakun
 */
public class ConfigDbDataSource {

    private static SQLiteDatabase database;
    private static ConfigDbSQLiteOpenHelper dbHelper;
    private String[] allColumns = {ConfigDbSQLiteOpenHelper.COLUMN_KEY,
        ConfigDbSQLiteOpenHelper.COLUMN_VALUE
    };
    private SQLiteStatement countStatement;

    public ConfigDbDataSource(Context context) {
        dbHelper = new ConfigDbSQLiteOpenHelper(context);
    }

    private SQLiteDatabase getDatabase() throws SQLException {
        if (database == null) {
            database = dbHelper.getWritableDatabase();
        }
        return database;
    }

    public void close() {
        LoggerUtils.debug("Closing configdb");
        if (database != null && database.isOpen()) {
            database.close();
        }
        database = null;
        dbHelper.close();
        dbHelper = null;
    }

    private int updateConfig(String key, String value) {
        ContentValues val = new ContentValues();
        val.put(ConfigDbSQLiteOpenHelper.COLUMN_VALUE, value);
        return getDatabase().update(ConfigDbSQLiteOpenHelper.TABLE_NAME, val,
                ConfigDbSQLiteOpenHelper.COLUMN_KEY + "=?", new String[]{key});
    }

    private long insertConfig(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(ConfigDbSQLiteOpenHelper.COLUMN_KEY, key);
        values.put(ConfigDbSQLiteOpenHelper.COLUMN_VALUE, value);
        return getDatabase().insert(ConfigDbSQLiteOpenHelper.TABLE_NAME, null, values);
    }

    public Map<String, String> fetchAllConfig() {
        Map<String, String> config = new HashMap<String, String>();

        Cursor cursor = null;

        try {
            cursor = getDatabase().query(ConfigDbSQLiteOpenHelper.TABLE_NAME,
                    allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String key = cursor.getString(0);
                String value = cursor.getString(1);
                config.put(key, value);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            LoggerUtils.error("ConfigDbDataSource.fetchAllConfig() exception:", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return config;
    }

    public boolean putAll(Map<String, String> config) {
        boolean success = true;
        try {
            getDatabase().beginTransaction();
            for (Iterator<Map.Entry<String, String>> iter = config.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<String, String> configParam = iter.next();
                String key = configParam.getKey();
                String value = configParam.getValue();
                if (hasConfig(key)) {
                    updateConfig(key, value);
                } else {
                    insertConfig(key, value);
                }
            }
            getDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            success = false;
            LoggerUtils.error("ConfigDbDataSource.putAll() exception:", e);
        } finally {
            try {
                getDatabase().endTransaction();
            } catch (Exception e) {
                LoggerUtils.error("ConfigDbDataSource.putAll() exception:", e);
            }
        }
        return success;
    }

    private boolean hasConfig(String key) {
        if (countStatement == null) {
            String countSql = "SELECT COUNT(*) FROM " + ConfigDbSQLiteOpenHelper.TABLE_NAME
                    + " where " + ConfigDbSQLiteOpenHelper.COLUMN_KEY + "=?";
            countStatement = getDatabase().compileStatement(countSql);
        }
        countStatement.bindString(1, key);
        long count = countStatement.simpleQueryForLong();

        return (count > 0);
    }

    public void deleteConfigParam(String key) {
        try {
            LoggerUtils.debug("Deleted config param with key: " + key);
            getDatabase().delete(ConfigDbSQLiteOpenHelper.TABLE_NAME, ConfigDbSQLiteOpenHelper.COLUMN_KEY
                    + "=?", new String[]{key});
        } catch (Exception e) {
            LoggerUtils.error("ConfigDbDataSource.deleteConfigParam exception:", e);
        }
    }
    
    public void delete() {
    	try {
            int count = getDatabase().delete(ConfigDbSQLiteOpenHelper.TABLE_NAME, "1", null);
            LoggerUtils.debug("Deleted " + count + " rows from config database");
        } catch (Exception e) {
            LoggerUtils.error("ConfigDbDataSource.delete exception:", e);
        }
    }
}
