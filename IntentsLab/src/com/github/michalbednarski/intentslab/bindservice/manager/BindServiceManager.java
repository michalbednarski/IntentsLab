package com.github.michalbednarski.intentslab.bindservice.manager;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;

import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.bindservice.AidlControlsFragment;
import com.github.michalbednarski.intentslab.bindservice.BaseServiceFragment;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by mb on 02.10.13.
 */
public class BindServiceManager {

    public static void prepareBinderAndShowUI(final Context context, final ServiceDescriptor descriptor) {
        final Helper helper = getBoundService(descriptor);
        helper.bindServiceAndRunWhenReady(context, new BinderReadyCallback() {
            @Override
            public void onBinderReady(IBinder binder) {
                if (binder != null) {
                    context.startActivity(
                            new Intent(context, SingleFragmentActivity.class)
                                    .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, AidlControlsFragment.class.getName())
                                    .putExtra(BaseServiceFragment.ARG_SERVICE_DESCRIPTOR, descriptor)
                    );
                } else {
                    helper.unbindAndRemove();
                }
            }
        });
    }


    private static HashMap<ServiceDescriptor, Helper> sBoundServices = new HashMap<ServiceDescriptor, Helper>();

    public static Helper getBoundService(ServiceDescriptor descriptor) {
        final Helper serviceHelper = sBoundServices.get(descriptor);
        if (serviceHelper != null) {
            return serviceHelper;
        }
        return new Helper(descriptor);
    }

    public static ServiceDescriptor[] getBoundServices() {
        Set<ServiceDescriptor> descriptors = sBoundServices.keySet();
        return descriptors.toArray(new ServiceDescriptor[descriptors.size()]);
    }

    public interface BinderReadyCallback {
        void onBinderReady(IBinder binder);
    }

    public interface AidlReadyCallback {
        void onAidlReady(IAidlInterface anInterface);
    }

    public static class Helper {
        private IBinder mBoundService = null;
        private final ServiceDescriptor mDescriptor;
        private final ServiceDescriptor.ConnectionManager mConnectionManager;
        private IAidlInterface mAidlInterface;
        String mPackageName = null;
        private String mInterfaceDescriptor = null;
        private boolean mInterfaceDescriptorValid = false;

        private ArrayList<BinderReadyCallback> mBinderReadyCallbacks = null;
        private ArrayList<AidlReadyCallback> mAidlReadyCallbacks = null;

        private boolean mSandboxRefd = false;
        private boolean mBound = false;

        private Helper(ServiceDescriptor descriptor) {
            mDescriptor = descriptor;
            mConnectionManager = descriptor.getConnectionManager();
            mConnectionManager.mHelper = this;

            sBoundServices.put(mDescriptor, this);
        }

        void dispatchBound(IBinder binder) {
            try {
                mInterfaceDescriptor = binder.getInterfaceDescriptor();
                mInterfaceDescriptorValid = true;
            } catch (Exception e) {
                mInterfaceDescriptor = Utils.describeException(e);
                mInterfaceDescriptorValid = false;
            }

            mBoundService = binder;
            for (BinderReadyCallback aidlReadyCallback : mBinderReadyCallbacks) {
                aidlReadyCallback.onBinderReady(binder);
            }
            mBinderReadyCallbacks = null;
        }

        public void unbind() {
            if (mBound) {
                mConnectionManager.unbind();
                mBound = false;
            }
            if (mSandboxRefd) {
                SandboxManager.unrefSandbox();
                mSandboxRefd = false;
            }
        }

        public void unbindAndRemove() {
            unbind();
            sBoundServices.remove(mDescriptor);
        }

        public void bindServiceAndRunWhenReady(Context context, BinderReadyCallback whenReady) {
            if (mBoundService != null && mBoundService.isBinderAlive() && SandboxManager.isReady()) {
                // Service is ready already
                whenReady.onBinderReady(mBoundService);
            } else if (mBinderReadyCallbacks != null) {
                // We have started preparing aidl, queue callback
                mBinderReadyCallbacks.add(whenReady);
            } else {
                // Create list of ready callbacks
                mBinderReadyCallbacks = new ArrayList<BinderReadyCallback>();
                mBinderReadyCallbacks.add(whenReady);

                // Ref sandbox if needed
                if (!mSandboxRefd) {
                    SandboxManager.refSandbox();
                    mSandboxRefd = true;
                }

                // Start preparing service
                SandboxManager.initSandboxAndRunWhenReady(context, new Runnable() {
                    @Override
                    public void run() {
                        if (!mBound) {
                            mBound = true;
                            mConnectionManager.bind();
                        } else {
                            dispatchBound(mBoundService);
                        }
                    }
                });
            }
        }

        public void prepareAidlAndRunWhenReady(Context context, AidlReadyCallback whenReady) {
            if (mAidlInterface != null && mAidlInterface.asBinder().isBinderAlive()) {
                // Aidl is ready already
                if (whenReady != null) {
                    whenReady.onAidlReady(mAidlInterface);
                }
            } else if (mAidlReadyCallbacks != null) {
                // We have started preparing aidl, queue callback
                if (whenReady != null) {
                    mAidlReadyCallbacks.add(whenReady);
                }
            } else {
                // Start preparing aidl
                mAidlReadyCallbacks = new ArrayList<AidlReadyCallback>();
                if (whenReady != null) {
                    mAidlReadyCallbacks.add(whenReady);
                }
                bindServiceAndRunWhenReady(context, new BinderReadyCallback() {
                    @Override
                    public void onBinderReady(IBinder binder) {
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
                                for (AidlReadyCallback aidlReadyCallback : mAidlReadyCallbacks) {
                                    aidlReadyCallback.onAidlReady(aidlInterface);
                                }
                                mAidlReadyCallbacks = null;
                            }
                        }).execute();
                    }
                });
            }
        }

        public String getInterfaceDescriptor() {
            return mInterfaceDescriptor;
        }

        public boolean isInterfaceDescriptorValid() {
            return mInterfaceDescriptorValid;
        }

        public ServiceDescriptor getDescriptor() {
            return mDescriptor;
        }

        public IAidlInterface getAidlIfAvailable() {
            if (mAidlInterface != null && mAidlInterface.asBinder().isBinderAlive()
                    && mBoundService != null && mBoundService.isBinderAlive()) {
                return mAidlInterface;
            } else {
                return null;
            }
        }

        public IBinder getBinderIfAvailable() {
            if (mBoundService != null && mBoundService.isBinderAlive()) {
                return mBoundService;
            } else {
                return null;
            }
        }
    }
}
