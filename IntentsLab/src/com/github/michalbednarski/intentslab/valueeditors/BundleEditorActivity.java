package com.github.michalbednarski.intentslab.valueeditors;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.ListView;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

/**
 * Activity for editing (nested) Bundles
 *
 * @see com.github.michalbednarski.intentslab.valueeditors.framework.Editor.EditorActivity
 */
public class BundleEditorActivity extends FragmentActivity {

    private BundleAdapter mBundleAdapter;

    public static class DummyBundleEditorFragment extends Fragment implements BundleAdapter.BundleAdapterAggregate {
        private BundleAdapter mBundleAdapter;

        @Override
        public BundleAdapter getBundleAdapter() {
            return mBundleAdapter;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set title
        setTitle("Bundle: " + getIntent().getStringExtra(Editor.EXTRA_TITLE));

        // Get edited bundle
        Bundle bundle =
                savedInstanceState != null ?
                        savedInstanceState.getBundle(Editor.EXTRA_VALUE) :
                        getIntent().getBundleExtra(Editor.EXTRA_VALUE);

        // Create ListView
        ListView listView = new ListView(this);
        listView.setId(android.R.id.list);

        // Create or find dummy BundleAdapterAggregate
        DummyBundleEditorFragment dummyFragment;
        if (savedInstanceState == null) {
            dummyFragment = new DummyBundleEditorFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(dummyFragment, "BAAggregate")
                    .commit();
        } else {
            dummyFragment = (DummyBundleEditorFragment) getSupportFragmentManager().findFragmentByTag("BAAggregate");
        }

        // Create bundle adapter and use it
        mBundleAdapter = new BundleAdapter(this, bundle, new EditorLauncher(this, "editorLauncherInBundleEditor"), dummyFragment);
        mBundleAdapter.settleOnList(listView);
        dummyFragment.mBundleAdapter = mBundleAdapter;

        setContentView(listView);
    }

    @Override
    protected void onDestroy() {
        mBundleAdapter.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(Editor.EXTRA_VALUE, mBundleAdapter.getBundle());
    }

    // Return modified bundle on back press
    @Override
    public void onBackPressed() {
        setResult(
                0,
                new Intent()
                        .putExtra(Editor.EXTRA_VALUE, mBundleAdapter.getBundle())
        );
        finish();
    }
}