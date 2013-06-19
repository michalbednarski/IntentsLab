package com.example.testapp1.editor;

import android.app.AlertDialog;
import android.view.View;
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

	Flag(XmlPullParser flagSource, final Activity flagsContainer, ViewGroup checkboxesList) throws Exception {
		mFlagName = flagSource.getAttributeValue(null, "name");
		mFlagValue = Intent.class.getField(mFlagName).getInt(null);
		mCheckbox = new CheckBox(flagsContainer);
		mCheckbox.setText(mFlagName);

        // Documentation on long-click
        final String documentation = flagSource.getAttributeValue(null, "desc");
        mCheckbox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new AlertDialog.Builder(flagsContainer)
                        .setTitle(mFlagName)
                        .setMessage(documentation)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return true;
            }
        });

        // Add to list
		checkboxesList.addView(mCheckbox);
	}

	boolean appliesTo(int intentDisposition) {
		if (mFlagName.startsWith("FLAG_ACTIVITY_") &&
				intentDisposition != IntentEditorConstants.ACTIVITY
				) {
			return false;
		}
		if (mFlagName.startsWith("FLAG_RECEIVER_") &&
				intentDisposition != IntentEditorConstants.BROADCAST) {
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
