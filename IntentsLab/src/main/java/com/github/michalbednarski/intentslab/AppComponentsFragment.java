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

package com.github.michalbednarski.intentslab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.appinfo.MyComponentInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageManagerImpl;
import com.github.michalbednarski.intentslab.appinfo.MyPermissionInfo;
import com.github.michalbednarski.intentslab.appinfo.UsedAppPermissionDetails;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

import java.util.ArrayList;
import java.util.Collection;

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
    private MyComponentInfo[] mActivities;
    private MyComponentInfo[] mActivitiesNotExported;
    private MyComponentInfo[] mReceivers;
    private MyComponentInfo[] mServices;
    private MyComponentInfo[] mProviders;

    // Arrays of permissions
    private String[] mDefinedPermissions;
    private String[] mGrantedPermissions;
    private String[] mDeniedPermissions;
    private String[] mImplicitlyGrantedPermissions;

    // Name of package we're scanning
    private String mPackageName;

    private LayoutInflater mInflater;

    private DataSetObservable mObservable = new DataSetObservable();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mInflater = activity.getLayoutInflater();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        mPackageName = ((AppInfoHost) getActivity()).getViewedPackageName();

        final PackageManager packageManager = getActivity().getPackageManager();

        MyPackageManagerImpl
                .getInstance(getActivity())
                .getPackageInfo(false, mPackageName)
                .done(new DoneCallback<MyPackageInfo>() {
                    @Override
                    public void onDone(MyPackageInfo result) {
                        ArrayList<Integer> presentSections = new ArrayList<>();

                        // Scan activities and group them as exported and not exported
                        {
                            ArrayList<MyComponentInfo> exportedActivities = new ArrayList<>();
                            ArrayList<MyComponentInfo> notExportedActivities = new ArrayList<>();

                            for (MyComponentInfo activity : result.getActivities()) {
                                (activity.isExported() ? exportedActivities : notExportedActivities).add(activity);
                            }

                            if (exportedActivities.size() != 0) {
                                mActivities = exportedActivities.toArray(new MyComponentInfo[exportedActivities.size()]);
                                presentSections.add(SECTION_ACTIVITIES);
                            }
                            if (notExportedActivities.size() != 0) {
                                mActivitiesNotExported = notExportedActivities.toArray(new MyComponentInfo[notExportedActivities.size()]);
                                presentSections.add(SECTION_ACTIVITIES_NOT_EXPORTED);
                            }
                        }

                        // Check receivers, services and providers
                        MyComponentInfo[] componentsCollection = result.getReceivers();
                        if (componentsCollection.length != 0) {
                            mReceivers = componentsCollection;
                            presentSections.add(SECTION_RECEIVERS);
                        }
                        componentsCollection = result.getServices();
                        if (componentsCollection.length != 0) {
                            mServices = componentsCollection;
                            presentSections.add(SECTION_SERVICES);
                        }
                        componentsCollection = result.getProviders();
                        if (componentsCollection.length != 0) {
                            mProviders = componentsCollection;
                            presentSections.add(SECTION_PROVIDERS);
                        }

                        // Scan defined permissions
                        ArrayList<String> permissionList = new ArrayList<>();
                        for (MyPermissionInfo permission : result.getDefinedPermissions()) {
                            permissionList.add(permission.getName());
                        }
                        if (!permissionList.isEmpty()) {
                            mDefinedPermissions = permissionList.toArray(new String[permissionList.size()]);
                            presentSections.add(SECTION_DEFINED_PERMISSIONS);
                        }

                        // Scan requested permissions (granted and denied)
                        UsedAppPermissionDetails requestedAndGrantedPermissions = result.getRequestedAndGrantedPermissions(packageManager);

                        if (requestedAndGrantedPermissions.grantedPermissions.length != 0) {
                            mGrantedPermissions = requestedAndGrantedPermissions.grantedPermissions;
                            presentSections.add(SECTION_GRANTED_PERMISSIONS);
                        }
                        if (requestedAndGrantedPermissions.deniedPermissions.length != 0) {
                            mDeniedPermissions = requestedAndGrantedPermissions.deniedPermissions;
                            presentSections.add(SECTION_DENIED_PERMISSIONS);
                        }
                        if (requestedAndGrantedPermissions.implicitlyGrantedPermissions.length != 0) {
                            mImplicitlyGrantedPermissions = requestedAndGrantedPermissions.implicitlyGrantedPermissions;
                            presentSections.add(SECTION_IMPLICITLY_GRANTED_PERMISSIONS);
                        }

                        // Save present sections list as array
                        mPresentSections = new int[presentSections.size()];
                        for (int i = 0; i < presentSections.size(); i++) {
                            mPresentSections[i] = presentSections.get(i);
                        }

                        mObservable.notifyChanged();
                    }
                })
                .fail(new FailCallback<Void>() {
                    @Override
                    public void onFail(Void result) {
                        FragmentActivity activity = getActivity();
                        if (activity != null) {
                            Toast.makeText(activity, R.string.component_not_found, Toast.LENGTH_SHORT).show();
                            activity.finish();
                        }
                    }
                });
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        FrameLayout frameLayout = new FrameLayout(activity);
        FrameLayout.LayoutParams stretch = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        FrameLayout.LayoutParams center = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
        );

        final ExpandableListView expandableListView = new ExpandableListView(activity);
        expandableListView.setAdapter(this);
        expandableListView.setOnChildClickListener(this);
        expandableListView.setLayoutParams(stretch);
        frameLayout.addView(expandableListView);

        final TextView emptyView = new TextView(activity);
        emptyView.setText(activity.getString(R.string.no_components));
        emptyView.setLayoutParams(center);
        frameLayout.addView(emptyView);

        expandableListView.setEmptyView(emptyView);

        return frameLayout;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mObservable.registerObserver(observer);
    }

    @Override
    public int getGroupCount() {
        return mPresentSections == null ? 0 : mPresentSections.length;
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
                MyComponentInfo componentInfo = getComponentAt(groupPosition, childPosition);
                text = new ComponentName(mPackageName, componentInfo.getName()).getShortClassName();
                break;
            }
            case SECTION_PROVIDERS:
            {
                MyComponentInfo providerInfo = mProviders[childPosition];
                text = providerInfo.getProviderInfo().authority;
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
        return getGroupCount() == 0;
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
                MyComponentInfo componentInfo = getComponentAt(groupPosition, childPosition);
                startActivity(
                        new Intent(getActivity(), SingleFragmentActivity.class)
                        .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName())
                        .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, mPackageName)
                        .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, componentInfo.getName())
                        .putExtra(ComponentInfoFragment.ARG_COMPONENT_TYPE, componentInfo.getType())
                        .putExtra(ComponentInfoFragment.ARG_LAUNCHED_FROM_APP_INFO, true)
                );
                return true;
            }
            case SECTION_PROVIDERS:
            {
                MyComponentInfo providerInfo = mProviders[childPosition];
                startActivity(
                        new Intent(getActivity(), SingleFragmentActivity.class)
                        .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ProviderInfoFragment.class.getName())
                        .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, mPackageName)
                        .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, providerInfo.getName())
                        .putExtra(ComponentInfoFragment.ARG_LAUNCHED_FROM_APP_INFO, true)
                );
                return true;
            }
            case SECTION_DEFINED_PERMISSIONS:
            case SECTION_GRANTED_PERMISSIONS:
            case SECTION_DENIED_PERMISSIONS:
            case SECTION_IMPLICITLY_GRANTED_PERMISSIONS:
            {
                startActivity(
                        new Intent(getActivity(), SingleFragmentActivity.class)
                        .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, PermissionInfoFragment.class.getName())
                        .putExtra(PermissionInfoFragment.ARG_PERMISSION_NAME, getPermissionAt(groupPosition, childPosition))
                );
                return true;
            }
        }
        return false;
    }

    private MyComponentInfo getComponentAt(int groupPosition, int childPosition) {
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
