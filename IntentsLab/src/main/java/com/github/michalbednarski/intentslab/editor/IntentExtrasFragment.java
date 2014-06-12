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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

public class IntentExtrasFragment extends IntentEditorPanel implements BundleAdapter.BundleAdapterAggregate {
    public IntentExtrasFragment() {}

	ListView mExtrasList;
	private BundleAdapter<IntentExtrasFragment> mBundleAdapter;

    @Override
    public BundleAdapter getBundleAdapter() {
        return mBundleAdapter;
    }

    @Override
    public void onBundleModified() {}

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		super.onCreateView(inflater, container, savedInstanceState);

		mExtrasList = new ListView(inflater.getContext());

        if (mBundleAdapter == null) {
            mBundleAdapter = new BundleAdapter<IntentExtrasFragment>(getEditedIntent().getExtras(), new EditorLauncher(getActivity(), "IntentExtrasEditorLauncher"), this);
        }
		mBundleAdapter.settleOnList(mExtrasList);
		return mExtrasList;
	}

    @Override
    public void onDetach() {
        super.onDetach();
        if (mBundleAdapter != null) {
            mBundleAdapter.shutdown();
            mBundleAdapter = null;
        }
    }

    @Override
	public void updateEditedIntent(Intent editedIntent) {
		editedIntent.replaceExtras(mBundleAdapter.getBundle());
	}

	@Override
	public void onComponentTypeChanged(int newComponentType) {}
}
