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
        ((EditorLauncher.ActivityHandlingHeadlessFragment) getTargetFragment()).handleDialogResponse(getArguments().getString(EXTRA_KEY), newValue);
    }
}
