package com.example.testapp1.runas;

import android.util.SparseArray;

/**
 * Class managing running RunAs remote processes
 */
public class RunAsManager {
    private static final SparseArray<IRemoteInterface> sRemoteInterfaces = new SparseArray<IRemoteInterface>();

    static boolean registerRemote(int uid, IRemoteInterface remoteInterface) {
        synchronized (sRemoteInterfaces) {
            final IRemoteInterface oldRemoteInterface = sRemoteInterfaces.get(uid);
            if (oldRemoteInterface != null && oldRemoteInterface.asBinder().isBinderAlive()) {
                return false;
            }
            sRemoteInterfaces.put(uid, remoteInterface);
        }
        return true;
    }
}
