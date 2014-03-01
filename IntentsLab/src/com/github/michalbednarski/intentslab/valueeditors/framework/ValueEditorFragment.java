package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

/**
 * Created by mb on 27.02.14.
 */
public abstract class ValueEditorFragment extends Fragment {
    final static String ARG_EDITED_OBJECT = "SingleEditorActivity.editedObject";
    private static final String STATE_MODIFIED = "ObjectEditorActivity.modified";
    private static final String STATE_MODIFIED_SANDBOXED_OBJECT = "ValueEditorFragment.modifiedObj";



    private SandboxedObject mModifiedSandboxedObject = null;
    boolean mModified = false;

    /**
     * Get modified value, this must be overriden
     * and will be called only if you call {@link #markAsModified()}
     *
     * See also: {@link #isEditorResultSandboxed()}
     */
    protected abstract Object getEditorResult();

    /**
     * Is value returned by {@link #getEditorResult()} sandboxed?
     */
    public boolean isEditorResultSandboxed() {
        return false;
    }

    /**
     * Get original or retained object to be edited
     */
    protected final SandboxedObject getSandboxedEditedObject() {
        return mModifiedSandboxedObject != null ? mModifiedSandboxedObject :
                (isStandalone() ?
                getActivity().getIntent().<SandboxedObject>getParcelableExtra(ARG_EDITED_OBJECT) :
                getArguments().<SandboxedObject>getParcelable(ARG_EDITED_OBJECT));
    }

    /**
     * Mark object as modified, this must be called in order to save modifications
     */
    protected void markAsModified() {
        if (!mModified) {
            mModified = true;
            ActivityCompat.invalidateOptionsMenu(getActivity());
        }
    }



    /**
     * True if this editor is running alone in activity,
     * as opposed to running in MasterDetailActivity
     */
    private boolean isStandalone() {
        return getActivity() instanceof SingleEditorActivity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state
        if (savedInstanceState != null) {
            mModified = savedInstanceState.getBoolean(STATE_MODIFIED);
            mModifiedSandboxedObject = savedInstanceState.getParcelable(STATE_MODIFIED_SANDBOXED_OBJECT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_MODIFIED, mModified);
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.editor_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.discard)
                .setVisible(mModified && isStandalone());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discard:
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.discard_changes_confirm))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getActivity().finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
