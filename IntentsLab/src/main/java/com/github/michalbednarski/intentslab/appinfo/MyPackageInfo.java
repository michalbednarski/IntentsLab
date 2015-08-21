package com.github.michalbednarski.intentslab.appinfo;

import java.util.Collection;

/**
 * Information about application/package
 */
public interface MyPackageInfo {
    String getPackageName();

    Collection<MyComponentInfo> getActivities();
    MyComponentInfo getActivityByName(String name);

    Collection<MyComponentInfo> getReceivers();
    MyComponentInfo getReceiverByName(String name);

    Collection<MyComponentInfo> getServices();
    MyComponentInfo getServiceByName(String name);

    Collection<MyComponentInfo> getProviders();
    MyComponentInfo getProviderByName(String name);

    boolean isApplicationEnabled();
}
