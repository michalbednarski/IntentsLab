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

package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.ArrayEditorFragment;
import com.github.michalbednarski.intentslab.valueeditors.BundleEditorFragment;
import com.github.michalbednarski.intentslab.valueeditors.EnumEditor;
import com.github.michalbednarski.intentslab.valueeditors.StringLikeItemEditor;
import com.github.michalbednarski.intentslab.valueeditors.object.ObjectEditorFragment;

import java.util.HashMap;

/**
 * Editor launcher for object editing
 */
public class EditorLauncher {
    /**
     * The editor registry
     *
     * @see Editor
     */
    private static final Editor[] EDITOR_REGISTRY = {
            // Boolean flipper
            new Editor.InPlaceValueToggler() {
                @Override
                public boolean canEdit(Object value) {
                    return value instanceof Boolean;
                }

                @Override
                public Object toggleObjectValue(Object originalValue) {
                    return !((Boolean) originalValue);
                }
            },

            // String like editor
            new StringLikeItemEditor.LaunchableEditor(),

            // Bundle editor
            new BundleEditorFragment.LaunchableEditor(),

            // Intent editor
            new IntentEditorActivity.LaunchableEditor(),

            // Enum editor
            new EnumEditor.LaunchableEditor(),

            // Array editor
            new ArrayEditorFragment.LaunchableEditor(),

            // Generic Parcelable object editor
            new ObjectEditorFragment.LaunchableEditor()
    };

    interface EditorLauncherCallbackBase {}

    /**
     * EditorLauncher Callback
     *
     * This is interface implemented by caller of launchEditor
     * returning data after edit, passed to constructor
     */
    public interface EditorLauncherCallback extends EditorLauncherCallbackBase {
        void onEditorResult(String key, Object newValue);
    }

    /**
     * EditorLauncher Callback with support for {@link #launchEditorForSandboxedObject(String, String, SandboxedObject)}
     *
     * This is interface implemented by caller of launchEditor
     * returning data after edit, passed to constructor
     */
    public interface EditorLauncherWithSandboxCallback extends EditorLauncherCallback {
        void onSandboxedEditorResult(String key, SandboxedObject newWrappedValue);
    }

    /**
     * EditorLauncher Callback Delegate
     *
     * This is interface implemented by caller of launchEditor
     * returning data after edit, passed to constructor
     */
    public interface EditorLauncherCallbackDelegate extends EditorLauncherCallbackBase {
        EditorLauncherCallback getEditorLauncherCallback();
    }


    private static final int REQUEST_CODE_INTERNAL_EDITOR = 0;

    private HelperFragment mHelperFragment;

    HelperFragment getHelperFragment() {
        return mHelperFragment;
    }

    private Fragment getOwnerFragment() {
        return mHelperFragment.getParentFragment();
    }

    private EditorLauncherCallback getCallback() {
        Fragment ownerFragment = getOwnerFragment();
        if (ownerFragment instanceof EditorLauncherCallbackDelegate) {
            return ((EditorLauncherCallbackDelegate) ownerFragment).getEditorLauncherCallback();
        } else {
            return (EditorLauncherCallback) ownerFragment;
        }
    }


    /**
     * For state restoring after process being killed
     *
     * ownerFragment -> editorLauncher
     */
    private static HashMap<Fragment, EditorLauncher> sPendingInitializations = new HashMap<Fragment, EditorLauncher>();

    EditorLauncher(final Fragment ownerFragment) {
        // Verify interface
        if (BuildConfig.DEBUG && !(
                ownerFragment instanceof EditorLauncherCallback ||
                ownerFragment instanceof EditorLauncherCallbackDelegate
                )) {
            throw new AssertionError("Fragment doesn't implement callback interface");
        }

        // Find existing helper fragment
        FragmentManager fragmentManager = ownerFragment.getChildFragmentManager();
        mHelperFragment = (HelperFragment) fragmentManager.findFragmentByTag(TAG_HELPER_FRAGMENT);

        // Create it if it doesn't exist
        if (mHelperFragment == null) {
            mHelperFragment = new HelperFragment();
            fragmentManager.beginTransaction().add(mHelperFragment, TAG_HELPER_FRAGMENT).commit();
            fragmentManager.executePendingTransactions();

            // If our process has been restarted fragment manager won't
            // use fragment we added above.
            // Add owner (parent) fragment to pending initializations and let it
            // associate with us in it's onCreate
            sPendingInitializations.put(ownerFragment, this);
            (new Handler()).post(new Runnable() {
                @Override
                public void run() {
                    sPendingInitializations.remove(ownerFragment);
                }
            });
        }

        // Assign to helper fragment
        mHelperFragment.mEditorLauncher = this;
    }

