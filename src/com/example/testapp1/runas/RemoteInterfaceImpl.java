package com.example.testapp1.runas;

import android.os.RemoteException;

/**
 * Class providing remote interface
 */
class RemoteInterfaceImpl extends IRemoteInterface.Stub {
    @Override
    public void nop() throws RemoteException {

    }
}
