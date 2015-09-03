package com.github.michalbednarski.intentslab.appinfo;

import android.content.pm.PackageManager;

/**
 * Result of {@link MyPackageInfo#getRequestedAndGrantedPermissions(PackageManager)}
 */
public class UsedAppPermissionDetails {
    public String[] grantedPermissions;
    public String[] implicitlyGrantedPermissions;
    public String[] deniedPermissions;
}
