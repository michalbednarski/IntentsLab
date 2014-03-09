package com.github.michalbednarski.intentslab.xposedhooks.internal;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.xposedhooks.BuildConfig;
import com.github.michalbednarski.intentslab.xposedhooks.app.QueryPermissionsReceiver;
import com.github.michalbednarski.intentslab.xposedhooks.app.RequestPermissionActivity;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by mb on 01.03.14.
 */
class SystemService extends IInternalInterface.Stub {

    private final Context mContext;

    private boolean mInitializationDone = false;
    private final Object mAllowedUidsLock = new Object();
    private int[] mAllowedUids = null;



    SystemService(Context context) {
        mContext = context;
    }

    private boolean checkCallingPermission() {
        synchronized (mAllowedUidsLock) {
            if (!mInitializationDone) {
                mInitializationDone = true;
                registerAllowedUidReceiversLocked();
                refreshPermissions(null);
            }
            int uid = Binder.getCallingUid();
            return uid == 0 ||
                    (mAllowedUids != null && Arrays.binarySearch(mAllowedUids, uid) >= 0);
        }
    }

    private void enforcePermission() {
        if (!checkCallingPermission()) {
            throw new SecurityException("XIntentsLab permission denied");
        }
    }

    @Override
    public boolean havePermission() {
        return checkCallingPermission();
    }

    @Override
    public void refreshPermissions(final IRefreshPermissionsCallback callback) {
        mContext.sendOrderedBroadcast(
                new Intent().setClassName(BuildConfig.PACKAGE_NAME, QueryPermissionsReceiver.class.getName()),
                null, // No permission - explicit intent
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        synchronized (mAllowedUidsLock) {
                            // Save allowed uids list
                            mAllowedUids = getResultExtras(true).getIntArray(QueryPermissionsReceiver.RESULT_EXTRA_ALLOWED_UIDS);
                        }
                        if (callback != null) {
                            try {
                                callback.refreshDone();
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }
                    }
                },
                null,
                0,
                null,
                null
        );
    }

    private void registerAllowedUidReceiversLocked() {
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        synchronized (mAllowedUidsLock) {
                            int removedUid = intent.getIntExtra(Intent.EXTRA_UID, 0);
                            if (mAllowedUids != null) {
                                int index = Arrays.binarySearch(mAllowedUids, removedUid);
                                if (index >= 0) {
                                    int[] newAllowedUids = new int[mAllowedUids.length - 1];
                                    System.arraycopy(mAllowedUids, 0, newAllowedUids, 0, index);
                                    System.arraycopy(mAllowedUids, index + 1, newAllowedUids, index, mAllowedUids.length - index - 1);
                                    mAllowedUids = newAllowedUids;
                                }
                            }
                        }
                    }
                },
                new IntentFilter(Intent.ACTION_UID_REMOVED)
        );
    }

    @Override
    public PendingIntent getRequestPermissionPendingIntent(String packageName) {
        int uid = Binder.getCallingUid();
        long origIdentity = Binder.clearCallingIdentity();
        try {
            return PendingIntent.getActivity(
                    mContext,
                    0,
                    new Intent()
                            .setClassName(BuildConfig.PACKAGE_NAME, RequestPermissionActivity.class.getName())
                            .setAction(uid + (packageName == null ? "" : ("|" + packageName))),
                    0
                    );
        } finally {
            Binder.restoreCallingIdentity(origIdentity);
        }
    }


    // Object tracking
    final Set<IBinder> mAllowedTrackingTags =
            // http://stackoverflow.com/a/4062950
            Collections.newSetFromMap(new WeakHashMap<IBinder, Boolean>());

    @Override
    public void allowTrackingTag(final IBinder tag) throws RemoteException {
        enforcePermission();
        synchronized (mAllowedTrackingTags) {
            mAllowedTrackingTags.add(tag);
        }
    }

    @Override
    public boolean isTrackingTagAllowed(IBinder tag) throws RemoteException {
        synchronized (mAllowedTrackingTags) {
            return mAllowedTrackingTags.contains(tag);
        }
    }

    // System pid
    @Override
    public int getSystemPid() throws RemoteException {
        return android.os.Process.myPid();
    }
}
