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

package com.github.michalbednarski.intentslab.valueeditors;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.valueeditors.framework.Editor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.framework.ValueEditorFragment;

import java.lang.reflect.Array;

/**
 * Editor for primitive and object arrays
 */
public class ArrayEditorFragment extends ValueEditorFragment implements EditorLauncher.EditorLauncherCallback, AdapterView.OnItemClickListener {
    private Object mArray;
    private EditorLauncher mEditorLauncher;
    private final Adapter mAdapter = new Adapter();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mArray = getSandboxedEditedObject().unwrap(null);

        mEditorLauncher = new EditorLauncher(getActivity(), "ArrEdiEdLa");
        mEditorLauncher.setCallback(this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView listView = new ListView(getActivity());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        return listView;
    }

    @Override
    public void onEditorResult(String key, Object newValue) {
        Array.set(mArray, Integer.parseInt(key), newValue);
        markAsModified();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mEditorLauncher.launchEditor(String.valueOf(position), Array.get(mArray, position));
    }

    @Override
    protected Object getEditorResult() {
        return mArray;
    }

    private class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return Array.getLength(mArray);
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
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            ((TextView) convertView).setText(position + ": " + String.valueOf(Array.get(mArray, position)));
            return convertView;
        }
    }

    public static class LaunchableEditor implements Editor.FragmentEditor {
        @Override
        public boolean canEdit(Object value) {
            return value != null && value.getClass().isArray();
        }

        @Override
        public Class<? extends ValueEditorFragment> getEditorFragment() {
            return ArrayEditorFragment.class;
        }
    }
}
