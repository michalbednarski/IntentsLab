package com.example.testapp1.editor;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.testapp1.R;

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
			} catch(ClassCastException e) {
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
		try {
			menu.findItem(R.id.menu_run_intent)
				.setTitle(IntentGeneralFragment.getMethodNamesArray(getResources(), mComponentType)[mMethodId]);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		menu.findItem(R.id.detach_intent_filter)
			.setVisible(mAttachedIntentFilters != null);
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void safelyInvalidateOptionsMenu() {
		try {
			invalidateOptionsMenu();
		} catch(NoSuchMethodError error) {}
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
				case IntentEditorConstants.BROADCAST_METHOD_SENDORDEREDBROADCAST:
					{
						sendOrderedBroadcast(
							mEditedIntent, // intent
							null, // permission
							new BroadcastReceiver() { // resultReceiver
								@Override
								public void onReceive(Context context, Intent intent) {
									// TODO Auto-generated method stub
									Toast.makeText(IntentEditorActivity.this, "Received result", Toast.LENGTH_SHORT).show();
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
		} catch(Exception exception) {
			String exceptionName = exception.getClass().getName();
			exceptionName = exceptionName.substring(exceptionName.lastIndexOf('.') + 1);
			Toast.makeText(this, exceptionName + ": " + exception.getMessage(), Toast.LENGTH_LONG).show();
		}
	}



	// RESULT
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		if (requestCode == REQUEST_CODE_TEST_STARTACTIVITYFORRESULT) {
			Toast.makeText(this, "GOT ACTIVITY RESULT", Toast.LENGTH_SHORT).show();
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
