package com.github.michalbednarski.intentslab.bindservice;

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Created by mb on 30.09.13.
 */
public class BinderWrapper extends Binder {
    public BinderWrapper(Binder wrappedBinder) {
        mWrappedBinder = wrappedBinder;
    }

    private final Binder mWrappedBinder;

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return mWrappedBinder.transact(code, data, reply, flags);
    }
}
