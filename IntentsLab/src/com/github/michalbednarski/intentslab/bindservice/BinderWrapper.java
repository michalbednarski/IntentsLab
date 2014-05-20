/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
