package com.example.testapp1.providerlab;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.Utils;

/**
 *
 */
public class QueryResultActivity extends Activity {
    private static final String TAG = "QueryResultActivity";

    private String[] mColumnNames = null;

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

        mColumnNames = cursor.getColumnNames();

        DataGridView v = new DataGridView(this);
        v.setId(R.id.data);
        v.setCursor(cursor);
        setContentView(v);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.query_result, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_query: {
                Intent myIntent = getIntent();
                Intent queryEditorIntent =
                    new Intent(this, AdvancedQueryActivity.class)
                        .setData(myIntent.getData())
                        .putExtras(myIntent);
                String[] projection = myIntent.getStringArrayExtra(AdvancedQueryActivity.EXTRA_PROJECTION);
                if ((projection == null || projection.length == 0) && mColumnNames != null) {
                    queryEditorIntent.putExtra(AdvancedQueryActivity.EXTRA_PROJECTION_AVAILABLE_COLUMNS, mColumnNames);
                }
                startActivity(queryEditorIntent);
                return true;
            }
        }
        return false;
    }
}