package com.github.michalbednarski.intentslab.editor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SavedItemsDatabase;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceDescriptor;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;
import com.github.michalbednarski.intentslab.browser.ExtendedPackageInfo;
import com.github.michalbednarski.intentslab.runas.IRemoteInterface;
import com.github.michalbednarski.intentslab.runas.RunAsInitReceiver;
import com.github.michalbednarski.intentslab.runas.RunAsManager;
import com.github.michalbednarski.intentslab.sandbox.SandboxManager;
import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.xposedhooks.api.IntentTracker;
import com.github.michalbednarski.intentslab.xposedhooks.api.XIntentsLab;
import com.github.michalbednarski.intentslab.xposedhooks.api.XIntentsLabStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * Intent editor activity
 */
public class IntentEditorActivity extends FragmentTabsActivity/*FragmentActivity*/ {
    private static final String TAG = "IntentEditor";
    private static final int REQUEST_CODE_TEST_STARTACTIVITYFORRESULT = 657;
    private static final int REQUEST_CODE_RESULT_INTENT_EDITOR = 754;
    public static final int REQUEST_CODE_REQUEST_INTENT_TRACKER_PERMISSION = 2243;

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

    private static final class LocalIntentEditorState extends Binder {
        IntentTracker intentTracker = null;
    }

