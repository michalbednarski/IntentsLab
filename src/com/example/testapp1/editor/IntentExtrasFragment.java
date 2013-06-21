package com.example.testapp1.editor;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class IntentExtrasFragment extends IntentEditorPanel {
	public IntentExtrasFragment() {}

	ListView mExtrasList;
	BundleAdapter mBundleAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		mExtrasList = new ListView(inflater.getContext());
		mBundleAdapter = new BundleAdapter(getActivity(), getEditedIntent().getExtras(), getIntentEditor().extrasEditorActivityResultHandler);
		mBundleAdapter.settleOnList(mExtrasList);
		return mExtrasList;
	}

	@Override
	public void updateEditedIntent(Intent editedIntent) {
		editedIntent.replaceExtras(mBundleAdapter.getBundle());
	}

	@Override
	public void onComponentTypeChanged(int newComponentType) {}
}
