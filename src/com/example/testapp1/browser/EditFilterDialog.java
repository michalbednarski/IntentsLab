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
    private BrowseAppsActivity mActivity;
    private View mView;
    private static final boolean DEVELOPMENT_PERMISSIONS_SUPPORTED =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    private static final int PERMISSIONS_MASK =
            DEVELOPMENT_PERMISSIONS_SUPPORTED ?
                    AppsBrowserFilter.PROTECTION_ANY :
                    (AppsBrowserFilter.PROTECTION_ANY ^ AppsBrowserFilter.PROTECTION_DEVELOPMENT);

    EditFilterDialog(BrowseAppsActivity activity) {
        mActivity = activity;
        mView = activity.getLayoutInflater().inflate(R.layout.apps_filter, null);

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
                            updateProtectionFilter(AppsBrowserFilter.PROTECTION_ANY, false);
                            break;
                        case 1:
                            updateProtectionFilter(AppsBrowserFilter.PROTECTION_ANY_EXPORTED, false);
                            break;
                        case 2:
                            updateProtectionFilter(AppsBrowserFilter.PROTECTION_ANY_OBTAINABLE, false);
                            break;
                        case 3:
                            updateProtectionFilter(AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE, false);
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

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

    private void setCheckBoxChecked(int id, boolean checked) {
        ((CheckBox) mView.findViewById(id)).setChecked(checked);
    }

    private boolean isCheckBoxChecked(int id) {
        return ((CheckBox) mView.findViewById(id)).isChecked();
    }

    private void updateProtectionFilter(int protection, boolean mayModifySpinner) {
        protection &= PERMISSIONS_MASK;

        if (mayModifySpinner) {
            Spinner spinner = (Spinner) mView.findViewById(R.id.permission_filter_spinner);
            if (protection == (AppsBrowserFilter.PROTECTION_ANY & PERMISSIONS_MASK)) {
                spinner.setSelection(0);
            } else if (protection == (AppsBrowserFilter.PROTECTION_ANY_EXPORTED & PERMISSIONS_MASK)) {
                spinner.setSelection(1);
            } else if (protection == AppsBrowserFilter.PROTECTION_ANY_OBTAINABLE) {
                spinner.setSelection(2);
            } else if (protection == AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE) {
                spinner.setSelection(3);
            } else {
                spinner.setSelection(4);
            }
        }

        setCheckBoxChecked(R.id.permission_filter_world_accessible, (protection & AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE) != 0);
        setCheckBoxChecked(R.id.permission_filter_normal, (protection & AppsBrowserFilter.PROTECTION_NORMAL) != 0);
        setCheckBoxChecked(R.id.permission_filter_dangerous, (protection & AppsBrowserFilter.PROTECTION_DANGEROUS) != 0);
        setCheckBoxChecked(R.id.permission_filter_signature, (protection & AppsBrowserFilter.PROTECTION_SIGNATURE) != 0);
        setCheckBoxChecked(R.id.permission_filter_system, (protection & AppsBrowserFilter.PROTECTION_SYSTEM) != 0);
        setCheckBoxChecked(R.id.permission_filter_development, (protection & AppsBrowserFilter.PROTECTION_DEVELOPMENT) != 0);
        setCheckBoxChecked(R.id.permission_filter_unexported, (protection & AppsBrowserFilter.PROTECTION_UNEXPORTED) != 0);
        setCheckBoxChecked(R.id.permission_filter_unknown, (protection & AppsBrowserFilter.PROTECTION_UNKNOWN) != 0);
    }

    private void updateCheckboxesFromFilter() {
        AppsBrowserFilter filter = mActivity.filter;
        setCheckBoxChecked(R.id.system_apps, (filter.appType & AppsBrowserFilter.APP_TYPE_SYSTEM) != 0);
        setCheckBoxChecked(R.id.user_apps, (filter.appType & AppsBrowserFilter.APP_TYPE_USER) != 0);

        setCheckBoxChecked(R.id.activities, (filter.type & AppsBrowserFilter.TYPE_ACTIVITY) != 0);
        setCheckBoxChecked(R.id.receivers, (filter.type & AppsBrowserFilter.TYPE_RECEIVER) != 0);
        setCheckBoxChecked(R.id.services, (filter.type & AppsBrowserFilter.TYPE_SERVICE) != 0);
        setCheckBoxChecked(R.id.content_providers, (filter.type & AppsBrowserFilter.TYPE_CONTENT_PROVIDER) != 0);

        updateProtectionFilter(filter.protection, true);

        setCheckBoxChecked(R.id.metadata, filter.requireMetaData);
        if (filter.requireMetaData) {
            ((TextView) mView.findViewById(R.id.metadata_substring)).setText(filter.requireMetaDataSubstring);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        AppsBrowserFilter filter = new AppsBrowserFilter();
        filter.appType =
                (isCheckBoxChecked(R.id.system_apps) ? AppsBrowserFilter.APP_TYPE_SYSTEM : 0) |
                        (isCheckBoxChecked(R.id.user_apps) ? AppsBrowserFilter.APP_TYPE_USER : 0);

        filter.type =
                (isCheckBoxChecked(R.id.activities) ? AppsBrowserFilter.TYPE_ACTIVITY : 0) |
                (isCheckBoxChecked(R.id.receivers) ? AppsBrowserFilter.TYPE_RECEIVER : 0) |
                (isCheckBoxChecked(R.id.services) ? AppsBrowserFilter.TYPE_SERVICE : 0) |
                (isCheckBoxChecked(R.id.content_providers) ? AppsBrowserFilter.TYPE_CONTENT_PROVIDER : 0);

        filter.protection =
                (isCheckBoxChecked(R.id.permission_filter_world_accessible) ? AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE : 0) |
                (isCheckBoxChecked(R.id.permission_filter_normal) ? AppsBrowserFilter.PROTECTION_NORMAL : 0) |
                (isCheckBoxChecked(R.id.permission_filter_dangerous) ? AppsBrowserFilter.PROTECTION_DANGEROUS : 0) |
                (isCheckBoxChecked(R.id.permission_filter_signature) ? AppsBrowserFilter.PROTECTION_SIGNATURE : 0) |
                (isCheckBoxChecked(R.id.permission_filter_system) ? AppsBrowserFilter.PROTECTION_SYSTEM : 0) |
                (isCheckBoxChecked(R.id.permission_filter_development) ? AppsBrowserFilter.PROTECTION_DEVELOPMENT : 0) |
                (isCheckBoxChecked(R.id.permission_filter_unexported) ? AppsBrowserFilter.PROTECTION_UNEXPORTED : 0) |
                (isCheckBoxChecked(R.id.permission_filter_unknown) ? AppsBrowserFilter.PROTECTION_UNKNOWN : 0);

        filter.requireMetaData = isCheckBoxChecked(R.id.metadata);
        filter.requireMetaDataSubstring =
                filter.requireMetaData ?
                        ((TextView) mView.findViewById(R.id.metadata_substring)).getText().toString() :
                        null;
        mActivity.setCustomFilter(filter);
    }
}
