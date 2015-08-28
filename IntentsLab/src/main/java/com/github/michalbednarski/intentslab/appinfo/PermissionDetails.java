package com.github.michalbednarski.intentslab.appinfo;

/**
 * Detailed information about permission that are not cached
 */
public class PermissionDetails {
    public MyPermissionInfo permissionInfo;

    public MyPackageInfo[] grantedTo, implicitlyGrantedTo, deniedTo;

    public MyComponentInfo[] enforcingComponents;
}
