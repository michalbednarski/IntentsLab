package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.FormattedTextBuilder;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static com.github.michalbednarski.intentslab.FormattedTextBuilder.ValueSemantic;

/**
 * Activity for editing general parcelable class
 */
public class ParcelableStructureEditorActivity extends FragmentActivity implements EditorLauncher.EditorLauncherCallback {
    private static final int REQUEST_CODE_EDITOR = 0;

    private static final String STATE_SHOW_NON_PUBLIC_FIELDS = "ParcelableStructureEditorActivity.showNonPublicFields";
    private static final String STATE_MODIFIED = "ParcelableStructureEditorActivity.modified";

    private Parcelable mObject;
    private boolean mHasNonPublicFields = false;
    private boolean mShowNonPublicFields = false;
    private boolean mModified = false;





    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init EditorLauncher
        mEditorLauncher = new EditorLauncher(this, "PaScEdLa");
        mEditorLauncher.setCallback(this);

        // Get edited object and/or load saved state
        if (savedInstanceState != null) {
            mObject = savedInstanceState.getParcelable(Editor.EXTRA_VALUE);
            mShowNonPublicFields = savedInstanceState.getBoolean(STATE_SHOW_NON_PUBLIC_FIELDS);
            mModified = savedInstanceState.getBoolean(STATE_MODIFIED);
        } else {
            mObject = getIntent().getParcelableExtra(Editor.EXTRA_VALUE);
        }
        assert mObject != null;

        // Prepare layout
        setContentView(R.layout.strucutre_editor_wrapper);
        ViewGroup layout = (ViewGroup) findViewById(R.id.wrapper);

        Class<? extends Parcelable> aClass = mObject.getClass();

        // Prepare field editors
        for (Class fieldsClass = aClass; fieldsClass != null; fieldsClass = fieldsClass.getSuperclass()) {
            for (Field field : fieldsClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();


                if (
                        (modifiers & Modifier.STATIC) == 0 && // Not static field
                        !mFieldEditHashMap.containsKey(field.getName()) // Not scanned already
                        ) {
                    FieldEdit fieldEdit = new FieldEdit(field);
                    layout.addView(fieldEdit.mView); // Add field to layout
                }
            }
        }


        // Prepare getters TextView
        mGettersOutput = new TextView(this);
        layout.addView(mGettersOutput);

        // List getters
        final Pattern getterPattern = Pattern.compile("(?!getClass$)(is|get)[A-Z].*");
        for (Method method : aClass.getMethods()) {
            if (
                    getterPattern.matcher(method.getName()).matches() && // method name looks like getter
                    (method.getModifiers() & Modifier.STATIC) == 0 && // method isn't static
                    method.getParameterTypes().length == 0 // has no arguments
                    ) {
                mGetterMethods.add(method);
            }
        }

