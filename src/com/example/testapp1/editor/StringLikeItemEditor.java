package com.example.testapp1.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.testapp1.R;

class StringLikeItemEditor implements OnClickListener {
    private Context mContext;
    private Bundle mBundle;
    private String mOriginalName;
    private TextView mNameTextView;
    private Spinner mTypeSpinner;
    private TextView mValueTextView;

	/*private final static Class StringClasses[] = {
        String.class,
		CharSequence.class
	};*/

    private static abstract class Serializer {
        Class typeClass;
        String typeName;

        Serializer(Class typeClass, String typeName) {
            this.typeClass = typeClass;
            this.typeName = typeName;
        }

        String makeString(Object o) {
            return o.toString();
        }

        abstract void putInBundle(Bundle bundle, String name, String value);
    }

    private final static Serializer serializers[] = {
            new Serializer(String.class, "String") {
                @Override
                void putInBundle(Bundle bundle, String name, String value) {
                    bundle.putString(name, value);
                }
            },
            // CharSequence is string

            new Serializer(Integer.class, "Int") {
                @Override
                void putInBundle(Bundle bundle, String name, String value) {
                    bundle.putInt(name, Integer.valueOf(value));
                }
            }
    };

    private final static String serializerNames[] = new String[serializers.length];
    private EditorCallback mChangeObserver;

    static {
        for (int i = 0; i < serializers.length; i++) {
            serializerNames[i] = serializers[i].typeName;
        }
    }

    static boolean editIfCan(Context context, Bundle bundle, String name, Object value, EditorCallback changeObserver) {
        for (int i = 0; i < serializers.length; i++) {
            Serializer s = serializers[i];
            if (s.typeClass.isInstance(value)) {
                new StringLikeItemEditor(context, bundle, name, s.makeString(value), i, changeObserver);
                return true;
            }
        }
        return false;
    }

    private StringLikeItemEditor(Context context, Bundle bundle, String name, String originalValue, int typeId, EditorCallback changeObserver) {
        mContext = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_stringlike_editor, null);

        mNameTextView = (TextView) view.findViewById(R.id.name);
        mTypeSpinner = (Spinner) view.findViewById(R.id.typespinner);
        mValueTextView = (TextView) view.findViewById(R.id.value);

        mNameTextView.setText(name);
        mTypeSpinner.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, serializerNames));
        mTypeSpinner.setSelection(typeId);
        mValueTextView.setText(originalValue);

        mBundle = bundle;
        mOriginalName = name;
        mChangeObserver = changeObserver;

        new AlertDialog.Builder(context)
                .setTitle(name)
                .setView(view)
                .setPositiveButton("OK", this)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String newName = mNameTextView.getText().toString();
            if (!newName.equals(mOriginalName)) {
                mBundle.remove(mOriginalName);
            }
            serializers[mTypeSpinner.getSelectedItemPosition()]
                    .putInBundle(
                            mBundle,
                            newName,
                            mValueTextView.getText().toString());
            mChangeObserver.afterEdit(!newName.equals(mOriginalName));
        }
    }
}
