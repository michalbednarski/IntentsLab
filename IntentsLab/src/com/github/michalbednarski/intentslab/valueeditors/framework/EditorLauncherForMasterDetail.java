package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.os.Bundle;

import com.github.michalbednarski.intentslab.MasterDetailActivity;

/**
 * Variant of EditorLauncher for using editors as detail
 *
 * This will auto-save on switch, so detail must not be used for other stuff
 * TODO: auto-save on switch and add method for explicit save for use on exit
 */
public class EditorLauncherForMasterDetail extends EditorLauncher {
    /**
     * Constructor
     *
     * @param activity
     * @param tag
     */
    public EditorLauncherForMasterDetail(MasterDetailActivity activity, String tag) {
        super(activity, tag);
    }

    @Override
    void openEditorFragment(Class<? extends ValueEditorFragment> editorFragment, Bundle args) {
        MasterDetailActivity activity = (MasterDetailActivity) mFragment.getActivity();
        if (activity.usingTabletView()) {
            activity.openFragment(
                    editorFragment,
                    args
            );
        } else {
            super.openEditorFragment(editorFragment, args);
        }
    }
}
