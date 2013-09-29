package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import com.github.michalbednarski.intentslab.Utils;

/**
 * Generic enum editor
 */
public class EnumEditor extends ValueEditorDialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Object value = getOriginalValue();
        final Enum[] enumConstants = (Enum[]) value.getClass().getEnumConstants();
        String names[] = new String[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            names[i] = enumConstants[i].name();
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(Utils.afterLastDot(value.getClass().getName()))
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResultAndDismiss(enumConstants[which]);
                    }
                })
                .create();
    }

    public static class LaunchableEditor implements Editor.DialogFragmentEditor {

        @Override
        public boolean canEdit(Object value) {
            return value instanceof Enum;
        }

        @Override
        public ValueEditorDialogFragment getEditorDialogFragment() {
            return new EnumEditor();
        }
    }
}
