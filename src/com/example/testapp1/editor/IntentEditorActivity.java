package com.example.testapp1.editor;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.testapp1.R;
import com.example.testapp1.Utils;
import com.example.testapp1.browser.ComponentInfoActivity;

import java.util.ArrayList;

/**
 * Intent editor activity
 */
public class IntentEditorActivity extends FragmentTabsActivity/*FragmentActivity*/ {
    private static final String TAG = "IntentEditor";
    private static final int REQUEST_CODE_TEST_STARTACTIVITYFORRESULT = 657;

    public static final String EXTRA_COMPONENT_TYPE = "componentType_";
    public static final String EXTRA_INTENT_FILTERS = "intentFilters_";

    //private BundleAdapter mExtrasAdapter;

    ArrayList<IntentEditorPanel> loadedPanels = new ArrayList<IntentEditorPanel>(3);

    private Intent mEditedIntent;
    private int mComponentType;
    private int mMethodId;
    private IntentFilter[] mAttachedIntentFilters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load intent
        Parcelable[] uncastedIntentFilters = null;
        if (savedInstanceState != null) {
            mEditedIntent = savedInstanceState.getParcelable("intent");
            mComponentType = savedInstanceState.getInt("componentType");
            mMethodId = savedInstanceState.getInt("methodId");
            uncastedIntentFilters = savedInstanceState.getParcelableArray("intentFilters");
        } else {
            mEditedIntent = getIntent().getParcelableExtra("intent");
            mComponentType = getIntent().getIntExtra(EXTRA_COMPONENT_TYPE, IntentEditorConstants.ACTIVITY);
            mMethodId = getIntent().getIntExtra("methodId", 0);
            uncastedIntentFilters = getIntent().getParcelableArrayExtra(EXTRA_INTENT_FILTERS);
            if (mEditedIntent == null) {
                mEditedIntent = new Intent();
            }
        }
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
            addTab(getString(R.string.general), IntentGeneralWithExtrasFragment.class);
        } else {
            addTab(getString(R.string.general), IntentGeneralFragment.class);
            addTab(getString(R.string.intent_extras), IntentExtrasFragment.class);
        }
        addTab("Flags", IntentFlagsFragment.class);


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
                runIntentOption.setVisible(false);
            } else {
                runIntentOption
                    .setVisible(true)
                    .setTitle(IntentGeneralFragment.getMethodNamesArray(getResources(), mComponentType)[mMethodId]);
            }
        }
        menu.findItem(R.id.detach_intent_filter)
                .setVisible(mAttachedIntentFilters != null);
        menu.findItem(R.id.component_info)
                .setVisible(mEditedIntent.getComponent() != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_run_intent:
                runIntent();
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
                );
            }
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
        updateIntent();
        mAttachedIntentFilters = null;
        for (IntentEditorPanel panel : loadedPanels) {
            panel.onIntentFiltersChanged(null);
        }
        safelyInvalidateOptionsMenu();
    }

    public void runIntent() {
        updateIntent();
        try {
            switch (getComponentType()) {

                case IntentEditorConstants.ACTIVITY:
                    switch (getMethodId()) {
                        case IntentEditorConstants.ACTIVITY_METHOD_STARTACTIVITY:
                            startActivity(mEditedIntent);
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
                    Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
                    // TODO runIntent services
                    break;
            }
        } catch (Exception exception) {
            Utils.toastException(this, exception);
        }
    }


    // RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    final Intent resultIntent) {
        if (requestCode == REQUEST_CODE_TEST_STARTACTIVITYFORRESULT) {
            if (resultIntent == null) {
                Toast.makeText(this, getString(R.string.startactivityforresult_no_result), Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.startactivityforresult_got_result)) // TODO
                    .setPositiveButton(getString(R.string.startactivityforresult_view_result), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(
                                new Intent(IntentEditorActivity.this, IntentEditorActivity.class)
                                    .putExtra("intent", resultIntent)
                                    .putExtra(IntentEditorActivity.EXTRA_COMPONENT_TYPE, IntentEditorConstants.RESULT));
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            }
        }
        // TODO Auto-generated method stub
    }

    Intent getEditedIntent() {
        return mEditedIntent;
    }

    IntentFilter[] getAttachedIntentFilters() {
        return mAttachedIntentFilters;
    }
}
