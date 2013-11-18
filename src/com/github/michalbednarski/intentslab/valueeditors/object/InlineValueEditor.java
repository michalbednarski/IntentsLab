package com.github.michalbednarski.intentslab.valueeditors.object;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by mb on 03.10.13.
 */
public class InlineValueEditor {
    private final Class<?> mValueType;
    private final String mLabel;
    final boolean mHidden;
    private final ValueAccessors mValueAccessors;

    private final boolean mStringLike;
    private final boolean mStringLikeValueNullable;
    private boolean mUseEmptyStringInsteadNull;

    private final ArrayList<WeakReference<Button>> mButtonsDisplayingValue;




    public InlineValueEditor(Class type, String name, ValueAccessors valueAccessors) {
        this(type, name, false, valueAccessors);
    }

    public InlineValueEditor(Class type, String name, boolean hidden, ValueAccessors valueAccessors) {
        //
        mValueType = type;
        mHidden = hidden;
        mValueAccessors = valueAccessors;

        // Build text for label
        StringBuilder label = new StringBuilder();
        if (name != null) {
            label.append(name);
        } else {
            label.append(type.getName());
        }
        mLabel = label.append(':').toString();

        // Check if this is string-like value
        mStringLike =
                type == String.class ||
                type == Byte.TYPE || type == Byte.class ||
                type == Character.TYPE || type == Character.class ||
                type == Short.TYPE || type == Short.class ||
                type == Integer.TYPE || type == Integer.class ||
                type == Long.TYPE || type == Long.class ||
                type == Float.TYPE || type == Float.class ||
                type == Double.TYPE || type == Double.class;
        mStringLikeValueNullable =
                type == String.class ||
                type == Byte.class ||
                type == Character.class ||
                type == Short.class ||
                type == Integer.class ||
                type == Long.class ||
                type == Float.class ||
                type == Double.class;

        // For non-string types create list of buttons
        if (!mStringLike) {
            mButtonsDisplayingValue = new ArrayList<WeakReference<Button>>();
        } else {
            mButtonsDisplayingValue = null;
        }
    }

    public ValueAccessors getAccessors() {
        return mValueAccessors;
    }

    /**
     * Create View to be used in {@link InlineValueEditorsLayout}
     */
    View createView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(
                mStringLike ?
                        R.layout.structure_editor_row_with_textfield :
                        R.layout.strucutre_editor_row_with_button,
                parent,
                false);
        // Set label
        ((TextView) view.findViewById(R.id.header)).setText(mLabel);

        // Prepare text field or button
        final TextView fieldOrButton = (TextView) view.findViewById(R.id.value);
        if (mStringLike) {
            final Object value = mValueAccessors.getValue();

            // Use empty text for null
            if (value == null) {
                fieldOrButton.setText("");
            } else {
                fieldOrButton.setText(String.valueOf(value));
            }

            // Set up context menu for ""/null toggling
            if (mValueType == String.class) {
                mUseEmptyStringInsteadNull = "".equals(value);
                fieldOrButton.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        if (TextUtils.isEmpty(fieldOrButton.getText())) {
                            final boolean setToNull = mUseEmptyStringInsteadNull;
                            menu.add(
                                    setToNull ?
                                            fieldOrButton.getContext().getString(R.string.set_to_null) :
                                            fieldOrButton.getContext().getString(R.string.set_to_empty_string)
                            ).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    mValueAccessors.setValue(setToNull ? null : "");
                                    mUseEmptyStringInsteadNull = !setToNull;
                                    fieldOrButton.setHint(mUseEmptyStringInsteadNull ? "\"\"" : "null");
                                    return true;
                                }
                            });
                        }
                    }
                });
            }

            // Set null hint
            if (mStringLikeValueNullable) {
                fieldOrButton.setHint(mUseEmptyStringInsteadNull ? "\"\"" : "null");
            }

            // Add text change listener
            fieldOrButton.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    parseValueAndInvokeSetter(fieldOrButton);
                }
            });
        } else {
            fieldOrButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mValueAccessors.startEditor();
                }
            });
            mButtonsDisplayingValue.add(new WeakReference<Button>((Button) fieldOrButton));
            updateTextOnButton();
        }
        return view;
    }

    /**
     * Remove any released Weak References
     */
    public void cleanup() {
        if (mButtonsDisplayingValue != null) {
            Iterator<WeakReference<Button>> iterator = mButtonsDisplayingValue.iterator();
            while (iterator.hasNext()) {
                WeakReference<Button> next = iterator.next();
                if (next.get() == null) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Update text on button, should be invoke after receiving editor result
     */
    public void updateTextOnButton() {
        assert !mStringLike;
        boolean shouldCleanup = false;
        for (WeakReference<Button> buttonWeakReference : mButtonsDisplayingValue) {
            Button button = buttonWeakReference.get();
            if (button != null) {
                button.setText(String.valueOf(mValueAccessors.getValue()));
            } else {
                shouldCleanup = true;
            }
        }
        if (shouldCleanup) {
            cleanup();
        }
    }

    /**
     * Parse value in given text view and invoke setter or show parse error to user
     */
    private void parseValueAndInvokeSetter(TextView textView) {
        String valueAsString = textView.getText().toString();
        Class<?> type = mValueType;
        Object value;
        try {
            // Get type and parse value
            if (mStringLikeValueNullable && "".equals(valueAsString)) {
                value = mUseEmptyStringInsteadNull ? "" : null;
            } else if (type == String.class) {
                value = valueAsString;
            } else if (type == Byte.TYPE || type == Byte.class) {
                value = Byte.valueOf(valueAsString);
            } else if (type == Character.TYPE || type == Character.class) {
                if (valueAsString.length() == 1) {
                    value = valueAsString.charAt(0);
                } else {
                    throw new NumberFormatException(); // TODO: report wrong length instead generic parse error
                }
            } else if (type == Short.TYPE || type == Short.class) {
                value = Short.valueOf(valueAsString);
            } else if (type == Integer.TYPE || type == Integer.class) {
                value = Integer.valueOf(valueAsString);
            } else if (type == Long.TYPE || type == Long.class) {
                value = Long.valueOf(valueAsString);
            } else if (type == Float.TYPE || type == Float.class) {
                value = Float.valueOf(valueAsString);
            } else if (type == Double.TYPE || type == Double.class) {
                value = Double.valueOf(valueAsString);
            } else {
                throw new RuntimeException("Unexpected string-like type");
            }
        } catch (NumberFormatException e) {
            // Show parse error and return
            textView.setError(textView.getContext().getString(R.string.value_parse_error));
            return;
        }
        textView.setError(null);
        mValueAccessors.setValue(value);
    }

    public interface ValueAccessors {
        public Object getValue();

        public void setValue(Object newValue);

        public void startEditor();
    }
}
