package com.example.testapp1.editor;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.SavedItemsDatabase;
import com.example.testapp1.Utils;
import com.example.testapp1.browser.ComponentInfoActivity;
import com.example.testapp1.browser.ExtendedPackageInfo;
import com.example.testapp1.runas.IRemoteInterface;
import com.example.testapp1.runas.RunAsManager;
import com.example.testapp1.valueeditors.Editor;

import java.util.ArrayList;

/**
 * Intent editor activity
 */
public class IntentEditorActivity extends FragmentTabsActivity/*FragmentActivity*/ {
    private static final String TAG = "IntentEditor";
    private static final int REQUEST_CODE_TEST_STARTACTIVITYFORRESULT = 657;
    private static final int REQUEST_CODE_RESULT_INTENT_EDITOR = 754;

    public static final String EXTRA_COMPONENT_TYPE = "componentType_";
    public static final String EXTRA_METHOD_ID = "intents_lab.intent_editor.methodId";
    public static final String EXTRA_INTENT_FILTERS = "intentFilters_";
    private static final String EXTRA_FORWARD_ABLE_RESULT = "intents_lab.intent_editor.internal.forward_result";
    private static final String EXTRA_FORWARD_RESULT_CODE = "intents_lab.intent_editor.internal.forward_result.code";
    private static final String EXTRA_FORWARD_RESULT_INTENT = "intents_lab.intent_editor.internal.forward_result.intent";

    //private BundleAdapter mExtrasAdapter;

    ArrayList<IntentEditorPanel> loadedPanels = new ArrayList<IntentEditorPanel>(3);

    private Intent mEditedIntent;
    private int mComponentType;
    private int mMethodId;
    private IntentFilter[] mAttachedIntentFilters = null;

    private boolean mGenericEditorMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load intent
        Parcelable[] uncastedIntentFilters = null;
        if (savedInstanceState != null) {
            // Saved instance state
            mEditedIntent = savedInstanceState.getParcelable("intent");
            mComponentType = savedInstanceState.getInt("componentType");
            mMethodId = savedInstanceState.getInt("methodId");
            uncastedIntentFilters = savedInstanceState.getParcelableArray("intentFilters");
        } else if (isInterceptedIntent()) {
            // Intercept
            mEditedIntent = getIntent();
            mEditedIntent.setComponent(null);
            mComponentType = IntentEditorConstants.ACTIVITY;
            mMethodId =
                    getCallingPackage() != null ?
                            IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITYFORRESULT :
                            IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITY;
        } else if (getIntent().hasExtra(Editor.EXTRA_VALUE)) {
            mEditedIntent = getIntent().getParcelableExtra(Editor.EXTRA_VALUE);
            mGenericEditorMode = true;
        } else {
            // Start of editor
            mEditedIntent = getIntent().getParcelableExtra("intent");
            mComponentType = getIntent().getIntExtra(EXTRA_COMPONENT_TYPE, IntentEditorConstants.ACTIVITY);
            mMethodId = getIntent().getIntExtra(EXTRA_METHOD_ID, 0);
            uncastedIntentFilters = getIntent().getParcelableArrayExtra(EXTRA_INTENT_FILTERS);
            if (mEditedIntent == null) {
                mEditedIntent = new Intent();
            }
        }

        // Manually cast array of intent filters
        if (uncastedIntentFilters != null) {
            try {
                mAttachedIntentFilters = new IntentFilter[uncastedIntentFilters.length];
                for (int i = 0; i < uncastedIntentFilters.length; i++) {
                    mAttachedIntentFilters[i] = (IntentFilter) uncastedIntentFilters[i];
                }
            } catch (ClassCastException e) {
                Log.w(TAG, "Invalid intent filters");
                mAttachedIntentFilters = null;
            }
        }

        // Setup tabs
        if (getResources().getBoolean(R.bool.merge_general_and_extras_tabs)) {
            addTab(getString(R.string.general), new IntentGeneralWithExtrasFragment());
        } else {
            addTab(getString(R.string.general), new IntentGeneralFragment());
            addTab(getString(R.string.intent_extras), new IntentExtrasFragment());
        }
        addTab("Flags", new IntentFlagsFragment());


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateIntent();
        outState.putParcelable("intent", mEditedIntent);
        outState.putInt("componentType", mComponentType);
        outState.putInt("methodId", mMethodId);
        outState.putParcelableArray("intentFilters", mAttachedIntentFilters);
        /*if (mTabsHelper != null) {
			outState.putInt("tab", mTabsHelper.getCurrentView());
		}*/
        // TODO: current tab?
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_intent_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        {
            MenuItem runIntentOption = menu.findItem(R.id.menu_run_intent);
            if (mComponentType == IntentEditorConstants.RESULT) {
                if (getIntent().getBooleanExtra(EXTRA_FORWARD_ABLE_RESULT, false)) {
                    runIntentOption
                            .setVisible(true)
                            .setTitle("setResult(); finish()");
                } else {
                    runIntentOption.setVisible(false);
                }
            } else {
                runIntentOption
                    .setVisible(true)
                    .setTitle(IntentGeneralFragment.getMethodNamesArray(getResources(), mComponentType)[mMethodId]);
            }
        }
        menu.findItem(R.id.set_editor_result)
                .setVisible(mGenericEditorMode);
        menu.findItem(R.id.detach_intent_filter)
                .setVisible(mAttachedIntentFilters != null);
        menu.findItem(R.id.component_info)
                .setVisible(mEditedIntent.getComponent() != null);

