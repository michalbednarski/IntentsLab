package com.github.michalbednarski.intentslab.appinfo;

import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.github.michalbednarski.intentslab.editor.IntentEditorConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * See {@link MyPackageInfo}
 */
class MyPackageInfoImpl implements MyPackageInfo {

    private static final String TAG = "MyPackageInfoImpl";
    private static final MyComponentInfo[] EMPTY_COMPONENTS_ARRAY = new MyComponentInfo[0];

    String mPackageName;

    boolean mIntentFiltersLoaded;

    PackageInfo mSystemPackageInfo;
    Map<String, MyComponentInfoImpl> mActivitiesMap, mReceiversMap, mServicesMap, mProvidersMap;
    MyComponentInfo[] mActivities, mReceivers, mServices, mProviders;

    List<MyPermissionInfo> mDefinedPermissions;

    /**
     * Scan components array, build our wrappers
     * and fill in provided outComponentsMap and outComponentsList
     * @return true if any components were found
     */
    private boolean convertComponentsToMy(int type, ComponentInfo[] systemComponentInfos, Map<String, MyComponentInfoImpl> outComponentsMap, List<MyComponentInfo> outComponentsList) {

        if (systemComponentInfos != null) {
            for (ComponentInfo component : systemComponentInfos) {
                // TODO: handle duplicate components in manifest
                MyComponentInfoImpl myComponentInfo = new MyComponentInfoImpl(type, component, this);
                outComponentsMap.put(component.name, myComponentInfo);
                outComponentsList.add(myComponentInfo);
            }
        }

        return !outComponentsList.isEmpty();
    }


    @SuppressWarnings("unchecked")
    MyPackageInfoImpl(PackageInfo packageInfo) {
        mPackageName = packageInfo.packageName;
        mSystemPackageInfo = packageInfo;

        // Activities
        ArrayMap<String, MyComponentInfoImpl> componentsMap = new ArrayMap<>();
        ArrayList<MyComponentInfo> componentsList = new ArrayList<>();
        if (convertComponentsToMy(IntentEditorConstants.ACTIVITY, packageInfo.activities, componentsMap, componentsList)) {
            mActivities = componentsList.toArray(new MyComponentInfo[componentsList.size()]);
            mActivitiesMap = componentsMap;
        } else {
            mActivities = EMPTY_COMPONENTS_ARRAY;
            mActivitiesMap = Collections.EMPTY_MAP;
        }

        // Receivers
        componentsMap = new ArrayMap<>();
        componentsList = new ArrayList<>();
        if (convertComponentsToMy(IntentEditorConstants.BROADCAST, packageInfo.receivers, componentsMap, componentsList)) {
            mReceivers = componentsList.toArray(new MyComponentInfo[componentsList.size()]);
            mReceiversMap = componentsMap;
        } else {
            mReceivers = EMPTY_COMPONENTS_ARRAY;
            mReceiversMap = Collections.EMPTY_MAP;
        }

        // Services
        componentsMap = new ArrayMap<>();
        componentsList = new ArrayList<>();
        if (convertComponentsToMy(IntentEditorConstants.SERVICE, packageInfo.services, componentsMap, componentsList)) {
            mServices = componentsList.toArray(new MyComponentInfo[componentsList.size()]);
            mServicesMap = componentsMap;
        } else {
            mServices = EMPTY_COMPONENTS_ARRAY;
            mServicesMap = Collections.EMPTY_MAP;
        }

        // Providers
        componentsMap = new ArrayMap<>();
        componentsList = new ArrayList<>();
        if (convertComponentsToMy(IntentEditorConstants.PROVIDER, packageInfo.providers, componentsMap, componentsList)) {
            mProviders = componentsList.toArray(new MyComponentInfo[componentsList.size()]);
            mProvidersMap = componentsMap;
        } else {
            mProviders = EMPTY_COMPONENTS_ARRAY;
            mProvidersMap = Collections.EMPTY_MAP;
        }
    }