    static EditorLauncher getExistingForFragment(Fragment ownerFragment) {
        HelperFragment helperFragment = (HelperFragment) ownerFragment.getChildFragmentManager().findFragmentByTag(TAG_HELPER_FRAGMENT);
        if (helperFragment != null) {
            return helperFragment.mEditorLauncher;
        }
        return null;
    }

    public static <F extends Fragment & EditorLauncher.EditorLauncherCallbackBase> EditorLauncher getForFragment(F ownerFragment) {
        EditorLauncher editorLauncher = getExistingForFragment(ownerFragment);
        if (editorLauncher == null) {
            editorLauncher = new EditorLauncher(ownerFragment);
        }
        return editorLauncher;
    }

    /**
     * Start an editor or show Toast message if no applicable editor is available
     *
     * Results are returned to {@link EditorLauncher.EditorLauncherCallback#onEditorResult(String, Object) your onEditorResult method}
     *
     * @param key Key/title, will be visible to user, returned intact to onEditorResult
     * @param value Value to be edited
     */
    public void launchEditor(final String key, Object value) {
        launchEditor(key, key, value);
    }

    /**
     * Start an editor or show Toast message if no applicable editor is available
     *
     * Results are returned to {@link EditorLauncherCallback#onEditorResult(String, Object) your onEditorResult method}
     *
     * @param key Key, returned to onEditorResult
     * @param title Title that may be displayed on editor
     * @param value Value to be edited
     */
    public void launchEditor(final String key, String title, Object value) {
        launchEditor(key, title, value, null);
    }

    /**
     * Start an editor or show Toast message if no applicable editor is available
     *
     * Results are returned to {@link EditorLauncherCallback#onEditorResult(String, Object) your onEditorResult method}
     *
     * @param key Key, returned to onEditorResult
     * @param title Title that may be displayed on editor
     * @param value Value to be edited
     * @param type Type of value
     */
    public void launchEditor(final String key, String title, Object value, Class<?> type) {
        // Create new if value is null
        if (value == null && type != null) {
            launchEditorForNew(key, new SandboxedType(type));
            return;
        }

        // Find applicable editor
        for (Editor editor : EDITOR_REGISTRY) {
            if (editor.canEdit(value)) {
                if (editor instanceof Editor.EditorActivity) {
                    // Editor in activity
                    Intent editorIntent = ((Editor.EditorActivity) editor).getEditorIntent(mHelperFragment.getActivity());
                    mHelperFragment.startEditorInActivity(editorIntent, key, value);
                } else if (editor instanceof Editor.DialogFragmentEditor) {
                    // Editor in DialogFragment
                    ValueEditorDialogFragment d = ((Editor.DialogFragmentEditor) editor).getEditorDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(ValueEditorDialogFragment.EXTRA_KEY, key);
                    args.putString(Editor.EXTRA_TITLE, title);
                    BundleAdapter.putInBundle(args, Editor.EXTRA_VALUE, value);
                    d.setArguments(args);
                    d.setTargetFragment(mHelperFragment, 0);
                    d.show(mHelperFragment.getActivity().getSupportFragmentManager(), "DFEditorFor" + key + Math.random());
                } else if (editor instanceof Editor.FragmentEditor) {
                    Bundle args = new Bundle();
                    args.putString(SingleEditorActivity.EXTRA_ECHOED_KEY, key);
                    args.putParcelable(ValueEditorFragment.ARG_EDITED_OBJECT, new SandboxedObject(value));
                    openEditorFragment(((Editor.FragmentEditor) editor).getEditorFragment(), args);
                } else if (editor instanceof Editor.InPlaceValueToggler) {
                    // In place value toggler (eg. for Boolean)
                    Object newValue = ((Editor.InPlaceValueToggler) editor).toggleObjectValue(value);
                    getCallback().onEditorResult(key, newValue);
                }
                return;
            }
        }
        Toast.makeText(mHelperFragment.getActivity(), R.string.type_unsupported, Toast.LENGTH_SHORT).show();
    }

