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

package com.github.michalbednarski.intentslab.providerlab.proxy;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.*;
import android.widget.CursorAdapter;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.providerlab.AdvancedQueryActivity;

/**
 * Fragment that shows list of operations performed by provider
 */
public class LogViewerFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static String methodIdToName(int method) {
        switch (method) {
            case AdvancedQueryActivity.METHOD_QUERY: return "query";
            case AdvancedQueryActivity.METHOD_INSERT: return "insert";
            case AdvancedQueryActivity.METHOD_UPDATE: return "update";
            case AdvancedQueryActivity.METHOD_DELETE: return "delete";
            case AdvancedQueryActivity.METHOD_GET_TYPE: return "getType";
            case AdvancedQueryActivity.METHOD_OPEN_FILE: return "openFile";
            case AdvancedQueryActivity.METHOD_OPEN_ASSET_FILE: return "openAssetFile";
        }
        return "?";
    }

    private static final int OPERATION_LOG_LOADER = 1;

    private static final String[] COLUMNS = new String[] {
        "_id", // 0
        "method", // 1
        "uri", // 2
        "uid", // 3
        "exception" // 4
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        getLoaderManager().initLoader(OPERATION_LOG_LOADER, null,this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(getActivity().getString(R.string.proxy_log_empty));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != OPERATION_LOG_LOADER) {
            return null; // We don't know other loaders
        }
        return new CursorLoader(getActivity()) {
            private ForceLoadContentObserver mObserver = new ForceLoadContentObserver();
            private ProxyProviderDatabase mProxyProviderDatabase = ProxyProviderDatabase.getInstance(getActivity());
            private boolean mObserverRegistered = false;

            @Override
            public Cursor loadInBackground() {
                return mProxyProviderDatabase.getReadableDatabase().query(
                        "operations",
                        COLUMNS,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }

            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                if (!mObserverRegistered) {
                    mProxyProviderDatabase.mContentObservable.registerObserver(mObserver);
                    mObserverRegistered = true;
                }
            }

            @Override
            protected void onReset() {
                if (mObserverRegistered) {
                    mProxyProviderDatabase.mContentObservable.unregisterObserver(mObserver);
                    mObserverRegistered = false;
                }
                super.onReset();
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        setListAdapter(new CursorAdapter(getActivity(), data, false) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                // First row: method and app name
                final String appName = getActivity().getPackageManager().getNameForUid(cursor.getInt(3));
                final String methodName = methodIdToName(cursor.getInt(1));
                ((TextView) view.findViewById(android.R.id.text1)).setText(methodName + "() by " + appName);

                // Second row: uri and exception
                final String uri = cursor.getString(2);
                String exception = cursor.getString(4);
                SpannableStringBuilder text2 = new SpannableStringBuilder(uri);
                if (exception != null) {
                    text2.append("\n");
                    final int start = text2.length();
                    text2.append(exception);
                    final int end = text2.length();
                    text2.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    // TODO: result / empty result warning
                }
                ((TextView) view.findViewById(android.R.id.text2)).setText(text2);
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.proxy_operations_log, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_log:
                ProxyProviderDatabase.getInstance(getActivity()).clearLog();
                return true;
        }
        return false;
    }
}
