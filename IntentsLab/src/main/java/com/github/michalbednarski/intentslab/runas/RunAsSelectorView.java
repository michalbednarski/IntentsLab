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

package com.github.michalbednarski.intentslab.runas;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * View for selecting uid to run as
 */
public class RunAsSelectorView extends LinearLayout {

    private final DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            setVisibility(RunAsManager.getSelectorAdapter().getCount() >= 2 ? VISIBLE : GONE);
        }
    };

    private static final AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            RunAsManager.sSelectedId = id;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Spinner cannot have nothing selected
        }
    };

    private Spinner mSpinner;

    public RunAsSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(HORIZONTAL);

        final TextView label = new TextView(context);
        label.setText("Run as:");

        mSpinner = new Spinner(context);
        mSpinner.setLayoutParams(new LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        addView(label);
        addView(mSpinner);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSpinner.setAdapter(RunAsManager.getSelectorAdapter());
        mSpinner.setSelection(RunAsManager.getSelectedSpinnerPosition());
        RunAsManager.getSelectorAdapter().registerDataSetObserver(mObserver);
        mSpinner.setOnItemSelectedListener(mOnItemSelectedListener);
        mObserver.onChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        RunAsManager.getSelectorAdapter().unregisterDataSetObserver(mObserver);
        mSpinner.setOnItemSelectedListener(null);
        mSpinner.setAdapter(null);
        super.onDetachedFromWindow();
    }
}
