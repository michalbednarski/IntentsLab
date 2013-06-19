package com.example.testapp1.providerlab;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.Utils;

/**
 *
 */
public class QueryResultActivity extends Activity {
    private static final String TAG = "QueryResultActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            Log.e(TAG, "No uri");
            return;
        }
        String[] projection = intent.getStringArrayExtra(AdvancedQueryActivity.EXTRA_PROJECTION);
        String selection = intent.getStringExtra(AdvancedQueryActivity.EXTRA_SELECTION);
        String[] selectionArgs = intent.getStringArrayExtra(AdvancedQueryActivity.EXTRA_SELECTION_ARGS);
        if (selectionArgs == null) {
            selectionArgs = new String[0];
        }
        String order = intent.getStringExtra(AdvancedQueryActivity.EXTRA_SORT_ORDER);

        // query()
        Cursor cursor;
        try {
            cursor = getContentResolver().query(uri, projection, selection, selectionArgs, order);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.toastException(this, e);
            finish();
            return;
        }
        if (cursor == null) {
            Toast.makeText(this, String.format(getString(R.string.a_returned_b), "getContentResolver().query()", "null"), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DataGridView v = new DataGridView(this);
        v.setCursor(cursor);
        setContentView(v);
    }
}