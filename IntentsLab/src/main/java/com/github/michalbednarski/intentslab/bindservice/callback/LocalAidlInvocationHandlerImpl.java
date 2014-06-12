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

package com.github.michalbednarski.intentslab.bindservice.callback;

import android.os.Handler;

/**
 * Created by mb on 26.03.14.
 */
class LocalAidlInvocationHandlerImpl extends BaseAidlInvocationHandler {
    private CallbackInterfacesManager.CallbackInfo mCallbackInfo;
    private Handler mHandler = new Handler();

    LocalAidlInvocationHandlerImpl(CallbackInterfacesManager.CallbackInfo callbackInfo) {
        mCallbackInfo = callbackInfo;
    }

    @Override
    protected Object invokeAidl(final Object[] args, final String methodName, Class returnType, boolean oneWay) {
        // Since this method is called from binder thread
        // we have to post call to UI thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final CallbackInterfacesManager.LocalCallInfo callInfo = new CallbackInterfacesManager.LocalCallInfo();
                callInfo.methodName = methodName;
                callInfo.arguments = args;
                mCallbackInfo.calls.add(callInfo);
                mCallbackInfo.callsObservable.notifyChanged();
            }
        });

        return null;
    }
}
