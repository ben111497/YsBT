package com.orange.obd.test.utils

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "app.db"
private const val DB_VERSION = 1

class SqlDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun exec(sql: String) {
        writableDatabase.apply {
            beginTransaction()
            try {
                execSQL(sql)
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    fun query(sql: String): ArrayList<MutableMap<String, Any>> {
        val db = readableDatabase
        val cursor: Cursor = db.rawQuery(sql, null)
        val list = ArrayList<MutableMap<String, Any>>()

        cursor.use { cur ->
            while (cur.moveToNext()) {
                val row = mutableMapOf<String, Any>()
                for (i in 0 until cur.columnCount) {
                    val name = cur.getColumnName(i)
                    val value = when (cur.getType(i)) {
                        Cursor.FIELD_TYPE_INTEGER -> cur.getLong(i)
                        Cursor.FIELD_TYPE_FLOAT   -> cur.getDouble(i)
                        Cursor.FIELD_TYPE_STRING  -> cur.getString(i)
                        Cursor.FIELD_TYPE_BLOB    -> cur.getBlob(i)
                        Cursor.FIELD_TYPE_NULL    -> ""
                        else                      -> ""
                    }
                    row[name] = value
                }
                list.add(row)
            }
        }

        return list
    }
}
