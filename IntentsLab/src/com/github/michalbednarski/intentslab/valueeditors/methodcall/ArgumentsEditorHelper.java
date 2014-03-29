package com.github.michalbednarski.intentslab.valueeditors.methodcall;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.runas.RunAsSelectorView;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.object.InlineValueEditor;
import com.github.michalbednarski.intentslab.valueeditors.object.InlineValueEditorsLayout;

/**
 * Helper class for editing arguments for method/constructor call
 */
public class ArgumentsEditorHelper implements EditorLauncher.EditorLauncherWithSandboxCallback {

    private final boolean mWithRunAsSpinner;
    private EditorLauncher mEditorLauncher;
    private final InlineValueEditor[] mValueEditors;
    private final Object[] mArguments;
    private final boolean[] mArgumentsSandboxed;


    public ArgumentsEditorHelper(SandboxedMethod method, boolean withRunAsSpinner) {
        mWithRunAsSpinner = withRunAsSpinner;

        // Initialize arrays
        final int argumentsCount = method.argumentTypes.length;
        mArguments = new Object[argumentsCount];
        mArgumentsSandboxed = new boolean[argumentsCount];
        mValueEditors = new InlineValueEditor[argumentsCount];

        // Prepare arguments and editors
        for (int ii = 0; ii < argumentsCount; ii++) {
            final SandboxedType type = method.argumentTypes[ii];
            mArguments[ii] = type.getDefaultValue();
            mArgumentsSandboxed[ii] = false;

            // Make value editor
            final int i = ii;
            mValueEditors[ii] = new InlineValueEditor(
                    type.aClass,
                    type.toString(),
                    new InlineValueEditor.ValueAccessors() {
                        @Override
                        public Object getValue() {
                            return mArguments[i];
                        }

                        @Override
                        public void setValue(Object newValue) {
                            mArguments[i] = newValue;
                            mArgumentsSandboxed[i] = false;
                        }

                        @Override
                        public void startEditor() {
                            final Object value = getValue();
                            if (value == null) {
                                mEditorLauncher.launchEditorForNew("arg" + i, type);
                            } else if (mArgumentsSandboxed[i]) {
                                mEditorLauncher.launchEditorForSandboxedObject("arg" + i, "arg" + i, (SandboxedObject) value);
                            } else {
                                mEditorLauncher.launchEditor("arg" + i, value);
                            }
                        }
                    }
            );
        }
    }

    public void setSandboxedArguments(SandboxedObject[] arguments) {
        int i = 0;
        for (SandboxedObject wrapped : arguments) {
            try {
                final Object unwrapped = wrapped.unwrap(null);
                mArguments[i] = unwrapped;
                mArgumentsSandboxed[i] = false;
            } catch (Exception e) {
                mArguments[i] = wrapped;
                mArgumentsSandboxed[i] = true;
            }
            i++;
        }
    }

    public SandboxedObject[] getSandboxedArguments() {
        final int argumentsCount = mArguments.length;
        SandboxedObject[] sandboxedArguments = new SandboxedObject[argumentsCount];
        for (int i = 0; i < argumentsCount; i++) {
            sandboxedArguments[i] =
                    mArgumentsSandboxed[i] ?
                            (SandboxedObject) mArguments[i] :
                            new SandboxedObject(mArguments[i]);
        }
        return sandboxedArguments;
    }

    public Object[] getArguments() {
        final int argumentsCount = mArguments.length;
        Object[] arguments = new Object[argumentsCount];
        for (int i = 0; i < argumentsCount; i++) {
            arguments[i] =
                    mArgumentsSandboxed[i] ?
                            ((SandboxedObject) mArguments[i]).unwrap(null) :
                            mArguments[i];
        }
        return arguments;
    }

    /**
     * Fill the editors layout
     */
    public void fillEditorsLayout(InlineValueEditorsLayout editorsLayout) {
        if (mWithRunAsSpinner) {
            editorsLayout.addHeaderOrFooter(new RunAsSelectorView(editorsLayout.getContext(), null));
        }
        editorsLayout.setValueEditors(mValueEditors);
    }

    /**
     * Set EditorLauncher for use by our editors.
     * This will also set it's callback
     */
    public void setEditorLauncher(EditorLauncher editorLauncher) {
        mEditorLauncher = editorLauncher;
        editorLauncher.setCallback(this);
    }


    /**
     * Handling of EditorLauncher results
     */
    @Override
    public void onEditorResult(String key, Object newValue) {
        int i = parseKey(key);
        mArguments[i] = newValue;
        mArgumentsSandboxed[i] = false;
        mValueEditors[i].updateTextOnButton();
    }

    // Like above
    @Override
    public void onSandboxedEditorResult(String key, SandboxedObject newWrappedValue) {
        int i = parseKey(key);
        mArguments[i] = newWrappedValue;
        mArgumentsSandboxed[i] = true;
        mValueEditors[i].updateTextOnButton();
    }



    /**
     * Parse key passed to EditorLauncher and get index of argument
     */
    private int parseKey(String key) {
        if (BuildConfig.DEBUG && !key.startsWith("arg")) {
            throw new AssertionError("Invalid key");
        }
        return Integer.parseInt(key.substring(3));
    }
}