    @Override
    public String getPackageName() {
        return mPackageName;
    }

    @Override
    public CharSequence loadLabel(PackageManager pm) {
        return mSystemPackageInfo.applicationInfo.loadLabel(pm);
    }

    @Override
    public Drawable loadIcon(PackageManager pm) {
        return mSystemPackageInfo.applicationInfo.loadIcon(pm);
    }

    @Override
    public MyComponentInfo[] getActivities() {
        return mActivities;
    }

    @Override
    public MyComponentInfo getActivityByName(String name) {
        return mActivitiesMap.get(name);
    }

    @Override
    public MyComponentInfo[] getReceivers() {
        return mReceivers;
    }

    @Override
    public MyComponentInfo getReceiverByName(String name) {
        return mReceiversMap.get(name);
    }

    @Override
    public MyComponentInfo[] getServices() {
        return mServices;
    }

    @Override
    public MyComponentInfo getServiceByName(String name) {
        return mServicesMap.get(name);
    }

    @Override
    public MyComponentInfo[] getProviders() {
        return mProviders;
    }

    @Override
    public MyComponentInfo getProviderByName(String name) {
        return mProvidersMap.get(name);
    }

    @Override
    public Collection<MyPermissionInfo> getDefinedPermissions() {
        return mDefinedPermissions;
    }

    @Override
    public UsedAppPermissionDetails getRequestedAndGrantedPermissions(PackageManager pm) {

        HashSet<String> testedPermissions = new HashSet<>();
        ArrayList<String> grantedPermissions = new ArrayList<>();
        ArrayList<String> deniedPermissions = new ArrayList<>();

        if (mSystemPackageInfo.requestedPermissions != null) {
            for (String permission : mSystemPackageInfo.requestedPermissions) {
                if (pm.checkPermission(permission, mPackageName) == PackageManager.PERMISSION_GRANTED) {
                    grantedPermissions.add(permission);
                } else {
                    deniedPermissions.add(permission);
                }
                testedPermissions.add(permission);
            }
        }

        // Scan shared user packages for permissions (implicitly granted)
        ArrayList<String> implicitlyGrantedPermissions = new ArrayList<String>();
        for (String sharedUidPackageName : pm.getPackagesForUid(mSystemPackageInfo.applicationInfo.uid)) {
            if (mPackageName.equals(sharedUidPackageName)) {
                continue;
            }
            final PackageInfo sharedUidPackageInfo;
            try {
                sharedUidPackageInfo = pm.getPackageInfo(sharedUidPackageName, PackageManager.GET_PERMISSIONS);
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Shared uid package not found", e);
                continue;
            }
            if (sharedUidPackageInfo.requestedPermissions == null) {
                continue;
            }
            for (String permission : sharedUidPackageInfo.requestedPermissions) {
                if (!testedPermissions.contains(permission)) {
                    testedPermissions.add(permission);
                    if (pm.checkPermission(permission, mPackageName) == PackageManager.PERMISSION_GRANTED) {
                        implicitlyGrantedPermissions.add(permission);
                    }
                }
            }
        }

        // Wrap result
        UsedAppPermissionDetails result = new UsedAppPermissionDetails();
        result.grantedPermissions = grantedPermissions.toArray(new String[grantedPermissions.size()]);
        result.implicitlyGrantedPermissions = implicitlyGrantedPermissions.toArray(new String[implicitlyGrantedPermissions.size()]);
        result.deniedPermissions = deniedPermissions.toArray(new String[deniedPermissions.size()]);
        return result;
    }

    @Override
    public boolean isApplicationEnabled() {
        return mSystemPackageInfo.applicationInfo.enabled;
    }


    @Override
    public boolean isSystemApplication() {
        return (mSystemPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }

    @Override
    public Bundle getMetaData() {
        return mSystemPackageInfo.applicationInfo.metaData;
    }
}
