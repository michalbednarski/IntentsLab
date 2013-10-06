package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.content.Context;
import android.content.Intent;

/**
* Created by mb on 20.06.13.
*/
public interface Editor {

    /**
     * Intent extra for value passed to and from editor
     *
     * @see EditorActivity
     */
    public String EXTRA_TITLE = "editor_launcher.activity.title";

    /**
     * Intent extra for value passed to and from editor
     *
     * @see EditorActivity
     */
    public String EXTRA_VALUE = "editor_launcher.activity.value";



    /**
     * Checks if this editor can edit this value
     */
    public boolean canEdit(Object value);

    /* *
     * Opens non-activity editor for editing value
     * /
    public void edit(String key, Object value, EditorCallback editorCallback, Context context);*/

    /**
     * Interface for returning results from non-activity editor
     *
     * @ see #edit(String, Object, EditorCallback, Context)
     */
    public interface EditorCallback {
        void sendEditorResult(Object newValue);
    }

    /**
     * Pointer to editor implemented in external activity
     *
     * @see ValueEditorDialogFragment#EXTRA_KEY
     * @see Editor#EXTRA_VALUE
     */
    public static abstract class EditorActivity implements Editor {
        /**
         * Returns intent for launching editor,
         * EXTRA_KEY and EXTRA_VALUE will be added to it
         */
        public abstract Intent getEditorIntent(Context context);

        /*@Override
        public final void edit(String key, Object value, EditorCallback editorCallback, Context context) {
            throw new RuntimeException("EditorActivity is magic, don't use edit method with it");
        }*/
    }

    public static interface DialogFragmentEditor extends Editor {
        ValueEditorDialogFragment getEditorDialogFragment();
    }

    public static interface InPlaceValueToggler extends Editor {
        Object toggleObjectValue(Object originalValue);
    }


}
