/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab.editor;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
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
        final String documentationDetails = flagSource.getAttributeValue(null, "details");
        mCheckbox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                SpannableString message = new SpannableString(
                        documentation +
                                (documentationDetails != null ? "\n\n" + documentationDetails: "")
                );
                message.setSpan(new StyleSpan(Typeface.BOLD), 0, documentation.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                new AlertDialog.Builder(flagsContainer)
                        .setTitle(mFlagName)
                        .setMessage(message)
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
