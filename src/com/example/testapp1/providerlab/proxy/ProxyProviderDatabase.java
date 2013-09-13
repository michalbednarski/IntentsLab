package com.example.testapp1.providerlab.proxy;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObservable;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;
import com.example.testapp1.Utils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by mb on 10.09.13.
 */
class ProxyProviderDatabase extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    private static ProxyProviderDatabase sInstance = null;

    public static ProxyProviderDatabase getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ProxyProviderDatabase(context);
        }
        return sInstance;
    }

    public final ContentObservable mContentObservable = new ContentObservable();

    private ProxyProviderDatabase(Context context) {
        super(context.getApplicationContext(), "ProxyProviderDatabase", null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 0:
                db.execSQL(
                        "CREATE TABLE operations (" +
                                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                "method INTEGER NOT NULL," +
                                "uri TEXT NOT NULL," +
                                "result," + /* no type */
                                "others TEXT," + /* JSON */
                                "exception TEXT," +
                                "uid INTEGER NOT NULL" +
                        ")"
                );
            // case 1: // current version
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { /* Do nothing */ }

    class OperationLogEntryBuilder {
        private ContentValues mContentValues = new ContentValues();
        private JSONObject mOthers = new JSONObject();

        OperationLogEntryBuilder(int method, Uri unwrappedUri) {
            mContentValues.put("method", method);
            mContentValues.put("uri", unwrappedUri.toString());
        }

        private OperationLogEntryBuilder setOtherValue(String name, String value) {
            try {
                mOthers.put("name", value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        private OperationLogEntryBuilder setOtherValue(String name, String[] value) {
            try {
                mOthers.put("name", Utils.toJsonArray(value));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return this;
        }


        OperationLogEntryBuilder setProjection(String[] projection) {
            return setOtherValue("projection", projection);
        }

        OperationLogEntryBuilder setSelection(String selection) {
            return setOtherValue("selection", selection);
        }

        OperationLogEntryBuilder setSelectionArgs(String[] selectionArgs) {
            return setOtherValue("selectionArgs", selectionArgs);
        }

        OperationLogEntryBuilder setSortOrder(String sortOrder) {
            return setOtherValue("sortOrder", sortOrder);
        }

        OperationLogEntryBuilder setValues(ContentValues values) {
            try {
                mOthers.put("values", Utils.contentValuesToJsonObject(values));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        OperationLogEntryBuilder setMode(String mode) {
            return setOtherValue("mode", mode);
        }

        OperationLogEntryBuilder setResult(String result) {
            mContentValues.put("result", result);
            return this;
        }

        OperationLogEntryBuilder setResult(int result) {
            mContentValues.put("result", result);
            return this;
        }

        OperationLogEntryBuilder setException(Throwable e) {
            mContentValues.put("exception", Utils.describeException(e));
            return this;
        }

        void writeToLog() {
            mContentValues.put("uid", Binder.getCallingUid());
            mContentValues.put("others", mOthers.toString());
            getWritableDatabase().insertOrThrow("operations", "uri", mContentValues);
            mContentObservable.dispatchChange(false);
        }
    }

    void clearLog() {
        getWritableDatabase().delete("operations", null, null);
        mContentObservable.dispatchChange(false);
    }
}
