package com.example.testapp1.providerlab;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
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

        // Prepare table
        TableLayout table = new TableLayout(this);

        TableLayout.LayoutParams tableParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);

        // Table header
        {
            TableRow headerRow = new TableRow(this);
            for (String name : cursor.getColumnNames()) {
                TextView textView = new TextView(this);
                textView.setTypeface(Typeface.create((String) null, Typeface.BOLD));
                //textView.setPadding(2,0,0,0);
                textView.setText(name);
                headerRow.addView(textView);
            }
            table.addView(headerRow, rowParams);
        }

        // Rows
        int columnCount = cursor.getColumnCount();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            TableRow row = new TableRow(this);
            for (int i = 0; i < columnCount; i++) {
                //int type = cursor.getType(i);
                String value = cursor.getString(i);


                TextView textView = new TextView(this);
                //textView.setPadding(2,0,0,0);
                textView.setText(value);
                row.addView(textView);
            }
            table.addView(row, rowParams);
        }

        // Wrap in HorizontalScrollView in ScrollView and setContentView
        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        horizontalScrollView.addView(table, tableParams);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(horizontalScrollView);
        setContentView(scrollView);
    }
}