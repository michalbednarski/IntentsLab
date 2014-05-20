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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class PickRecentlyRunningActivity extends ListActivity implements AdapterView.OnItemClickListener {

    private ListView mListView;
    private RecentsAdapter mAdapter;

    private static final String PREF_EXCLUDED_COMPONENTS = "excluded_components";
    private Set<String> mExcludedComponents;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_recenly_running);
        setTitle(R.string.pick_recent_task);
        if (Build.VERSION.SDK_INT >= 11) {
            prepareActionBar();
        }

        mExcludedComponents = new HashSet<String>();
        for (String s : getDefaultSharedPreferences(this).getString(PREF_EXCLUDED_COMPONENTS, "").split("//")) {
            if (!s.equals("")) {
                mExcludedComponents.add(s);
            }
        }

        mListView = getListView();
        mListView.setOnItemClickListener(this);
        registerForContextMenu(mListView);
        mAdapter = new RecentsAdapter(this);
        setListAdapter(mAdapter);
    }

    @TargetApi(11)
    private void prepareActionBar() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pick_recenly_running, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_show_boring:
                mAdapter.excludeBoringIntents = !mAdapter.excludeBoringIntents;
                mAdapter.refresh();
                safelyInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(11)
    void safelyInvalidateOptionsMenu() {
        if (Build.VERSION.SDK_INT >= 11) {
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_show_boring)
                .setTitle((mAdapter.excludeBoringIntents ? R.string.menu_show_boring : R.string.menu_hide_boring));
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isLauncherIntent(Intent intent) {
        String action = intent.getAction();
        Set<String> categories = intent.getCategories();
        Uri data = intent.getData();
        Bundle extras = intent.getExtras();
        return ((action != null && action.equals(Intent.ACTION_MAIN)) &&
                (categories != null && categories.size() == 1 && categories.contains(Intent.CATEGORY_LAUNCHER)) &&
                data == null &&
                (extras == null || extras.isEmpty()));
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Intent baseIntent = mAdapter.getItem(position).baseIntent;
        Intent i = new Intent(this, IntentEditorActivity.class);
        i.putExtra("intent", baseIntent);
        startActivity(i);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Intent intent;
        try {
            AdapterView.AdapterContextMenuInfo aMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            intent = mAdapter.getItem(aMenuInfo.position).baseIntent;
        } catch (ClassCastException e) {
            return;
        }
        if (isLauncherIntent(intent)) {
            return;
        }
        final String componentName = intent.getComponent().flattenToShortString();
        final boolean nowExcluded = mExcludedComponents.contains(componentName);
        menu.add(nowExcluded ? R.string.exclude_item_from_list : R.string.restore_item_onto_list).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (nowExcluded) {
                    mExcludedComponents.remove(componentName);
                } else {
                    mExcludedComponents.add(componentName);
                }
                String excludedComponentsString = "";
                if (!mExcludedComponents.isEmpty()) {
                    for (String component : mExcludedComponents) {
                        excludedComponentsString += "//" + component;
                    }
                    excludedComponentsString = excludedComponentsString.substring(2);
                }
                Utils.applyOrCommitPrefs(
                        getDefaultSharedPreferences(PickRecentlyRunningActivity.this)
                                .edit()
                                .putString(PREF_EXCLUDED_COMPONENTS, excludedComponentsString));
                mAdapter.refresh();
                Toast.makeText(PickRecentlyRunningActivity.this, nowExcluded ? R.string.item_restored_onto_list : R.string.item_excluded_from_list, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private class RecentsAdapter extends ArrayAdapter<RecentTaskInfo> {
        private ActivityManager mAm;
        private LayoutInflater mInflater;
        private PackageManager mPm;
        boolean excludeBoringIntents = true;

        RecentsAdapter(Context ctx) {
            super(ctx, 0);
            mAm = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
            mInflater = (LayoutInflater) ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
            mPm = ctx.getPackageManager();
        }

        void refresh() {
            setNotifyOnChange(false);
            clear();
            List<RecentTaskInfo> tasks = mAm.getRecentTasks(30, ActivityManager.RECENT_WITH_EXCLUDED);
            for (RecentTaskInfo task : tasks) {
                Intent baseIntent = task.baseIntent;
                if (excludeBoringIntents &&
                        (mExcludedComponents.contains(baseIntent.getComponent().flattenToShortString()) ||
                                isLauncherIntent(baseIntent))) {
                    continue;
                }
                add(task);
            }
            setNotifyOnChange(true);
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.simple_list_item_2_with_icon, parent, false);
            }

            ActivityManager.RecentTaskInfo task = getItem(position);
            Intent baseIntent = task.baseIntent;

            // Icon
            ImageView iconView = (ImageView) convertView.findViewById(R.id.app_icon);
            if (iconView != null) {
                try {
                    iconView.setImageDrawable(
                            mPm.getActivityIcon(baseIntent.getComponent()));
                } catch (NameNotFoundException e) {
                    iconView.setImageDrawable(null);
                }
            }

            // Action
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(baseIntent.getAction());

            // Data and component
            Uri intentData = baseIntent.getData();
            ((TextView) convertView.findViewById(android.R.id.text2))
                    .setText(
                            (intentData == null ? "" : intentData.toString() + "\n") +
                                    baseIntent.getComponent().flattenToShortString()
                    );

            return convertView;
        }
    }
}
