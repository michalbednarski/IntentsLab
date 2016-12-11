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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;

import com.github.michalbednarski.intentslab.TextFragment;
import com.github.michalbednarski.intentslab.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by mb on 18.08.14.
 */
public class DumpSysFragment extends TextFragment {
    public static final String ARG_SERVICE_NAME = "svcN";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        new DumpTask().execute();
    }

    private class DumpTask extends AsyncTask<Object, Object, Object> {
        Context mContext = getActivity().getApplicationContext();
        String mServiceName = getArguments().getString(ARG_SERVICE_NAME);

        private StringBuilder mText = new StringBuilder();

        private void addLine(String line) {
            if (mText.length() != 0) {
                mText.append('\n');
            }
            mText.append(line);
        }

        @Override
        protected ArrayList<String> doInBackground(Object[] params) {
            try {
                InputStream inputStream = Utils.dumpSystemService(mContext, mServiceName, new String[0]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    addLine(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
                addLine(Utils.describeException(e));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            publishText(new SpannableString(mText));
        }
    }
}