    public void launchEditorForNew(String key, SandboxedType type) {
        CreateNewDialog d = new CreateNewDialog();
        Bundle args = new Bundle();
        args.putString(ValueEditorDialogFragment.EXTRA_KEY, key);
        args.putParcelable(CreateNewDialog.ARG_SANDBOXED_TYPE, type);
        args.putBoolean(CreateNewDialog.ARG_ALLOW_SANDBOX, false);
        d.setArguments(args);
        d.setTargetFragment(mHelperFragment, 0);
        d.show(mHelperFragment.getActivity().getSupportFragmentManager(), "DFNewFor" + key);
    }

    /**
     * Start an editor or show Toast message if no applicable editor is available
     *
     * Results are returned to {@link EditorLauncherWithSandboxCallback#onSandboxedEditorResult(String, com.github.michalbednarski.intentslab.sandbox.SandboxedObject)}
     *
     * @param key Key, returned to onEditorResult
     * @param title Title that may be displayed on editor
     * @param wrappedValue Wrapped value to be edited
     */
    public void launchEditorForSandboxedObject(final String key, String title, SandboxedObject wrappedValue) {
unwrap: {
            Object unwrapped;
            try {
                unwrapped = wrappedValue.unwrap(null);
            } catch (Throwable e) {
                break unwrap;
            }
            launchEditor(key, title, unwrapped);
            return;
        }

        Bundle args = new Bundle();
        args.putString(SingleEditorActivity.EXTRA_ECHOED_KEY, key);
        args.putParcelable(ValueEditorFragment.ARG_EDITED_OBJECT, wrappedValue);
        openEditorFragment(ObjectEditorFragment.class, args);
    }

    void openEditorFragment(Class<? extends ValueEditorFragment> editorFragment, Bundle args) {
        args.putString(SingleEditorActivity.EXTRA_FRAGMENT_CLASS_NAME, editorFragment.getName());
        Intent intent = new Intent(mHelperFragment.getActivity(), SingleEditorActivity.class);
        intent.replaceExtras(args);
        ChildFragmentWorkaround.startActivityForResultFromFragment(mHelperFragment, intent, REQUEST_CODE_INTERNAL_EDITOR);
    }

    /**
     * Extra info about started activity
     */
    private static class ActivityRequestInfo {
        String key;
        boolean isSandboxed;
        Class requestedType;

        ActivityRequestInfo(String key, boolean isSandboxed) {
            this.key = key;
            this.isSandboxed = isSandboxed;
        }

        public ActivityRequestInfo(String key, Class requestedType) {
            this.key = key;
            this.requestedType = requestedType;
        }

        Object deepCastIfNeeded(Object object) {
            if (object instanceof Object[] && requestedType != null && requestedType.isArray() &&
                    !requestedType.getComponentType().isPrimitive()) {
                return Utils.deepCastArray((Object[]) object, requestedType);
            }
            return object;
        }

