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

package com.github.michalbednarski.intentslab.editor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;

/**
 * Created by mb on 21.08.13.
 */
public class NewExtraPickerDialog extends DialogFragment implements AdapterView.OnItemClickListener {

    /**
     * Default constructor, required and should by only used by framework
     */
    public NewExtraPickerDialog() {}

    /**
     * Constructor for use in BundleAdapter
     */
    NewExtraPickerDialog(IntentExtrasFragment intentExtrasFragment) {
        setTargetFragment(intentExtrasFragment, 0);
    }

    /**
     * Check if we're in IntentEditorActivity and if yes check if we are insterested in it's intent
     *
     * @return true if BundleAdapter should use this dialog, false to redirect it to {@link NewBundleEntryDialog}
     */
    public static boolean mayHaveSomethingForIntent(Activity activity) {
        if (!(activity instanceof IntentEditorActivity)) {
            return false;
        }
        final IntentEditorActivity intentEditor = ((IntentEditorActivity) activity);
        intentEditor.updateIntent();
        final Intent editedIntent = intentEditor.getEditedIntent();
        return !Utils.stringEmptyOrNull(editedIntent.getAction()) /*|| editedIntent.getComponent() != null*/;
    }

    /**
     * Get BundleAdapter this fragment is associated with, may change on configuration change
     * if instance is retained
     */
    private BundleAdapter getBundleAdapter() {
        return ((IntentExtrasFragment) getTargetFragment()).getBundleAdapter();
    }

    /**
     * Task for filling {@link #mSuggestionsByAction}
     */
    private NewExtraSuggesterByAction mFetchSuggestionsByActionTask = null;

    /**
     * Array of options under "By action" heading
     */
    private ExtraSuggestion[] mSuggestionsByAction = null;

    /**
     * Reserved for future use
     */
    private ExtraSuggestion[] mSuggestionsByClass = null;

    /**
     * Adapter for list in dialog
     */
    private BaseAdapter mAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return 1 +
                    (mSuggestionsByAction != null ? mSuggestionsByAction.length + 1 : 0) +
                    (mSuggestionsByClass != null ? mSuggestionsByClass.length + 1 : 0);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) {
                return 0;
            }
            position -= 1;
            if (mSuggestionsByAction != null) {
                if (position <= mSuggestionsByAction.length) {
                    return position + 100;
                }
                position -= mSuggestionsByAction.length + 1;
            }
            return position + 1000;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int dummy_import = R.layout.xml_viewer; // TODO: remove
            long id = getItemId(position);
            if (id == 100 || id == 1000) {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(android.R.layout.preference_category, parent, false);
                }
                ((TextView) convertView).setText(id == 100 ? "By action" : "By scanned class");

            } else {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                String text = "Other...";
                if (id > 1000) {
                    text = mSuggestionsByClass[((int) (id - 1001))].name;
                } else if (id > 100) {
                    text = mSuggestionsByAction[((int) (id - 101))].name;
                }
                ((TextView) convertView).setText(text);
            }
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            long id = getItemId(position);
            return (id == 100 || id == 1000) ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            long id = getItemId(position);
            return id != 100 && id != 1000;
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // "Other" option
        if (id == 0) {
            jumpToNewBundleEntryDialog(null);
            return;
        }

        // Get ExtraSuggestion
        ExtraSuggestion extraSuggestion =
                id > 1000 ?
                        mSuggestionsByClass[((int) (id - 1001))] :
                        mSuggestionsByAction[((int) (id - 101))];

        String type = extraSuggestion.type;

        // Ask user if type is unknown
        if (type == null) {
            jumpToNewBundleEntryDialog(extraSuggestion.name);
        }

        // Auto-expand "java/lang/"
        if (type.indexOf('.') == -1) {
            type = "java.lang." + type;
        }

        // For boolean we just set them to true
        if (type.equals("java.lang.Boolean")) {
            getBundleAdapter().onEditorResult(extraSuggestion.name, true);
            dismiss();
            return;
        }

        // Instantiate new object and pass it to editor
        final Object initialValue;
        try {
            Class c = Class.forName(type);
            initialValue = c.newInstance();
        } catch (ClassNotFoundException e) {
            Utils.toastException(getActivity(), "Class.forName", e);
            return;
        } catch (java.lang.InstantiationException e) {
            Utils.toastException(getActivity(), "aClass.newInstance", e);
            return;
        } catch (IllegalAccessException e) {
            Utils.toastException(getActivity(), "aClass.newInstance", e);
            return;
        }
        dismiss();
        getBundleAdapter().launchEditorForNewEntry(extraSuggestion.name, initialValue);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        IntentEditorActivity intentEditor = (IntentEditorActivity) getActivity();
        intentEditor.updateIntent();
        final Intent editedIntent = intentEditor.getEditedIntent();
        String action = editedIntent.getAction();
        if (!Utils.stringEmptyOrNull(action)) {
            mFetchSuggestionsByActionTask = new NewExtraSuggesterByAction(intentEditor, action, false, this);
            mFetchSuggestionsByActionTask.execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView listView = new ListView(getActivity());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        Utils.fixListViewInDialogBackground(listView);
        return listView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        try {
            // Work around dismiss on rotation after setRetainInstance(true)
            // http://stackoverflow.com/a/13596466
            getDialog().setOnDismissListener(null);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        if (mFetchSuggestionsByActionTask != null) {
            mFetchSuggestionsByActionTask.cancel(true);
        }
        super.onDestroy();
    }

    public static class ExtraSuggestion {
        String name;
        String type;
    }

    void onExtrasByActionReceived(ExtraSuggestion[] suggestions) {
        mFetchSuggestionsByActionTask = null;
        if (suggestions != null && suggestions.length == 0) {
            suggestions = null;
        }
        if (suggestions != mSuggestionsByAction) {
            mSuggestionsByAction = suggestions;
            mAdapter.notifyDataSetChanged();
        }
        skipSuggestionDialogIfNotNeeded();
    }

    void onExtrasByClassReceived(ExtraSuggestion[] suggestions) { // TODO
        if (suggestions != null && suggestions.length == 0) {
            suggestions = null;
        }
        if (suggestions != mSuggestionsByClass) {
            mSuggestionsByClass = suggestions;
            mAdapter.notifyDataSetChanged();
        }
        skipSuggestionDialogIfNotNeeded();
    }


    /**
     * Check if we have no data nor we are not loading some and if so replace ourselves with {@link NewBundleEntryDialog}
     */
    private void skipSuggestionDialogIfNotNeeded() {
        if (mSuggestionsByAction == null && mFetchSuggestionsByActionTask == null && mSuggestionsByClass == null) {
            jumpToNewBundleEntryDialog(null);
        }
    }

    /**
     * Replace this dialog with {@link NewBundleEntryDialog}
     */
    private void jumpToNewBundleEntryDialog(String defaultName) {
        dismiss();
        NewBundleEntryDialog newBundleEntryDialog = new NewBundleEntryDialog(getTargetFragment());
        if (defaultName != null) {
            Bundle arguments = new Bundle();
            arguments.putString(NewBundleEntryDialog.ARG_DEFAULT_NAME, defaultName);
            newBundleEntryDialog.setArguments(arguments);
        }
        newBundleEntryDialog.show(getFragmentManager(), "newBundleEntryFallback");
    }
}