    LocalIntentEditorState mLocalState = new LocalIntentEditorState();


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
            Object uncastedLocalState = savedInstanceState.get("localIEState");
            if (uncastedLocalState instanceof LocalIntentEditorState) {
                mLocalState = (LocalIntentEditorState) uncastedLocalState;
            }
        } else if (isInterceptedIntent()) {
            // Intercept
            mEditedIntent = new Intent(getIntent());
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
		allTabsAdded();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateIntent();
        outState.putParcelable("intent", mEditedIntent);
        outState.putInt("componentType", mComponentType);
        outState.putInt("methodId", mMethodId);
        outState.putParcelableArray("intentFilters", mAttachedIntentFilters);
        RunAsInitReceiver.putBinderInBundle(outState, "localIEState", mLocalState);
        /*if (mTabsHelper != null) {
			outState.putInt("tab", mTabsHelper.getCurrentView());
		}*/
        // TODO: current tab?
    }

    boolean isIntentTrackerAvailable() {
        XIntentsLab xIntentsLab = XIntentsLabStatic.getInstance();
        return xIntentsLab != null && xIntentsLab.supportsObjectTracking();
    }

    void createIntentTracker() {
        IntentTracker newTracker = XIntentsLabStatic.getInstance().createIntentTracker();
        IntentTracker oldTracker = mLocalState.intentTracker;
        mLocalState.intentTracker = newTracker;
        for (IntentEditorPanel panel : loadedPanels) {
            panel.onIntentTrackerChanged(newTracker, oldTracker);
        }
        ActivityCompat.invalidateOptionsMenu(this);
    }

    void removeIntentTracker() {
        if (mLocalState.intentTracker != null) {
            IntentTracker oldTracker = mLocalState.intentTracker;
            mLocalState.intentTracker = null;
            for (IntentEditorPanel panel : loadedPanels) {
                panel.onIntentTrackerChanged(null, oldTracker);
            }
        }
        ActivityCompat.invalidateOptionsMenu(this);
    }

    public IntentTracker getIntentTracker() {
        return mLocalState.intentTracker;
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

        // Intent tracking options
        {
            boolean intentTrackerAvailable = isIntentTrackerAvailable();
            MenuItem trackIntentOption = menu.findItem(R.id.track_intent);
            trackIntentOption.setVisible(intentTrackerAvailable);
            trackIntentOption.setEnabled(intentTrackerAvailable);
            trackIntentOption.setChecked(getIntentTracker() != null);
        }

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
                        new Intent(this, SingleFragmentActivity.class)
                                .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName())
                                .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, component.getPackageName())
                                .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, component.getClassName())
                                .putExtra(ComponentInfoFragment.ARG_LAUNCHED_FROM_INTENT_EDITOR, true)
                );
            }
            return true;
            case R.id.save:{
                updateIntent();
                SavedItemsDatabase.getInstance(this).saveIntent(this, mEditedIntent, mComponentType, mMethodId);
            }
            return true;
            case R.id.track_intent:
            {
                if (!item.isChecked()) {
                    XIntentsLab xIntentsLab = XIntentsLabStatic.getInstance();
                    if (xIntentsLab.havePermission()) {
                        createIntentTracker();
                    } else {
                        try {
                            startIntentSenderForResult(
                                    xIntentsLab.getRequestPermissionIntent(getPackageName()).getIntentSender(),
                                    REQUEST_CODE_REQUEST_INTENT_TRACKER_PERMISSION,
                                    null,
                                    0,
                                    0,
                                    0
                            );
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                            // TODO: can we handle this?
                        }
                    }
                } else {
                    removeIntentTracker();
                }
                return true;
            }
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

    private IBinder getTokenForRemoteInterface() {
        try {
            return (IBinder) getClass().getMethod("getActivityToken").invoke(this);
        } catch (Exception e) {
            try {
                final Field field = Activity.class.getDeclaredField("mToken");
                field.setAccessible(true);
                return (IBinder) field.get(this);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private void startActivityRemote(IRemoteInterface remoteInterface, boolean forResult) throws Throwable {
        Bundle result = remoteInterface.startActivity(
                mEditedIntent,
                ((mEditedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0 && !forResult) ?
                        null :
                        getTokenForRemoteInterface(),
                forResult ?
                        REQUEST_CODE_TEST_STARTACTIVITYFORRESULT :
                        -1
        );
        Throwable exception = (Throwable) result.getSerializable("exception");
        if (exception != null) {
            throw exception;
        }
    }

    public void runIntent() {
        updateIntent();
        IRemoteInterface remoteInterface = RunAsManager.getSelectedRemoteInterface();
        Intent intent = mEditedIntent;
        IntentTracker intentTracker = getIntentTracker();
        if (intentTracker != null) {
            intent = intentTracker.tagIntent(intent);
        }
        try {
            switch (getComponentType()) {

                case IntentEditorConstants.ACTIVITY:
                    switch (getMethodId()) {
                        case IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITY:
                            if (remoteInterface != null) {
                                startActivityRemote(remoteInterface, false);
                            } else {
                                startActivity(intent);
                            }
                            break;
                        case IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITYFORRESULT:
                            if (remoteInterface != null) {
                                startActivityRemote(remoteInterface, true);
                            } else {
                                startActivityForResult(intent, REQUEST_CODE_TEST_STARTACTIVITYFORRESULT);
                            }
                            break;
                    }
                    break;


                case IntentEditorConstants.BROADCAST:
                    switch (getMethodId()) {
                        case IntentEditorConstants.BROADCAST_METHOD_SENDBROADCAST:
                            sendBroadcast(intent);
                            break;
                        case IntentEditorConstants.BROADCAST_METHOD_SENDORDEREDBROADCAST: {
                            sendOrderedBroadcast(
                                    intent, // intent
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
                            sendStickyBroadcast(intent);
                            break;
                    }
                    break;


                case IntentEditorConstants.SERVICE:
                    switch (getMethodId()) {
                        case IntentEditorConstants.SERVICE_METHOD_STARTSERVICE:
                            startService(intent);
                            break;
                        default:
                        {
                            if (!SandboxManager.isSandboxInstalled(this)) {
                                SandboxManager.requestSandboxInstall(this);
                            } else {
                                BindServiceManager.prepareBinderAndShowUI(this, new BindServiceDescriptor(new Intent(intent)));
                            }
                        }
                    }
                    break;

                case IntentEditorConstants.RESULT:
                    setResult(
                            0,
                            new Intent()
                            .putExtra(EXTRA_FORWARD_RESULT_CODE, mMethodId)
                            .putExtra(EXTRA_FORWARD_RESULT_INTENT, intent)
                    );
                    finish();
                    break;
            }
        } catch (Throwable exception) {
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

        } else if (requestCode == REQUEST_CODE_REQUEST_INTENT_TRACKER_PERMISSION) {
            if (resultCode == RESULT_OK) {
                createIntentTracker();
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
