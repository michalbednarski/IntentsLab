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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * See {@link MyPackageInfo}
 */
class MyPackageInfoImpl implements MyPackageInfo {

    private static final String TAG = "MyPackageInfoImpl";
    String mPackageName;

    boolean mIntentFiltersLoaded;

    PackageInfo mSystemPackageInfo;
    Map<String, MyComponentInfoImpl> mActivities, mReceivers, mServices, mProviders;

    List<MyPermissionInfo> mDefinedPermissions;

    private Map<String, MyComponentInfoImpl> convertComponentsToMy(int type, ComponentInfo[] systemComponentInfos) {
        Map<String, MyComponentInfoImpl> processedComponents = new ArrayMap<>();

        if (systemComponentInfos != null) {
            for (ComponentInfo component : systemComponentInfos) {
                // TODO: handle duplicate components in manifest
                processedComponents.put(component.name, new MyComponentInfoImpl(type, component, this));
            }
        }

        return processedComponents;
    }

    MyPackageInfoImpl(PackageInfo packageInfo) {
        mPackageName = packageInfo.packageName;
        mSystemPackageInfo = packageInfo;
        mActivities = convertComponentsToMy(IntentEditorConstants.ACTIVITY, packageInfo.activities);
        mReceivers = convertComponentsToMy(IntentEditorConstants.BROADCAST, packageInfo.receivers);
        mServices = convertComponentsToMy(IntentEditorConstants.SERVICE, packageInfo.services);
        mProviders = convertComponentsToMy(IntentEditorConstants.PROVIDER, packageInfo.providers);
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
    @SuppressWarnings("unchecked")
    public Collection<MyComponentInfo> getActivities() {
        return ((Collection) mActivities.values());
    }

    @Override
    public MyComponentInfo getActivityByName(String name) {
        return mActivities.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MyComponentInfo> getReceivers() {
        return ((Collection) mReceivers.values());
    }

    @Override
    public MyComponentInfo getReceiverByName(String name) {
        return mReceivers.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MyComponentInfo> getServices() {
        return ((Collection) mServices.values());
    }

    @Override
    public MyComponentInfo getServiceByName(String name) {
        return mServices.get(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<MyComponentInfo> getProviders() {
        return ((Collection) mProviders.values());
    }

    @Override
    public MyComponentInfo getProviderByName(String name) {
        return mProviders.get(name);
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