        // invoke getters
        invokeGetters();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Editor.EXTRA_VALUE, mObject);
        outState.putBoolean(STATE_SHOW_NON_PUBLIC_FIELDS, mShowNonPublicFields);
        outState.putBoolean(STATE_MODIFIED, mModified);
    }

    @Override
    public void onBackPressed() {
        if (mModified) {
            setResult(
                    0,
                    new Intent()
                    .putExtra(Editor.EXTRA_KEY, getIntent().getStringExtra(Editor.EXTRA_KEY))
                    .putExtra(Editor.EXTRA_VALUE, mObject)
            );
        }
        finish();
    }

    // Options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.parcelable_strucutre_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.show_non_public_fields)
                .setVisible(mHasNonPublicFields)
                .setChecked(mShowNonPublicFields);

        menu.findItem(R.id.discard)
                .setVisible(mModified);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.show_non_public_fields:
                mShowNonPublicFields = !mShowNonPublicFields;
                for (FieldEdit fieldEdit : mFieldEditHashMap.values()) {
                    fieldEdit.showOrHide();
                }
                ActivityCompat.invalidateOptionsMenu(this);
                return true;
            case R.id.discard:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.discard_changes_confirm))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                return true;
            default:
                return false;
        }
    }

    private void markModified() {
        if (!mModified) {
            mModified = true;
            ActivityCompat.invalidateOptionsMenu(this);
        }
    }


    // Editing fields
    EditorLauncher mEditorLauncher;

    @Override
    public void onEditorResult(String key, Object newValue) {
        try {
            FieldEdit fieldEdit = mFieldEditHashMap.get(key);
            fieldEdit.mField.set(mObject, newValue);
            markModified();
            fieldEdit.updateText();
            invokeGetters();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.toastException(this, "Field.set", e);
        }
    }

    // Field name to FieldEdit wrapper map
    private HashMap<String, FieldEdit> mFieldEditHashMap = new HashMap<String, FieldEdit>();

    private class FieldEdit implements View.OnClickListener, TextWatcher {
        final ViewGroup mView;
        final Field mField;
        private final TextView mValueTextView;
        private final boolean mIsPublic;
        private boolean mUseNullInsteadEmptyString = false;

        FieldEdit(Field field) {
            // Disable access checks for this field
            field.setAccessible(true);

            // Store field instance
            mField = field;
            mFieldEditHashMap.put(field.getName(), this);

            // Check if it's public
            mIsPublic = (field.getModifiers() & Modifier.PUBLIC) != 0;
            if (!mIsPublic) {
                // and if it's not set flag that there are non-public fields
                mHasNonPublicFields = true;
            }

            // Check if value is string-like,
            // that is can be edited inline in EditText
            boolean isStringLike =
                    field.getType() == String.class ||
                    field.getType() == Byte.TYPE ||
                    field.getType() == Character.TYPE ||
                    field.getType() == Short.TYPE ||
                    field.getType() == Integer.TYPE ||
                    field.getType() == Long.TYPE ||
                    field.getType() == Float.TYPE ||
                    field.getType() == Double.TYPE;

            // Inflate table row
            mView = (ViewGroup) getLayoutInflater().inflate(
                    isStringLike ?
                            R.layout.structure_editor_row_with_textfield :
                            R.layout.strucutre_editor_row_with_button,
                    null
            );

            // Fill header (name) text
            TextView headerText = (TextView) mView.findViewById(R.id.header);
            headerText.setText(field.getName() + ": ");

            // Find value TextView/Button
            mValueTextView = (TextView) mView.findViewById(R.id.value);

            // Prepare value text
            updateText();

            // Hide by default if this isn't public field
            showOrHide();

            // Bind events
            if (isStringLike) {
                mValueTextView.addTextChangedListener(this);

                // Empty string/null switching
                if (field.getType() == String.class) {
                    mValueTextView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                        @Override
                        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                            if (mValueTextView.getText().length() == 0) {
                                final boolean useNull = !mUseNullInsteadEmptyString;
                                menu.add(useNull ? getString(R.string.set_to_null) : getString(R.string.set_to_empty_string)).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        try {
                                            mField.set(mObject, useNull ? null : "");
                                        } catch (IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        }
                                        markModified();
                                        updateText();
                                        return true;
                                    }
                                });
                            }
                        }
                    });
                }
            } else {
                mValueTextView.setOnClickListener(this);
            }

        }

        /**
         * Update text displayed on Button or EditText
         * based on field value
         */
        void updateText() {
            try {
                Object value = mField.get(mObject);
                if (mField.getType() == String.class) {
                    if (value == null) {
                        value = "";
                        mUseNullInsteadEmptyString = true;
                    } else if ("".equals(value)) {
                        mUseNullInsteadEmptyString = false;
                    }
                    mValueTextView.setHint(mUseNullInsteadEmptyString ? "null" : "\"\"");
                }
                mValueTextView.setText(value + "");
            } catch (IllegalAccessException e) {
                // Shouldn't happen, we make all field accessible
                throw new RuntimeException(e);
            }
        }

        @Override // For non-EditText types
        public void onClick(View v) {

            try {
                if (mField.getType() == Boolean.TYPE) {
                    // Booleans are primitives, so they cannot be handled by EditorLauncher
                    mField.setBoolean(mObject, !mField.getBoolean(mObject));
                    markModified();
                    updateText();
                    invokeGetters();
                } else {
                    // Non-primitive non-string-like types are handled by EditorLauncher
                    mEditorLauncher.launchEditor(mField.getName(), mField.get(mObject));
                }
            } catch (IllegalAccessException e) {
                //e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        /**
         * Update field value based on text
         *
         * This is used for String and primitive types except boolean
         */
        private void updateValueFromText() {
            String value = mValueTextView.getText().toString();
            Class type = mField.getType();
            try {
                // Convert to right type and set field value
                if (type == String.class) {
                    // String (may be null)
                    if (mUseNullInsteadEmptyString && "".equals(value)) {
                        value = null;
                    }
                    mField.set(mObject, value);
                } else if (type == Byte.TYPE) {
                    mField.setByte(mObject, Byte.parseByte(value));
                } else if (type == Character.TYPE) {
                    // Character
                    if (value.length() == 1) {
                        mField.setChar(mObject, value.charAt(0));
                    } else {
                        throw new NumberFormatException(); // TODO: report wrong length instead generic parse error
                    }
                } else if (type == Short.TYPE) {
                    mField.setShort(mObject, Short.parseShort(value));
                } else if (type == Integer.TYPE) {
                    mField.setInt(mObject, Integer.parseInt(value));
                } else if (type == Long.TYPE) {
                    mField.setLong(mObject, Long.parseLong(value));
                } else if (type == Float.TYPE) {
                    mField.setFloat(mObject, Float.parseFloat(value));
                } else if (type == Double.TYPE) {
                    mField.setDouble(mObject, Double.parseDouble(value));
                }
            } catch (NumberFormatException e) {

                // Set error message for text field
                mValueTextView.setError(getString(R.string.value_parse_error));
                return;
            } catch (Exception e) {

                // Shouldn't happen
                throw new RuntimeException(e);
            }

            // Nothing was thrown
            // Set modified flag
            markModified();

            // Clear error
            mValueTextView.setError(null);

            // Update getter values
            invokeGetters();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            updateValueFromText();
        }

        @Override
        public void afterTextChanged(Editable s) {}

        /**
         * Show or hide this field edit
         *
         * Field will be hidden if it's not public and "show non-public fields" is off
         */
        void showOrHide() {
            mView.setVisibility(mIsPublic || mShowNonPublicFields ? View.VISIBLE : View.GONE);
        }
    }

    // Getters
    TextView mGettersOutput;
    ArrayList<Method> mGetterMethods = new ArrayList<Method>();

    /**
     * Invoke getter methods stored in {@link #mGetterMethods}
     * and show their results in {@link #mGettersOutput} TextView
     *
     * This should be called after every change of {@link #mObject} fields
     */
    private void invokeGetters() {
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        for (Method method : mGetterMethods) {
            ValueSemantic valueSemantic = ValueSemantic.NONE;
            String value;
            try {

                // Invoke getter method
                Object o = method.invoke(mObject);

                // Use "null" if result is null
                value = o == null ? "null" : o.toString();

            } catch (InvocationTargetException wrappedException) {
                // Getter method thrown exception,
                // Unwrap it and display as method result
                final Throwable targetException = wrappedException.getTargetException();
                assert targetException != null;
                value = Utils.describeException(targetException);
                valueSemantic = ValueSemantic.ERROR;

            } catch (IllegalAccessException e) {
                // Shouldn't happen, non-public methods are excluded in onCreate
                throw new RuntimeException("Accessor method not accessible", e);
            }
            ftb.appendValue(method.getName() + "()", value, false, valueSemantic);
        }

        // Show results in EditText
        mGettersOutput.setText(ftb.getText());
    }



    // Starting this editor from other types
    static class LaunchableEditor extends Editor.EditorActivity {

        @Override
        public boolean canEdit(Object value) {
            return value instanceof Parcelable; // TODO: stricter checking
        }

        @Override
        public Intent getEditorIntent(Context context) {
            return new Intent(context, ParcelableStructureEditorActivity.class);
        }
    }
}