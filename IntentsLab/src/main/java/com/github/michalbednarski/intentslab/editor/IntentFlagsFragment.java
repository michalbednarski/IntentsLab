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

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.michalbednarski.intentslab.R;

public class IntentFlagsFragment extends IntentEditorPanel {
    private ArrayList<Flag> mFlags = null;

    public IntentFlagsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.intent_editor_flags, container, false);

        mFlags = new ArrayList<Flag>();

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
                } catch (Exception e) {
                }
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
