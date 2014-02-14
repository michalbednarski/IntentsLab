package com.github.michalbednarski.intentslab.valueeditors;

import android.app.Dialog;
import android.os.Bundle;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.framework.CreateNewDialog;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorDialogFragment;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

/**
 * Dialog for creating object using it's constructor
 *
 * TODO
 */
public class ConstructorDialog extends ValueEditorDialogFragment {

    private static final String ARG_CONSTRUCTOR_NR = "ConstructorDialog.nr";
    private static final String ARG_SANDBOXED_TYPE = "ConstructorDialog.type";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getTitle());
        return dialog;
    }



    public static class ValueCreator implements CreateNewDialog.SyncCreator {
        @Override
        public CreateNewDialog.CreatorOption[] getCreatorOptions(final SandboxedType sandboxedType, boolean allowSandbox) {
            // Check if we'll be able to construct this object
            if (sandboxedType.type != SandboxedType.Type.OBJECT || sandboxedType.aClass == null) {
                return null;
            }
            ArrayList<CreateNewDialog.CreatorOption> options = new ArrayList<CreateNewDialog.CreatorOption>();

            // Scan constructors
            Constructor<?>[] constructors = sandboxedType.aClass.getConstructors();
            for (int i = 0, j = constructors.length; i < j; i++) {
                final Constructor<?> constructor = constructors[i];
                final int ii = i;
                options.add(new CreateNewDialog.CreatorOption(constructor.toGenericString()) {
                    @Override
                    public void onOptionSelected(CreateNewDialog.EditorRedirect redirect) {
                        if (constructor.getParameterTypes().length == 0) {
                            // Constructor without arguments, invoke directly
                            try {
                                redirect.returnObject(constructor.newInstance());
                            } catch (Exception e) {
                                Utils.toastException(redirect.getContext(), e);
                            }
                        } else {
                            // Constructor has arguments, open dialog
                            Bundle arguments = new Bundle();
                            arguments.putParcelable(ARG_SANDBOXED_TYPE, sandboxedType);
                            arguments.putInt(ARG_CONSTRUCTOR_NR, ii);
                            redirect.runEditorInDialogFragment(new ConstructorDialog(), arguments);
                        }
                    }
                });
            }

            return options.toArray(new CreateNewDialog.CreatorOption[options.size()]);
        }

        @Override
        public int getCategoryNameResource() {
            return R.string.creator_constructors;
        }
    }
}
