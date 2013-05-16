package com.example.testapp1.editor;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.testapp1.R;

public class IntentFlagsFragment extends IntentEditorPanel {
	private ArrayList<Flag> mFlags = new ArrayList<Flag>();

	public IntentFlagsFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		View v = inflater.inflate(R.layout.intent_editor_flags, container, false);

		IntentEditorActivity activity = ((IntentEditorActivity) getActivity());
		Intent editedIntent = activity.getEditedIntent();
		int initialFlags = editedIntent.getFlags();
		int componentType = activity.getComponentType();

		try {
			LinearLayout l = (LinearLayout) v.findViewById(R.id.flags);
			XmlPullParser xrp = getResources().getXml(R.xml.intent_flags);

			int parserEvent;
			while ((parserEvent = xrp.next()) != XmlPullParser.END_DOCUMENT) {
				if (parserEvent != XmlPullParser.START_TAG
						|| !xrp.getName().equals("flag")) {
					continue;
				}

				try {
					Flag flag = new Flag(xrp, getActivity(), l);
					flag.updateValue(initialFlags, componentType);
					mFlags.add(flag);
					l.addView(flag.mCheckbox);
				} catch (Exception e) {}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return v;
	}

	private int getFlagsFromCheckboxes() {
		int flags = 0;
		for (Flag flag : mFlags) {
			flags |= flag.getValue();
		}
		return flags;
	}

	@Override
	public void updateEditedIntent(Intent editedIntent) {
		editedIntent.setFlags(getFlagsFromCheckboxes());
	}

	@Override
	public void onComponentTypeChanged(int newComponentType) {
		int flags = getFlagsFromCheckboxes();
		for (Flag flag : mFlags) {
			flag.updateValue(flags, newComponentType);
		}
	}
}
