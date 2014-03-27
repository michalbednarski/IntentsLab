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
