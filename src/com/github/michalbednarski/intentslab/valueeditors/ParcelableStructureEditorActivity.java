package com.github.michalbednarski.intentslab.valueeditors;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
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
    private static final String STATE_SHOW_NON_PUBLIC_FIELDS = "ParcelableStructureEditorActivity.showNonPublicFields";
    private static final String STATE_MODIFIED = "ParcelableStructureEditorActivity.modified";

    private Parcelable mObject;
    private boolean mHasNonPublicFields = false;
    private boolean mShowNonPublicFields = false;
    private boolean mModified = false;

    private HashMap<String, InlineValueEditor> mValueEditors;
    private InlineValueEditorsLayout mInlineValueEditorsLayout;


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

        // Get class
        Class<? extends Parcelable> aClass = mObject.getClass();

        // Prepare field editors
        mValueEditors = new HashMap<String, InlineValueEditor>();
        ArrayList<InlineValueEditor> valueEditors = new ArrayList<InlineValueEditor>();

        for (Class fieldsClass = aClass; fieldsClass != null; fieldsClass = fieldsClass.getSuperclass()) {
            for (final Field field : fieldsClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();


                if (
                        !Modifier.isStatic(modifiers) && // Not static field
                        !mValueEditors.containsKey(field.getName()) // Not scanned already
                        ) {




                    // Set flag if there are non-public fields
                    boolean isPublic = Modifier.isPublic(modifiers);
                    if (!isPublic) {
                        mHasNonPublicFields = true;
                        field.setAccessible(true);
                    }

                    // Create an editor object
                    InlineValueEditor editor = new InlineValueEditor(
                            field.getType(),
                            field.getName(),
                            !isPublic,
                            new InlineValueEditor.ValueAccessors() {
                                @Override
                                public Object getValue() {
                                    try {
                                        return field.get(mObject);
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void setValue(Object newValue) {
                                    try {
                                        field.set(mObject, newValue);
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    }
                                    markModified();
                                    invokeGetters();
                                }

                                @Override
                                public void startEditor() {
                                    mEditorLauncher.launchEditor(field.getName(), getValue());
                                }
                            }
                    );

                    // Register editor
                    mValueEditors.put(field.getName(), editor);
                    valueEditors.add(editor);


                }
            }
        }

        // Prepare getters TextView
        mGettersOutput = new TextView(this);

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

        // Invoke getters
        invokeGetters();

        // Set up final layout
        mInlineValueEditorsLayout = new InlineValueEditorsLayout(this);
        mInlineValueEditorsLayout.setValueEditors(valueEditors.toArray(new InlineValueEditor[valueEditors.size()]));
        mInlineValueEditorsLayout.addHeaderOrFooter(mGettersOutput);
        mInlineValueEditorsLayout.setHiddenEditorsVisible(mShowNonPublicFields);
        setContentView(mInlineValueEditorsLayout);
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
                mInlineValueEditorsLayout.setHiddenEditorsVisible(mShowNonPublicFields);
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
            final InlineValueEditor valueEditor = mValueEditors.get(key);
            valueEditor.getAccessors().setValue(newValue);
            valueEditor.updateTextOnButton();
            markModified();
            invokeGetters();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.toastException(this, "Field.set", e);
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