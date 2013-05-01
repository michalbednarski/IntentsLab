package com.example.testapp1.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testapp1.R;
import com.example.testapp1.TabsHelper;

public class IntentEditorActivity extends Activity implements OnItemSelectedListener {
	private static final String TAG = "IntentEditor";

	public static final String EXTRA_DISPOSITION = "intenteditor.disposition";

	public static final int COMPONENT_TYPE_ACTIVITY = 0;
	public static final int COMPONENT_TYPE_BROADCAST = 1;
	public static final int COMPONENT_TYPE_SERVICE = 2;

	private TextView mActionText;
	private TextView mDataText;
	private TextView mComponentText;
	private Spinner mComponentTypeSpinner;
	private Spinner mMethodSpinner;
	private ViewGroup mCategoriesContainer;
	private ArrayList<ViewGroup> mCategoryRows = new ArrayList<ViewGroup>();

	private BundleAdapter mExtrasAdapter;

	private ArrayList<Flag> mFlags = new ArrayList<Flag>();

	private TabsHelper mTabsHelper = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Tab switching and setContentView
		mTabsHelper = TabsHelper.getBuilder(this)
			.setLayout(
					R.layout.intent_editor_with_tabhost,
					R.layout.intent_editor
			)
			.tryTabsConfiguration(
					"General", R.id.generalAndExtrasWrapper,
					"Flags", R.id.flagsWrapper
			)
			.tryTabsConfiguration(
					"General", R.id.generalWrapper,
					"Extras", R.id.extrasList,
					"Flags", R.id.flagsWrapper
			)
			.build();


		// Prepare form
		mActionText = (TextView) findViewById(R.id.action);
		mDataText = (TextView) findViewById(R.id.data);
		mComponentText = (TextView) findViewById(R.id.component);

