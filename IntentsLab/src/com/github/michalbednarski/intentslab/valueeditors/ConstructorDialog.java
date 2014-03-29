package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.framework.CreateNewDialog;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorDialogFragment;
import com.github.michalbednarski.intentslab.valueeditors.methodcall.ArgumentsEditorHelper;
import com.github.michalbednarski.intentslab.valueeditors.object.InlineValueEditorsLayout;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Dialog for creating object using it's constructor
 */
public class ConstructorDialog extends ValueEditorDialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_CONSTRUCTOR_NR = "ConstructorDialog.nr";
    private static final String ARG_SANDBOXED_TYPE = "ConstructorDialog.type";
    private static final String ARG_METHOD_INFO = "ConstructorDialog.method-info";

    private static final String STATE_EDITOR_LAUNCHER_TAG = "ConstructorDialog.s.EditorLauncherTag";

    private EditorLauncher mEditorLauncher;
    private ArgumentsEditorHelper mArgumentsEditorHelper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prepare EditorLauncher and ArgumentsEditorHelper
        mEditorLauncher = new EditorLauncher(
                getActivity(),
                savedInstanceState != null ?
                        savedInstanceState.getString(STATE_EDITOR_LAUNCHER_TAG) :
                        null
        );
        mArgumentsEditorHelper = new ArgumentsEditorHelper(getArguments().<SandboxedMethod>getParcelable(ARG_METHOD_INFO), false);
        mArgumentsEditorHelper.setEditorLauncher(mEditorLauncher);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Prepare content
        InlineValueEditorsLayout view = new InlineValueEditorsLayout(getActivity());
        mArgumentsEditorHelper.fillEditorsLayout(view);

        // Prepare dialog
        return new AlertDialog.Builder(getActivity())
                .setTitle(getTitle())
                .setView(view)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    // On ok button click
    @Override
    public void onClick(DialogInterface dialog, int which) {
        final Bundle arguments = getArguments();
        final SandboxedType type = arguments.getParcelable(ARG_SANDBOXED_TYPE);
        final int constructorNr = arguments.getInt(ARG_CONSTRUCTOR_NR);

        // Get constructor
        final Constructor<?> constructor = type.aClass.getDeclaredConstructors()[constructorNr];
        constructor.setAccessible(true);

        try {
            // Invoke constructor and send result
            sendResult(constructor.newInstance(mArgumentsEditorHelper.getArguments()));
        } catch (InvocationTargetException e) {
            // Show exception
            Utils.toastException(getActivity(), e.getTargetException());
        } catch(Exception e) {
            // We don't expect any other exceptions
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EDITOR_LAUNCHER_TAG, mEditorLauncher.getTag());
    }

    // Information about creation
    public static class ValueCreator implements CreateNewDialog.SyncCreator {
        @Override
        public CreateNewDialog.CreatorOption[] getCreatorOptions(final SandboxedType sandboxedType, boolean allowSandbox) {
            // Check if we'll be able to construct this object
            if (sandboxedType.type != SandboxedType.Type.OBJECT || sandboxedType.aClass == null) {
                return null;
            }
            ArrayList<CreateNewDialog.CreatorOption> options = new ArrayList<CreateNewDialog.CreatorOption>();

            // Scan constructors
            Constructor<?>[] constructors = sandboxedType.aClass.getDeclaredConstructors();
            for (int i = 0, j = constructors.length; i < j; i++) {
                final Constructor<?> constructor = constructors[i];
                final int ii = i;
                options.add(new CreateNewDialog.CreatorOption(constructor.toGenericString()) {
                    @Override
                    public void onOptionSelected(CreateNewDialog.EditorRedirect redirect) {
                        if (constructor.getParameterTypes().length == 0) {
                            // Constructor without arguments, invoke directly
                            try {
                                constructor.setAccessible(true);
                                redirect.returnObject(constructor.newInstance());
                            } catch (Exception e) {
                                Utils.toastException(redirect.getContext(), e);
                            }
                        } else {
                            // Constructor has arguments, open dialog
                            Bundle arguments = new Bundle();
                            arguments.putParcelable(ARG_SANDBOXED_TYPE, sandboxedType);
                            arguments.putInt(ARG_CONSTRUCTOR_NR, ii);
                            arguments.putParcelable(ARG_METHOD_INFO, new SandboxedMethod(constructor));
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
