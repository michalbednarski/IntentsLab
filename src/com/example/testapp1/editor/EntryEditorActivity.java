package com.example.testapp1.editor;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.testapp1.R;

public class EntryEditorActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_editor);
        ArrayAdapter<String> entryTypes = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        entryTypes.add("Boolean"); // + Array
        entryTypes.add("Byte"); // + Array
        entryTypes.add("Char"); // + Array
        entryTypes.add("Short"); // + Array
        entryTypes.add("Int"); // + Array + ArrayList<Integer>
        entryTypes.add("Long"); // + Array
        entryTypes.add("Float"); // + Array
        entryTypes.add("Double"); // + Array
        entryTypes.add("String"); // + Array + ArrayList
        entryTypes.add("CharSequence"); // + Array + ArrayList
        entryTypes.add("Parcelable"); // + Array + ArrayList + SparseArray
        entryTypes.add("Serializable");
        entryTypes.add("Bundle");
        entryTypes.add("IBinder");
        ((Spinner)findViewById(R.id.typesSpinner)).setAdapter(entryTypes);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_entry_editor, menu);
        return true;
    }
}
