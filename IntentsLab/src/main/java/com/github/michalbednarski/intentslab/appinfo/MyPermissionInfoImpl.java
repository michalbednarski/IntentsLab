package com.github.michalbednarski.intentslab.appinfo;

import android.annotation.SuppressLint;
import android.content.pm.PermissionInfo;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * See {@link MyPermissionInfo}
 */
@SuppressLint("InlinedApi")
class MyPermissionInfoImpl implements MyPermissionInfo {
    PermissionInfo mSystemPermissionInfo;
    MyPackageInfoImpl mOwnerPackage;

    boolean mOwnerVerified;
    ArrayList<MyPackageInfoImpl> mPackagesTryingDefine = new ArrayList<>();

    @Override
    public String getName() {
        return mSystemPermissionInfo.name;
    }

    @Override
    public MyPackageInfo getOwnerPackage() {
        return mOwnerPackage;
    }

    @Override
    public boolean isNormal() {
        return (mSystemPermissionInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
                == PermissionInfo.PROTECTION_NORMAL;
    }

    @Override
    public boolean isDangerous() {
        return (mSystemPermissionInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
                == PermissionInfo.PROTECTION_DANGEROUS;
    }

    @Override
    public boolean isSignature() {
        int baseProtection = mSystemPermissionInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
        return
                baseProtection == PermissionInfo.PROTECTION_SIGNATURE ||
                baseProtection == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM;
    }

    @Override
    public boolean isSystem() {
        if (!isSignature()) {
            return false;
        }

        int protectionLevel = mSystemPermissionInfo.protectionLevel;

        return
                (protectionLevel & PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM ||
                (protectionLevel & PermissionInfo.PROTECTION_FLAG_SYSTEM) != 0;
    }

    @Override
    public boolean isDevelopment() {
        return isSignature() && (mSystemPermissionInfo.protectionLevel & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0;
    }

    /**
     * Fill this object with info obtained from {@link android.content.pm.PackageManager#getPermissionInfo(String, int)}
     */
    void fillWithInfoFromPackageManager(PermissionInfo permissionInfo) {
        // If PackageManager haven't found permission this arg will be null
        if (permissionInfo == null) {
            // TODO: handle declared but missing permission
            return;
        }

        // Find defining package in mPackagesTryingDefine
        for (Iterator<MyPackageInfoImpl> iterator = mPackagesTryingDefine.iterator(); iterator.hasNext(); ) {
            MyPackageInfoImpl triedPackage = iterator.next();
            if (permissionInfo.packageName.equals(triedPackage.mPackageName)) {
                mOwnerPackage = triedPackage;
                iterator.remove();
            }
        }

        // Export data into fields
        mOwnerVerified = true;
        mSystemPermissionInfo = permissionInfo;
    }

    /**
     * Add data gathered from getInstalledPackages() to this object
     *
     * @return True if {@link MyPackageManagerImpl} should call {@link #fillWithInfoFromPackageManager(PermissionInfo)}
     */
    boolean fillWithInfoFromApp(PermissionInfo info, MyPackageInfoImpl ownerPackage) {
        // If we already have info obtained directly from PackageManager
        // nothing more to do
        if (mOwnerVerified) {
            if (mSystemPermissionInfo.packageName.equals(ownerPackage.mPackageName)) {
                mOwnerPackage = ownerPackage;
            } else {
                // Add this package to list of packages trying to define this permission
                mPackagesTryingDefine.add(ownerPackage);
            }
            return false;
        }

        // Add this package to list of packages trying to define this permission
        mPackagesTryingDefine.add(ownerPackage);

        // If this is first time we see this permission, it's okay to assume it is the right one
        if (mSystemPermissionInfo == null) {
            mSystemPermissionInfo = info;
            return false;
        }

        // This is another package trying to define this permission
        // request package manager for details
        return true;
    }
}
