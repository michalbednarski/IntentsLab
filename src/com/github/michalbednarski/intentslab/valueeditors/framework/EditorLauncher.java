package com.github.michalbednarski.intentslab.valueeditors.framework;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.valueeditors.BundleEditorActivity;
import com.github.michalbednarski.intentslab.valueeditors.EnumEditor;
import com.github.michalbednarski.intentslab.valueeditors.ParcelableStructureEditorActivity;
import com.github.michalbednarski.intentslab.valueeditors.StringLikeItemEditor;

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
            new Editor.EditorActivity() {

                @Override
                public Intent getEditorIntent(Context context) {
                    return new Intent(context, BundleEditorActivity.class);
                }

                @Override
                public boolean canEdit(Object value) {
                    return value instanceof Bundle;
                }
            },

            // Intent editor
            new IntentEditorActivity.LaunchableEditor(),

            // Enum editor
            new EnumEditor.LaunchableEditor(),

            // Generic Parcelable structure editor
            new ParcelableStructureEditorActivity.LaunchableEditor()
    };

    /**
     * EditorLauncher Callback
     *
     * This is interface implemented by caller of launchEditor
     * returning data after edit, passed to constructor
     */
    public interface EditorLauncherCallback {
        void onEditorResult(String key, Object newValue);
    }

    /**
     * EditorLauncher Callback with support for {@link #launchEditorForSandboxedObject(String, String, android.os.Bundle)}
     *
     * This is interface implemented by caller of launchEditor
     * returning data after edit, passed to constructor
     */
    public interface EditorLauncherWithSandboxCallback extends EditorLauncherCallback {
        void onSandboxedEditorResult(String key, Bundle newWrappedValue);
    }


    private final ActivityHandlingHeadlessFragment mFragment;

    //private final ActivityResultHandler mActivityResultHandler;
    private EditorLauncherCallback mEditorLauncherCallback = null;


    /**
     * Constructor
     *
     * Note: creating this object immediately triggers pending calls to onEditorResult's
     *       Only use this when you are ready to receive them
     *
     */
    public EditorLauncher(FragmentActivity fragmentActivity, String tag) {
        final android.support.v4.app.FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
        ActivityHandlingHeadlessFragment fragment = (ActivityHandlingHeadlessFragment) fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = new ActivityHandlingHeadlessFragment();
            fragmentManager.beginTransaction().add(fragment, tag).commit();
        }
        fragment.mEditorLauncher = this;
        mFragment = fragment;
    }

    public void setCallback(EditorLauncherCallback callback) {
        mEditorLauncherCallback = callback;
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
        for (Editor editor : EDITOR_REGISTRY) {
            if (editor.canEdit(value)) {
                if (editor instanceof Editor.EditorActivity) {
                    // Editor in activity
                    Intent editorIntent = ((Editor.EditorActivity) editor).getEditorIntent(mFragment.getActivity());
                    mFragment.startEditorInActivity(editorIntent, key, value);
                } else if (editor instanceof Editor.DialogFragmentEditor) {
                    // Editor in DialogFragment
                    ValueEditorDialogFragment d = ((Editor.DialogFragmentEditor) editor).getEditorDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(ValueEditorDialogFragment.EXTRA_KEY, key);
                    args.putString(Editor.EXTRA_TITLE, title);
                    BundleAdapter.putInBundle(args, Editor.EXTRA_VALUE, value);
                    d.setArguments(args);
                    d.setTargetFragment(mFragment, 0);
                    d.show(mFragment.getActivity().getSupportFragmentManager(), "DFEditorFor" + key);
                } else if (editor instanceof Editor.InPlaceValueToggler) {
                    // In place value toggler (eg. for Boolean)
                    Object newValue = ((Editor.InPlaceValueToggler) editor).toggleObjectValue(value);
                    mEditorLauncherCallback.onEditorResult(key, newValue);
                }
                return;
            }
        }
        Toast.makeText(mFragment.getActivity(), R.string.type_unsupported, Toast.LENGTH_SHORT).show();
    }

    /**
     * Start an editor or show Toast message if no applicable editor is available
     *
     * Results are returned to {@link EditorLauncherWithSandboxCallback#onSandboxedEditorResult(String, android.os.Bundle)}
     *
     * @param key Key, returned to onEditorResult
     * @param title Title that may be displayed on editor
     * @param wrappedValue Value to be edited wrapped by {@link com.github.michalbednarski.intentslab.sandbox.SandboxManager#wrapObject(Object)}
     */
    public void launchEditorForSandboxedObject(final String key, String title, Bundle wrappedValue) {
        // Build intent
        Intent editorIntent = new Intent(mFragment.getActivity(), ParcelableStructureEditorActivity.class);
        editorIntent.putExtra(Editor.EXTRA_TITLE, title);
        editorIntent.putExtra(Editor.EXTRA_VALUE, wrappedValue);
        editorIntent.putExtra(ParcelableStructureEditorActivity.EXTRA_VALUE_IS_SANDBOXED, true);

        // Register callback and start for result
        final int requestCode = mFragment.mState.saveRequestInfoAndGetCode(new ActivityRequestInfo(key, true));
        mFragment.startActivityForResult(editorIntent, requestCode);
    }

    /**
     * Extra info about started activity
     */
    private static class ActivityRequestInfo {
        String key;
        boolean isSandboxed;

        ActivityRequestInfo(String key, boolean isSandboxed) {
            this.key = key;
            this.isSandboxed = isSandboxed;
        }

        // Pseudo parcelable
        ActivityRequestInfo(Parcel source) {
            key = source.readString();
            isSandboxed = source.readInt() != 0;
        }

        void writeToParcel(Parcel dest) {
            dest.writeString(key);
            dest.writeInt(isSandboxed ? 1 : 0);
        }
    }


    /**
     * Parcelable class managing mapping of activity request codes and key passed to launchEditor
     */
    private static class EditorLauncherState implements Parcelable {
        private int nextFreeRequestCode = 0;
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
                int tmpNextFreeRequestCode = 0;
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
     */
    public static class ActivityHandlingHeadlessFragment extends Fragment {
        public ActivityHandlingHeadlessFragment() {}
        private EditorLauncher mEditorLauncher;
        private EditorLauncherState mState;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode >= 0 && requestCode <= 0xffff) {
                final ActivityRequestInfo requestInfo = mState.getRequestInfoAndReleaseCode(requestCode);
                final String key = requestInfo.key;
                if (data != null && data.getExtras() != null) {
                    Object newValue = data.getExtras().get(Editor.EXTRA_VALUE);
                    if (requestInfo.isSandboxed) {
                        ((EditorLauncherWithSandboxCallback) mEditorLauncher.mEditorLauncherCallback)
                                .onSandboxedEditorResult(key, (Bundle) newValue);
                    } else {
                        mEditorLauncher.mEditorLauncherCallback.onEditorResult(key, newValue);
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        void startEditorInActivity(Intent baseEditorIntent, String key, Object value) {
            Bundle extras = new Bundle(1);
            BundleAdapter.putInBundle(extras, Editor.EXTRA_VALUE, value);
            int requestCode = mState.saveRequestInfoAndGetCode(new ActivityRequestInfo(key, false));
            startActivityForResult(baseEditorIntent.putExtras(extras), requestCode);
        }

        void handleDialogResponse(String key, Object newValue) {
            mEditorLauncher.mEditorLauncherCallback.onEditorResult(key, newValue);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putParcelable(STATE_EDITOR_LAUNCHER_STATE, mState);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                mState = new EditorLauncherState();
            } else {
                mState = savedInstanceState.getParcelable(STATE_EDITOR_LAUNCHER_STATE);
            }
        }
    }

    private static final String STATE_EDITOR_LAUNCHER_STATE = "EditorLauncher.internal.genericState";

}
