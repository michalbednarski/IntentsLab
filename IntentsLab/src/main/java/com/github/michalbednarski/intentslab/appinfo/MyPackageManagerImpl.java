package com.github.michalbednarski.intentslab.appinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.github.michalbednarski.intentslab.BuildConfig;

import org.jdeferred.Promise;
import org.jdeferred.android.AndroidDeferredObject;
import org.jdeferred.impl.DeferredObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around PackageManager that performs caching and additional information loading
 */
public class MyPackageManagerImpl implements MyPackageManager {
    private static final String TAG = "MyPackageManagerImpl";

    public static final int STANDARD_FLAGS = PackageManager.GET_ACTIVITIES |
            PackageManager.GET_RECEIVERS |
            PackageManager.GET_SERVICES |
            PackageManager.GET_PROVIDERS |
            PackageManager.GET_DISABLED_COMPONENTS |
            PackageManager.GET_URI_PERMISSION_PATTERNS |
            PackageManager.GET_META_DATA;

    private static MyPackageManagerImpl sInstance;

    public static MyPackageManager getInstance(Context context) {
        if (BuildConfig.DEBUG) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                throw new AssertionError("getInstance must be called on main thread");
            }
            if (context == null) {
                throw new AssertionError("context must not be null");
            }
        }
        if (sInstance == null) {
            sInstance = new MyPackageManagerImpl(context.getApplicationContext());
        }
        return sInstance;
    }

    private Context mContext;
    private PackageManager mPm;

    private Handler mWorkerHandler;

    final Object mLock = new Object();

    /**
     * Scanned and not stale packages
     * Guarded by {@link #mLock}
     */
    Map<String, MyPackageInfoImpl> mPackages = new ArrayMap<>();

    /**
     * True if we can just return mPackages.values()
     * in {@link #getPackages(boolean)}
     * Guarded by {@link #mLock}
     */
    boolean mLoadedAllPackages;


    private MyPackageManagerImpl(Context context) {
        // 'context' is application context
        mContext = context;
        mPm = context.getPackageManager();

        // Start worker thread
        HandlerThread workerThread = new HandlerThread("MyPackageManager");
        workerThread.start();
        mWorkerHandler = new Handler(workerThread.getLooper());

        // Register receiver for updates
        PackagesChangedReceiver receiver = new PackagesChangedReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        context.registerReceiver(receiver, filter);
    }


    private void loadAllInstalledPackagesInfoIfNeeded() {
        // Check if we have to do update
        synchronized (mLock) {
            if (mLoadedAllPackages) {
                return;
            }
        }

        // Load all packages
        boolean workAroundSmallBinderBuffer = false;
        List<PackageInfo> allPackages = null;
        try {
            allPackages = mPm.getInstalledPackages(STANDARD_FLAGS);
        } catch (Exception e) {
            Log.w(TAG, "Loading all apps at once failed, retrying separately", e);
        }

        // If loading with STANDARD_FLAGS failed, retry with 0 flags
        // and ask system for package details separately
        if (allPackages == null || allPackages.isEmpty()) {
            workAroundSmallBinderBuffer = true;
            allPackages = mPm.getInstalledPackages(0);
        }

        for (PackageInfo pack : allPackages) {
            if (workAroundSmallBinderBuffer) {
                loadPackageInfo(pack.packageName);
            } else {
                convertPackageInfoAndAddToCache(pack);
            }
        }

        mLoadedAllPackages = true;
    }

    private void fillIntentFiltersForPackage(MyPackageInfoImpl myPackageInfo) {
        if (!myPackageInfo.mIntentFiltersLoaded) {
            ScanManifestTask.parseInstalledPackage(mContext, myPackageInfo);
        }
    }

    private void fillIntentFiltersForAllPackages() {
        for (MyPackageInfoImpl myPackageInfo : mPackages.values()) {
            fillIntentFiltersForPackage(myPackageInfo);
        }
    }

    private MyPackageInfoImpl loadPackageInfoOrGetCached(String packageName) {
        synchronized (mLock) {
            if (mPackages.containsKey(packageName)) {
                return mPackages.get(packageName);
            }
        }
        return loadPackageInfo(packageName);
    }

    private MyPackageInfoImpl loadPackageInfo(String packageName) {
        PackageInfo packageInfo;
        try {
            // Try loading package info normally
            packageInfo = mPm.getPackageInfo(packageName, STANDARD_FLAGS);
        } catch (PackageManager.NameNotFoundException e) {

            // Failed: no such package
            Log.w(TAG, "getPackageInfo() thrown NameNotFoundException for " + packageName, e);
            return null;
        } catch (Exception e) {

            // Failed: problem with binder buffer, retry by component types
            try {
                // Activities and general
                packageInfo = mPm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA | PackageManager.GET_DISABLED_COMPONENTS);
                if (packageInfo.applicationInfo == null) { return null; }

                // Receivers
                PackageInfo partialPackageInfo = mPm.getPackageInfo(packageName, PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA | PackageManager.GET_DISABLED_COMPONENTS);
                packageInfo.receivers = partialPackageInfo.receivers;

                // Services
                partialPackageInfo = mPm.getPackageInfo(packageName, PackageManager.GET_SERVICES | PackageManager.GET_META_DATA | PackageManager.GET_DISABLED_COMPONENTS);
                packageInfo.services = partialPackageInfo.services;

                // Providers
                partialPackageInfo = mPm.getPackageInfo(packageName, PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA | PackageManager.GET_URI_PERMISSION_PATTERNS | PackageManager.GET_DISABLED_COMPONENTS);
                packageInfo.providers = partialPackageInfo.providers;

                // Permissions
                partialPackageInfo = mPm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
                packageInfo.permissions = partialPackageInfo.permissions;
            } catch (Exception e1) {
                Log.w(TAG, "getPackageInfo() by components thrown Exception for " + packageName + "", e1);
                return null;
            }
        }

        // Construct our wrapper
        return convertPackageInfoAndAddToCache(packageInfo);
    }

    private MyPackageInfoImpl convertPackageInfoAndAddToCache(PackageInfo packageInfo) {
        // Filter out non-applications
        if (packageInfo.applicationInfo == null) {
            return null;
        }

        // Load component information separately if they were to big to send them all at once
        MyPackageInfoImpl myPackageInfo = new MyPackageInfoImpl(packageInfo);
        synchronized (mLock) {
            mPackages.put(packageInfo.packageName, myPackageInfo);
        }
        return myPackageInfo;
    }


    @Override
    @SuppressWarnings("unchecked")
    public Promise<Collection<MyPackageInfo>, Void, Void> getPackages(final boolean withIntentFilters) {
        final DeferredObject<Collection<MyPackageInfo>, Void, Void> deferred = new DeferredObject<>();
        synchronized (mLock) {
            if (mLoadedAllPackages) {
                deferred.resolve((Collection) mPackages.values());
            } else {
                mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadAllInstalledPackagesInfoIfNeeded();
                        if (withIntentFilters) {
                            fillIntentFiltersForAllPackages();
                        }
                        deferred.resolve((Collection) mPackages.values());
                    }
                });
            }
        }

        return new AndroidDeferredObject<>(deferred);
    }

    @Override
    public Promise<MyPackageInfo, Void, Void> getPackageInfo(final boolean withIntentFilters, final String packageName) {
        final DeferredObject<MyPackageInfo, Void, Void> deferred = new DeferredObject<>();
        synchronized (mLock) {
            if (mPackages.containsKey(packageName) && !withIntentFilters) { // TODO: load intent filters only if needed
                deferred.resolve(mPackages.get(packageName));
            } else {
                mWorkerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        MyPackageInfoImpl myPackageInfo = loadPackageInfoOrGetCached(packageName);
                        if (myPackageInfo == null) {
                            deferred.reject(null);
                            return;
                        }
                        if (withIntentFilters) {
                            fillIntentFiltersForPackage(myPackageInfo);
                        }
                        deferred.resolve(myPackageInfo);
                    }
                });
            }
        }

        return new AndroidDeferredObject<>(deferred);
    }

    private class PackagesChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            synchronized (mLock) {
                mPackages.remove(packageName);
                mLoadedAllPackages = false;
            }
        }
    }
}
