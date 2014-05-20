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

package com.github.michalbednarski.intentslab.bindservice.callback;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.IInterface;

import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.framework.CreateNewDialog;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorDialogFragment;

/**
 * Created by mb on 25.03.14.
 */
public class CreateCallbackDialog extends ValueEditorDialogFragment {

    private static final String ARG_CLASS = "CreateCallbackDialog.class";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage("mkif")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Class<?> aClass = getArguments().<SandboxedType>getParcelable(ARG_CLASS).aClass;
                        sendResult(CallbackInterfacesManager.createLocalCallback(aClass, getActivity().getCacheDir()));
                    }
                })
                .create();
    }

    public static final class ValueCreator implements CreateNewDialog.SyncCreator {

        @Override
        public CreateNewDialog.CreatorOption[] getCreatorOptions(final SandboxedType sandboxedType, boolean allowSandbox) {
            if (sandboxedType.aClass != null &&
                    sandboxedType.aClass.getInterfaces().length == 1 &&
                    sandboxedType.aClass.getInterfaces()[0] == IInterface.class) {
                return new CreateNewDialog.CreatorOption[] {
                        new CreateNewDialog.CreatorOption("New callback") { // TODO: resource
                            @Override
                            public void onOptionSelected(CreateNewDialog.EditorRedirect redirect) {
                                Bundle args = new Bundle();
                                args.putParcelable(ARG_CLASS, sandboxedType);
                                redirect.runEditorInDialogFragment(new CreateCallbackDialog(), args);
                            }
                        }
                };
            } else {
                return null;
            }

        }

        // Outside any category
        @Override
        public int getCategoryNameResource() {
            return 0;
        }
    };
}
