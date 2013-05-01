package com.example.testapp1.browser;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class AppsDB {
	private static final int DB_VERSION = 1;
	private static final String DB_SCHEMA =
			"DROP TABLE *;" +
			"CREATE TABLE [apps] (" +
					"[appId] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
					"[packageName] TEXT NOT NULL" +
			");" +
			"CREATE TABLE [components] (" +
				"[componentId] INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
				"[appId] INTEGER NOT NULL REFERENCES apps(appId)," +
				"[componentType] INTEGER NOT NULL," +
				"[action] TEXT NOT NULL" +
			");";

	private static AppsDB sInstance = null;
	public static AppsDB getInstance(Context context) {
		if (sInstance == null) {
			sInstance = new AppsDB(context);
		}
		return sInstance;
	}

	private SQLiteDatabase mDB = null;
	private String mDBPath;

	private AppsDB(Context context) {
		mDBPath = context.getCacheDir().getPath() + "/apps.db";
	}

	private void openDatabase(boolean mayCreate) {
		if (mDB == null || !mDB.isOpen()) {
			mDB = SQLiteDatabase.openDatabase(mDBPath, null, mayCreate ? SQLiteDatabase.CREATE_IF_NECESSARY : 0);
			if (mDB.getVersion() != DB_VERSION) {
				mDB.execSQL(DB_SCHEMA);
				mDB.setVersion(DB_VERSION);
			}
		}
	}

	SQLiteDatabase getDatabase(boolean mayCreate) {
		openDatabase(mayCreate);
		return mDB;
	}
}
