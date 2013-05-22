package com.example.testapp1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.testapp1.editor.IntentEditorActivity;

public class PickRecentlyRunningActivity extends Activity implements OnItemClickListener {

    private static final Set<String> boringCategories = new HashSet<String>();
    private static final Set<String> boringActions = new HashSet<String>();
    private ListView mListView;
    private RecentsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_recenly_running);
        setTitle(R.string.pick_recent_task);
        if (Build.VERSION.SDK_INT >= 11) {
            prepareActionBar();
        }
        if (boringActions.isEmpty()) {
            for (String boring : getResources().getStringArray(R.array.boring_actions)) {
                boringActions.add(boring);
            }
            for (String boring : getResources().getStringArray(R.array.boring_categories)) {
                boringCategories.add(boring);
            }
        }
        mListView = (ListView) findViewById(R.id.listView1);
        mListView.setOnItemClickListener(this);
        mAdapter = new RecentsAdapter(this);
        mListView.setAdapter(mAdapter);
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
                NavUtils.navigateUpFromSameTask(this);
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

    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Intent baseIntent = mAdapter.getItem(position).baseIntent;
        Intent i = new Intent(this, IntentEditorActivity.class);
        i.putExtra("intent", baseIntent);
        /*i.putExtra(IntentEditorActivity.EXTRA_ACTION, baseIntent.getAction());
		if (baseIntent.getCategories() != null) {
			i.putExtra(IntentEditorActivity.EXTRA_CATEGOTIES, baseIntent.getCategories().toArray());
		}
		i.putExtra(IntentEditorActivity.EXTRA_DATA, baseIntent.getData());
		i.putExtra(IntentEditorActivity.EXTRA_EXTRAS, baseIntent.getExtras());
		i.putExtra(IntentEditorActivity.EXTRA_COMPONENT, baseIntent.getComponent().flattenToShortString());*/
        startActivity(i);
    }

    private class RecentsAdapter extends ArrayAdapter<ActivityManager.RecentTaskInfo> {
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
            clear();
            List<RecentTaskInfo> tasks = mAm.getRecentTasks(30, ActivityManager.RECENT_WITH_EXCLUDED);
            for (RecentTaskInfo task : tasks) {
                Intent baseIntent = task.baseIntent;
                String action = baseIntent.getAction();
                Uri data = baseIntent.getData();
                Set<String> categories = baseIntent.getCategories();
                Bundle extras = baseIntent.getExtras();
                if (excludeBoringIntents &&
                        (action != null && boringActions.contains(action)) &&
                        (categories == null || boringCategories.containsAll(categories)) &&
                        data == null &&
                        (extras == null || extras.isEmpty())
                        ) {
                    continue;
                }
                add(task);
            }
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
