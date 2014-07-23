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

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.bindservice.AidlControlsFragment;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by mb on 02.10.13.
 */
public class BindServiceManager {

    private static final int UNBIND_UNUSED_SERVICE_TIMEOUT = 10 * 1000;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());

    public static void prepareBinderAndShowUI(final Context context, final ServiceDescriptor descriptor) {
        context.startActivity(
                new Intent(context, SingleFragmentActivity.class)
                        .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, AidlControlsFragment.class.getName())
                        .putExtra(BaseServiceFragment.ARG_SERVICE_DESCRIPTOR, descriptor)
        );
    }


    private static HashMap<ServiceDescriptor, Helper> sBoundServices = new HashMap<ServiceDescriptor, Helper>();

    static Helper getBoundService(ServiceDescriptor descriptor) {
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
        void onAidlReady(AidlInterface anInterface);
    }

    public static class Helper {
        private IBinder mBoundService = null;
        private final ServiceDescriptor mDescriptor;
        private Runnable mUnbindRunnable;
        private AidlInterface mAidlInterface;
        private String mPackageName = null;
        private String mInterfaceDescriptor = null;
        private boolean mInterfaceDescriptorValid = false;

        private ArrayList<BinderReadyCallback> mBinderReadyCallbacks = null;
        private ArrayList<AidlReadyCallback> mAidlReadyCallbacks = null;

        private boolean mSandboxRefd = false;

        private int mUserRefs = 0;
        private boolean mPersistInClipboard; // TODO: require this flag to show service in ClipboardActivity
        private Runnable mShutdownMessage;

        void userRef() {
            mUserRefs++;
            if (mShutdownMessage != null) {
                sHandler.removeCallbacks(mShutdownMessage);
                mShutdownMessage = null;
            }
        }

        void userUnref() {
            mUserRefs--;
            if (BuildConfig.DEBUG && mUserRefs < 0) {
                throw new AssertionError("mUserRefs < 0");
            }
            if (BuildConfig.DEBUG && mShutdownMessage != null) {
                throw new AssertionError("mShutdownMessage already queued");
            }
            if (mUserRefs == 0 && !mPersistInClipboard) {
                mShutdownMessage = new Runnable() {
                    @Override
                    public void run() {
                        unbindAndRemove();
                    }
                };
                sHandler.postDelayed(mShutdownMessage, UNBIND_UNUSED_SERVICE_TIMEOUT);
            }
        }

        private Helper(ServiceDescriptor descriptor) {
            mDescriptor = descriptor;

            sBoundServices.put(mDescriptor, this);
        }

        private void unbindAndRemove() {
            if (mUnbindRunnable != null) {
                mUnbindRunnable.run();
                mUnbindRunnable = null;
            }
            if (mSandboxRefd) {
                SandboxManager.unrefSandbox();
                mSandboxRefd = false;
            }
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

                // Start preparing service
                mDescriptor.doBind(new BindServiceMediator(context.getApplicationContext()));
            }
        }

        class BindServiceMediator {
            private final Context mContext;

            private BindServiceMediator(Context context) {
                mContext = context;
            }

            Context getContext() {
                return mContext;
            }

            void setPackageName(String packageName) {
                mPackageName = packageName;
            }

            void refSandbox() {
                new AidlInterfaceMediator().refSandbox();
            }

            void registerUnbindRunnable(Runnable unbindRunnable) {
                if (BuildConfig.DEBUG && mUnbindRunnable != null) {
                    throw new AssertionError("unbindRunnable already registered");
                }

                mUnbindRunnable = unbindRunnable;
            }

            void dispatchBound(IBinder binder) {
                if (BuildConfig.DEBUG && Looper.myLooper() != Looper.getMainLooper()) {
                    throw new AssertionError("dispatchBound() must be called on main thread");
                }

                try {
                    mInterfaceDescriptor = binder.getInterfaceDescriptor();
                    mInterfaceDescriptorValid = true;
                } catch (Exception e) {
                    mInterfaceDescriptor = Utils.describeException(e);
                    mInterfaceDescriptorValid = false;
                }

                mBoundService = binder;
                if (mBinderReadyCallbacks != null) {
                    for (BinderReadyCallback aidlReadyCallback : mBinderReadyCallbacks) {
                        aidlReadyCallback.onBinderReady(binder);
                    }
                }
                mBinderReadyCallbacks = null;
            }
        }

        public void prepareAidlAndRunWhenReady(final Context context, AidlReadyCallback whenReady) {
            if (mAidlInterface != null && mAidlInterface.isReady()) {
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
                        ClassLoaderDescriptor classLoaderDescriptor = new ClassLoaderDescriptor(mPackageName);
                        AidlInterface.getAidlInterface(binder, classLoaderDescriptor, context, new AidlInterfaceMediator());
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

        public AidlInterface getAidlIfAvailable() {
            if (mAidlInterface != null && mAidlInterface.isReady()
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

        class AidlInterfaceMediator { // TODO: better name?
            private AidlInterfaceMediator() {}

            void refSandbox() {
                // Ref sandbox if needed
                if (!mSandboxRefd) {
                    SandboxManager.refSandbox();
                    mSandboxRefd = true;
                }
            }

            void handleAidlReady(AidlInterface aidlInterface) {
                if (BuildConfig.DEBUG && Looper.myLooper() != Looper.getMainLooper()) {
                    throw new AssertionError("handleAidlReady() must be called on main thread");
                }

                mAidlInterface = aidlInterface;
                for (AidlReadyCallback aidlReadyCallback : mAidlReadyCallbacks) {
                    aidlReadyCallback.onAidlReady(aidlInterface);
                }
                mAidlReadyCallbacks = null;
            }
        }
    }
}
