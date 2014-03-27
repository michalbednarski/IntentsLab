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
