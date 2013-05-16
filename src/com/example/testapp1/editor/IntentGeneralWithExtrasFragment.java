package com.example.testapp1.editor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.testapp1.R;

public class IntentGeneralWithExtrasFragment extends Fragment {
	public IntentGeneralWithExtrasFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.intent_editor_general_with_extras, container, false);
		getChildFragmentManager().beginTransaction()
			.add(R.id.general, new IntentGeneralFragment())
			.add(R.id.extras, new IntentExtrasFragment())
			.commit();
		return v;
	}


}
