package com.example.testapp1.editor;

import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.CheckBox;

class Flag {
	private static final String TAG = "FLAG";

	CheckBox mCheckbox;
	String mFlagName;
	int mFlagValue;

	Flag(XmlPullParser flagSource, Activity flagsContainer, ViewGroup checkboxesList) throws Exception {
		mFlagName = flagSource.getAttributeValue(null, "name");
		mFlagValue = Intent.class.getField(mFlagName).getInt(null);
		mCheckbox = new CheckBox(flagsContainer);
		mCheckbox.setText(mFlagName);
		checkboxesList.addView(mCheckbox);
	}

	boolean appliesTo(int intentDisposition) {
		if (mFlagName.startsWith("FLAG_ACTIVITY_") &&
				intentDisposition != IntentEditorActivity.COMPONENT_TYPE_ACTIVITY
				) {
			return false;
		}
		if (mFlagName.startsWith("FLAG_RECEIVER_") &&
				intentDisposition != IntentEditorActivity.COMPONENT_TYPE_BROADCAST) {
			return false;
		}
		return true;
	}

	void updateValue(int newFlags, int intentDisposition) {
		boolean applies = appliesTo(intentDisposition);
		mCheckbox.setEnabled(applies);
		mCheckbox.setChecked(applies && ((newFlags & mFlagValue) != 0));
	}

	int getValue() {
		return mCheckbox.isChecked() ? mFlagValue : 0;
	}
}
