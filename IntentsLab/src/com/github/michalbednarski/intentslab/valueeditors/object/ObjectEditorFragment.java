/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab.valueeditors.object;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorFragment;

/**
 * Activity for editing general objects
 */
public class ObjectEditorFragment extends ValueEditorFragment implements ObjectEditorHelper.ObjectEditorHelperCallback {

    private static final String STATE_SHOW_NON_PUBLIC_FIELDS = "ObjectEditorActivity.showNonPublicFields";

    private boolean mShowNonPublicFields = false;

    private InlineValueEditorsLayout mInlineValueEditorsLayout;
    private ObjectEditorHelper mObjectEditorHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Load saved state
        if (savedInstanceState != null) {
            mShowNonPublicFields = savedInstanceState.getBoolean(STATE_SHOW_NON_PUBLIC_FIELDS);
        }

        // Init EditorLauncher and ObjectEditorHelper
        EditorLauncher editorLauncher = new EditorLauncher(getActivity(), "PaScEdLa");
        boolean sandboxed = false;
        SandboxedObject sandboxedObject = getSandboxedEditedObject();
        Object object;
        try {
            object = sandboxedObject.unwrap(null);
        } catch (Exception e) {
            sandboxed = true;
            object = null; // Unused assignment, just for compiler
        }
        if (sandboxed) {
            final SandboxedObjectEditorHelper helper = new SandboxedObjectEditorHelper(getActivity(), sandboxedObject, editorLauncher, this);
            editorLauncher.setCallback(helper);
            mObjectEditorHelper = helper;
            helper.initializeAndRunWhenReady(new Runnable() {
                @Override
                public void run() {
                    invokeGetters();
                    ActivityCompat.invalidateOptionsMenu(getActivity());
                }
            });
        } else {
            final LocalObjectEditorHelper helper = new LocalObjectEditorHelper(getActivity(), object, editorLauncher, this);
            editorLauncher.setCallback(helper);
            mObjectEditorHelper = helper;
        }

        // Enable options menu
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Prepare getters TextView
        mGettersOutput = new TextView(getActivity());

        // Set up final layout
        mInlineValueEditorsLayout = new InlineValueEditorsLayout(getActivity());
        mInlineValueEditorsLayout.setValueEditors(mObjectEditorHelper.getInlineValueEditors());
        mInlineValueEditorsLayout.addHeaderOrFooter(mGettersOutput);
        mInlineValueEditorsLayout.setHiddenEditorsVisible(mShowNonPublicFields);

        // Invoke getters
        invokeGetters();

        return mInlineValueEditorsLayout;
    }

    @Override
    public void onDestroyView() {
        mGettersOutput = null;
        mInlineValueEditorsLayout = null;
        super.onDestroyView();
    }

    @Override
    protected Object getEditorResult() {
        return mObjectEditorHelper.getObject();
    }

    @Override
    public boolean isEditorResultSandboxed() {
        return mObjectEditorHelper instanceof SandboxedObjectEditorHelper;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHOW_NON_PUBLIC_FIELDS, mShowNonPublicFields);
    }

    @Override
    public void onDestroy() {
        if (mObjectEditorHelper instanceof SandboxedObjectEditorHelper) {
            ((SandboxedObjectEditorHelper) mObjectEditorHelper).shutdown();
        }
        super.onDestroy();
    }



    // Options menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.parcelable_strucutre_editor, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.show_non_public_fields)
                .setVisible(mObjectEditorHelper.hasNonPublicFields())
                .setChecked(mShowNonPublicFields);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_non_public_fields:
                mShowNonPublicFields = !mShowNonPublicFields;
                mInlineValueEditorsLayout.setHiddenEditorsVisible(mShowNonPublicFields);
                ActivityCompat.invalidateOptionsMenu(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onModified() {
        markAsModified();

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
        if (mGettersOutput != null) {
            mGettersOutput.setText(mObjectEditorHelper.getGetterValues());
        }
    }


    // Starting this editor from other types
    public static class LaunchableEditor implements Editor.FragmentEditor {

        @Override
        public boolean canEdit(Object value) {
            return value instanceof Parcelable; // TODO: stricter checking
        }

        @Override
        public Class<? extends ValueEditorFragment> getEditorFragment() {
            return ObjectEditorFragment.class;
        }
    }
}