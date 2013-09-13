package com.example.testapp1.providerlab;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.example.testapp1.R;
import com.example.testapp1.Utils;

import java.util.ArrayList;

public class AdvancedQueryActivity extends Activity {
    public static final int METHOD_QUERY = 0;
    public static final int METHOD_INSERT = 1;
    public static final int METHOD_UPDATE = 2;
    public static final int METHOD_DELETE = 3;
    public static final int METHOD_GET_TYPE = 4;
    public static final int METHOD_OPEN_FILE = 5;
    public static final int METHOD_OPEN_ASSET_FILE = 6;
    private static final String[] METHOD_NAMES = new String[]{
            "query",
            "insert",
            "update",
            "delete"
    };

    public static final String EXTRA_METHOD = "[method]__";
    public static final String EXTRA_PROJECTION = "[pr]";
    public static final String EXTRA_PROJECTION_AVAILABLE_COLUMNS = "[pr-a]";
    public static final String EXTRA_SELECTION = "[sel]";
    public static final String EXTRA_SELECTION_ARGS = "[sel-a]";
    public static final String EXTRA_CONTENT_VALUES = "[cv]";
    public static final String EXTRA_SORT_ORDER = "_order[_]";


    private AutoCompleteTextView mUriTextView;
    private Button mExecuteButton;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_query);

        Intent intent = getIntent();
        Bundle instanceStateOrExtras =
                savedInstanceState != null ? savedInstanceState : intent.getExtras();
        if (instanceStateOrExtras == null) {
            instanceStateOrExtras = Bundle.EMPTY;
        }

        // Uri
        mUriTextView = (AutoCompleteTextView) findViewById(R.id.uri);
        if (intent.getData() != null) {
            mUriTextView.setText(intent.getDataString());
        }
        mUriTextView.setAdapter(new UriAutocompleteAdapter(this));

        // Projection
        {
            mSpecifyProjectionCheckBox = (CheckBox) findViewById(R.id.specify_projection);
            mProjectionLayout = (LinearLayout) findViewById(R.id.projection_columns);

            // Bind events for master CheckBox and add new button
            findViewById(R.id.new_projection_column).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new UserProjectionColumn("");
                }
            });
            mSpecifyProjectionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mProjectionLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });

            // Get values to fill
            String[] availableProjectionColumns = intent.getStringArrayExtra(EXTRA_PROJECTION_AVAILABLE_COLUMNS);
            String[] specifiedProjectionColumns = instanceStateOrExtras.getStringArray(EXTRA_PROJECTION);

            if (specifiedProjectionColumns == null) {
                mSpecifyProjectionCheckBox.setChecked(false);
            }

            if (availableProjectionColumns != null && availableProjectionColumns.length == 0) {
                availableProjectionColumns = null;
            }
            if (availableProjectionColumns != null && specifiedProjectionColumns == null) {
                specifiedProjectionColumns = availableProjectionColumns;
            }

            // Create available column checkboxes
            int i = 0;
            if (availableProjectionColumns != null) {
                for (String availableProjectionColumn : availableProjectionColumns) {
                    boolean isChecked =
                            i < specifiedProjectionColumns.length &&
                            availableProjectionColumn.equals(specifiedProjectionColumns[i]);
                    new DefaultProjectionColumn(availableProjectionColumn, isChecked);
                    if (isChecked) {
                        i++;
                    }
                }
            }

            // Create user column text fields
            if (specifiedProjectionColumns != null && i < specifiedProjectionColumns.length) {
                for (int il = specifiedProjectionColumns.length; i < il; i++) {
                    new UserProjectionColumn(specifiedProjectionColumns[i]);
                }
            }


        }

        // Selection
        {
            // Find views
            mSelectionCheckBox = (CheckBox) findViewById(R.id.selection_header);
            mSelectionText = (TextView) findViewById(R.id.selection);
            mSelectionLayout = findViewById(R.id.selection_layout);
            mSelectionArgsTable = (TableLayout) findViewById(R.id.selection_args);

            // Bind events for add button and master CheckBox
            findViewById(R.id.selection_add_arg).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new SelectionArg("", true);
                }
            });
            mSelectionCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mSelectionLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });

            // Fill selection text view and CheckBox
            String selection = intent.getStringExtra(EXTRA_SELECTION);
            String[] selectionArgs = instanceStateOrExtras.getStringArray(EXTRA_SELECTION_ARGS);

            mSelectionCheckBox.setChecked(selection != null);
            if (selection != null) {
                mSelectionText.setText(selection);
            }

            // Fill selection arguments
            if ((selection != null || savedInstanceState != null) && selectionArgs != null && selectionArgs.length != 0) {
                for (String selectionArg : selectionArgs) {
                    new SelectionArg(selectionArg);
                }
            }
        }


        // Content values
        {
            // Find views
            mContentValuesHeader = (TextView) findViewById(R.id.content_values_header);
            mContentValuesTable = (TableLayout) findViewById(R.id.content_values);
            mContentValuesTableHeader = (TableRow) findViewById(R.id.content_values_table_header);

            // Bind add new button event
            findViewById(R.id.new_content_value).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new ContentValue("", "", true);
                }
            });

            // Create table
            ContentValues contentValues = instanceStateOrExtras.getParcelable(EXTRA_CONTENT_VALUES);
            if (contentValues != null) {
                contentValues.valueSet();
                for (String key : Utils.getKeySet(contentValues)) {
                    new ContentValue(key, contentValues.getAsString(key));
                }
            }
        }

        // Order
        {
            // Find views
            mSpecifyOrderCheckBox = (CheckBox) findViewById(R.id.specify_order);
            mOrderTextView = (TextView) findViewById(R.id.order);

            // Bind events
            mSpecifyOrderCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mOrderTextView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });

            // Fill fields
            String order = intent.getStringExtra(EXTRA_SORT_ORDER);
            if (order == null) {
                mSpecifyOrderCheckBox.setChecked(false);
            } else {
                mOrderTextView.setText(order);
            }
        }

        // Execute `method()` button
        mExecuteButton = (Button) findViewById(R.id.execute);

        // Method (affects previous views so they must be initialized first)
        mMethodSpinner = (Spinner) findViewById(R.id.method);
        mMethodSpinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, METHOD_NAMES));
        mMethodSpinner.setOnItemSelectedListener(onMethodSpinnerItemSelectedListener);
        mMethodSpinner.setSelection(intent.getIntExtra(EXTRA_METHOD, METHOD_QUERY));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArray(EXTRA_PROJECTION, getProjection());
        outState.putStringArray(EXTRA_SELECTION_ARGS, getSelectionArgs());
        outState.putParcelable(EXTRA_CONTENT_VALUES, getContentValues());
    }

    // Method
    private Spinner mMethodSpinner;
    private final AdapterView.OnItemSelectedListener onMethodSpinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            boolean showProjectionAndOrder, showSelection, showContentValues;

            // determine if views should be shown or hidden
            switch (position) {
                case METHOD_QUERY:
                    showProjectionAndOrder = true;
                    showSelection = true;
                    showContentValues = false;
                    break;
                case METHOD_INSERT:
                    showProjectionAndOrder = false;
                    showSelection = false;
                    showContentValues = true;
                    break;
                case METHOD_UPDATE:
                    showProjectionAndOrder = false;
                    showSelection = true;
                    showContentValues = true;
                    break;
                case METHOD_DELETE:
                    showProjectionAndOrder = false;
                    showSelection = true;
                    showContentValues = false;
                    break;
                default:
                    throw new RuntimeException("Unexpected method spinner position");
            }

            // Show/hide projection
            mSpecifyProjectionCheckBox.setVisibility(showProjectionAndOrder ? View.VISIBLE : View.GONE);
            mProjectionLayout.setVisibility((showProjectionAndOrder && mSpecifyProjectionCheckBox.isChecked()) ? View.VISIBLE : View.GONE);

            // Show/hide selection (WHERE)
            mSelectionCheckBox.setVisibility(showSelection ? View.VISIBLE : View.GONE);
            mSelectionLayout.setVisibility((showSelection && mSelectionCheckBox.isChecked()) ? View.VISIBLE : View.GONE);

            // Show/hide ContentValues
            mContentValuesHeader.setVisibility(showContentValues ? View.VISIBLE : View.GONE);
            mContentValuesTable.setVisibility(showContentValues ? View.VISIBLE : View.GONE);

            // Show/hide order
            mSpecifyOrderCheckBox.setVisibility(showProjectionAndOrder ? View.VISIBLE : View.GONE);
            mOrderTextView.setVisibility((showProjectionAndOrder && mSpecifyOrderCheckBox.isChecked()) ? View.VISIBLE : View.GONE);

            // Set execute `method()` button label
            mExecuteButton.setText(METHOD_NAMES[position] + "()");
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Spinner won't have nothing selected
        }
    };

    // Projection
    private CheckBox mSpecifyProjectionCheckBox;
    private LinearLayout mProjectionLayout;

    private final ArrayList<ProjectionColumn> mProjectionColumns = new ArrayList<ProjectionColumn>();

    private String[] getProjection() {
        if (!mSpecifyProjectionCheckBox.isChecked()) {
            return null;
        }
        ArrayList<String> projection = new ArrayList<String>();
        for (ProjectionColumn projectionColumn : mProjectionColumns) {
            String name = projectionColumn.getName();
            if (name != null) {
                projection.add(name);
            }
        }
        return projection.toArray(new String[projection.size()]);
    }

    private interface ProjectionColumn {
        String getName();
    }

    private class DefaultProjectionColumn implements ProjectionColumn {
        private final String mName;
        private final CheckBox mCheckBox;

        DefaultProjectionColumn(String name, boolean isDefaultSelected) {
            mName = name;
            mCheckBox = new CheckBox(AdvancedQueryActivity.this);
            mCheckBox.setText(name);
            mCheckBox.setChecked(isDefaultSelected);
            mProjectionLayout.addView(mCheckBox, mProjectionLayout.getChildCount() - 1);
            mProjectionColumns.add(this);
        }

        @Override
        public String getName() {
            return mCheckBox.isChecked() ? mName : null;
        }
    }

    private class UserProjectionColumn implements ProjectionColumn, View.OnClickListener {

        private final View mView;
        private final TextView mTextView;

        UserProjectionColumn(String name) {
            mView = getLayoutInflater().inflate(R.layout.category_row, null);
            mTextView = (TextView) mView.findViewById(R.id.categoryText);
            mTextView.setText(name);
            mView.findViewById(R.id.row_remove).setOnClickListener(this);
            mProjectionLayout.addView(mView, mProjectionLayout.getChildCount() - 1);
            mProjectionColumns.add(this);
        }

        // For remove button
        @Override
        public void onClick(View v) {
            mProjectionLayout.removeView(mView);
            mProjectionColumns.remove(this);
        }

        @Override
        public String getName() {
            return mTextView.getText().toString();
        }
    }
    // /Projection

    // Selection
    private CheckBox mSelectionCheckBox;
    private TextView mSelectionText;
    private View mSelectionLayout;
    private TableLayout mSelectionArgsTable;

    private final ArrayList<SelectionArg> mSelectionArgs = new ArrayList<SelectionArg>();

    private class SelectionArg implements View.OnClickListener {
        private final TextView mArgNumber;
        private final TextView mArgValue;
        private final View mView;

        SelectionArg(String value) {
            this(value, false);
        }

        SelectionArg(String value, boolean shouldRequestFocus) {
            // Inflate
            mView = getLayoutInflater().inflate(R.layout.selection_arg, mSelectionArgsTable, false);

            // Find text views
            mArgNumber = (TextView) mView.findViewById(R.id.arg_number);
            mArgValue = (TextView) mView.findViewById(R.id.arg);

            // Bind remove button
            mView.findViewById(R.id.row_remove).setOnClickListener(this);

            // Fill texts
            setArgNumber(mSelectionArgs.size());
            mArgValue.setText(value);

            // Register and attach
            mSelectionArgsTable.addView(mView);
            mSelectionArgs.add(this);

            // Request focus
            if (shouldRequestFocus) {
                mArgValue.requestFocus();
            }
        }

        @Override
        public void onClick(View v) { // For remove button
            mSelectionArgsTable.removeView(mView); // detach from view
            mSelectionArgs.remove(this); // un-register
            reorderSelectionArgs();
        }

        void setArgNumber(int number) {
            mArgNumber.setText(number + ":");
        }

        String getValue() {
            return mArgValue.getText().toString();
        }
    }

    private void reorderSelectionArgs() {
        for (int i = 0; i < mSelectionArgs.size(); i++) {
            SelectionArg selectionArg = mSelectionArgs.get(i);
            selectionArg.setArgNumber(i);
        }
    }

    private String getSelection() {
        return mSelectionCheckBox.isChecked() ? mSelectionText.getText().toString() : null;
    }

    private String[] getSelectionArgs() {
        int selectionArgsCount = mSelectionArgs.size();
        String[] arr = new String[selectionArgsCount];
        for (int i = 0; i < selectionArgsCount; i++) {
            SelectionArg selectionArg = mSelectionArgs.get(i);
            arr[i] = selectionArg.getValue();
        }
        return arr;
    }
    // /Selection

    // Content values
    private final ArrayList<ContentValue> mContentValues = new ArrayList<ContentValue>();
    private TextView mContentValuesHeader;
    private TableLayout mContentValuesTable;
    private TableRow mContentValuesTableHeader;


    private class ContentValue implements View.OnClickListener {

        private final TextView mName;
        private final TextView mValue;
        private final View mView;

        ContentValue(String name, String value) {
            this(name, value, false);
        }

        ContentValue(String name, String value, boolean shouldRequestFocus) {
            // Inflate
            mView = getLayoutInflater().inflate(R.layout.content_value, mContentValuesTable, false);

            // Find text views
            mName = (TextView) mView.findViewById(R.id.name);
            mValue = (TextView) mView.findViewById(R.id.value);

            // Fill text views
            mName.setText(name);
            if (value != null) {
                mValue.setText(value);
            }

            // Register and show table header if needed
            mContentValues.add(this);
            if (mContentValues.size() == 1) {
                mContentValuesTableHeader.setVisibility(View.VISIBLE);
            }

            // Bind remove button
            mView.findViewById(R.id.row_remove).setOnClickListener(this);

            // Attach to table before add new button
            mContentValuesTable.addView(mView, mContentValuesTable.getChildCount() - 1);

            if (shouldRequestFocus) {
                mName.requestFocus();
            }
        }

        void putContentValue(ContentValues values) {
            values.put(mName.getText().toString(), mValue.getText().toString());
        }

        void remove() {
            // remove from table
            mContentValuesTable.removeView(mView);

            // Un-register and hide table header if no longer needed
            mContentValues.remove(this);
            if (mContentValues.size() == 0) {
                mContentValuesTableHeader.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) { // For remove button
            remove();
        }
    }


    private ContentValues getContentValues() {
        ContentValues values = new ContentValues();
        for (ContentValue contentValue : mContentValues) {
            contentValue.putContentValue(values);
        }
        return values;
    }
    // /Content values

    // Order
    private CheckBox mSpecifyOrderCheckBox;
    private TextView mOrderTextView;
    private String getSortOrder() {
        return mSpecifyOrderCheckBox.isChecked() ? mOrderTextView.getText().toString() : null;
    }
    // /Order

    public void executeQuery(View v) {
        Uri uri = Uri.parse(mUriTextView.getText().toString());
        try {
            switch (mMethodSpinner.getSelectedItemPosition()) {
                case METHOD_QUERY:
                    startActivity(
                            new Intent(this, QueryResultActivity.class)
                            .setData(uri)
                            .putExtra(EXTRA_PROJECTION, getProjection())
                            .putExtra(EXTRA_PROJECTION_AVAILABLE_COLUMNS, getIntent().getStringArrayExtra(EXTRA_PROJECTION_AVAILABLE_COLUMNS))
                            .putExtra(EXTRA_SELECTION, getSelection())
                            .putExtra(EXTRA_SELECTION_ARGS, getSelectionArgs())
                            .putExtra(EXTRA_SORT_ORDER, getSortOrder())
                    );
                    break;
                case METHOD_INSERT:
                    getContentResolver().insert(uri, getContentValues());
                    break;
                case METHOD_UPDATE:
                    getContentResolver().update(uri, getContentValues(), getSelection(), getSelectionArgs());
                    break;
                case METHOD_DELETE:
                    getContentResolver().delete(uri, getSelection(), getSelectionArgs());
                    break;
            }
        } catch (Exception e) {
            Utils.toastException(this, e);
        }
    }
}