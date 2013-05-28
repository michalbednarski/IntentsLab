package com.example.testapp1.browser;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.View;
import android.widget.*;
import com.example.testapp1.R;

/**
 *
 */
class EditFilterDialog implements DialogInterface.OnClickListener {
    private BrowseComponentsActivity mActivity;
    private View mView;
    private static final boolean DEVELOPMENT_PERMISSIONS_SUPPORTED =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    private static final int PERMISSIONS_MASK =
            DEVELOPMENT_PERMISSIONS_SUPPORTED ?
                    ComponentsFilter.PROTECTION_ANY :
                    (ComponentsFilter.PROTECTION_ANY ^ ComponentsFilter.PROTECTION_DEVELOPMENT);

    EditFilterDialog(BrowseComponentsActivity activity) {
        mActivity = activity;
        mView = activity.getLayoutInflater().inflate(R.layout.components_filter, null);

        // Permission/Protection filter
        if (!DEVELOPMENT_PERMISSIONS_SUPPORTED) {
            mView.findViewById(R.id.permission_filter_development).setEnabled(false);
        }
        {
            Spinner spinner = (Spinner) mView.findViewById(R.id.permission_filter_spinner);
            spinner.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item,
                    new String[]{
                            activity.getString(R.string.permission_filter_show_all), // 0
                            activity.getString(R.string.permission_filter_show_exported), // 1
                            activity.getString(R.string.permission_filter_show_obtainable), // 2
                            activity.getString(R.string.permission_filter_world_accessible), // 3
                            activity.getString(R.string.filter_custom) // 4
                    }
            ));
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mView.findViewById(R.id.permission_filter_details).setVisibility(
                            position == 4 ?
                                    View.VISIBLE :
                                    View.GONE);
                    switch (position) {
                        case 0:
                            updateProtectionFilter(ComponentsFilter.PROTECTION_ANY, false, false);
                            break;
                        case 1:
                            updateProtectionFilter(ComponentsFilter.PROTECTION_ANY_EXPORTED, false, false);
                            break;
                        case 2:
                            updateProtectionFilter(ComponentsFilter.PROTECTION_ANY_OBTAINABLE, false, false);
                            break;
                        case 3:
                            updateProtectionFilter(ComponentsFilter.PROTECTION_WORLD_ACCESSIBLE, false, false);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // Content provider options
        ((CheckBox) mView.findViewById(R.id.content_providers)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mView.findViewById(R.id.content_provider_permission_type).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // Metadata expanding
        ((CheckBox) mView.findViewById(R.id.metadata)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((TextView) mView.findViewById(R.id.metadata_substring)).setText("");
                mView.findViewById(R.id.metadata_details).setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
    }

    void showDialog() {
        updateCheckboxesFromFilter();
        new AlertDialog.Builder(mActivity)
                .setView(mView)
                .setPositiveButton(R.string.set_custom_filter, this)
                .show();
    }

    private void setBoxChecked(int id, boolean checked) {
        ((CompoundButton) mView.findViewById(id)).setChecked(checked);
    }

    private boolean isBoxChecked(int id) {
        return ((CompoundButton) mView.findViewById(id)).isChecked();
    }

    private void updateProtectionFilter(int protection, boolean mayModifySpinner, boolean checkWritePermission) {

        // Fix values to supported by platform version and component type
        protection &= PERMISSIONS_MASK;
        checkWritePermission = checkWritePermission && isBoxChecked(R.id.content_providers);

        // Protection spinner (quick setting)
        if (mayModifySpinner) {
            Spinner spinner = (Spinner) mView.findViewById(R.id.permission_filter_spinner);
            if (protection == (ComponentsFilter.PROTECTION_ANY & PERMISSIONS_MASK)) {
                spinner.setSelection(0);
                checkWritePermission = false;
            } else if (protection == (ComponentsFilter.PROTECTION_ANY_EXPORTED & PERMISSIONS_MASK)) {
                spinner.setSelection(1);
                checkWritePermission = false;
            } else if (protection == ComponentsFilter.PROTECTION_ANY_OBTAINABLE && !checkWritePermission) {
                spinner.setSelection(2);
            } else if (protection == ComponentsFilter.PROTECTION_WORLD_ACCESSIBLE && !checkWritePermission) {
                spinner.setSelection(3);
            } else {
                spinner.setSelection(4);
            }
        }

        // Protection checkboxes
        setBoxChecked(R.id.permission_filter_world_accessible, (protection & ComponentsFilter.PROTECTION_WORLD_ACCESSIBLE) != 0);
        setBoxChecked(R.id.permission_filter_normal, (protection & ComponentsFilter.PROTECTION_NORMAL) != 0);
        setBoxChecked(R.id.permission_filter_dangerous, (protection & ComponentsFilter.PROTECTION_DANGEROUS) != 0);
        setBoxChecked(R.id.permission_filter_signature, (protection & ComponentsFilter.PROTECTION_SIGNATURE) != 0);
        setBoxChecked(R.id.permission_filter_system, (protection & ComponentsFilter.PROTECTION_SYSTEM) != 0);
        setBoxChecked(R.id.permission_filter_development, (protection & ComponentsFilter.PROTECTION_DEVELOPMENT) != 0);
        setBoxChecked(R.id.permission_filter_unexported, (protection & ComponentsFilter.PROTECTION_UNEXPORTED) != 0);
        setBoxChecked(R.id.permission_filter_unknown, (protection & ComponentsFilter.PROTECTION_UNKNOWN) != 0);

        // For content providers: read/write radio buttons
        setBoxChecked(checkWritePermission ? R.id.write_permission : R.id.read_permission, true);
    }

    private void updateCheckboxesFromFilter() {
        ComponentsFilter filter = mActivity.filter;
        setBoxChecked(R.id.system_apps, (filter.appType & ComponentsFilter.APP_TYPE_SYSTEM) != 0);
        setBoxChecked(R.id.user_apps, (filter.appType & ComponentsFilter.APP_TYPE_USER) != 0);

        setBoxChecked(R.id.activities, (filter.type & ComponentsFilter.TYPE_ACTIVITY) != 0);
        setBoxChecked(R.id.receivers, (filter.type & ComponentsFilter.TYPE_RECEIVER) != 0);
        setBoxChecked(R.id.services, (filter.type & ComponentsFilter.TYPE_SERVICE) != 0);
        setBoxChecked(R.id.content_providers, (filter.type & ComponentsFilter.TYPE_CONTENT_PROVIDER) != 0);

        updateProtectionFilter(filter.protection, true, filter.testWritePermissionForProviders);

        setBoxChecked(R.id.metadata, filter.requireMetaData);
        if (filter.requireMetaData) {
            ((TextView) mView.findViewById(R.id.metadata_substring)).setText(filter.requireMetaDataSubstring);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        ComponentsFilter filter = new ComponentsFilter();
        filter.appType =
                (isBoxChecked(R.id.system_apps) ? ComponentsFilter.APP_TYPE_SYSTEM : 0) |
                        (isBoxChecked(R.id.user_apps) ? ComponentsFilter.APP_TYPE_USER : 0);

        filter.type =
                (isBoxChecked(R.id.activities) ? ComponentsFilter.TYPE_ACTIVITY : 0) |
                (isBoxChecked(R.id.receivers) ? ComponentsFilter.TYPE_RECEIVER : 0) |
                (isBoxChecked(R.id.services) ? ComponentsFilter.TYPE_SERVICE : 0) |
                (isBoxChecked(R.id.content_providers) ? ComponentsFilter.TYPE_CONTENT_PROVIDER : 0);

        filter.protection =
                (isBoxChecked(R.id.permission_filter_world_accessible) ? ComponentsFilter.PROTECTION_WORLD_ACCESSIBLE : 0) |
                (isBoxChecked(R.id.permission_filter_normal) ? ComponentsFilter.PROTECTION_NORMAL : 0) |
                (isBoxChecked(R.id.permission_filter_dangerous) ? ComponentsFilter.PROTECTION_DANGEROUS : 0) |
                (isBoxChecked(R.id.permission_filter_signature) ? ComponentsFilter.PROTECTION_SIGNATURE : 0) |
                (isBoxChecked(R.id.permission_filter_system) ? ComponentsFilter.PROTECTION_SYSTEM : 0) |
                (isBoxChecked(R.id.permission_filter_development) ? ComponentsFilter.PROTECTION_DEVELOPMENT : 0) |
                (isBoxChecked(R.id.permission_filter_unexported) ? ComponentsFilter.PROTECTION_UNEXPORTED : 0) |
                (isBoxChecked(R.id.permission_filter_unknown) ? ComponentsFilter.PROTECTION_UNKNOWN : 0);

        filter.requireMetaData = isBoxChecked(R.id.metadata);
        filter.requireMetaDataSubstring =
                filter.requireMetaData ?
                        ((TextView) mView.findViewById(R.id.metadata_substring)).getText().toString() :
                        null;

        filter.testWritePermissionForProviders = isBoxChecked(R.id.write_permission);

        mActivity.setCustomFilter(filter);
    }
}
