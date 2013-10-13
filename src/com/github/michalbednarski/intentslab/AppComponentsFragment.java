package com.github.michalbednarski.intentslab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.*;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.browser.ComponentInfoActivity;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoActivity;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Fragment hosted in AppInfoActivity, displays components and permissions of app
 */
public class AppComponentsFragment extends Fragment implements ExpandableListAdapter, ExpandableListView.OnChildClickListener {
    // Component sections (Identifiers must be unique and refer to header text resources)
    private static final int SECTION_ACTIVITIES = R.string.activities;
    private static final int SECTION_ACTIVITIES_NOT_EXPORTED = R.string.activities_not_exported;
    private static final int SECTION_RECEIVERS = R.string.broadcast_receivers;
    private static final int SECTION_SERVICES = R.string.services;
    private static final int SECTION_PROVIDERS = R.string.content_providers;

    // Permission sections
    private static final int SECTION_DEFINED_PERMISSIONS = R.string.defined_permissions;
    private static final int SECTION_GRANTED_PERMISSIONS = R.string.granted_permissions;
    private static final int SECTION_DENIED_PERMISSIONS = R.string.denied_permissions;
    private static final int SECTION_IMPLICITLY_GRANTED_PERMISSIONS = R.string.implicitly_granted_permissions;

    // List of sections displayed, as defined by SECTION_* constants
    private int[] mPresentSections;

    // Arrays of components
    private ComponentInfo[] mActivities;
    private ComponentInfo[] mActivitiesNotExported;
    private ComponentInfo[] mReceivers;
    private ComponentInfo[] mServices;
    private ProviderInfo[] mProviders;

    // Arrays of permissions
    private String[] mDefinedPermissions;
    private String[] mGrantedPermissions;
    private String[] mDeniedPermissions;
    private String[] mImplicitlyGrantedPermissions;

    // Name of package we're scanning
    private String mPackageName;

    private LayoutInflater mInflater;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mInflater = activity.getLayoutInflater();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackageName = ((AppInfoHost) getActivity()).getViewedPackageName();

        final PackageManager packageManager = getActivity().getPackageManager();

        ArrayList<Integer> presentSections = new ArrayList<Integer>();



