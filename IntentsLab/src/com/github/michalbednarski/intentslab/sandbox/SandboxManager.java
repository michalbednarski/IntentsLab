package com.github.michalbednarski.intentslab.sandbox;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;

import com.github.michalbednarski.intentslab.BuildConfig;

import java.util.ArrayList;

/**
 * Created by mb on 30.09.13.
 */
public class SandboxManager {

    private static final String SANDBOX_PACKAGE = "com.github.michalbednarski.intentslab.sandbox";
    private static final String SANDBOX_SERVICE_CLASS = "com.github.michalbednarski.intentslab.sandbox.SandboxService";
    private static final String RESET_RECEIVER_CLASS = "com.github.michalbednarski.intentslab.sandbox.ResetReceiver";

    private static Context sApplicationContext;
    private static final ServiceConnection sServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sSandbox = ISandbox.Stub.asInterface(service);

            // Invoke all "sandbox ready" callbacks
            final ArrayList<Runnable> sandboxReadyCallbacks = sSandboxReadyCallbacks;
            if (sandboxReadyCallbacks != null) {
                sSandboxReadyCallbacks = null;
                for (Runnable sandboxReadyCallback : sandboxReadyCallbacks) {
                    sandboxReadyCallback.run();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sSandbox = null;
        }
    };

    /**
     * Disallow instantiate
     */
    private SandboxManager() {}


    private static ISandbox sSandbox = null;
    private static ArrayList<Runnable> sSandboxReadyCallbacks = null;


    public static boolean isReady() {
        return sSandbox != null && sSandbox.asBinder().isBinderAlive();
    }

    public static void initSandboxAndRunWhenReady(Context context, Runnable whenReady) {
        if (BuildConfig.DEBUG && sRefCount == 0) {
            throw new AssertionError("initSandboxAndRunWhenReady called without refSandbox");
        }
        if (isReady()) {
            whenReady.run();
        } else if (sSandboxReadyCallbacks != null) {
            sSandboxReadyCallbacks.add(whenReady);
        } else{
            sSandboxReadyCallbacks = new ArrayList<Runnable>(1);
            sSandboxReadyCallbacks.add(whenReady);
            if (sApplicationContext == null) {
                sApplicationContext = context.getApplicationContext();
            }
            sApplicationContext.bindService(
                    new Intent().setClassName(SANDBOX_PACKAGE, SANDBOX_SERVICE_CLASS),
                    sServiceConnection,
                    Context.BIND_AUTO_CREATE
            );
        }
    }

    public static void resetSandbox(Context context) {
        context.sendBroadcast(new Intent().setClassName(SANDBOX_PACKAGE, RESET_RECEIVER_CLASS));
    }

    public static boolean isSandboxInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(SANDBOX_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void requestSandboxInstall(FragmentActivity activity) {
        (new SandboxInstallRequestDialog()).show(activity.getSupportFragmentManager(), "sandboxInstallRequest");
    }

    @Deprecated
    public static Context getService() {
        return sApplicationContext;
    }

    public static ISandbox getSandbox() {
        return sSandbox;
    }

    private static int sRefCount = 0;
    public static void refSandbox() {
        sRefCount++;
    }

    public static void unrefSandbox() {
        sRefCount--;
        if (BuildConfig.DEBUG && sRefCount < 0) {
            throw new AssertionError("Too many unrefSandbox calls");
        }
    }
}
