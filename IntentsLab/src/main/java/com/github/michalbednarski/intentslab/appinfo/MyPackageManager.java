package com.github.michalbednarski.intentslab.appinfo;

import android.content.Context;

import org.jdeferred.Promise;

import java.util.Collection;
import java.util.Map;

/**
 * Wrapper around package manager
 *
 * Get instance with {@link MyPackageManagerImpl#getInstance(Context)}
 */
public interface MyPackageManager {
    Promise<Collection<MyPackageInfo>, Void, Void> getPackages(boolean withIntentFilters);

    Promise<MyPackageInfo, Void, Void> getPackageInfo(boolean withIntentFilters, String packageName);

    Promise<Map<String, MyPermissionInfo>, Void, Void> getPermissions();
}
