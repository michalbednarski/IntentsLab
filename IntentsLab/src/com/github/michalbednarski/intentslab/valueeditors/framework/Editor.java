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

package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.content.Context;
import android.content.Intent;

/**
* Created by mb on 20.06.13.
*/
public interface Editor {

    /**
     * Intent extra for value passed to and from editor
     *
     * @see EditorActivity
     */
    public String EXTRA_TITLE = "editor_launcher.activity.title";

    /**
     * Intent extra for value passed to and from editor
     *
     * @see EditorActivity
     */
    public String EXTRA_VALUE = "editor_launcher.activity.value";



    /**
     * Checks if this editor can edit this value
     */
    public boolean canEdit(Object value);

    /**
     * Pointer to editor implemented in external activity
     *
     * @see ValueEditorDialogFragment#EXTRA_KEY
     * @see Editor#EXTRA_VALUE
     */
    public static abstract class EditorActivity implements Editor {
        /**
         * Returns intent for launching editor,
         * EXTRA_KEY and EXTRA_VALUE will be added to it
         */
        public abstract Intent getEditorIntent(Context context);
    }

    public static interface DialogFragmentEditor extends Editor {
        ValueEditorDialogFragment getEditorDialogFragment();
    }

    public static interface FragmentEditor extends Editor {
        Class<? extends ValueEditorFragment> getEditorFragment();
    }

    public static interface InPlaceValueToggler extends Editor {
        Object toggleObjectValue(Object originalValue);
    }


}