        // Pseudo parcelable
        ActivityRequestInfo(Parcel source) {
            key = source.readString();
            isSandboxed = source.readInt() != 0;
            String requestedTypeName = source.readString();
            if (requestedTypeName != null) {
                try {
                    requestedType = Class.forName(requestedTypeName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void writeToParcel(Parcel dest) {
            dest.writeString(key);
            dest.writeInt(isSandboxed ? 1 : 0);
            dest.writeString(requestedType != null ? requestedType.getName() : null);
        }
    }


    /**
     * Parcelable class managing mapping of activity request codes and key passed to launchEditor
     */
    private static class EditorLauncherState implements Parcelable {
        private int nextFreeRequestCode = 1;
        private SparseArray<ActivityRequestInfo> requestInfos = new SparseArray<ActivityRequestInfo>();

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            final int count = requestInfos.size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                dest.writeInt(requestInfos.keyAt(0));
                requestInfos.valueAt(0).writeToParcel(dest);
            }
        }

        public static final Creator<EditorLauncherState> CREATOR = new Creator<EditorLauncherState>() {
            @Override
            public EditorLauncherState createFromParcel(Parcel source) {
                final EditorLauncherState editorLauncherState = new EditorLauncherState();
                int count = source.readInt();
                int tmpNextFreeRequestCode = 1;
                for (int i = 0; i < count; i++) {
                    int requestCode = source.readInt();
                    if (requestCode >= tmpNextFreeRequestCode) {
                        tmpNextFreeRequestCode = requestCode + 1;
                    }
                    editorLauncherState.requestInfos.put(requestCode, new ActivityRequestInfo(source));
                }
                editorLauncherState.nextFreeRequestCode = tmpNextFreeRequestCode;
                return editorLauncherState;
            }

            @Override
            public EditorLauncherState[] newArray(int size) {
                return new EditorLauncherState[size];
            }
        };

        int saveRequestInfoAndGetCode(ActivityRequestInfo key) {
            int requestCode = nextFreeRequestCode++;
            requestInfos.put(requestCode, key);
            return requestCode;
        }

        ActivityRequestInfo getRequestInfoAndReleaseCode(int requestCode) {
            final ActivityRequestInfo requestInfo = requestInfos.get(requestCode);
            requestInfos.remove(requestCode);
            return requestInfo;
        }
    }

    /**
     * Fragment used for activity handling and retaining state
     * Added as child fragment of EditorLauncher owner fragment
     */
    public static class HelperFragment extends Fragment {
        public HelperFragment() {}
        private EditorLauncher mEditorLauncher;
        private EditorLauncherState mState;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_CODE_INTERNAL_EDITOR) {
                if (resultCode == Activity.RESULT_OK) {
                    final String key = data.getStringExtra(SingleEditorActivity.EXTRA_ECHOED_KEY);
                    final SandboxedObject sandboxedObject = data.getParcelableExtra(SingleEditorActivity.EXTRA_RESULT);
                    EditorLauncherCallback callback = mEditorLauncher.getCallback();
                    if (callback instanceof EditorLauncherWithSandboxCallback) {
                        ((EditorLauncherWithSandboxCallback) callback).onSandboxedEditorResult(key, sandboxedObject);
                    } else {
                        callback.onEditorResult(key, sandboxedObject.unwrap(null));
                    }
                }
            } else if (requestCode >= 1 && requestCode <= 0xffff) {
                final ActivityRequestInfo requestInfo = mState.getRequestInfoAndReleaseCode(requestCode);
                final String key = requestInfo.key;
                if (data != null && data.getExtras() != null) {
                    Object newValue = data.getExtras().get(Editor.EXTRA_VALUE);
                    if (requestInfo.isSandboxed) {
                        ((EditorLauncherWithSandboxCallback) mEditorLauncher.getCallback())
                                .onSandboxedEditorResult(key, (SandboxedObject) newValue);
                    } else {
                        mEditorLauncher.getCallback().onEditorResult(key, requestInfo.deepCastIfNeeded(newValue));
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        void startEditorInActivity(Intent baseEditorIntent, String key, Object value) {
            Bundle extras = new Bundle(1);
            BundleAdapter.putInBundle(extras, Editor.EXTRA_VALUE, value);
            int requestCode = mState.saveRequestInfoAndGetCode(new ActivityRequestInfo(key, value.getClass()));
            ChildFragmentWorkaround.startActivityForResultFromFragment(this, baseEditorIntent.putExtras(extras), requestCode);
        }

        void handleDialogResponse(String key, Object newValue) {
            mEditorLauncher.getCallback().onEditorResult(key, newValue);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putParcelable(STATE_EDITOR_LAUNCHER_STATE, mState);
            Utils.putLiveRefInBundle(outState, STATE_EDITOR_LAUNCHER_INSTANCE, mEditorLauncher);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                mState = new EditorLauncherState();
            } else {
                mState = savedInstanceState.getParcelable(STATE_EDITOR_LAUNCHER_STATE);
                if (mEditorLauncher == null) {
                    mEditorLauncher = Utils.getLiveRefFromBundle(savedInstanceState, STATE_EDITOR_LAUNCHER_INSTANCE);
                    if (mEditorLauncher == null) {
                        mEditorLauncher = sPendingInitializations.get(getParentFragment());
                    }
                    if (mEditorLauncher != null) {
                        mEditorLauncher.mHelperFragment = this;
                    }
                }
            }
        }
    }

    private static final String STATE_EDITOR_LAUNCHER_STATE = "EditorLauncher.internal.genericState";
    private static final String STATE_EDITOR_LAUNCHER_INSTANCE = "EditorLauncher.internal.this";

    private static final String TAG_HELPER_FRAGMENT = "EditorLauncher.HF";

}
