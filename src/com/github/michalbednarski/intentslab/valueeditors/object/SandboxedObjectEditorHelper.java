package com.github.michalbednarski.intentslab.valueeditors.object;

import android.content.Context;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import com.github.michalbednarski.intentslab.sandbox.ClassLoaderDescriptor;
import com.github.michalbednarski.intentslab.sandbox.ISandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;
import com.github.michalbednarski.intentslab.sandbox.SandboxedClassField;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by mb on 15.11.13.
 */
class SandboxedObjectEditorHelper implements EditorLauncher.EditorLauncherWithSandboxCallback, ObjectEditorHelper {
    private final Context mContext;
    private Bundle mWrappedObject;
    private final EditorLauncher mEditorLauncher;
    private final ObjectEditorHelperCallback mObjectEditorHelperCallback;
    private ISandboxedObject mSandboxedObject;
    private boolean mShutDown = false;
    private Handler mHandler = new Handler();
    private SandboxedClassField[] mFields = null;
    private HashMap<String, InlineValueEditor> mValueEditors = new HashMap<String, InlineValueEditor>();
    private boolean mHasNonPublicFields = false;
    private boolean mHasGetters;

    private ArrayList<Runnable> mReadyCallbacks = new ArrayList<Runnable>();


    private Runnable mWrapInSandboxRunnable = new Runnable() {
        @Override
        public void run() {
            // Check if sandboxed object isn't ready already
            if (isSandboxedObjectReady()) {
                return;
            }

            // Init sandbox
            SandboxManager.initSandboxAndRunWhenReady(mContext, new Runnable() {
                @Override
                public void run() {
                    // Check again
                    if (isSandboxedObjectReady()) {
                        return;
                    }


                    try {
                        // Unwrap object in sandbox
                        mSandboxedObject = SandboxManager.getSandbox().sandboxObject(mWrappedObject, new ClassLoaderDescriptor("com.github.michalbednarski.intentslab.samples")); // TODO: don't hardcode

                        mHasGetters = mSandboxedObject.gettersExits();

                        // Initialize Fields, Accessors and InlineValueEditors
                        if (mFields == null) {
                            mFields = mSandboxedObject.getNonStaticFields();
                            for (SandboxedClassField field : mFields) {
                                final String name = field.name;
                                final Accessors accessors = new Accessors(name);
                                boolean isNonPublic = (field.modifiers & Modifier.PUBLIC) == 0;
                                if (isNonPublic) {
                                    mHasNonPublicFields = true;
                                }
                                mValueEditors.put(field.name,
                                        new InlineValueEditor(
                                                field.type.aClass,
                                                field.name,
                                                isNonPublic,
                                                accessors
                                        )
                                );
                            }

                            // Run ready callbacks
                            for (Runnable readyCallback : mReadyCallbacks) {
                                readyCallback.run();
                            }
                            mReadyCallbacks.clear();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace(); // Cannot do anything about that
                    }
                }
            });
        }
    };


    SandboxedObjectEditorHelper(Context context, Bundle wrappedObject, EditorLauncher editorLauncher, ObjectEditorHelperCallback objectEditorHelperCallback) {
        mContext = context;
        mWrappedObject = wrappedObject;
        mEditorLauncher = editorLauncher;
        mObjectEditorHelperCallback = objectEditorHelperCallback;
        SandboxManager.refSandbox();
        mWrapInSandboxRunnable.run();
    }

    void shutdown() {
        assert !mShutDown;
        mShutDown = true;
        mHandler.removeCallbacks(mWrapInSandboxRunnable);
        SandboxManager.unrefSandbox();
    }

    void initializeAndRunWhenReady(Runnable whenReady) {
        if (isSandboxedObjectReady()) {
            whenReady.run();
        } else {
            mReadyCallbacks.add(whenReady);
            mWrapInSandboxRunnable.run();
        }
    }

    private boolean isSandboxedObjectReady() {
        assert !mShutDown;
        return mSandboxedObject != null && mSandboxedObject.asBinder().isBinderAlive();
    }

    @Override
    public void onEditorResult(String key, Object newValue) {
        final InlineValueEditor valueEditor = mValueEditors.get(key);
        final Accessors accessors = (Accessors) valueEditor.getAccessors();
        assert !accessors.mValueIsSandboxed;
        accessors.setValue(newValue);
        valueEditor.updateTextOnButton();
        if (mObjectEditorHelperCallback != null) {
            mObjectEditorHelperCallback.onModified();
        }
    }

    @Override
    public void onSandboxedEditorResult(String key, Bundle newWrappedValue) {
        final InlineValueEditor valueEditor = mValueEditors.get(key);
        final Accessors accessors = (Accessors) valueEditor.getAccessors();
        assert accessors.mValueIsSandboxed;
        accessors.setValue(newWrappedValue);
        valueEditor.updateTextOnButton();
        if (mObjectEditorHelperCallback != null) {
            mObjectEditorHelperCallback.onModified();
        }
    }

    private static final Object VALUE_NOT_SYNCHRONIZED = new Object();

    private class Accessors implements InlineValueEditor.ValueAccessors {
        String mFieldName;
        Object mCachedValue;
        String mCachedStringValue;
        boolean mValueIsSandboxed = false;

        /**
         * State of value synchronization:
         *  null = Value doesn't need synchronization with sandbox (not modified or retrieved sandboxed object)
         *  {@link #VALUE_NOT_SYNCHRONIZED} = Value wasn't synchronized with any object and needs to be synchronized
         *  {@link android.os.IBinder} from {@link ISandboxedObject} - Value synchronized with that sandboxed object,
         *    must be pushed again if sandbox has crashed
         */
        Object mValueSynchronizedWith = null;

        Accessors(String fieldName) throws RemoteException {
            mFieldName = fieldName;

            // Get value
            final Bundle wrappedValue = mSandboxedObject.getFieldValue(mFieldName);
            try {
                // Try to unwrap, if successful use that value
                mCachedValue = SandboxManager.unwrapObject(wrappedValue);
            } catch (BadParcelableException e) {
                // Unwrap failed, use sandbox
                mValueIsSandboxed = true;
                mCachedStringValue = mSandboxedObject.getFieldValueAsString(mFieldName);
                mCachedValue = wrappedValue;
            }
        }

        @Override
        public Object getValue() {
            return mValueIsSandboxed ? mCachedStringValue : mCachedValue;
        }

        @Override
        public void setValue(Object newValue) {
            assert !mValueIsSandboxed || newValue instanceof Bundle;
            mCachedValue = newValue;
            mValueSynchronizedWith = VALUE_NOT_SYNCHRONIZED;

            try {
                syncValue();
            } catch (RemoteException e) {
                mHandler.post(mWrapInSandboxRunnable);
            }

            if (mObjectEditorHelperCallback != null) {
                mObjectEditorHelperCallback.onModified();
            }
        }

        void syncValue() throws RemoteException {
            final Bundle newWrappedValue = mValueIsSandboxed ?
                    ((Bundle) mCachedValue) :
                    SandboxManager.wrapObject(mCachedValue);
            mSandboxedObject.setFieldValue(mFieldName, newWrappedValue);
            mValueSynchronizedWith = mSandboxedObject.asBinder();
        }

        @Override
        public void startEditor() {
            if (mValueIsSandboxed) {
                mEditorLauncher.launchEditorForSandboxedObject(mFieldName, mFieldName, (Bundle) mCachedValue);
            } else {
                mEditorLauncher.launchEditor(mFieldName, mCachedValue);
            }
        }
    }


    @Override
    public InlineValueEditor[] getInlineValueEditors() {
        return mValueEditors.values().toArray(new InlineValueEditor[mValueEditors.size()]);
    }

    @Override
    public boolean hasNonPublicFields() {
        return mHasNonPublicFields;
    }

    @Override
    public Bundle getObject() {
        assert !mShutDown;
        if (mSandboxedObject != null) {
            try {
                // Synchronize values if they weren't already
                final IBinder sandboxedObjectBinder = mSandboxedObject.asBinder();
                final ArrayList<Accessors> mSynchronizedValues = new ArrayList<Accessors>();
                for (InlineValueEditor valueEditor : mValueEditors.values()) {
                    Accessors accessors = (Accessors) valueEditor.getAccessors();
                    if (accessors.mValueSynchronizedWith != null) { // Modified
                        if (accessors.mValueSynchronizedWith != sandboxedObjectBinder) { // Not synchronized
                            accessors.syncValue();
                        }
                        mSynchronizedValues.add(accessors);
                    }
                }

                // Get wrapped object
                mWrappedObject = mSandboxedObject.getWrappedObject();

                // Flag all synchronized values as so
                for (Accessors accessors : mSynchronizedValues) {
                    accessors.mValueSynchronizedWith = null;
                }
            } catch (RemoteException ignored) {}
        }
        return mWrappedObject;
    }

    @Override
    public CharSequence getGetterValues() {
        if (mHasGetters) {
            try {
                return mSandboxedObject.getGetterValues();
            } catch (RemoteException e) {
                return "";
            }
        }
        return "";
    }
}
