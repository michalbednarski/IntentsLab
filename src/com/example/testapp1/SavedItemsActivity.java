package com.example.testapp1;

import android.app.ListActivity;
import android.os.Bundle;

/**
 * View list of saved items
 */
public class SavedItemsActivity extends ListActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_items);
        new SavedItemsDatabase(this).lazyAttachListAdapter(getListView());
    }
}