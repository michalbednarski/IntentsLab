package com.example.testapp1.valueeditors;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.editor.BundleAdapter;
import com.example.testapp1.editor.IntentEditorActivity;
import com.example.testapp1.editor.StringLikeItemEditor;

import java.util.ArrayList;

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

    /**
     * Class passing activity result to EditorLauncherCallback
     *
     * Must be used by activity using EditorLauncher
     */
    public static class ActivityResultHandler {
        private final Activity mActivity;
        private final int  mActivityResultId;
        private ArrayList<Intent> mPendingResultIntents = null;
        private EditorLauncher mEditorLauncher = null;

        /**
         * @param activity Activity which is used for startActivityForResult
         * @param activityResultId requestCode for {@link Activity#startActivityForResult(Intent, int)}
         */
        public ActivityResultHandler(Activity activity, int activityResultId) {
            mActivity = activity;
            mActivityResultId = activityResultId;
        }

        /**
         * Must be called from {@link Activity#onActivityResult(int, int, android.content.Intent)}
         * when requestCode is equal to one passed to constructor
         */
        public void handleActivityResult(Intent resultIntent) {
            if (resultIntent != null) {
                if (mEditorLauncher != null) {
                    mEditorLauncher.doHandleActivityResult(resultIntent);
                } else {
                    if (mPendingResultIntents == null) {
                        mPendingResultIntents = new ArrayList<Intent>();
                    }
                    mPendingResultIntents.add(resultIntent);
                }
            }
        }
    }






    private final ActivityResultHandler mActivityResultHandler;
    private final EditorLauncherCallback mEditorLauncherCallback;


    /**
     * Constructor
     *
     * Note: creating this object immediately triggers pending calls to onEditorResult's
     *       Only use this when you are ready to receive them
     *
     * @param activityResultHandler ActivityResultHandler maintained by activity for using startActivityForResult and onActivityResult
     * @param editorLauncherCallback Listener for editing results
     */
    public EditorLauncher(ActivityResultHandler activityResultHandler, EditorLauncherCallback editorLauncherCallback) {
        mActivityResultHandler = activityResultHandler;
        mEditorLauncherCallback = editorLauncherCallback;

        activityResultHandler.mEditorLauncher = this;

        // Handle pending result intents
        if (activityResultHandler.mPendingResultIntents != null) {
            for (Intent pendingResultIntent : activityResultHandler.mPendingResultIntents) {
                doHandleActivityResult(pendingResultIntent);
            }
            activityResultHandler.mPendingResultIntents = null;
        }
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
        Activity activity = mActivityResultHandler.mActivity;
        for (Editor editor : EDITOR_REGISTRY) {
            if (editor.canEdit(value)) {
                if (editor instanceof Editor.EditorActivity) {
                    Intent editorIntent = ((Editor.EditorActivity) editor).getEditorIntent(activity);
                    Bundle untypedExtras = new Bundle();
                    BundleAdapter.putInBundle(untypedExtras, Editor.EXTRA_VALUE, value);
                    editorIntent.putExtras(untypedExtras);
                    editorIntent.putExtra(Editor.EXTRA_KEY, key);
                    activity.startActivityForResult(editorIntent, mActivityResultHandler.mActivityResultId);
                } else {
                    editor.edit(key, value, new Editor.EditorCallback() {
                        @Override
                        public void sendEditorResult(Object newValue) {
                            mEditorLauncherCallback.onEditorResult(key, newValue);
                        }
                    }, activity);
                }
                return;
            }
        }
        Toast.makeText(activity, R.string.type_unsupported, Toast.LENGTH_SHORT).show();
    }



    private void doHandleActivityResult(Intent resultIntent) {
        String key = resultIntent.getStringExtra(Editor.EXTRA_KEY);
        if (key == null) {
            throw new RuntimeException("EXTRA_KEY is null");
        }
        Bundle extras = resultIntent.getExtras();
        assert extras != null;
        Object value = extras.get(Editor.EXTRA_VALUE);
        mEditorLauncherCallback.onEditorResult(key, value);
    }
}
