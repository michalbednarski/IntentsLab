package com.example.testapp1.runas;

import android.content.Intent;
import android.os.RemoteException;

/**
 * Class providing remote interface
 */
class RemoteInterfaceImpl extends IRemoteInterface.Stub {

    @Override
    public void startActivity(Intent intent) throws RemoteException {
        ActivityManagerWrapper.get().startActivity(
                "intent", intent
        );
    }
}
