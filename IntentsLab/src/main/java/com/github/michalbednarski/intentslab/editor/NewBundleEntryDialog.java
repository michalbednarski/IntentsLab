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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;

/**
 * Dialog used for creating bundle entry without suggestions
 */
public class NewBundleEntryDialog extends DialogFragment {
    public static final String ARG_DEFAULT_NAME = "NewBundleEntry.defaultName";

    /**
     * Type values, must match array used in {@link #onCreateDialog(android.os.Bundle)}
     */
    private static final int TYPE_STRING_OR_NUMBER = 0;
    private static final int TYPE_BOOLEAN = 1;


    /**
     * Default constructor, required and should by only used by framework
     */
    public NewBundleEntryDialog() {}

    /**
     * Constructor for use in BundleAdapter
     *
     * @param fragment Fragment hosting BundleAdapter, must implement {@link BundleAdapter.BundleAdapterAggregate}
     */
    public NewBundleEntryDialog(Fragment fragment) {
        if (!(fragment instanceof BundleAdapter.BundleAdapterAggregate)) {
            throw new RuntimeException("fragment must implement BundleAdapterAggregate");
        }
        setTargetFragment(fragment, 0);
    }

    /**
     * Get BundleAdapter this fragment is associated with, may change on configuration change
     * if instance is retained
     */
    private BundleAdapter getBundleAdapter() {
        return ((BundleAdapter.BundleAdapterAggregate) getTargetFragment()).getBundleAdapter();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Inflate layout
        View view = getActivity().getLayoutInflater().inflate(R.layout.add_extra, null);

        // Find and fill name field
        Bundle arguments = getArguments();
        final TextView nameTextView = (TextView) view.findViewById(R.id.name);
        if (arguments != null) {
            nameTextView.setText(arguments.getString(ARG_DEFAULT_NAME));
        }

        // Find and fill type Spinner
        final Spinner typeSpinner = (Spinner) view.findViewById(R.id.typespinner);
        typeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, new String[] {
                "String/Number", // 0 = TYPE_STRING_OR_NUMBER
                "Boolean" // 1 = TYPE_BOOLEAN
        }));

        // Create AlertDialog
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.btn_add)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = nameTextView.getText().toString();
                        switch (typeSpinner.getSelectedItemPosition()) {
                            case TYPE_STRING_OR_NUMBER:
                                getBundleAdapter().launchEditorForNewEntry(name, "");
                                break;
                            case TYPE_BOOLEAN:
                                getBundleAdapter().onEditorResult(name, true);
                                break;
                        }
                    }
                })
                .create();
    }
}
