package com.github.michalbednarski.intentslab.valueeditors.object;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

import java.io.Serializable;

/**
 * Activity for editing general objects
 */
public class ObjectEditorActivity extends FragmentActivity implements ObjectEditorHelper.ObjectEditorHelperCallback {
    public static final String EXTRA_VALUE_IS_SANDBOXED = "ObjectEditorActivity.valueIsSandboxed";

    private static final String STATE_SHOW_NON_PUBLIC_FIELDS = "ObjectEditorActivity.showNonPublicFields";
    private static final String STATE_MODIFIED = "ObjectEditorActivity.modified";

    private boolean mShowNonPublicFields = false;
    private boolean mModified = false;

    private InlineValueEditorsLayout mInlineValueEditorsLayout;
    private ObjectEditorHelper mObjectEditorHelper;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get edited object and/or load saved state
        Object object;
        if (savedInstanceState != null) {
            object = savedInstanceState.get(Editor.EXTRA_VALUE);
            mShowNonPublicFields = savedInstanceState.getBoolean(STATE_SHOW_NON_PUBLIC_FIELDS);
            mModified = savedInstanceState.getBoolean(STATE_MODIFIED);
        } else {
            object = getIntent().getExtras().get(Editor.EXTRA_VALUE);
        }
        boolean sandboxed = getIntent().getBooleanExtra(EXTRA_VALUE_IS_SANDBOXED, false);
        assert object != null;

        // Prepare getters TextView
        mGettersOutput = new TextView(this);

        // Init EditorLauncher and ObjectEditorHelper
        EditorLauncher editorLauncher = new EditorLauncher(this, "PaScEdLa");
        if (sandboxed) {
            final SandboxedObjectEditorHelper helper = new SandboxedObjectEditorHelper(this, (SandboxedObject) object, editorLauncher, this);
            editorLauncher.setCallback(helper);
            mObjectEditorHelper = helper;
            helper.initializeAndRunWhenReady(new Runnable() {
                @Override
                public void run() {
                    makeEditorsLayout();
                }
            });
        } else {
            final LocalObjectEditorHelper helper = new LocalObjectEditorHelper(this, object, editorLauncher, this);
            editorLauncher.setCallback(helper);
            mObjectEditorHelper = helper;
            makeEditorsLayout();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final Object object = mObjectEditorHelper.getObject();
        try {
            outState.putParcelable(Editor.EXTRA_VALUE, (Parcelable) object);
        } catch (ClassCastException e) {
            outState.putSerializable(Editor.EXTRA_VALUE, (Serializable) object);
        }
        outState.putBoolean(STATE_SHOW_NON_PUBLIC_FIELDS, mShowNonPublicFields);
        outState.putBoolean(STATE_MODIFIED, mModified);
    }

    @Override
    protected void onDestroy() {
        if (mObjectEditorHelper instanceof SandboxedObjectEditorHelper) {
            ((SandboxedObjectEditorHelper) mObjectEditorHelper).shutdown();
        }
        super.onDestroy();
    }

    private void makeEditorsLayout() {
        // Set up final layout
        mInlineValueEditorsLayout = new InlineValueEditorsLayout(this);
        mInlineValueEditorsLayout.setValueEditors(mObjectEditorHelper.getInlineValueEditors());
        mInlineValueEditorsLayout.addHeaderOrFooter(mGettersOutput);
        mInlineValueEditorsLayout.setHiddenEditorsVisible(mShowNonPublicFields);
        setContentView(mInlineValueEditorsLayout);

        // Invoke getters
        invokeGetters();
    }


    @Override
    public void onBackPressed() {
        if (mModified) {
            final Object object = mObjectEditorHelper.getObject();
            try {
                setResult(0, new Intent().putExtra(Editor.EXTRA_VALUE, (Parcelable) object));
            } catch (ClassCastException e) {
                setResult(0, new Intent().putExtra(Editor.EXTRA_VALUE, (Serializable) object));
            }
        }
        finish();
    }

    // Options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.parcelable_strucutre_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.show_non_public_fields)
                .setVisible(mObjectEditorHelper.hasNonPublicFields())
                .setChecked(mShowNonPublicFields);

        menu.findItem(R.id.discard)
                .setVisible(mModified);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_non_public_fields:
                mShowNonPublicFields = !mShowNonPublicFields;
                mInlineValueEditorsLayout.setHiddenEditorsVisible(mShowNonPublicFields);
                ActivityCompat.invalidateOptionsMenu(this);
                return true;
            case R.id.discard:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.discard_changes_confirm))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onModified() {
        if (!mModified) {
            mModified = true;
            ActivityCompat.invalidateOptionsMenu(this);
        }

        invokeGetters();
    }

    // Getters
    TextView mGettersOutput;

    /**
     * Invoke getter methods of edited object
     * and show their results in {@link #mGettersOutput} TextView
     *
     * This is called at initialization and every object modification
     */
    private void invokeGetters() {
        mGettersOutput.setText(mObjectEditorHelper.getGetterValues());
    }


    // Starting this editor from other types
    public static class LaunchableEditor extends Editor.EditorActivity {

        @Override
        public boolean canEdit(Object value) {
            return value instanceof Parcelable; // TODO: stricter checking
        }

        @Override
        public Intent getEditorIntent(Context context) {
            return new Intent(context, ObjectEditorActivity.class);
        }
    }
}