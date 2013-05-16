package com.example.testapp1.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.testapp1.R;

public class IntentGeneralFragment extends IntentEditorPanel implements OnItemSelectedListener {

	private TextView mDataText;
	private TextView mComponentText;
	private Spinner mComponentTypeSpinner;
	private Spinner mMethodSpinner;
	private Intent mEditedIntent;



	public IntentGeneralFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.intent_editor_general, container, false);

		// Prepare form
		mActionText = (TextView) v.findViewById(R.id.action);
		mActionsSpinner  = (Spinner) v.findViewById(R.id.action_spinner);
		mDataText = (TextView) v.findViewById(R.id.data);
		mComponentText = (TextView) v.findViewById(R.id.component);

		mComponentTypeSpinner = (Spinner) v.findViewById(R.id.componenttype);
		mComponentTypeSpinner.setAdapter(new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_item, getResources()
				.getStringArray(R.array.componenttypes)));

		mMethodSpinner = (Spinner) v.findViewById(R.id.method);

		mCategoriesContainer = (ViewGroup) v.findViewById(R.id.categories);
		mAddCategoryButton = (Button) v.findViewById(R.id.category_add);

		// Apparently using android:scrollHorizontally="true" does not work.
		// http://stackoverflow.com/questions/9011944/android-ice-cream-sandwich-edittext-disabling-spell-check-and-word-wrap
		mComponentText.setHorizontallyScrolling(true);

		// Bind button actions (android:onClick="" applies to hosting activity)
		mAddCategoryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addCategoryTextField("");
			}
		});
		v.findViewById(R.id.component_pick).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pickComponent();
			}
		});
		v.findViewById(R.id.component_clear).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mComponentText.setText("");
			}
		});

		// Fill the form
		mEditedIntent = getEditedIntent();

		int componentType = getComponentType();
		initMethodSpinner(componentType);
		mComponentTypeSpinner.setSelection(componentType);
		mMethodSpinner.setSelection(getIntentEditor().getMethodId());


		mDataText.setText(mEditedIntent.getDataString());
		if (mEditedIntent.getComponent() != null) {
			mComponentText.setText(mEditedIntent.getComponent()
					.flattenToShortString());
		}

		setupActionSpinnerOrField();
		updateCategoriesList();

		// Set spinner listeners
		mComponentTypeSpinner.setOnItemSelectedListener(this);
		mMethodSpinner.setOnItemSelectedListener(this);

		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mActionText = null;
		mDataText = null;
		mComponentText = null;
		mComponentTypeSpinner = null;
		mMethodSpinner = null;
		mCategoriesContainer = null;
		mCategoryTextInputs = null;
		mCategoryCheckBoxes = null;
		mAddCategoryButton = null;
	}

	// Component type and method
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {

		if (parent == mComponentTypeSpinner) {
			if (getComponentType() != position) {
				initMethodSpinner(position);
				getIntentEditor().setComponentType(position);
			}

		} else if (parent == mMethodSpinner) {
			if (getIntentEditor().getMethodId() != position) {
				getIntentEditor().setMethodId(position);
			}
		}
	}


	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// Spinner won't have nothing selected
	}

	static String[] getMethodNamesArray(Resources res, int componentType) {
		return res.getStringArray(
				componentType == IntentEditorConstants.ACTIVITY ? R.array.activitymethods :
				componentType == IntentEditorConstants.BROADCAST ? R.array.broadcastmethods :
				componentType == IntentEditorConstants.SERVICE ? R.array.servicemethods : 0
			);
	}

	private void initMethodSpinner(int componentType) {
		mMethodSpinner.setAdapter(
			new ArrayAdapter<String>(
				getActivity(),
				android.R.layout.simple_spinner_item,
				getMethodNamesArray(getResources(), componentType)
			)
		);
	}

	// ACTIONS
	private TextView mActionText;
	private Spinner mActionsSpinner;
	private String[] mAvailbleActions;

	private void setupActionSpinnerOrField() {
		IntentFilter[] intentFilters = getIntentEditor().getAttachedIntentFilters();
		if (intentFilters != null) {
			// Build action set
			Set<String> actionSet = new HashSet<String>();
			for (IntentFilter filter : intentFilters) {
				for(int i = 0, j = filter.countActions(); i < j; i++) {
					actionSet.add(filter.getAction(i));
				}
			}

			// Check if we have any action and if not (invalid intent-filter)
			// switch to textfield
			if (actionSet.isEmpty()) {
				setupActionField();
				return;
			}

			// Convert action set to array
			mAvailbleActions = actionSet.toArray(new String[actionSet.size()]);
			Arrays.sort(mAvailbleActions);

			// Select current action
			String action = mEditedIntent.getAction();
			if (action == null) {
				action = Intent.ACTION_VIEW;
			}
			int position = Arrays.binarySearch(mAvailbleActions, action);
			mActionsSpinner.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mAvailbleActions));
			if (position == -1) {
				position = 0;
			}
			mActionsSpinner.setSelection(position);

			// Show spinner and hide textfield
			mActionText.setVisibility(View.GONE);
			mActionsSpinner.setVisibility(View.VISIBLE);
		} else {
			setupActionField();
		}
	}

	private void setupActionField() {
		mAvailbleActions = null;
		mActionText.setText(mEditedIntent.getAction());
		mActionText.setVisibility(View.VISIBLE);
		mActionsSpinner.setVisibility(View.GONE);
	}


	// CATEGORIES
	private ViewGroup mCategoriesContainer;
	private ArrayList<TextView> mCategoryTextInputs;
	private CheckBox[] mCategoryCheckBoxes;
	private Button mAddCategoryButton;
	private boolean mShowSelectableCategories = false;

	public void addCategoryTextField(String category) {
		final ViewGroup row = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.category_row, mCategoriesContainer, false);
		final TextView textField = ((TextView) row.findViewById(R.id.categoryText));
		textField.setText(category);
		row.findViewById(R.id.row_remove).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCategoriesContainer.removeView(row);
				mCategoryTextInputs.remove(textField);
			}
		});
		mCategoriesContainer.addView(row);
		mCategoryTextInputs.add(textField);
	}


	private void updateCategoriesList() {
		IntentFilter[] attachedIntentFilters = getIntentEditor().getAttachedIntentFilters();

		mShowSelectableCategories = attachedIntentFilters != null;

		mCategoriesContainer.removeAllViews();

		if (mShowSelectableCategories) {

			Set<String> availbleCategories = new HashSet<String>();
			for (IntentFilter filter : attachedIntentFilters) {
				for (int i = 0, j = filter.countCategories(); i < j; i++) {
					availbleCategories.add(filter.getCategory(i));
				}
			}

			mCategoryCheckBoxes = new CheckBox[availbleCategories.size()];
			int i = 0;
			for (String category : availbleCategories) {
				CheckBox cb = new CheckBox(getActivity());
				cb.setText(category);
				cb.setTag(category);
				cb.setChecked(mEditedIntent.hasCategory(category));
				mCategoriesContainer.addView(cb);
				mCategoryCheckBoxes[i++] = cb;
			}

			mAddCategoryButton.setVisibility(View.GONE);

			mCategoryTextInputs = null;

		} else {

			mCategoryTextInputs = new ArrayList<TextView>();
			Set<String> categories = mEditedIntent.getCategories();
			if (categories != null) {
				for (String category : categories) {
					addCategoryTextField(category);
				}
			}

			mAddCategoryButton.setVisibility(View.VISIBLE);

			mCategoryCheckBoxes = null;
		}

	}

	// COMPONENT
	private void pickComponent() {
		Intent intent = new Intent(getEditedIntent(true)).setComponent(null);
		PackageManager pm = getActivity().getPackageManager();
		List<ResolveInfo> ri = null;

		switch (getComponentType()) {
		case IntentEditorConstants.ACTIVITY:
			ri = pm.queryIntentActivities(intent, 0);
			break;
		case IntentEditorConstants.BROADCAST:
			ri = pm.queryBroadcastReceivers(intent, 0);
			break;
		case IntentEditorConstants.SERVICE:
			ri = pm.queryIntentServices(intent, 0);
			break;
		}
		new ComponentPicker(getActivity(), ri, mComponentText).show();

	}

	// Contact with activity
	@Override
	public void updateEditedIntent(Intent editedIntent) {
		// Intent action

		if (mAvailbleActions != null) {
			editedIntent.setAction((String) mActionsSpinner.getSelectedItem());
		} else {
			String action = mActionText.getText().toString();
			if (action.equals("")) {
				action = Intent.ACTION_VIEW; // system default action
			}
			editedIntent.setAction(action);
		}

		// Categories
		{
			// Clear categories (why there's no api for this)
			Set<String> origCategories = editedIntent.getCategories();
			if (origCategories != null) {
				for (String category : origCategories.toArray(new String[origCategories.size()])) {
					editedIntent.removeCategory(category);
				}
			}
		}
		// Fill categories
		if (mShowSelectableCategories) {
			// Fill categories from checkboxes
			for (CheckBox cb : mCategoryCheckBoxes) {
				String category = (String) cb.getTag();
				if (cb.isChecked()) {
					editedIntent.addCategory(category);
				}
			}
		} else {
			// Fill categories from textfields
			for (TextView categoryTextView : mCategoryTextInputs) {
				editedIntent.addCategory(categoryTextView.getText().toString());
			}
		}

		// Intent data (Uri)
		String data = mDataText.getText().toString();
		if (!data.equals("")) {
			editedIntent.setData(Uri.parse(data));
		}

		// Set component for explicit intent
		String component = mComponentText.getText().toString();
		if (component.equals("")) {
			editedIntent.setComponent(null);
		} else {
			editedIntent.setComponent(ComponentName.unflattenFromString(component));
		}

	}

	@Override
	void onIntentFiltersChanged(IntentFilter[] newIntentFilters) {
		updateEditedIntent(mEditedIntent);
		setupActionSpinnerOrField();
		updateCategoriesList();
	}

	@Override
	public void onComponentTypeChanged(int newComponentType) {
		if (mComponentTypeSpinner.getSelectedItemId() != newComponentType) {
			mComponentTypeSpinner.setSelection(newComponentType);
		}
	}

}