        // "Disable interception" option
        menu.findItem(R.id.disable_interception)
                .setVisible(isInterceptedIntent())
                .setEnabled(
                        isInterceptedIntent() &&
                        getPackageManager().getComponentEnabledSetting(
                                new ComponentName(this, IntentEditorInterceptedActivity.class)
                        ) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_run_intent:
                runIntent();
                return true;
            case R.id.set_editor_result:
                updateIntent();
                setResult(
                        0,
                        new Intent()
                        .putExtra(Editor.EXTRA_KEY, getIntent().getStringExtra(Editor.EXTRA_KEY))
                        .putExtra(Editor.EXTRA_VALUE, mEditedIntent)
                );
                finish();
                return true;
            case R.id.attach_intent_filter:
                updateIntent();
                if (mEditedIntent.getComponent() != null) {
                    // We have specified component, just find IntentFilters for it
                    final ComponentName componentName = mEditedIntent.getComponent();
                    final ExtendedPackageInfo extendedPackageInfo = new ExtendedPackageInfo(this, componentName.getPackageName());
                    extendedPackageInfo.runWhenReady(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                setAttachedIntentFilters(extendedPackageInfo.getComponentInfo(componentName.getClassName()).intentFilters);
                                Toast.makeText(IntentEditorActivity.this, R.string.intent_filter_attached, Toast.LENGTH_SHORT).show();
                            } catch (NullPointerException e) {
                                Toast.makeText(IntentEditorActivity.this, R.string.no_intent_filters_found, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    // Otherwise show dialog for selecting app
                    (new AttachIntentFilterDialog()).show(getSupportFragmentManager(), "attach_intent_filter_dialog");
                }

                return true;
            case R.id.detach_intent_filter:
                clearAttachedIntentFilters();
                return true;
            case R.id.component_info: {
                ComponentName component = mEditedIntent.getComponent();
                startActivity(
                        new Intent(this, ComponentInfoActivity.class)
                                .putExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME, component.getPackageName())
                                .putExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME, component.getClassName())
                                .putExtra(ComponentInfoActivity.EXTRA_LAUNCHED_FROM_INTENT_EDITOR, true)
                );
            }
            return true;
            case R.id.save:{
                updateIntent();
                SavedItemsDatabase.getInstance(this).saveIntent(this, mEditedIntent, mComponentType, mMethodId);
            }
            return true;
            case R.id.disable_interception:
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(this, IntentEditorInterceptedActivity.class),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                );
                Toast.makeText(this, R.string.interception_disabled, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void safelyInvalidateOptionsMenu() {
        try {
            invalidateOptionsMenu();
        } catch (NoSuchMethodError ignored) {
        }
    }

    public void updateIntent() {
        for (IntentEditorPanel panel : loadedPanels) {
            panel.updateEditedIntent(mEditedIntent);
        }
    }


    int getComponentType() {
        return mComponentType;
    }

    void setComponentType(int newComponentType) {
        mComponentType = newComponentType;
        for (IntentEditorPanel panel : loadedPanels) {
            panel.onComponentTypeChanged(newComponentType);
        }
        safelyInvalidateOptionsMenu();
    }

    int getMethodId() {
        return mMethodId;
    }

    void setMethodId(int newMethodId) {
        mMethodId = newMethodId;
        safelyInvalidateOptionsMenu();
    }

    void clearAttachedIntentFilters() {
        setAttachedIntentFilters(null);
    }

    void setAttachedIntentFilters(IntentFilter[] intentFilters) {
        updateIntent();
        mAttachedIntentFilters = intentFilters;
        for (IntentEditorPanel panel : loadedPanels) {
            panel.onIntentFiltersChanged(null);
        }
        safelyInvalidateOptionsMenu();
    }

    void setComponentName(ComponentName componentName) {
        mEditedIntent.setComponent(componentName);
        for (IntentEditorPanel panel : loadedPanels) {
            if (panel instanceof IntentGeneralFragment) {
                ((IntentGeneralFragment) panel).updateComponent();
            }
            panel.onIntentFiltersChanged(null);
        }
    }

    public void runIntent() {
        updateIntent();
        IRemoteInterface remoteInterface = RunAsManager.getSelectedRemoteInterface();
        try {
            switch (getComponentType()) {

                case IntentEditorConstants.ACTIVITY:
                    switch (getMethodId()) {
                        case IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITY:
                            if (remoteInterface != null) {
                                remoteInterface.startActivity(mEditedIntent);
                            } else {
                                startActivity(mEditedIntent);
                            }
                            break;
                        case IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITYFORRESULT:
                            startActivityForResult(mEditedIntent, REQUEST_CODE_TEST_STARTACTIVITYFORRESULT);
                            break;
                    }
                    break;


                case IntentEditorConstants.BROADCAST:
                    switch (getMethodId()) {
                        case IntentEditorConstants.BROADCAST_METHOD_SENDBROADCAST:
                            sendBroadcast(mEditedIntent);
                            break;
                        case IntentEditorConstants.BROADCAST_METHOD_SENDORDEREDBROADCAST: {
                            sendOrderedBroadcast(
                                    mEditedIntent, // intent
                                    null, // permission
                                    new BroadcastReceiver() { // resultReceiver
                                        @Override
                                        public void onReceive(Context context, Intent intent) {
                                            Bundle resultExtras = getResultExtras(false);
                                            new AlertDialog.Builder(IntentEditorActivity.this)
                                                .setMessage(
                                                        getString(R.string.received_broadcast_result) +
                                                        "\ngetResultCode() = " + getResultCode() +
                                                        "\ngetResultData() = " + getResultData() +
                                                        "\ngetResultExtras() = " +
                                                                (resultExtras == null ? "null" :
                                                                 resultExtras.isEmpty() ? "[Empty Bundle]" : "[Bundle]")

                                                ).setPositiveButton("OK", null)
                                                .show();
                                        }
                                    },
                                    null, // scheduler
                                    0, // initialCode
                                    null, // initialData
                                    null // initialExtras
                            );
                        }
                        break;
                        case IntentEditorConstants.BROADCAST_METHOD_SENDSTICKYBROADCAST:
                            sendStickyBroadcast(mEditedIntent);
                            break;
                    }
                    break;


                case IntentEditorConstants.SERVICE:
                    switch (getMethodId()) {
                        case IntentEditorConstants.SERVICE_METHOD_STARTSERVICE:
                            startService(mEditedIntent);
                            break;
                        default:
                            // TODO runIntent bindService
                            Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case IntentEditorConstants.RESULT:
                    setResult(
                            0,
                            new Intent()
                            .putExtra(EXTRA_FORWARD_RESULT_CODE, mMethodId)
                            .putExtra(EXTRA_FORWARD_RESULT_INTENT, mEditedIntent)
                    );
                    finish();
                    break;
            }
        } catch (Exception exception) {
            Utils.toastException(this, exception);
        }
    }


    // RESULT
    @Override
    protected void onActivityResult(int requestCode, final int resultCode,
                                    final Intent resultIntent) {
        if (requestCode == REQUEST_CODE_TEST_STARTACTIVITYFORRESULT) {
            // Result of tested startActivityForResult
            if (resultIntent == null) {
                Toast.makeText(this, getString(R.string.startactivityforresult_no_result, resultCode), Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.startactivityforresult_got_result));

                if (isInterceptedIntent() && (getCallingPackage() != null)) {
                    alertBuilder
                    .setPositiveButton(getString(R.string.edit_intercepted_result), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(
                                    new Intent(IntentEditorActivity.this, IntentEditorActivity.class)
                                            .putExtra("intent", resultIntent)
                                            .putExtra(EXTRA_COMPONENT_TYPE, IntentEditorConstants.RESULT)
                                            .putExtra(EXTRA_METHOD_ID, resultCode)
                                            .putExtra(EXTRA_FORWARD_ABLE_RESULT, true),
                                    REQUEST_CODE_RESULT_INTENT_EDITOR
                            );
                        }
                    })
                    .setNeutralButton(getString(R.string.forward_intercepted_result), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(resultCode, resultIntent);
                            finish();
                        }
                    });
                } else {
                    alertBuilder
                    .setPositiveButton(getString(R.string.startactivityforresult_view_result), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(
                                    new Intent(IntentEditorActivity.this, IntentEditorActivity.class)
                                            .putExtra("intent", resultIntent)
                                            .putExtra(EXTRA_COMPONENT_TYPE, IntentEditorConstants.RESULT)
                            );
                        }
                    });
                }
                alertBuilder
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            }

        } else if (requestCode == REQUEST_CODE_RESULT_INTENT_EDITOR) {
            // Result intent editor requesting forward result
            if (resultIntent != null && resultIntent.hasExtra(EXTRA_FORWARD_RESULT_INTENT)) {
                setResult(
                        resultIntent.getIntExtra(EXTRA_FORWARD_RESULT_CODE, 0),
                        (Intent) resultIntent.getParcelableExtra(EXTRA_FORWARD_RESULT_INTENT)
                );
                finish();
            }

        } else {
            super.onActivityResult(requestCode, resultCode, resultIntent);
        }
    }

    Intent getEditedIntent() {
        return mEditedIntent;
    }

    IntentFilter[] getAttachedIntentFilters() {
        return mAttachedIntentFilters;
    }

    protected boolean isInterceptedIntent() {
        return false;
    }

    public static class LaunchableEditor extends Editor.EditorActivity {

        @Override
        public Intent getEditorIntent(Context context) {
            return new Intent(context, IntentEditorActivity.class);
        }

        @Override
        public boolean canEdit(Object value) {
            return value instanceof Intent;
        }
    }
}
