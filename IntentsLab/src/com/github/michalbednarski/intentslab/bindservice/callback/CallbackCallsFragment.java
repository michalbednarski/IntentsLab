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

package com.github.michalbednarski.intentslab.bindservice.callback;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.Utils;

/**
 * Fragment displaying list of fragments available
 */
public class CallbackCallsFragment extends ListFragment {
    static final String ARG_CALLBACK_INFO = "IL.CallbackCF.callbackInfoLiveRef";

    private CallbackInterfacesManager.CallbackInfo mCallbackInfo;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCallbackInfo = Utils.getLiveRefFromBundle(getArguments(), ARG_CALLBACK_INFO);

        // TODO: remove this if mCallbackInfo is null
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Callback doesn't exist, we should be removed shortly, don't crash
        if (mCallbackInfo == null) {
            return;
        }

        // Set list adapter
        setListShownNoAnimation(true);
        setListAdapter(mAdapter);
    }

    /**
     * Adapter for list
     */
    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mCallbackInfo.calls.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final CallbackInterfacesManager.BaseCallInfo callInfo = mCallbackInfo.calls.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            ((TextView) convertView).setText(callInfo.methodName);

            return convertView;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mCallbackInfo.callsObservable.registerObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mCallbackInfo.callsObservable.unregisterObserver(observer);
        }
    };

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show();
    }
}
