package com.github.michalbednarski.intentslab.appinfo;

/**
 * Information about permission
 */
public interface MyPermissionInfo {
    String getName();

    MyPackageInfo getOwnerPackage();

    boolean isNormal();

    boolean isDangerous();

    /**
     * True if this is any of signature permissions
     * such as signature|system
     */
    boolean isSignature();

    boolean isSystem();

    boolean isDevelopment();
}
