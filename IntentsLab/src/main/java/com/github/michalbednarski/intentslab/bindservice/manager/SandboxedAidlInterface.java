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

package com.github.michalbednarski.intentslab.bindservice.manager;

import android.os.IBinder;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;

/**
 * Created by mb on 21.07.14.
 */
class SandboxedAidlInterface extends AidlInterface {

    private final IAidlInterface mAidlInterface;
    private final String mInterfaceDescriptor;

    SandboxedAidlInterface(IAidlInterface aidlInterface, String interfaceDescriptor) {
        mAidlInterface = aidlInterface;
        mInterfaceDescriptor = interfaceDescriptor;
    }

    @Override
    public String getInterfaceName() {
        return mInterfaceDescriptor;
    }

    @Override
    public SandboxedMethod[] getMethods() {
        try {
            return mAidlInterface.getMethods();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InvokeMethodResult invokeMethod(int methodNumber, SandboxedObject[] arguments) {
        try {
            return mAidlInterface.invokeMethod(methodNumber, arguments);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InvokeMethodResult invokeMethodUsingBinder(IBinder binder, int methodNumber, SandboxedObject[] arguments) {
        try {
            return mAidlInterface.invokeMethodUsingBinder(binder, methodNumber, arguments);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    boolean isReady() {
        return mAidlInterface.asBinder().isBinderAlive();
    }
}
