package com.github.michalbednarski.intentslab.appinfo;

import android.annotation.SuppressLint;
import android.content.pm.PermissionInfo;

/**
 * See {@link MyPermissionInfo}
 */
@SuppressLint("InlinedApi")
class MyPermissionInfoImpl implements MyPermissionInfo {
    PermissionInfo mSystemPermissionInfo;
    MyPackageInfoImpl mOwnerPackage;

    boolean mOwnerVerified;
    String mRealOwnerPackageName;

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

    MyPermissionInfoImpl(PermissionInfo systemPermissionInfo, MyPackageInfoImpl ownerPackage) {
        mSystemPermissionInfo = systemPermissionInfo;
        mOwnerPackage = ownerPackage;
    }
}
