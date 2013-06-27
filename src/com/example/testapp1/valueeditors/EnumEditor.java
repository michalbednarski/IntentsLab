package com.example.testapp1.valueeditors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.example.testapp1.Utils;

/**
 * Generic enum editor
 */
public class EnumEditor implements Editor {

    @Override
    public boolean canEdit(Object value) {
        return value instanceof Enum;
    }

    @Override
    public void edit(String key, Object value, final EditorCallback editorCallback, Context context) {
        final Enum[] enumConstants = (Enum[]) value.getClass().getEnumConstants();
        String names[] = new String[enumConstants.length];
        for (int i = 0; i < enumConstants.length; i++) {
            names[i] = enumConstants[i].name();
        }
        new AlertDialog.Builder(context)
                .setTitle(Utils.afterLastDot(value.getClass().getName()))
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        editorCallback.sendEditorResult(enumConstants[which]);
                    }
                })
                .show();
    }
}
