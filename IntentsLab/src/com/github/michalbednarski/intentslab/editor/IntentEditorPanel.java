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

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.michalbednarski.intentslab.xposedhooks.api.IntentTracker;

abstract class IntentEditorPanel extends Fragment {
	private static final String TAG = "IntentEditorPanel";

	/*
	 * Call super.onCreateView(inflater, container, savedInstanceState); from subclass!
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		getIntentEditor().loadedPanels.add(this);
		Log.v(TAG, "CreateView: " + getClass().getName());
		return null;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		updateEditedIntent(getEditedIntent());
		getIntentEditor().loadedPanels.remove(this);
		Log.v(TAG, "DestroyView: " + getClass().getName());
	}

	/**
	 * Get associated IntentEditorActivty
	 */
	IntentEditorActivity getIntentEditor() {
		return (IntentEditorActivity) getActivity();
	}

	/**
	 * Gets edited intent object,
	 * all changes must be reflected on it
	 *
	 * @see #updateEditedIntent
	 */
	Intent getEditedIntent() {
		return getIntentEditor().getEditedIntent();
	}

	/**
	 * Gets possibly updated edited intent.
	 * Update intent to read from it data set on other panels
	 */
	Intent getEditedIntent(boolean update) {
		if (update) {
			getIntentEditor().updateIntent();
		}
		return getEditedIntent();
	}

	int getComponentType() {
		return getIntentEditor().getComponentType();
	}

	abstract void updateEditedIntent(Intent editedIntent);
	void onComponentTypeChanged(int newComponentType) {}
	void onIntentFiltersChanged(IntentFilter[] newIntentFilters) {}
	void onIntentTrackerChanged(IntentTracker newTracker, IntentTracker oldTracker) {}
}
