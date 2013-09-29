package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;

import java.lang.reflect.InvocationTargetException;

public class StringLikeItemEditor extends ValueEditorDialogFragment implements OnClickListener {
    private Spinner mTypeSpinner;
    private TextView mValueTextView;

    /**
     * List of types this Dialog can edit.
     *
     * Every type for this list must be containable in Bundle
     * and support static method valueOf for converting from String
     */
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


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_stringlike_editor, null);

        mTypeSpinner = (Spinner) view.findViewById(R.id.typespinner);
        mValueTextView = (TextView) view.findViewById(R.id.value);

        mTypeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, getEditableTypeNames()));
        mTypeSpinner.setSelection(getTypeId(getOriginalValue()));
        mValueTextView.setText(getOriginalValue().toString());

        return new AlertDialog.Builder(getActivity())
                .setTitle(getKey())
                .setView(view)
                .setPositiveButton("OK", this)
                .create();
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
                    Toast.makeText(getActivity(), R.string.value_parse_error, Toast.LENGTH_SHORT).show();
                    return;
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't convert from string using valueOf", e);
                }
            }
            sendResultAndDismiss(newValue);
        }
    }

    private static int getTypeId(Object value) {
        for (int i = 0; i < EDITABLE_TYPES.length; i++) {
            Class<?> aClass = EDITABLE_TYPES[i];
            if (aClass.isInstance(value)) {
                return i;
            }
        }
        return -1;
    }

    public static class LaunchableEditor implements Editor.DialogFragmentEditor {

        @Override
        public boolean canEdit(Object value) {
            return getTypeId(value) != -1;
        }

        @Override
        public ValueEditorDialogFragment getEditorDialogFragment() {
            return new StringLikeItemEditor();
        }
    }
}
