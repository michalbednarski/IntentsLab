package com.example.testapp1.valueeditors;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import com.example.testapp1.editor.BundleAdapter;

/**
 * Activity for editing (nested) Bundles
 *
 * @see Editor.EditorActivity
 */
public class BundleEditorActivity extends ListActivity {
    private static final int REQUEST_CODE_EDIT_ITEM = 1;

    private EditorLauncher.ActivityResultHandler mActivityResultHandler;
    private BundleAdapter mBundleAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set title
        setTitle("Bundle: " + getIntent().getStringExtra(Editor.EXTRA_KEY));

        // Activity result handler for EditorLauncher
        mActivityResultHandler = new EditorLauncher.ActivityResultHandler(this, REQUEST_CODE_EDIT_ITEM);

        // Get edited bundle
        Bundle bundle =
                savedInstanceState != null ?
                        savedInstanceState.getBundle(Editor.EXTRA_VALUE) :
                        getIntent().getBundleExtra(Editor.EXTRA_VALUE);

        // Create bundle adapter and use it
        mBundleAdapter = new BundleAdapter(this, bundle, mActivityResultHandler);
        mBundleAdapter.settleOnList(getListView());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(Editor.EXTRA_VALUE, mBundleAdapter.getBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDIT_ITEM) {
            mActivityResultHandler.handleActivityResult(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Return modified bundle on back press
    @Override
    public void onBackPressed() {
        setResult(
                0,
                new Intent()
                        .putExtra(Editor.EXTRA_KEY, getIntent().getStringExtra(Editor.EXTRA_KEY))
                        .putExtra(Editor.EXTRA_VALUE, mBundleAdapter.getBundle())
        );
        finish();
    }
}