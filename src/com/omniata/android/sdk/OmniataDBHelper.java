package com.omniata.android.sdk;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* package */ class OmniataDBHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "omniata.db";
	private static final int DATABASE_VERSION = 1;
	
	private String name;
	
	public OmniataDBHelper(Context context, String name) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.name = name;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String TABLE_CREATE = "CREATE TABLE " + name
	 						+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
	 						+ "data TEXT);";
		
		db.execSQL(TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		
	}
	
	public static Cursor all(SQLiteDatabase db, String name) {
		return db.rawQuery("SELECT id, data FROM " + name + ";", null);
	}
	
	public static void deleteAll(SQLiteDatabase db, String name) {
		db.delete(name, null, null);
	}
	
	public static int delete(SQLiteDatabase db, String name, int id) {
		String[] whereArgs = {String.valueOf(id)};
		return db.delete(name, "id = ?", whereArgs);
	}
	
	public static Cursor first(SQLiteDatabase db, String name) {
		return db.rawQuery("SELECT id, data FROM " + name + " LIMIT 1;", null);
	}
	
	public static long insert(SQLiteDatabase db, String name, String data) {
		ContentValues values = new ContentValues();
		values.put("data", data);
		return db.insert(name, null, values);
	}
	
	public static void resetAutoIncrement(SQLiteDatabase db, String name) {
		String[] whereArgs = {name};
		db.delete("sqlite_sequence", "name = ?", whereArgs);
	}
	
	public static int size(SQLiteDatabase db, String name) {
		Cursor c = db.rawQuery("SELECT count(id) FROM " + name + ";", null);
		c.moveToFirst();
		int sz = c.getInt(0);
		c.close();
		return sz;
	}
}
