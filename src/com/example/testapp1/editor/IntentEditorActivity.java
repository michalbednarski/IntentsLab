package com.example.testapp1.editor;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testapp1.R;
import com.example.testapp1.TabsHelper;

public class IntentEditorActivity extends Activity implements
AdapterView.OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "IntentEditor";

	public static final String EXTRA_DISPOSITION = "intenteditor.disposition";

	public static final int DISPOSITION_ACTIVITY = 0;
	public static final int DISPOSITION_ACTIVITYFORRESULT = 1;
	public static final int DISPOSITION_BROADCAST = 2;
	public static final int DISPOSITION_SERVICE = 3;

	private TextView mActionText;
	private TextView mDataText;
	private TextView mComponentText;
	private Spinner mIntentDispositionSpinner;

	private CheckBox[] mFlagCheckboxes = new CheckBox[32];

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
					"General", R.id.generalAndFlagsWrapper,
					"Flags", R.id.flagsWrapper
			)
			.tryTabsConfiguration(
					"General", R.id.generalWrapper,
					"Extras", R.id.extrasWrapper,
					"Flags", R.id.flagsWrapper
			)
			.build();



		// Fill form
		Bundle extras;

		mActionText = (TextView) findViewById(R.id.action);
		mDataText = (TextView) findViewById(R.id.data);
		mComponentText = (TextView) findViewById(R.id.component);

		mIntentDispositionSpinner = (Spinner) findViewById(R.id.intenttype);
		mIntentDispositionSpinner.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, getResources()
				.getStringArray(R.array.intenttypes)));

		// Apparently using android:scrollHorizontally="true" does not work.
		// http://stackoverflow.com/questions/9011944/android-ice-cream-sandwich-edittext-disabling-spell-check-and-word-wrap
		mComponentText.setHorizontallyScrolling(true);

		Intent baseIntent = getIntent().getParcelableExtra("intent");

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

			int disposition = getIntent().getIntExtra(EXTRA_DISPOSITION,
					DISPOSITION_ACTIVITY);
			mIntentDispositionSpinner.setSelection(disposition);
		} else {
			extras = savedInstanceState.getBundle("_extras");
		}
		BundleAdapter hma = new BundleAdapter(this, extras);
		ListView extrasList = (ListView) findViewById(R.id.extrasList);
		extrasList.setAdapter(hma);
		extrasList.setOnItemClickListener(this);
		extrasList.setOnItemLongClickListener(this);

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
							String flagName = xrp.getAttributeValue(null, "name");
							int flagValue;
							try {
								flagValue = Intent.class.getField(flagName).getInt(null);
							} catch (NoSuchFieldException e) {
								Log.w(TAG, "Intent flag " + flagName
										+ " is unsupported on this version");
								continue;
							}
							int flagIndex = -1;

							for (int i = 0; i < 32; i++) {
								if (flagValue == 1 << i) {
									flagIndex = i;
									break;
								}
							}

							if (flagIndex == -1) {
								Log.w(TAG, "Unknown flag index for " + flagName);
								continue;
							}
							CheckBox cb = new CheckBox(this);
							cb.setText(flagName);
							cb.setChecked((baseIntentFlags & flagValue) != 0);
							l.addView(cb);
							mFlagCheckboxes[flagIndex] = cb;
						}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("flags", getFlagsFromCheckboxes());
		/*if (mTabsHelper != null) {
			outState.putInt("tab", mTabsHelper.getCurrentView());
		}*/
		// TODO: dump extras
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
		case R.id.menu_add:
			Intent i = new Intent(this, EntryEditorActivity.class);
			startActivity(i); // TODO: for result
			return true;
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

		// TODO extras and categories

		return intent;
	}

	public void startProvidedActivity() { // TODO better name

		Intent intent = buildIntent();
		try {
			switch (mIntentDispositionSpinner.getSelectedItemPosition()) {
			case 0:
				startActivity(intent);
				break;
			case 1:
				startActivityForResult(intent, 1);
				break;
			}
		} catch (ActivityNotFoundException exception) {
			Toast.makeText(this, R.string.startactivity_error_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException exception) {
			Toast.makeText(this,
					R.string.startactivity_error_security_exception,
					Toast.LENGTH_SHORT).show();
		}
	}

	public void pickComponent(View view) { // TODO
		ComponentName c = buildIntent().setComponent(null).resolveActivity(
				getPackageManager());
		if (c != null) {
			mComponentText.setText(c.flattenToShortString());
		}
	}

	public void clearComponent(View view) {
		((TextView) findViewById(R.id.component)).setText("");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		Toast.makeText(this, "GOT ACTIVITY RESULT", Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub
	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Toast.makeText(this,
				"onItemClick\nposition=" + position + "\nid=" + id,
				Toast.LENGTH_SHORT).show();
		// TODO Auto-generated method stub

	}

	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		Toast.makeText(this,
				"onItemLongClick\nposition=" + position + "\nid=" + id,
				Toast.LENGTH_SHORT).show();
		// view.startDrag(null, null, null, 0);
		// TODO Auto-generated method stub
		return false;
	}

	private int getFlagsFromCheckboxes() {
		int flags = 0;
		for (int i = 0; i < 32; i++) {
			CheckBox checkbox = mFlagCheckboxes[i];
			if (checkbox != null && checkbox.isChecked()) {
				flags |= 1 << i;
			}
		}
		return flags;
	}
}
