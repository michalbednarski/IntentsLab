package com.example.testapp1.providerlab;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.Utils;

/**
 *
 */
public class QueryResultActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri == null) {
            return;
        }

        // query()
        Cursor cursor;
        try {
            cursor = getContentResolver().query(uri, null, null, new String[0], null);
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