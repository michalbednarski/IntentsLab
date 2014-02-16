package com.github.michalbednarski.intentslab;

import android.app.ListActivity;
import android.os.Bundle;

/**
 * View list of saved items
 */
public class SavedItemsActivity extends ListActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saved_items);
        SavedItemsDatabase.getInstance(this).lazyAttachListAdapter(getListView());
    }
}