        try {
            final PackageInfo packageInfo = packageManager.getPackageInfo(
                    mPackageName,
                    PackageManager.GET_ACTIVITIES |
                    PackageManager.GET_RECEIVERS |
                    PackageManager.GET_SERVICES |
                    PackageManager.GET_PROVIDERS |
                    PackageManager.GET_PERMISSIONS
            );

            // Scan activities and group them as exported and not exported
            {
                ArrayList<ActivityInfo> exportedActivities = new ArrayList<ActivityInfo>();
                ArrayList<ActivityInfo> notExportedActivities = new ArrayList<ActivityInfo>();

                if (packageInfo.activities != null && packageInfo.activities.length != 0) {
                    for (ActivityInfo activity : packageInfo.activities) {
                        (activity.exported ? exportedActivities : notExportedActivities).add(activity);
                    }
                }

                if (exportedActivities.size() != 0) {
                    mActivities = exportedActivities.toArray(new ComponentInfo[exportedActivities.size()]);
                    presentSections.add(SECTION_ACTIVITIES);
                }
                if (notExportedActivities.size() != 0) {
                    mActivitiesNotExported = notExportedActivities.toArray(new ComponentInfo[notExportedActivities.size()]);
                    presentSections.add(SECTION_ACTIVITIES_NOT_EXPORTED);
                }
            }

            // Check receivers, services and providers
            if (packageInfo.receivers != null) {
                mReceivers = packageInfo.receivers;
                presentSections.add(SECTION_RECEIVERS);
            }
            if (packageInfo.services != null) {
                mServices = packageInfo.services;
                presentSections.add(SECTION_SERVICES);
            }
            if (packageInfo.providers != null) {
                mProviders = packageInfo.providers;
                presentSections.add(SECTION_PROVIDERS);
            }

            // Scan defined permissions
            if (packageInfo.permissions != null) {
                mDefinedPermissions = new String[packageInfo.permissions.length];
                for (int i = 0; i < packageInfo.permissions.length; i++) {
                    mDefinedPermissions[i] = packageInfo.permissions[i].name;
                }
                presentSections.add(SECTION_DEFINED_PERMISSIONS);
            }

            // Scan requested permissions (granted and denied)
            HashSet<String> testedPermissions = new HashSet<String>();
            if (packageInfo.requestedPermissions != null) {
                ArrayList<String> grantedPermissions = new ArrayList<String>();
                ArrayList<String> deniedPermissions = new ArrayList<String>();
                for (String permission : packageInfo.requestedPermissions) {
                    if (packageManager.checkPermission(permission, mPackageName) == PackageManager.PERMISSION_GRANTED) {
                        grantedPermissions.add(permission);
                    } else {
                        deniedPermissions.add(permission);
                    }
                    testedPermissions.add(permission);
                }
                if (grantedPermissions.size() != 0) {
                    mGrantedPermissions = grantedPermissions.toArray(new String[grantedPermissions.size()]);
                    presentSections.add(SECTION_GRANTED_PERMISSIONS);
                }
                if (deniedPermissions.size() != 0) {
                    mDeniedPermissions = deniedPermissions.toArray(new String[deniedPermissions.size()]);
                    presentSections.add(SECTION_DENIED_PERMISSIONS);
                }

            }

            // Scan shared user packages for permissions (implicitly granted)
            {
                ArrayList<String> implicitlyGrantedPermissions = new ArrayList<String>();
                for (String sharedUidPackageName : packageManager.getPackagesForUid(packageInfo.applicationInfo.uid)) {
                    if (mPackageName.equals(sharedUidPackageName)) {
                        continue;
                    }
                    final PackageInfo sharedUidPackageInfo = packageManager.getPackageInfo(sharedUidPackageName, PackageManager.GET_PERMISSIONS);
                    if (sharedUidPackageInfo.requestedPermissions == null) {
                        continue;
                    }
                    for (String permission : sharedUidPackageInfo.requestedPermissions) {
                        if (!testedPermissions.contains(permission)) {
                            testedPermissions.add(permission);
                            if (packageManager.checkPermission(permission, mPackageName) == PackageManager.PERMISSION_GRANTED) {
                                implicitlyGrantedPermissions.add(permission);
                            }
                        }
                    }
                }
                if (implicitlyGrantedPermissions.size() != 0) {
                    mImplicitlyGrantedPermissions = implicitlyGrantedPermissions.toArray(new String[implicitlyGrantedPermissions.size()]);
                    presentSections.add(SECTION_IMPLICITLY_GRANTED_PERMISSIONS);
                }
            }

            // Save present sections list as array
            mPresentSections = new int[presentSections.size()];
            for (int i = 0; i < presentSections.size(); i++) {
                mPresentSections[i] = presentSections.get(i);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ExpandableListView expandableListView = new ExpandableListView(getActivity());
        expandableListView.setAdapter(this);
        expandableListView.setOnChildClickListener(this);
        return expandableListView;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // This is immutable ExpandableListAdapter, do nothing
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // This is immutable ExpandableListAdapter, do nothing
    }

    @Override
    public int getGroupCount() {
        return mPresentSections.length;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        switch (mPresentSections[groupPosition]) {
            case SECTION_ACTIVITIES:
                return mActivities.length;
            case SECTION_ACTIVITIES_NOT_EXPORTED:
                return mActivitiesNotExported.length;
            case SECTION_RECEIVERS:
                return mReceivers.length;
            case SECTION_SERVICES:
                return mServices.length;
            case SECTION_PROVIDERS:
                return mProviders.length;
            case SECTION_DEFINED_PERMISSIONS:
                return mDefinedPermissions.length;
            case SECTION_GRANTED_PERMISSIONS:
                return mGrantedPermissions.length;
            case SECTION_DENIED_PERMISSIONS:
                return mDeniedPermissions.length;
            case SECTION_IMPLICITLY_GRANTED_PERMISSIONS:
                return  mImplicitlyGrantedPermissions.length;
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return mPresentSections[groupPosition];
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
        }
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(getActivity().getString(mPresentSections[groupPosition]));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        String text;
        switch (mPresentSections[groupPosition]) {
            case SECTION_ACTIVITIES:
            case SECTION_ACTIVITIES_NOT_EXPORTED:
            case SECTION_RECEIVERS:
            case SECTION_SERVICES:
            {
                ComponentInfo componentInfo = getComponentAt(groupPosition, childPosition);
                text = new ComponentName(mPackageName, componentInfo.name).getShortClassName();
                break;
            }
            case SECTION_PROVIDERS:
            {
                ProviderInfo providerInfo = mProviders[childPosition];
                text = providerInfo.authority;
                break;
            }
            case SECTION_DEFINED_PERMISSIONS:
            case SECTION_GRANTED_PERMISSIONS:
            case SECTION_DENIED_PERMISSIONS:
            case SECTION_IMPLICITLY_GRANTED_PERMISSIONS:
            {
                text = getPermissionAt(groupPosition, childPosition);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
        if (convertView == null) {
            convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }
        ((TextView) convertView).setText(text);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {}

    @Override
    public void onGroupCollapsed(int groupPosition) {}

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0x8000000000000000L | ((groupId & 0x7FFFFFFF) << 32) | (childId & 0xFFFFFFFF);
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return (groupId & 0x7FFFFFFF) << 32;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        switch (mPresentSections[groupPosition]) {
            case SECTION_ACTIVITIES:
            case SECTION_ACTIVITIES_NOT_EXPORTED:
            case SECTION_RECEIVERS:
            case SECTION_SERVICES: {
                ComponentInfo componentInfo = getComponentAt(groupPosition, childPosition);
                startActivity(
                        new Intent(getActivity(), ComponentInfoActivity.class)
                        .putExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME, mPackageName)
                        .putExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME, componentInfo.name)
                        .putExtra(ComponentInfoActivity.EXTRA_LAUNCHED_FROM_APP_INFO, true)
                );
                return true;
            }
            case SECTION_PROVIDERS:
            {
                ProviderInfo providerInfo = mProviders[childPosition];
                startActivity(
                        new Intent(getActivity(), ProviderInfoActivity.class)
                        .putExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME, mPackageName)
                        .putExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME, providerInfo.name)
                        .putExtra(ComponentInfoActivity.EXTRA_LAUNCHED_FROM_APP_INFO, true)
                );
                return true;
            }
            case SECTION_DEFINED_PERMISSIONS:
            case SECTION_GRANTED_PERMISSIONS:
            case SECTION_DENIED_PERMISSIONS:
            case SECTION_IMPLICITLY_GRANTED_PERMISSIONS:
            {
                startActivity(
                        new Intent(getActivity(), PermissionInfoActivity.class)
                        .putExtra(PermissionInfoActivity.EXTRA_PERMISSION_NAME, getPermissionAt(groupPosition, childPosition))
                );
                return true;
            }
        }
        return false;
    }

    private ComponentInfo getComponentAt(int groupPosition, int childPosition) {
        final int section = mPresentSections[groupPosition];
        return (
                section == SECTION_ACTIVITIES ? mActivities :
                section == SECTION_ACTIVITIES_NOT_EXPORTED ? mActivitiesNotExported :
                section == SECTION_RECEIVERS ? mReceivers :
                section == SECTION_SERVICES ? mServices : null
        )[childPosition];
    }

    private String getPermissionAt(int groupPosition, int childPosition) {
        final int section = mPresentSections[groupPosition];
        return (
                section == SECTION_DEFINED_PERMISSIONS ? mDefinedPermissions :
                section == SECTION_GRANTED_PERMISSIONS ? mGrantedPermissions :
                section == SECTION_DENIED_PERMISSIONS ? mDeniedPermissions :
                section == SECTION_IMPLICITLY_GRANTED_PERMISSIONS ? mImplicitlyGrantedPermissions : null
        )[childPosition];
    }

    public interface AppInfoHost {
        public String getViewedPackageName();
    }
}
