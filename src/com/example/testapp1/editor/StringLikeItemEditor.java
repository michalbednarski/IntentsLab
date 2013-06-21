package com.example.testapp1.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.Utils;
import com.example.testapp1.valueeditors.Editor;

import java.lang.reflect.InvocationTargetException;

public class StringLikeItemEditor implements OnClickListener {
    private final Context mContext;
    private final Spinner mTypeSpinner;
    private final TextView mValueTextView;
    private final Editor.EditorCallback mEditorCallback;



    public static class LaunchableEditor implements Editor {

        @Override
        public boolean canEdit(Object value) {
            for (Class<?> aClass : EDITABLE_TYPES) {
                if (aClass.isInstance(value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void edit(String key, Object value, EditorCallback editorCallback, Context context) {
            for (int i = 0; i < EDITABLE_TYPES.length; i++) {
                Class<?> aClass = EDITABLE_TYPES[i];
                if (aClass.isInstance(value)) {
                    new StringLikeItemEditor(context, key, value.toString(), i, editorCallback);
                    return;
                }
            }
        }
    }

    private static final Class<?>[] EDITABLE_TYPES = {
            String.class,
            Integer.class,
            Float.class,
            Double.class
    };

    private String[] getEditableTypeNames() {
        String[] editableTypeNames = new String[EDITABLE_TYPES.length];
        for (int i = 0; i < EDITABLE_TYPES.length; i++) {
            editableTypeNames[i] = Utils.afterLastDot(EDITABLE_TYPES[i].getName());
        }
        return editableTypeNames;
    }

    private StringLikeItemEditor(Context context, String name, String originalValue, int typeId, Editor.EditorCallback editorCallback) {
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_stringlike_editor, null);

        mTypeSpinner = (Spinner) view.findViewById(R.id.typespinner);
        mValueTextView = (TextView) view.findViewById(R.id.value);

        mTypeSpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, getEditableTypeNames()));
        mTypeSpinner.setSelection(typeId);
        mValueTextView.setText(originalValue);

        mEditorCallback = editorCallback;

        new AlertDialog.Builder(context)
                .setTitle(name)
                .setView(view)
                .setPositiveButton("OK", this)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            Class<?> newType = EDITABLE_TYPES[mTypeSpinner.getSelectedItemPosition()];
            String stringValue = mValueTextView.getText().toString();
            Object newValue;
            if (newType == String.class) {
                newValue = stringValue;
            } else {
                try {
                    newValue = newType.getMethod("valueOf", String.class).invoke(null, stringValue);
                } catch (InvocationTargetException e) {
                    Toast.makeText(mContext, R.string.value_parse_error, Toast.LENGTH_SHORT).show();
                    return;
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't convert from string using valueOf", e);
                }
            }
            mEditorCallback.sendEditorResult(newValue);
        }
    }
}
