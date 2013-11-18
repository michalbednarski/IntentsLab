package com.github.michalbednarski.intentslab.valueeditors.object;

/**
 * Created by mb on 16.11.13.
 */
interface ObjectEditorHelper {
    InlineValueEditor[] getInlineValueEditors();
    boolean hasNonPublicFields();
    Object getObject();
    CharSequence getGetterValues();

    interface ObjectEditorHelperCallback {
        void onModified();
    }
}
