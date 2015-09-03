package com.github.michalbednarski.intentslab.appinfo;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.util.Collection;

/**
 * Information about application/package
 */
public interface MyPackageInfo {
    String getPackageName();

    CharSequence loadLabel(PackageManager pm);
    Drawable loadIcon(PackageManager packageManager); // TODO: Context?

    Collection<MyComponentInfo> getActivities();
    MyComponentInfo getActivityByName(String name);

    Collection<MyComponentInfo> getReceivers();
    MyComponentInfo getReceiverByName(String name);

    Collection<MyComponentInfo> getServices();
    MyComponentInfo getServiceByName(String name);

    Collection<MyComponentInfo> getProviders();
    MyComponentInfo getProviderByName(String name);

    Collection<MyPermissionInfo> getDefinedPermissions();

    UsedAppPermissionDetails getRequestedAndGrantedPermissions(PackageManager pm); // TODO: async or preload

    boolean isApplicationEnabled();

    boolean isSystemApplication();

    /**
     * Get meta data for 'application'
     */
    Bundle getMetaData();
}
