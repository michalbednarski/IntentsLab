package com.github.michalbednarski.intentslab.valueeditors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorFragment;

/**
 * Activity for editing (nested) Bundles
 *
 * @see com.github.michalbednarski.intentslab.valueeditors.framework.Editor.EditorActivity
 */
public class BundleEditorFragment extends ValueEditorFragment implements BundleAdapter.BundleAdapterAggregate {

    private BundleAdapter<BundleEditorFragment> mBundleAdapter;

    @Override
    public BundleAdapter getBundleAdapter() {
        return mBundleAdapter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setRetainInstance(true); // TODO: Enable instance retaining
        setHasOptionsMenu(true);

        // Set title
        //setTitle("Bundle: " + getIntent().getStringExtra(Editor.EXTRA_TITLE));

        // Get edited bundle
        Bundle bundle = (Bundle) getSandboxedEditedObject().unwrap(null);

        // Create bundle adapter and use it
        mBundleAdapter = new BundleAdapter<BundleEditorFragment>(bundle, new EditorLauncher(getActivity(), "editorLauncherInBundleEditor"), this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create ListView
        ListView listView = new ListView(getActivity());
        listView.setId(android.R.id.list);
        mBundleAdapter.settleOnList(listView);
        return listView;
    }

    @Override
    public void onDestroy() {
        mBundleAdapter.shutdown();
        super.onDestroy();
    }

    // Return modified bundle
    @Override
    protected Object getEditorResult() {
        return mBundleAdapter.getBundle();
    }

    @Override
    public void onBundleModified() {
        markAsModified();
    }

    // For EditorLauncher
    public static class LaunchableEditor implements Editor.FragmentEditor {
        @Override
        public boolean canEdit(Object value) {
            return value instanceof Bundle;
        }

        @Override
        public Class<? extends ValueEditorFragment> getEditorFragment() {
            return BundleEditorFragment.class;
        }
    }
}