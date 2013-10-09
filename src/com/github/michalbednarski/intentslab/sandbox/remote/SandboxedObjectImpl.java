package com.github.michalbednarski.intentslab.sandbox.remote;

import android.os.RemoteException;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.ParcelableValue;

/**
 * Created by mb on 08.10.13.
 */
class SandboxedObjectImpl extends ISandboxedObject.Stub {


    private Object mWrappedObject;

    SandboxedObjectImpl(Object wrappedObject) {

        mWrappedObject = wrappedObject;
    }

    @Override
    public ParcelableValue getObject() throws RemoteException {
        return new ParcelableValue(mWrappedObject);
    }
}