		mComponentTypeSpinner = (Spinner) findViewById(R.id.componenttype);
		mComponentTypeSpinner.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, getResources()
				.getStringArray(R.array.componenttypes)));
		mComponentTypeSpinner.setOnItemSelectedListener(this);

		mMethodSpinner = (Spinner) findViewById(R.id.method);

		mCategoriesContainer = (ViewGroup) findViewById(R.id.categories);

		// Apparently using android:scrollHorizontally="true" does not work.
		// http://stackoverflow.com/questions/9011944/android-ice-cream-sandwich-edittext-disabling-spell-check-and-word-wrap
		mComponentText.setHorizontallyScrolling(true);


		// Load intent
		Intent baseIntent = getIntent().getParcelableExtra("intent");

		Bundle extras;
		int componentType = 0;

		if (savedInstanceState == null) {
			if (baseIntent != null) {
				mActionText.setText(baseIntent.getAction());
				mDataText.setText(baseIntent.getDataString());
				if (baseIntent.getComponent() != null) {
					mComponentText.setText(baseIntent.getComponent()
							.flattenToShortString());
				}
				extras = baseIntent.getExtras();
			} else {
				extras = new Bundle();
			}

			componentType = getIntent().getIntExtra(EXTRA_DISPOSITION,
					COMPONENT_TYPE_ACTIVITY);
			mComponentTypeSpinner.setSelection(componentType);
		} else {
			extras = savedInstanceState.getBundle("_extras");
		}

		Set<String> categories = baseIntent == null ? null : baseIntent.getCategories();
		if (categories != null) {
			for (String category : categories) {
				addCategory(category);
			}
		}
		mExtrasAdapter = new BundleAdapter(this, extras);
		ListView extrasList = (ListView) findViewById(R.id.extrasList);
		mExtrasAdapter.settleOnList(extrasList);

		initMethodSpinner();

		// Flags list
		try {
			int baseIntentFlags =
					savedInstanceState != null ? savedInstanceState.getInt("flags") :
						baseIntent != null ? baseIntent.getFlags() : 0;

						LinearLayout l = (LinearLayout) findViewById(R.id.flags);
						XmlPullParser xrp = getResources().getXml(R.xml.intent_flags);

						int parserEvent;
						while ((parserEvent = xrp.next()) != XmlPullParser.END_DOCUMENT) {
							if (parserEvent != XmlPullParser.START_TAG
									|| !xrp.getName().equals("flag")) {
								continue;
							}

							try {
								Flag flag = new Flag(xrp, this, l);
								flag.updateValue(baseIntentFlags, componentType);
								mFlags.add(flag);
								l.addView(flag.mCheckbox);
							} catch (Exception e) {}
						}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initMethodSpinner() {
		int componentType = getComponentType();
		mMethodSpinner.setAdapter(new ArrayAdapter<String>(this,
			android.R.layout.simple_spinner_item,
				getResources().getStringArray(
					componentType == COMPONENT_TYPE_ACTIVITY ? R.array.activitymethods :
					componentType == COMPONENT_TYPE_BROADCAST ? R.array.broadcastmethods :
					componentType == COMPONENT_TYPE_SERVICE ? R.array.servicemethods : 0
				)
			)
		);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBundle("_extras", mExtrasAdapter.getBundle());
		outState.putInt("flags", getFlagsFromCheckboxes());
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
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_startActivity:
			startProvidedActivity();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public Intent buildIntent() {
		Intent intent = new Intent();

		// Intent action
		String action = mActionText.getText().toString();
		if (!action.equals("")) {
			intent.setAction(action);
		}

		// Categories
		for (ViewGroup cat : mCategoryRows) {
			intent.addCategory(((TextView) cat.findViewById(R.id.categoryText)).getText().toString());
		}

		// Intent data (Uri)
		String data = mDataText.getText().toString();
		if (!data.equals("")) {
			intent.setData(Uri.parse(data));
		}

		// Set component for explicit intent
		String component = mComponentText.getText().toString();
		if (!component.equals("")) {
			intent.setComponent(ComponentName.unflattenFromString(component));
		}

		// Flags
		intent.setFlags(getFlagsFromCheckboxes());

		// Extras
		intent.putExtras(mExtrasAdapter.getBundle());

		return intent;
	}

	int getComponentType() {
		return mComponentTypeSpinner.getSelectedItemPosition();
	}
	int getMethodId() {
		return mMethodSpinner.getSelectedItemPosition();
	}

	public void startProvidedActivity() { // TODO better name

		Intent intent = buildIntent();
		try {
			switch (getComponentType()) {
			case COMPONENT_TYPE_ACTIVITY:
				startActivity(intent);
				break;
			/*case DISPOSITION_ACTIVITYFORRESULT:
				startActivityForResult(intent, 1);
				break;*/
			case COMPONENT_TYPE_BROADCAST:
				sendBroadcast(intent);
			}
		} catch(Exception exception) {
			String exceptionName = exception.getClass().getName();
			exceptionName = exceptionName.substring(exceptionName.lastIndexOf('.') + 1);
			Toast.makeText(this, exceptionName + ": " + exception.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	// CATEGORIES
	public void addCategory(String category) {
		ViewGroup row = (ViewGroup) getLayoutInflater().inflate(R.layout.category_row, mCategoriesContainer);
		((TextView) row.findViewById(R.id.categoryText)).setText(category);
		mCategoryRows.add(row);
	}
	public void addCategory(View view) {
		addCategory("");
	}
	public void removeCategory(View view) {
		ViewGroup row = (ViewGroup) view.getParent();
		mCategoriesContainer.removeView(row);
		mCategoryRows.remove(row);
	}

	// COMPONENT
	public void pickComponent(View view) {
		Intent intent = buildIntent().setComponent(null);
		PackageManager pm = getPackageManager();
		List<ResolveInfo> ri = null;

		switch (getComponentType()) {
		case COMPONENT_TYPE_ACTIVITY:
			ri = pm.queryIntentActivities(intent, 0);
			break;
		case COMPONENT_TYPE_BROADCAST:
			ri = pm.queryBroadcastReceivers(intent, 0);
			break;
		case COMPONENT_TYPE_SERVICE:
			ri = pm.queryIntentServices(intent, 0);
			break;
		}
		new ComponentPicker(this, ri, mComponentText).show();

	}

	public void clearComponent(View view) {
		mComponentText.setText("");
	}

	// RESULT
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		Toast.makeText(this, "GOT ACTIVITY RESULT", Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub
	}

	// FLAGS
	private int getFlagsFromCheckboxes() {
		int flags = 0;
		for (Flag flag : mFlags) {
			flags |= flag.getValue();
		}
		return flags;
	}

	// Component type and method
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (parent == mComponentTypeSpinner) {
			int flags = getFlagsFromCheckboxes();
			for (Flag flag : mFlags) {
				flag.updateValue(flags, position);
			}
			initMethodSpinner();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Spinner won't have nothing selected
	}
}
