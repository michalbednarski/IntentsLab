package com.github.michalbednarski.intentslab.valueeditors;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.editor.BundleAdapter;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;

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




    private final Fragment mFragment;

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
     * Results are returned to {@link EditorLauncherCallback#onEditorResult(String, Object) your onEditorResult method}
     *
     * @param key Key, will be visible to user, returned intact to onEditorResult
     * @param value Value to be edited
     */
    public void launchEditor(final String key, Object value) {
        for (Editor editor : EDITOR_REGISTRY) {
            if (editor.canEdit(value)) {
                if (editor instanceof Editor.EditorActivity) {
                    // Editor in activity
                    Intent editorIntent = ((Editor.EditorActivity) editor).getEditorIntent(mFragment.getActivity());
                    Bundle untypedExtras = new Bundle();
                    BundleAdapter.putInBundle(untypedExtras, Editor.EXTRA_VALUE, value);
                    editorIntent.putExtras(untypedExtras);
                    editorIntent.putExtra(Editor.EXTRA_KEY, key);
                    mFragment.startActivityForResult(editorIntent, REQUEST_CODE_EDITOR_LAUNCHER_HEADLESS_FRAGMENT);
                } else if (editor instanceof Editor.DialogFragmentEditor) {
                    // Editor in DialogFragment
                    ValueEditorDialogFragment d = ((Editor.DialogFragmentEditor) editor).getEditorDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(Editor.EXTRA_KEY, key);
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



    public void handleActivityResult(Intent resultIntent) {
        if (resultIntent == null) {
            return;
        }
        String key = resultIntent.getStringExtra(Editor.EXTRA_KEY);
        if (key == null) {
            throw new RuntimeException("EXTRA_KEY is null");
        }
        Bundle extras = resultIntent.getExtras();
        assert extras != null;
        Object value = extras.get(Editor.EXTRA_VALUE);
        mEditorLauncherCallback.onEditorResult(key, value);
    }

    public static class ActivityHandlingHeadlessFragment extends Fragment {
        public ActivityHandlingHeadlessFragment() {}
        private EditorLauncher mEditorLauncher;

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_CODE_EDITOR_LAUNCHER_HEADLESS_FRAGMENT) {
                mEditorLauncher.handleActivityResult(data);
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        void handleDialogResponse(String key, Object newValue) {
            mEditorLauncher.mEditorLauncherCallback.onEditorResult(key, newValue);
        }
    }

    private static final int REQUEST_CODE_EDITOR_LAUNCHER_HEADLESS_FRAGMENT = 1;

}
