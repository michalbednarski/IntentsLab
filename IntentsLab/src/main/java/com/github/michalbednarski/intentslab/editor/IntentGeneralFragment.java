/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab.editor;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.FormattedTextBuilder;
import com.github.michalbednarski.intentslab.NameAutocompleteAdapter;
import com.github.michalbednarski.intentslab.PackageNameAutocompleteAdapter;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.providerlab.AdvancedQueryActivity;
import com.github.michalbednarski.intentslab.providerlab.UriAutocompleteAdapter;
import com.github.michalbednarski.intentslab.providerlab.proxy.ProxyProvider;
import com.github.michalbednarski.intentslab.providerlab.proxy.ProxyProviderForGrantUriPermission;
import com.github.michalbednarski.intentslab.xposedhooks.api.IntentTracker;
import com.github.michalbednarski.intentslab.xposedhooks.api.ReadBundleEntryInfo;
import com.github.michalbednarski.intentslab.xposedhooks.api.TrackerUpdateListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class IntentGeneralFragment extends IntentEditorPanel implements OnItemSelectedListener, TrackerUpdateListener {

    /**
     * List of actions for which we don't imply that if there is accepted data type but no scheme then schemes
     * can be file: or content:.
     *
     * System makes such assumption for every IntentFilter, but in case of these we don't think application will expect
     * such behavior.
     */
    private static final HashSet<String> NO_IMPLICIT_URI_ACTIONS = new HashSet<String>(2);
    static {
        NO_IMPLICIT_URI_ACTIONS.add(Intent.ACTION_PICK);
        NO_IMPLICIT_URI_ACTIONS.add(Intent.ACTION_GET_CONTENT);
    };

    private static final String STATE_SHOW_ADVANCED = "intenteditor.general.showadvanced";

    private boolean mShowAdvanced;

    private AutoCompleteTextView mDataText;
    private View mDataTextWrapper;
    private View mDataTextHeader;
    private TextView mComponentText;
    private Spinner mComponentTypeSpinner;
    private Spinner mMethodSpinner;
    private TextView mResponseCodeTextView;
    private Intent mEditedIntent;
    private UriAutocompleteAdapter mUriAutocompleteAdapter;
    private AutoCompleteTextView mPackageNameText;
    private View mResultCodeWrapper;
    private View mComponentTypeAndMethodSpinners;
    private View mComponentHeader;
    private View mComponentFieldWithButtons;
    private View mPackageNameHeader;
    private TextView mIntentTrackerSummary;


    public IntentGeneralFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mShowAdvanced = savedInstanceState.getBoolean(STATE_SHOW_ADVANCED);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.intent_editor_general, container, false);

        // Prepare form
        mActionText = (AutoCompleteTextView) v.findViewById(R.id.action);
        mActionsSpinner = (Spinner) v.findViewById(R.id.action_spinner);
        mDataText = (AutoCompleteTextView) v.findViewById(R.id.data);
        mDataTextWrapper = v.findViewById(R.id.data_wrapper);
        mDataTextHeader = v.findViewById(R.id.data_header);
        mDataTypeHeader = v.findViewById(R.id.data_type_header);
        mDataTypeText = (TextView) v.findViewById(R.id.data_type);
        mDataTypeSpinner = (Spinner) v.findViewById(R.id.data_type_spinner);
        mDataTypeSpinnerWrapper = v.findViewById(R.id.data_type_spinner_wrapper);
        mDataTypeSlash = v.findViewById(R.id.data_type_slash);
        mDataSubtypeText = (TextView) v.findViewById(R.id.data_type_after_slash);
        mComponentText = (TextView) v.findViewById(R.id.component);

        mComponentTypeSpinner = (Spinner) v.findViewById(R.id.componenttype);
        mComponentTypeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.componenttypes)));

        mMethodSpinner = (Spinner) v.findViewById(R.id.method);

        mCategoriesContainer = (ViewGroup) v.findViewById(R.id.categories);
        mAddCategoryButton = (Button) v.findViewById(R.id.category_add);
        mCategoriesHeader = v.findViewById(R.id.categories_header);
        mResponseCodeTextView = (TextView) v.findViewById(R.id.response_code);
        mPackageNameText = (AutoCompleteTextView) v.findViewById(R.id.package_name);

        mResultCodeWrapper = v.findViewById(R.id.result_intent_wrapper);
        mComponentTypeAndMethodSpinners = v.findViewById(R.id.component_and_method_spinners);
        mComponentHeader = v.findViewById(R.id.component_header);
        mComponentFieldWithButtons = v.findViewById(R.id.component_field_with_buttons);
        mPackageNameHeader = v.findViewById(R.id.package_name_header);
        mIntentTrackerSummary = (TextView) v.findViewById(R.id.intent_tracker_summary);
        mIntentTrackerSummary.setMovementMethod(LinkMovementMethod.getInstance());

        // Apparently using android:scrollHorizontally="true" does not work.
        // http://stackoverflow.com/questions/9011944/android-ice-cream-sandwich-edittext-disabling-spell-check-and-word-wrap
        mComponentText.setHorizontallyScrolling(true);

        // Bind button actions (android:onClick="" applies to hosting activity)
        mAddCategoryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategoryTextField("");
            }
        });
        v.findViewById(R.id.component_pick).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pickComponent();
            }
        });
        v.findViewById(R.id.component_pick).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                findComponent();
                return true;
            }
        });
        v.findViewById(R.id.component_clear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mComponentText.setText("");
            }
        });
        v.findViewById(R.id.data_query_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getIntentEditor().updateIntent();
                startActivity(new Intent(getActivity(), AdvancedQueryActivity.class).setData(mEditedIntent.getData()));
            }
        });
        v.findViewById(R.id.data_query_button).setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                getIntentEditor().updateIntent();
                final Uri uri = mEditedIntent.getData();
                if (uri != null) {
                    final String scheme = uri.getScheme();
                    final String authority = uri.getAuthority();
                    if ("content".equals(scheme) && authority != null) {
                        menu.add("Wrap").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                mDataText.setText(
                                        "content://" +
                                        ((mEditedIntent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) != 0 ?
                                                ProxyProviderForGrantUriPermission.AUTHORITY :
                                                ProxyProvider.AUTHORITY) +
                                        "/" +
                                        authority +
                                        uri.getPath()
                                );
                                return true;
                            }
                        });
                    }
                }
            }
        });

        // Set up autocomplete
        mUriAutocompleteAdapter = new UriAutocompleteAdapter(getActivity());
        mDataText.setAdapter(mUriAutocompleteAdapter);
        mPackageNameText.setAdapter(new PackageNameAutocompleteAdapter(getActivity()));

        // Get edited intent for form filling
        mEditedIntent = getEditedIntent();

        // Component field, affects options menu
        if (mEditedIntent.getComponent() != null) {
            mComponentText.setText(mEditedIntent.getComponent()
                    .flattenToShortString());
        }
        mComponentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateIntentComponent();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Fill the form
        setupActionSpinnerOrField();
        updateNonActionIntentFilter(true);
        mDataText.setText(mEditedIntent.getDataString());
        mPackageNameText.setText(mEditedIntent.getPackage());

        showOrHideFieldsForResultIntent();
        showOrHideAdvancedFields();
        if (getComponentType() == IntentEditorConstants.RESULT) {
            mResponseCodeTextView.setText(String.valueOf(getIntentEditor().getMethodId()));
            mResponseCodeTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        getIntentEditor().setMethodId(Integer.parseInt(s.toString()));
                        mResponseCodeTextView.setError(null);
                    } catch (NumberFormatException e) {
                        mResponseCodeTextView.setError(getIntentEditor().getText(R.string.value_parse_error));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        } else {
            initComponentAndMethodSpinners();
        }

        setupActionAutocomplete();

        // Prepare intent tracker
        {
            IntentTracker tracker = getIntentEditor().getIntentTracker();
            if (tracker != null) {
                tracker.setUpdateListener(this, true);
            } else {
                onNoTracker();
            }
        }

        return v;
    }

    private void initComponentAndMethodSpinners() {
        int componentType = getComponentType();
        initMethodSpinner(componentType);
        mComponentTypeSpinner.setSelection(componentType);
        mMethodSpinner.setSelection(getIntentEditor().getMethodId());
        mComponentTypeSpinner.setOnItemSelectedListener(this);
        mMethodSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onDestroyView() {
        IntentTracker tracker = getIntentEditor().getIntentTracker();
        if (tracker != null) {
            tracker.clearUpdateListener();
        }

        super.onDestroyView();
        mActionText = null;
        mDataText = null;
        mComponentText = null;
        mComponentTypeSpinner = null;
        mMethodSpinner = null;
        mCategoriesContainer = null;
        mCategoryTextInputs = null;
        mCategoryCheckBoxes = null;
        mAddCategoryButton = null;
    }

    // Component type and method
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {

        if (parent == mComponentTypeSpinner) {
            if (getComponentType() != position) {
                initMethodSpinner(position);
                getIntentEditor().setComponentType(position);
            }

        } else if (parent == mMethodSpinner) {
            if (getIntentEditor().getMethodId() != position) {
                getIntentEditor().setMethodId(position);
            }
        } else if (parent == mActionsSpinner) {
            updateNonActionIntentFilter(false);
        }
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Spinner won't have nothing selected
    }

    static String[] getMethodNamesArray(Resources res, int componentType) {
        return res.getStringArray(
                componentType == IntentEditorConstants.ACTIVITY ? R.array.activitymethods :
                        componentType == IntentEditorConstants.BROADCAST ? R.array.broadcastmethods :
                                componentType == IntentEditorConstants.SERVICE ? R.array.servicemethods : 0
        );
    }

    private void initMethodSpinner(int componentType) {
        mMethodSpinner.setAdapter(
                new ArrayAdapter<String>(
                        getActivity(),
                        android.R.layout.simple_spinner_item,
                        getMethodNamesArray(getResources(), componentType)
                )
        );
    }

    // ACTIONS
    private AutoCompleteTextView mActionText;
    private Spinner mActionsSpinner;
    private String[] mAvailbleActions;

    private void setupActionSpinnerOrField() {
        IntentFilter[] intentFilters = getIntentEditor().getAttachedIntentFilters();
        if (intentFilters != null) {
            // Build action set
            Set<String> actionSet = new HashSet<String>();
            for (IntentFilter filter : intentFilters) {
                for (int i = 0, j = filter.countActions(); i < j; i++) {
                    actionSet.add(filter.getAction(i));
                }
            }

            // Check if we have any action and if not (invalid intent-filter)
            // switch to textfield
            if (actionSet.isEmpty()) {
                setupActionField();
                return;
            }

            // Convert action set to array
            mAvailbleActions = actionSet.toArray(new String[actionSet.size()]);
            Arrays.sort(mAvailbleActions);

            // Select current action
            String action = mEditedIntent.getAction();
            if (action == null) {
                action = Intent.ACTION_VIEW;
            }
            int position = Arrays.binarySearch(mAvailbleActions, action);
            mActionsSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mAvailbleActions));
            if (position < 0) {
                position = 0;
            }
            mActionsSpinner.setSelection(position);

            // Show spinner and hide textfield
            mActionText.setVisibility(View.GONE);
            mActionsSpinner.setVisibility(View.VISIBLE);

            // Set listener for action change to refresh intent filter
            mActionsSpinner.setOnItemSelectedListener(this);
        } else {
            setupActionField();
        }
    }

    private void setupActionField() {
        mAvailbleActions = null;
        mActionsSpinner.setOnItemSelectedListener(null);
        mActionText.setText(mEditedIntent.getAction());
        mActionText.setVisibility(View.VISIBLE);
        mActionsSpinner.setVisibility(View.GONE);
    }


    // CATEGORIES
    private ViewGroup mCategoriesContainer;
    private ArrayList<TextView> mCategoryTextInputs;
    private CheckBox[] mCategoryCheckBoxes;
    private Button mAddCategoryButton;
    private View mCategoriesHeader;

    public void addCategoryTextField(String category) {
        final ViewGroup row = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.category_row, mCategoriesContainer, false);
        final TextView textField = ((TextView) row.findViewById(R.id.categoryText));
        textField.setText(category);
        row.findViewById(R.id.row_remove).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCategoriesContainer.removeView(row);
                mCategoryTextInputs.remove(textField);
            }
        });
        mCategoriesContainer.addView(row);
        mCategoryTextInputs.add(textField);
    }



    private void setupCategoryCheckBoxes(Set<String> availableCategories) {

        mCategoriesContainer.removeAllViews();

        mCategoryCheckBoxes = new CheckBox[availableCategories.size()];
        int i = 0;
        for (String category : availableCategories) {
            CheckBox cb = new CheckBox(getActivity());
            cb.setText(category);
            cb.setTag(category);
            cb.setChecked(mEditedIntent.hasCategory(category));
            mCategoriesContainer.addView(cb);
            mCategoryCheckBoxes[i++] = cb;
        }

        mAddCategoryButton.setVisibility(View.GONE);
        mCategoriesHeader.setVisibility(availableCategories.size() == 0 ? View.GONE : View.VISIBLE);

        mCategoryTextInputs = null;
    }

    private void setFreeFormCategoryEditor() {
        if (mCategoryTextInputs != null) {
            return; // We are already in free form mode
        }

        mCategoriesContainer.removeAllViews();

        mCategoryTextInputs = new ArrayList<TextView>();
        Set<String> categories = mEditedIntent.getCategories();
        if (categories != null) {
            for (String category : categories) {
                addCategoryTextField(category);
            }
        }

        mAddCategoryButton.setVisibility(View.VISIBLE);
        mCategoriesHeader.setVisibility(View.VISIBLE);

        mCategoryCheckBoxes = null;
    }

    // COMPONENT
    private void pickComponent() {
        updateEditedIntent(mEditedIntent);

        ComponentPickerDialogFragment componentPickerDialog = new ComponentPickerDialogFragment();
        componentPickerDialog.setTargetFragment(this, 0);
        componentPickerDialog.show(getActivity().getSupportFragmentManager(), "component-picker");
    }

    private void findComponent() {
        getIntentEditor().updateIntent();
        new FindComponentDialog()
                .show(getActivity().getSupportFragmentManager(), "component-finder");
    }

    void setComponentText(String text) {
        mComponentText.setText(text);
    }

    private void updateIntentComponent() {
        ComponentName newComponentName = ComponentName.unflattenFromString(mComponentText.getText().toString());
        ComponentName oldComponentName = mEditedIntent.getComponent();
        mEditedIntent.setComponent(newComponentName);
        if ((newComponentName != null) != (oldComponentName != null)) {
            getIntentEditor().safelyInvalidateOptionsMenu();
        }
    }

    // Contact with activity
    @Override
    public void updateEditedIntent(Intent editedIntent) {
        // Intent action

        if (mAvailbleActions != null) {
            editedIntent.setAction((String) mActionsSpinner.getSelectedItem());
        } else {
            String action = mActionText.getText().toString();
            if ("".equals(action)) {
                action = null;
            }
            editedIntent.setAction(action);
        }

        // Categories
        {
            // Clear categories (why there's no api for this)
            Set<String> origCategories = editedIntent.getCategories();
            if (origCategories != null) {
                for (String category : origCategories.toArray(new String[origCategories.size()])) {
                    editedIntent.removeCategory(category);
                }
            }
        }
        // Fill categories
        if (mCategoryCheckBoxes != null) {
            // Fill categories from checkboxes
            for (CheckBox cb : mCategoryCheckBoxes) {
                String category = (String) cb.getTag();
                if (cb.isChecked()) {
                    editedIntent.addCategory(category);
                }
            }
        } else {
            // Fill categories from textfields
            for (TextView categoryTextView : mCategoryTextInputs) {
                editedIntent.addCategory(categoryTextView.getText().toString());
            }
        }

        // Intent data (Uri) and type (MIME)
        String data = mDataText.getText().toString();
        editedIntent.setDataAndType(
                data.equals("") ? null : Uri.parse(data),
                getDataType()
        );

        // Package name
        {
            String packageName = mPackageNameText.getText().toString();
            if ("".equals(packageName)) {
                editedIntent.setPackage(null);
            } else {
                editedIntent.setPackage(packageName);
            }
        }

        // Set component for explicit intent
        updateIntentComponent();
    }



    // DATA TYPE
    private View mDataTypeHeader;
    private TextView mDataTypeText;
    private Spinner mDataTypeSpinner;
    private View mDataTypeSpinnerWrapper;
    private View mDataTypeSlash;
    private TextView mDataSubtypeText;

    private boolean mUseDataType;
    private boolean mUseDataTypeSpinner;
    private boolean mDataTypeMayBeNull;

    /**
     * Switch data type selector to filtered (Spinner) mode and initialize it's value
     */
    private void setupFilteredDataTypeFields(boolean mayBeUntyped, boolean mayAutoDetect, Set<String> dataTypes) {
        mDataTypeMayBeNull = mayBeUntyped;

        if (dataTypes.size() == 0) {
            // IntentFilter doesn't accept data type
            mDataTypeHeader.setVisibility(View.GONE);
            mDataTypeText.setVisibility(View.GONE);
            mDataTypeSpinnerWrapper.setVisibility(View.GONE);

            mUseDataType = false;
        } else {
            // We have set of accepted data types, show them in spinner

            // Build array of items
            String[] spinnerItems = dataTypes.toArray(new String[dataTypes.size()]);

            // Sort them
            Arrays.sort(spinnerItems);

            // Find current
            int currentPosition = -1;
            int slashPos = -1;
            if (mEditedIntent.getType() != null) {
                currentPosition = Arrays.binarySearch(spinnerItems, mEditedIntent.getType());
                if (currentPosition < 0) {
                    // Try also partial type matching
                    slashPos = mEditedIntent.getType().indexOf('/');
                    if (slashPos != -1) {
                        currentPosition = Arrays.binarySearch(spinnerItems, mEditedIntent.getType().substring(0, slashPos));
                    }
                }
            }

            // If we also accept untyped variant add that option at first position
            mDataTypeMayBeNull = mayBeUntyped || mayAutoDetect;
            if (mDataTypeMayBeNull) {
                String[] shiftedSpinnerItems = new String[spinnerItems.length + 1];
                System.arraycopy(spinnerItems, 0, shiftedSpinnerItems, 1, spinnerItems.length);

                currentPosition++;
                shiftedSpinnerItems[0] = mayBeUntyped ?
                        getActivity().getString(R.string.data_type_unspecified) :
                        getActivity().getString(R.string.data_type_autodetect);

                spinnerItems = shiftedSpinnerItems;
            }

            // Put data in Spinner
            mDataTypeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, spinnerItems));
            if (currentPosition < 0) {
                currentPosition = 0;
            }
            mDataTypeSpinner.setSelection(currentPosition);


            // Show the Spinner
            mDataTypeHeader.setVisibility(View.VISIBLE);
            mDataTypeText.setVisibility(View.GONE);
            mDataTypeSpinnerWrapper.setVisibility(View.VISIBLE);

            // Prepare subtype field
            boolean isPartialType = useSpinnerAndTextEditForDataType();
            if (isPartialType && slashPos != -1) {
                mDataSubtypeText.setText(mEditedIntent.getType().substring(slashPos + 1));
            }
            mDataTypeSlash.setVisibility(isPartialType ? View.VISIBLE : View.GONE);
            mDataSubtypeText.setVisibility(isPartialType ? View.VISIBLE : View.GONE);

            // Bind event
            mDataTypeSpinner.setOnItemSelectedListener(mTypeSpinnerListener);

            // Set flags
            mUseDataType = true;
            mUseDataTypeSpinner = true;
        }
    }

    /**
     * Switch data type to unfiltered (TextView) mode and initialize it's value
     */
    private void setupUnfilteredDataTypeFields() {
        // Unbind event
        mDataTypeSpinner.setOnItemSelectedListener(null);

        // Show field and hide spinner
        mDataTypeHeader.setVisibility(View.VISIBLE);
        mDataTypeText.setVisibility(View.VISIBLE);
        mDataTypeSpinnerWrapper.setVisibility(View.GONE);

        // Show text
        mDataTypeText.setText(mEditedIntent.getType());

        // Set flags
        mUseDataType = true;
        mUseDataTypeSpinner = false;
    }

    private OnItemSelectedListener mTypeSpinnerListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // Show or hide data subtype field
            boolean isPartialType = useSpinnerAndTextEditForDataType();
            mDataTypeSlash.setVisibility(isPartialType ? View.VISIBLE : View.GONE);
            mDataSubtypeText.setVisibility(isPartialType ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Spinner won't have nothing selected
        }
    };

    /**
     * Returns true if we should have subtype text field next to data type spinner
     * If mUseDataTypeSpinner is false the result is not defined and method should not be called
     */
    private boolean useSpinnerAndTextEditForDataType() {
        if (mDataTypeMayBeNull && mDataTypeSpinner.getSelectedItemPosition() == 0) {
            return false;
        } else {
            return  !((String) mDataTypeSpinner.getSelectedItem()).contains("/");
        }
    }

    /**
     * Get usable data type as String or null if user choose to not provide any.
     */
    private String getDataType() {
        if (!mUseDataType) {
            // Intent filter disallow data type
            return null;
        }

        if (mUseDataTypeSpinner) {
            // We are using spinner for data type

            // Check if this is 'Unspecified' option
            if (mDataTypeMayBeNull && mDataTypeSpinner.getSelectedItemPosition() == 0) {
                return null;
            }

            // Get value from spinner
            String type = (String) mDataTypeSpinner.getSelectedItem();

            // And add value from subtype field if needed
            if (useSpinnerAndTextEditForDataType()) {
                type += "/" + mDataSubtypeText.getText().toString();
            }
            return type;
        } else {

            // Just use value from normal text field
            String dataType = mDataTypeText.getText().toString();
            return dataType.equals("") ? null : dataType;
        }
    }

    // COMMON
    private void showOrHideFieldsForResultIntent() {
        boolean isResultIntent = getComponentType() == IntentEditorConstants.RESULT;
        mResultCodeWrapper.setVisibility(isResultIntent ? View.VISIBLE : View.GONE);
        mComponentTypeAndMethodSpinners.setVisibility(isResultIntent ? View.GONE : View.VISIBLE);
    }

    @Override
    void onIntentFiltersChanged(IntentFilter[] newIntentFilters) {
        updateEditedIntent(mEditedIntent);
        setupActionSpinnerOrField();
        updateNonActionIntentFilter(false);
    }

    void updateComponent() {
        if (mEditedIntent.getComponent() != null) {
            mComponentText.setText(mEditedIntent.getComponent()
                    .flattenToShortString());
        } else {
            mComponentText.setText("");
        }
    }

    @Override
    public void onComponentTypeChanged(int newComponentType) {
        if (mComponentTypeSpinner.getSelectedItemId() != newComponentType) {
            mComponentTypeSpinner.setSelection(newComponentType);
        }

        setupActionAutocomplete();
    }

    private void setupActionAutocomplete() {
        final int componentType = getComponentType();
        mActionText.setAdapter(
                componentType == IntentEditorConstants.ACTIVITY ? new NameAutocompleteAdapter(getActivity(), R.raw.activity_actions) :
                componentType == IntentEditorConstants.BROADCAST ? new NameAutocompleteAdapter(getActivity(), R.raw.broadcast_actions) :
                null
        );
    }

    /**
     * Update IntentFilter of categories and data
     * @param isInit
     */
    private void updateNonActionIntentFilter(boolean isInit) {
        // If we don't have any IntentFilter use non-filtering editors
        if (mAvailbleActions == null) {
            setFreeFormCategoryEditor();
            mDataTextWrapper.setVisibility(View.VISIBLE);
            mDataTextHeader.setVisibility(View.VISIBLE);
            mUriAutocompleteAdapter.setIntentFilters(null);
            setupUnfilteredDataTypeFields();
            return;
        }

        // Update edited intent
        if (!isInit) {
            updateEditedIntent(mEditedIntent);
        }

        // Get all IntentFilters
        final IntentFilter[] allIntentFilters = getIntentEditor().getAttachedIntentFilters();

        // Get selected action
        String action = (String) mActionsSpinner.getSelectedItem();


        HashSet<String> availableCategories = new HashSet<String>();

        HashSet<String> availableMimeTypes = new HashSet<String>();
        boolean acceptsAnyDataType = false;
        boolean acceptsUntypedData = false;
        boolean mayAutoDetectType = false;

        boolean acceptsUris = false;

        ArrayList<IntentFilter> selectedIntentFilters = new ArrayList<IntentFilter>();

        // Iterate over intent filters that has selected action
        for (IntentFilter filter : allIntentFilters) {
            if (filter.hasAction(action)) {
                // List categories
                if (filter.countCategories() != 0) {
                    for (Iterator<String> iterator = filter.categoriesIterator(); iterator.hasNext();) {
                        availableCategories.add(iterator.next());
                    }
                }

                // List available types or set flag that we don't need them
                if (filter.countDataTypes() == 0) {
                    acceptsUntypedData = true;
                } else {
                    for (Iterator<String> iterator = filter.typesIterator(); iterator.hasNext();) {
                        final String type = iterator.next();
                        if ("*".equals(type)) {
                            acceptsAnyDataType = true;
                        } else {
                            availableMimeTypes.add(type);
                        }
                    }
                }

                // Scan schemes to see if system can auto detect type
                if (!mayAutoDetectType) {
                    if(filter.countDataSchemes() != 0) {
                        for (Iterator<String> iterator = filter.schemesIterator(); iterator.hasNext();) {
                            String scheme = iterator.next();
                            if ("content".equals(scheme) || "file".equals(scheme)) {
                                mayAutoDetectType = true;
                                break;
                            }
                        }
                    } else if ( // No schemes declared
                            filter.countDataTypes() != 0 && ( // There's at least one
                                    !NO_IMPLICIT_URI_ACTIONS.contains(action) || // Action is not on list
                                            filter.countDataAuthorities() != 0 // There is host specified
                                    )
                            ) {

                            // Intent will match empty, content: or file: scheme
                            acceptsUris = true;
                            mayAutoDetectType = true;
                    }
                }

                // Check if we have data
                if (filter.countDataSchemes() != 0) {
                    acceptsUris = true;
                }

                // Save used IntentFilter to list because UriAutocompleteAdapter scans them on his own
                selectedIntentFilters.add(filter);
            }
        }

        // Setup categories
        setupCategoryCheckBoxes(availableCategories);

        // Setup data type
        if (acceptsAnyDataType) {
            setupUnfilteredDataTypeFields();
        } else {
            setupFilteredDataTypeFields(acceptsUntypedData, mayAutoDetectType, availableMimeTypes);
        }

        // Setup data uri
        mDataTextWrapper.setVisibility(acceptsUris ? View.VISIBLE : View.GONE);
        mDataTextHeader.setVisibility(acceptsUris ? View.VISIBLE : View.GONE);
        if (!acceptsUris) {
            mDataText.setText("");
        }
        mUriAutocompleteAdapter.setIntentFilters(selectedIntentFilters.toArray(new IntentFilter[selectedIntentFilters.size()]));
    }


    // Advanced toggle
    private void showOrHideAdvancedFields() {
        boolean isResultIntent = getComponentType() == IntentEditorConstants.RESULT;


        // Component, advanced only in result intent
        boolean showComponent = !isResultIntent || mShowAdvanced || mEditedIntent.getComponent() != null;
        mComponentHeader.setVisibility(showComponent ? View.VISIBLE : View.GONE);
        mComponentFieldWithButtons.setVisibility(showComponent ? View.VISIBLE : View.GONE);

        // Package name
        boolean showPackage = mShowAdvanced || mEditedIntent.getPackage() != null;
        mPackageNameText.setVisibility(showPackage ? View.VISIBLE : View.GONE);
        mPackageNameHeader.setVisibility(showPackage ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.intent_editor_general, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem advanced = menu.findItem(R.id.advanced);
        advanced.setChecked(mShowAdvanced);
        Utils.updateLegacyCheckedIcon(advanced);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.advanced:
            updateEditedIntent(mEditedIntent);
            mShowAdvanced = !mShowAdvanced;
            showOrHideAdvancedFields();
            ActivityCompat.invalidateOptionsMenu(getActivity());
            return true;
        default:
            return false;
        }
    }

    // Intent tracker
    @Override
    void onIntentTrackerChanged(IntentTracker newTracker, IntentTracker oldTracker) {
        if (oldTracker != null) {
            oldTracker.clearUpdateListener();
            oldTracker.getExtrasTracker().clearUpdateListener();
        }
        if (newTracker != null) {
            newTracker.setUpdateListener(this, true);
            newTracker.getExtrasTracker().setUpdateListener(this, true);
        } else {
            onNoTracker();
        }
    }

    @Override
    public void onTrackerUpdate() {
        IntentTracker tracker = getIntentEditor().getIntentTracker();
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        ftb.appendRaw("[Tracking intent]\naction " + (tracker.actionRead() ? "" : "NOT ") + "read");
        ftb.appendText("Read extras:");
        for (final ReadBundleEntryInfo entryInfo : tracker.getExtrasTracker().getReadEntries()) {
            ftb.appendClickable(entryInfo.name, new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    String message = entryInfo.methodName;
                    if (entryInfo.stackTrace != null) {
                        message += "\n\n";
                        for (StackTraceElement element : entryInfo.stackTrace) {
                            message += element.getClassName() + " " + element.getMethodName() + " " + element.getLineNumber() + "\n";
                        }
                    }
                    new AlertDialog.Builder(getActivity())
                            .setTitle(entryInfo.name)
                            .setMessage(message)
                            .show();
                }
            });
        }
        mIntentTrackerSummary.setText(ftb.getText());
        mIntentTrackerSummary.setVisibility(View.VISIBLE);
    }

    private void onNoTracker() {
        mIntentTrackerSummary.setVisibility(View.GONE);
    }
}
