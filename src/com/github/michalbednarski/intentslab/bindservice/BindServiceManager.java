package com.github.michalbednarski.intentslab.bindservice;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by mb on 02.10.13.
 */
public class BindServiceManager {
    public static void bindServiceAndShowUI(Context context, Intent intent) {
        Helper helper = new Helper(intent, context);
        helper.registerPending();


        final Service sandboxService = SandboxManager.getService();
        if (sandboxService == null) {
            context.startService(new Intent(context, SandboxManager.AService.class));
        } else {
            executePendingBindServices(sandboxService);
        }
    }

    private static HashMap<BindServiceDescriptor, Helper> sBoundServices = new HashMap<BindServiceDescriptor, Helper>();
    private static ArrayList<Helper> sPendingBindServices = new ArrayList<Helper>();

    public static Helper getBoundService(BindServiceDescriptor descriptor) {
        final Helper serviceHelper = sBoundServices.get(descriptor);
        if (serviceHelper != null) {
            return serviceHelper;
        }
        return null;
    }

    static Helper[] getBoundServices() {
        Collection<Helper> values = sBoundServices.values();
        return values.toArray(new Helper[values.size()]);
    }

    public static void executePendingBindServices(Service sandboxService) {
        assert sandboxService != null;
        for (Helper pendingBindService : sPendingBindServices) {
            pendingBindService.doBindService(sandboxService);
        }
        sPendingBindServices.clear();
    }

    static class Helper implements ServiceConnection {
        IBinder mBoundService;
        private Context mContextForShowingDialogs; // Must null this out after use
        final BindServiceDescriptor mDescriptor;
        IAidlInterface mAidlInterface;
        private String mPackageName;
        private ArrayList<Runnable> mAidlReadyCallbacks = null;
        private String mTitle;


        private Helper(Intent intent, Context requesterContext) {
            mContextForShowingDialogs = requesterContext;

            mDescriptor = new BindServiceDescriptor();
            mDescriptor.intent = intent;

            mTitle = Utils.afterLastDot(intent.getAction());
        }

        private void registerPending() {
            sBoundServices.put(mDescriptor, this);
            sPendingBindServices.add(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Remove from list of pending connections
            sPendingBindServices.remove(this);

            // Save package name
            mPackageName = name.getPackageName();

            // Register ourselves
            mBoundService = service;

            // Show activity for messing with service
            if (mContextForShowingDialogs != null) {
                mContextForShowingDialogs.startActivity(
                        new Intent(mContextForShowingDialogs, BoundServiceActivity.class)
                        .putExtra(BoundServiceActivity.EXTRA_SERVICE, mDescriptor)
                );
                mContextForShowingDialogs = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }



        public void unbind() {
            Service sandboxService = SandboxManager.getService();
            if (mBoundService != null && sandboxService != null) {
                sandboxService.unbindService(this);
                mBoundService = null;
            }
            sBoundServices.remove(mDescriptor);
            if (mBoundService == null) {
                sPendingBindServices.remove(this);
            }
        }

        public void prepareAidlAndRunWhenReady(Context context, Runnable whenReady) {
            if (mAidlInterface != null && mAidlInterface.asBinder().isBinderAlive()) {
                // Aidl is ready already
                whenReady.run();
            } else if (mAidlReadyCallbacks != null) {
                // We have started preparing aidl, queue callback
                mAidlReadyCallbacks.add(whenReady);
            } else {
                // Start preparing aidl
                mAidlReadyCallbacks = new ArrayList<Runnable>();
                mAidlReadyCallbacks.add(whenReady);
                SandboxManager.initSandboxAndRunWhenReady(context, new Runnable() {
                    @Override
                    public void run() {
                        (new AsyncTask<Object, Object, IAidlInterface>() {
                            @Override
                            protected IAidlInterface doInBackground(Object... params) {
                                try {
                                    return SandboxManager.getSandbox().queryInterface(mBoundService, new ClassLoaderDescriptor(mPackageName));
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }

                            @Override
                            protected void onPostExecute(IAidlInterface aidlInterface) {
                                mAidlInterface = aidlInterface;
                                for (Runnable aidlReadyCallback : mAidlReadyCallbacks) {
                                    aidlReadyCallback.run();
                                }
                                mAidlReadyCallbacks = null;
                            }
                        }).execute();
                    }
            });
            }
        }

        private void doBindService(Service sandboxService) {
            if (!sandboxService.bindService(mDescriptor.intent, this, Context.BIND_AUTO_CREATE)) {
                Toast.makeText(mContextForShowingDialogs, "bindService failed...", Toast.LENGTH_LONG).show();
                mContextForShowingDialogs = null;
                unbind();
            }
        }

        public String getTitle() {
            return mTitle;
        }
    }
}
