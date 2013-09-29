package com.github.michalbednarski.intentslab.runas;

import android.app.IActivityController;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;

/**
 * Class providing remote interface
 */
class RemoteInterfaceImpl extends IRemoteInterface.Stub {

    @Override
    public Bundle startActivity(Intent intent, IBinder token, int requestCode) throws RemoteException {
        Bundle result = new Bundle();
        try {
            ActivityManagerWrapper.get().startActivity(
                    "intent", intent,
                    "resultTo", token,
                    "requestCode", requestCode
            );
        } catch (InvocationTargetException e) {
            result.putSerializable("exception", e.getTargetException());
        }
        return result;
    }

    @Override
    public void setActivityController(IActivityController controller) throws RemoteException {
        ActivityManagerWrapper.get().setActivityController(controller);
    }
}
