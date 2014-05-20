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

package com.github.michalbednarski.intentslab.browser;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;

/**
 * Fetcher options.
 * Reference to {@link BrowseComponentsFragment} must be set using {@link #setTargetFragment(android.support.v4.app.Fragment, int)}
 */
public class FetcherOptionsDialog extends DialogFragment {
    private static final String STATE_FETCHER = "FetcherOptions.TheFetcher";

    private Fetcher mFetcher;
    private ViewGroup mTopLayout;
    private int mTopLayoutWithoutFetcherChildCount;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mFetcher = savedInstanceState.getParcelable(STATE_FETCHER);
        } else {
            mFetcher = ((BrowseComponentsFragment) getTargetFragment()).getFetcher().clone();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_FETCHER, mFetcher);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate and get views
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.browse_components_top_options, null, false);
        mTopLayout = (ViewGroup) view.findViewById(R.id.top_layout);
        Spinner fetcherTypeSpinner = (Spinner) mTopLayout.findViewById(R.id.fetcher_type);
        mTopLayoutWithoutFetcherChildCount = mTopLayout.getChildCount();

        // Prepare fetcher spinner
        fetcherTypeSpinner.setAdapter(
                new ArrayAdapter<String>(
                        getActivity(),
                        android.R.layout.simple_spinner_item,
                        FetcherManager.getFetcherNames(getActivity())
                )
        );
        fetcherTypeSpinner.setSelection(FetcherManager.getFetcherIndex(mFetcher));
        fetcherTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (FetcherManager.getFetcherIndex(mFetcher) != position) {
                    mFetcher = FetcherManager.createNewFetcherByIndex(position);
                    createFetcherConfigurationView();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Spinner won't have nothing selected
            }
        });

        // Prepare fetcher configuration
        createFetcherConfigurationView();

        // Create dialog
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.set_custom_filter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mFetcher.updateFromConfigurationForm(FetcherOptionsDialog.this);
                        ((BrowseComponentsFragment) getTargetFragment()).setFetcher(mFetcher);
                    }
                })
                .create();
    }

    private void createFetcherConfigurationView() {
        while (mTopLayout.getChildCount() > mTopLayoutWithoutFetcherChildCount) {
            mTopLayout.removeViewAt(mTopLayout.getChildCount() - 1);
        }
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTopLayout.addView(inflater.inflate(mFetcher.getConfigurationLayout(), mTopLayout, false));
        mFetcher.initConfigurationForm(this);
    }

    /*
     * UI helper methods for Fetchers
     */
    View findView(int id) {
        return mTopLayout.findViewById(id);
    }

    void setBoxChecked(int id, boolean checked) {
        ((CompoundButton) findView(id)).setChecked(checked);
    }

    boolean isBoxChecked(int id) {
        return ((CompoundButton) findView(id)).isChecked();
    }

    void setTextInField(int id, String text) {
        ((TextView) findView(id)).setText(text != null ? text : "");
    }

    String getTextFromField(int id) {
        return ((TextView) findView(id)).getText().toString();
    }
}
