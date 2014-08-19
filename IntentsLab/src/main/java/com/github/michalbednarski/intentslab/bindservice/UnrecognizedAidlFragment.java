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

package com.github.michalbednarski.intentslab.bindservice;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.bindservice.manager.BaseServiceFragmentWithMenu;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;

/**
 * Created by mb on 16.10.13.
 */
public class UnrecognizedAidlFragment extends BaseServiceFragmentWithMenu {

    String mMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        BindServiceManager.Helper serviceHelper = getServiceHelper();
        mMessage = "No AIDL found\ngetInterfaceDescriptor() = " + serviceHelper.getInterfaceDescriptor();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.unrecognized_aidl, container, false);
        ((TextView) view.findViewById(R.id.message)).setText(mMessage);
        view.findViewById(R.id.unbind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
        return view;
    }
}