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

package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorDialogFragment;

/**
 * Generic enum editor
 */
public class EnumEditor extends ValueEditorDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Object value = getOriginalValue();
        final Enum[] enumConstants = (Enum[]) value.getClass().getEnumConstants();
        String names[] = new String[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            names[i] = enumConstants[i].name();
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(Utils.afterLastDot(value.getClass().getName()))
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(enumConstants[which]);
                    }
                })
                .create();
    }

    public static class LaunchableEditor implements Editor.DialogFragmentEditor {

        @Override
        public boolean canEdit(Object value) {
            return value instanceof Enum;
        }

        @Override
        public ValueEditorDialogFragment getEditorDialogFragment() {
            return new EnumEditor();
        }
    }
}
