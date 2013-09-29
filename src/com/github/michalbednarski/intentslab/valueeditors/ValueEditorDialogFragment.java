package com.github.michalbednarski.intentslab.valueeditors;

import android.support.v4.app.DialogFragment;

/**
 * Created by mb on 24.08.13.
 */
public class ValueEditorDialogFragment extends DialogFragment {
    protected String getKey() {
        return getArguments().getString(Editor.EXTRA_KEY);
    }

    protected Object getOriginalValue() {
        return getArguments().get(Editor.EXTRA_VALUE);
    }

    protected void sendResultAndDismiss(Object newValue) {
        ((EditorLauncher.ActivityHandlingHeadlessFragment) getTargetFragment()).handleDialogResponse(getKey(), newValue);
    }
}
