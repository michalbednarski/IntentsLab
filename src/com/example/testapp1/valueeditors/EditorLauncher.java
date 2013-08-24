package com.example.testapp1.valueeditors;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.editor.BundleAdapter;
import com.example.testapp1.editor.IntentEditorActivity;
import com.example.testapp1.editor.StringLikeItemEditor;

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
            new Editor() {
                @Override
                public boolean canEdit(Object value) {
                    return value instanceof Boolean;
                }

                @Override
                public void edit(String key, Object value, EditorCallback editorCallback, Context context) {
                    editorCallback.sendEditorResult(!((Boolean) value));
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
            new EnumEditor(),

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
                    Intent editorIntent = ((Editor.EditorActivity) editor).getEditorIntent(mFragment.getActivity());
                    Bundle untypedExtras = new Bundle();
                    BundleAdapter.putInBundle(untypedExtras, Editor.EXTRA_VALUE, value);
                    editorIntent.putExtras(untypedExtras);
                    editorIntent.putExtra(Editor.EXTRA_KEY, key);
                    mFragment.startActivityForResult(editorIntent, REQUEST_CODE_EDITOR_LAUNCHER_HEADLESS_FRAGMENT);
                } else {
                    editor.edit(key, value, new Editor.EditorCallback() {
                        @Override
                        public void sendEditorResult(Object newValue) {
                            mEditorLauncherCallback.onEditorResult(key, newValue);
                        }
                    }, mFragment.getActivity());
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
    }

    private static final int REQUEST_CODE_EDITOR_LAUNCHER_HEADLESS_FRAGMENT = 1;

}
