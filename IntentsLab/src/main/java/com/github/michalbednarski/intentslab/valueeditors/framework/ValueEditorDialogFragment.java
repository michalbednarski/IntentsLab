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

import android.support.v4.app.DialogFragment;

/**
 * Dialog fragment used to implement Editors launchable via EditorLauncher
 */
public class ValueEditorDialogFragment extends DialogFragment {
    static final String EXTRA_KEY = "editor_launcher.DialogFragment.key";

    protected String getTitle() {
        return getArguments().getString(Editor.EXTRA_TITLE);
    }

    protected Object getOriginalValue() {
        return getArguments().get(Editor.EXTRA_VALUE);
    }

    protected void sendResult(Object newValue) {
        ((EditorLauncher.HelperFragment) getTargetFragment()).handleDialogResponse(getArguments().getString(EXTRA_KEY), newValue);
    }
}
