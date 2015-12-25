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

package com.github.michalbednarski.intentslab;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Base fragment for displaying long, possibly chunked for performance text
 */
public class TextFragment extends Fragment {
    private View mLoaderView;
    private View mXmlWrapperView;
    private TextView mXmlTextView;
    private ListView mFakeLongText;

    private Object mText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.xml_viewer, container, false);
        mLoaderView = view.findViewById(R.id.loader);
        mXmlWrapperView = view.findViewById(R.id.xml_wrapper);
        mXmlTextView = (TextView) view.findViewById(R.id.xml);
        mXmlTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mFakeLongText = (ListView) view.findViewById(R.id.xml_fake_long_view);
        if (mText != null) {
            putTextInView();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        mLoaderView = null;
        mXmlWrapperView = null;
        mXmlTextView = null;
        mFakeLongText = null;
        super.onDestroyView();
    }

    protected void publishText(Object text) {
        if (BuildConfig.DEBUG &&
                !(text instanceof CharSequence) &&
                !(text instanceof CharSequence[])
                ) {
            throw new AssertionError("Text must be CharSequence or array");
        }
        mText = text;
        if (mLoaderView != null) {
            putTextInView();
        }
    }

    private void putTextInView() {
        if (mText instanceof CharSequence[]) {
            mFakeLongText.setAdapter(new ArrayAdapter<CharSequence>(getActivity(), 0, (CharSequence[]) mText) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        final TextView textView = new TextView(getContext());
                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                        convertView = textView;
                    }
                    ((TextView) convertView).setText(getItem(position));
                    return convertView;
                }

                @Override
                public boolean areAllItemsEnabled() {
                    return false;
                }

                @Override
                public boolean isEnabled(int position) {
                    return false;
                }
            });
            mFakeLongText.setVisibility(View.VISIBLE);
        } else {
            FormattedTextBuilder.putInTextView(mXmlTextView, (CharSequence) mText);
            mXmlWrapperView.setVisibility(View.VISIBLE);
        }

        mLoaderView.setVisibility(View.GONE);
    }
}
