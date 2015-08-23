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

package com.github.michalbednarski.intentslab.browser;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.AppInfoActivity;
import com.github.michalbednarski.intentslab.BuildConfig;
import com.github.michalbednarski.intentslab.PermissionInfoFragment;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.appinfo.MyComponentInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageInfo;
import com.github.michalbednarski.intentslab.editor.IntentEditorConstants;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

/**
 * Fragment for displaying components or other data provided by {@link Fetcher}
 */
public class BrowseComponentsFragment extends Fragment {
    public static final String ARG_FETCHER = "ComponentBrowserFetcher";

    /**
     * Options to be hidden by default
     */
    private static final int[] QUICK_FILTER_OPTIONS_IDS = new int[] {
            R.id.system_apps,
            R.id.user_apps,
            R.id.simple_filter_permission
    };

    private Fetcher mFetcher;
    private MyCallback mCallback;

    private ProgressBar mProgressIndicator;
    private ExpandableListView mExpandableListView;
    private ListView mNonExpandableListView;
    private TextView mEmptyMessage;
    private TextView mCustomErrorText;

    private Object mLoadedData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (savedInstanceState != null) {
            mFetcher = savedInstanceState.getParcelable(ARG_FETCHER);
        } else {
            mFetcher = getArguments().getParcelable(ARG_FETCHER);
            if (mFetcher == null) {
                mFetcher = new ComponentFetcher();
            }
        }
        setHasOptionsMenu(true);
        startFetcher();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_FETCHER, mFetcher);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.browse_components, container, false);

        // Find views
        mProgressIndicator = (ProgressBar) view.findViewById(R.id.progressBar);
        mExpandableListView = (ExpandableListView) view.findViewById(R.id.expandableListView);
        mNonExpandableListView = (ListView) view.findViewById(R.id.listView);
        mEmptyMessage = (TextView) view.findViewById(R.id.empty_message);
        mCustomErrorText = (TextView) view.findViewById(R.id.custom_error);

        // Fill views
        if (mLoadedData != null) {
            updateView();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mProgressIndicator = null;
        mExpandableListView = null;
        mNonExpandableListView = null;
        mEmptyMessage = null;
        mCustomErrorText = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCallback != null) {
            mCallback.mCancelled = true;
        }
    }

    Fetcher getFetcher() {
        return mFetcher;
    }

    void setFetcher(Fetcher fetcher) {
        mFetcher = fetcher;
        if (mCallback != null) {
            mCallback.mCancelled = true;
        }

        clearDataAndShowLoadingIndicator();
        startFetcher();
    }

    private void startFetcher() {
        mCallback = new MyCallback();
        mFetcher
                .getEntriesAsync(getActivity().getApplicationContext())
                .done(mCallback)
                .fail(mCallback);
    }

    private class MyCallback implements DoneCallback<Object>, FailCallback<Throwable> {
        boolean mCancelled;

        @Override
        public void onDone(Object result) {
            if (!mCancelled) {
                if (BuildConfig.DEBUG && !(
                        result instanceof Fetcher.Category[] ||
                                result instanceof Fetcher.Component[] ||
                                result instanceof Fetcher.CustomError
                )) {
                    throw new AssertionError("Fetcher " + mFetcher + " returned unexpected value " + result);
                }
                mLoadedData = result;
                updateView();
                mCallback = null;
            }
        }

        @Override
        public void onFail(Throwable result) {
            if (!mCancelled) {
                mLoadedData = new Fetcher.CustomError(result.getMessage());
                updateView();
            }
        }
    }

    private void updateView() {
        Object o = mLoadedData;
        if (o instanceof Fetcher.Category[]) {
            ExpandableAdapter adapter = new ExpandableAdapter((Fetcher.Category[]) o);
            mExpandableListView.setEmptyView(mEmptyMessage);
            mExpandableListView.setAdapter(adapter);
            mExpandableListView.setOnChildClickListener(adapter);
            mExpandableListView.setVisibility(View.VISIBLE);
        } else if (o instanceof Fetcher.Component[]) {
            NonExpandableAdapter adapter = new NonExpandableAdapter((Fetcher.Component[]) o);
            mNonExpandableListView.setEmptyView(mEmptyMessage);
            mNonExpandableListView.setAdapter(adapter);
            mNonExpandableListView.setOnItemClickListener(adapter);
            mNonExpandableListView.setVisibility(View.VISIBLE);
        } else {
            mCustomErrorText.setText(((Fetcher.CustomError) o).message);
            mCustomErrorText.setVisibility(View.VISIBLE);
        }
        mProgressIndicator.setVisibility(View.GONE);
    }

    private void clearDataAndShowLoadingIndicator() {
        if (mProgressIndicator != null) {
            mNonExpandableListView.setEmptyView(null);
            mNonExpandableListView.setVisibility(View.GONE);
            mNonExpandableListView.setOnItemClickListener(null);
            mNonExpandableListView.setAdapter(null);

            mExpandableListView.setEmptyView(null);
            mExpandableListView.setVisibility(View.GONE);
            mExpandableListView.setOnChildClickListener(null);
            mExpandableListView.setAdapter((ExpandableListAdapter) null);

            mEmptyMessage.setVisibility(View.GONE);
            mCustomErrorText.setVisibility(View.GONE);
            mProgressIndicator.setVisibility(View.VISIBLE);
        }

        mLoadedData = null;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_browse_apps, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        for (int id : QUICK_FILTER_OPTIONS_IDS) {
            menu.findItem(id).setVisible(false);
        }
        menu.findItem(R.id.dummy_component_type).setChecked(true); // Deselect component
        mFetcher.onPrepareOptionsMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (mFetcher.onOptionsItemSelected(itemId)) {
            setFetcher(mFetcher);
            return true;
        }

        switch (itemId) {
            case R.id.custom_filter:
                FetcherOptionsDialog fetcherOptionsDialog = new FetcherOptionsDialog();
                fetcherOptionsDialog.setTargetFragment(this, 0);
                fetcherOptionsDialog.show(getFragmentManager(), "filterOptions");
                return true;
            case R.id.activities:
            case R.id.broadcasts:
            case R.id.services:
            case R.id.content_providers: {
                // Fetcher isn't component fetcher, otherwise it would intercept this above
                ComponentFetcher fetcher = new ComponentFetcher();
                fetcher.type =
                        itemId == R.id.activities ? PackageManager.GET_ACTIVITIES :
                        itemId == R.id.broadcasts ? PackageManager.GET_RECEIVERS :
                        itemId == R.id.services ? PackageManager.GET_SERVICES :
                        itemId == R.id.content_providers ? PackageManager.GET_PROVIDERS : 0;
                if (mFetcher instanceof ApplicationFetcher) {
                    fetcher.appType = ((ApplicationFetcher) mFetcher).appType;
                }
                setFetcher(fetcher);
                return true;
            }
            case R.id.applications: {
                if (!(mFetcher instanceof ApplicationFetcher)) {
                    ApplicationFetcher fetcher = new ApplicationFetcher();
                    if (mFetcher instanceof ComponentFetcher) {
                        fetcher.appType = ((ComponentFetcher) mFetcher).appType;
                    }
                    setFetcher(fetcher);
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void showComponent(Object baseComponentInfo) {
        BrowseComponentsActivity activity = (BrowseComponentsActivity) getActivity();
        // Registered receiver
        if (baseComponentInfo instanceof RegisteredReceiverInfo) {
            Bundle arguments = new Bundle();
            arguments.putParcelable(
                    RegisteredReceiverInfoFragment.ARG_REGISTERED_RECEIVER,
                    (RegisteredReceiverInfo) baseComponentInfo
            );
            activity.openFragment(
                    RegisteredReceiverInfoFragment.class,
                    arguments
            );
            return;
        }

        // Package
        if (baseComponentInfo instanceof MyPackageInfo) {
            MyPackageInfo packageInfo = (MyPackageInfo) baseComponentInfo;
            startActivity(
                    new Intent(getActivity(), AppInfoActivity.class)
                            .putExtra(AppInfoActivity.EXTRA_PACKAGE_NAME, packageInfo.getPackageName())
            );
            return;
        }

        // Permission
        if (baseComponentInfo instanceof PermissionInfo) {
            PackageItemInfo componentInfo = (PackageItemInfo) baseComponentInfo;
            Bundle arguments = new Bundle();
            arguments.putString(PermissionInfoFragment.ARG_PERMISSION_NAME, componentInfo.name);
            activity.openFragment(
                    PermissionInfoFragment.class,
                    arguments
            );
            return;
        }

        // Component
        if (baseComponentInfo instanceof MyComponentInfo) {
            MyComponentInfo componentInfo = (MyComponentInfo) baseComponentInfo;
            int type = componentInfo.getType();

            Bundle arguments = new Bundle();
            arguments.putString(ComponentInfoFragment.ARG_PACKAGE_NAME, componentInfo.getOwnerPackage().getPackageName());
            arguments.putString(ComponentInfoFragment.ARG_COMPONENT_NAME, componentInfo.getName());

            if (type == IntentEditorConstants.PROVIDER) {
                activity.openFragment(
                        ProviderInfoFragment.class,
                        arguments
                );
            } else {
                arguments.putInt(ComponentInfoFragment.ARG_COMPONENT_TYPE, type);
                activity.openFragment(
                        ComponentInfoFragment.class,
                        arguments
                );
            }
        }
    }

    private class ExpandableAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnChildClickListener {
        private final Fetcher.Category[] mCategories;

        ExpandableAdapter(Fetcher.Category[] categories) {
            mCategories = categories;
        }

        @Override
        public int getGroupCount() {
            return mCategories.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return mCategories[groupPosition].components.length;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return null;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return getCombinedGroupId(groupPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return getCombinedChildId(groupPosition, childPosition);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
            }
            Fetcher.Category category = mCategories[groupPosition];
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(category.title);
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(category.subtitle);
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            Fetcher.Component component = mCategories[groupPosition].components[childPosition];
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(component.title);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            showComponent(mCategories[groupPosition].components[childPosition].componentInfo);
            return false;
        }
    }

    private class NonExpandableAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private final Fetcher.Component[] mComponents;

        private NonExpandableAdapter(Fetcher.Component[] components) {
            mComponents = components;
        }

        @Override
        public int getCount() {
            return mComponents.length;
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
                LayoutInflater inflater =
                        (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            Fetcher.Component component = mComponents[position];
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(component.title);
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(component.subtitle);
            return convertView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showComponent(mComponents[position].componentInfo);
        }
    }
}
