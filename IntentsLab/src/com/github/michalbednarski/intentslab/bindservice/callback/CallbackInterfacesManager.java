package com.github.michalbednarski.intentslab.bindservice.callback;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.github.michalbednarski.intentslab.MasterDetailActivity;
import com.github.michalbednarski.intentslab.Utils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by mb on 25.03.14.
 */
public class CallbackInterfacesManager {

    private static final ArrayList<CallbackInfo> sCallbacks = new ArrayList<CallbackInfo>();
    private static final DataSetObservable sCallbacksObservable = new DataSetObservable();


    static IBinder createLocalCallback(Class<?> anInterface, File cacheDir) {
        CallbackInfo callbackInfo = new CallbackInfo();
        final IBinder iBinder;

        // Create implementation class
        try {
            // TODO: organize
            final Class<?> stubClass = Class.forName(anInterface.getName() + "$Stub");
            iBinder = new AidlImplGenerator(cacheDir).makeAidlImpl((Class<? extends Binder>) stubClass, new LocalAidlInvocationHandlerImpl(callbackInfo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Fill callback info
        callbackInfo.binder = iBinder;

        // Register callback info
        sCallbacks.add(callbackInfo);
        sCallbacksObservable.notifyChanged();

        return iBinder;
    }

    // For ClipboardActivity
    public static int getCallbacksCount() {
        return sCallbacks.size();
    }

    public static void registerCallbacksObserver(DataSetObserver observer) {
        sCallbacksObservable.registerObserver(observer);
    }

    public static void unregisterCallbacksObserver(DataSetObserver observer) {
        sCallbacksObservable.unregisterObserver(observer);
    }

    public static void openLogForCallbackAt(MasterDetailActivity activity, int position) {
        final Bundle arguments = new Bundle();
        Utils.putLiveRefInBundle(arguments, CallbackCallsFragment.ARG_CALLBACK_INFO, sCallbacks.get(position));
        activity.openFragment(
                CallbackCallsFragment.class,
                arguments
        );
    }


    // Models
    static class CallbackInfo {
        ArrayList<BaseCallInfo> calls = new ArrayList<BaseCallInfo>();
        DataSetObservable callsObservable = new DataSetObservable();
        IBinder binder;
    }

    static abstract class BaseCallInfo {
        String methodName;
    }

    static class LocalCallInfo extends BaseCallInfo {
        Object[] arguments;
    }
}
