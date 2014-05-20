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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TableLayout;

/**
 * Created by mb on 05.10.13.
 */
public class InlineValueEditorsLayout extends ScrollView {
    private final TableLayout mTableLayout;
    private InlineValueEditor[] mValueEditors;
    private View[] mEditorViews;

    public InlineValueEditorsLayout(Context context) {
        this(context, null);
    }

    public InlineValueEditorsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTableLayout = new TableLayout(context);
        addView(mTableLayout);
    }

    /**
     * Put value editors in view, this method must be called only once
     */
    public void setValueEditors(InlineValueEditor[] valueEditors) {
        // Ensure this is called only once
        assert mValueEditors == null && valueEditors != null;

        // Save editors list
        mValueEditors = valueEditors;

        // Get views and add to layout
        mEditorViews = new View[valueEditors.length];
        for (int i = 0; i < valueEditors.length; i++) {
            final View view = valueEditors[i].createView(mTableLayout);
            mEditorViews[i] = view;
            mTableLayout.addView(view);
        }
    }

    /**
     * Add header or footer view depending on whether this method
     * was called before or after {@link #setValueEditors(InlineValueEditor[])}
     */
    public void addHeaderOrFooter(View headerOrFooter) {
        mTableLayout.addView(headerOrFooter);
    }

    /**
     * Show or hide value editors
     */
    public void setHiddenEditorsVisible(boolean visible) {
        for (int i = 0; i < mValueEditors.length; i++) {
            if (mValueEditors[i].mHidden) {
                mEditorViews[i].setVisibility(visible ? VISIBLE : GONE);
            }
        }
    }
}
