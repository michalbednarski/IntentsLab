package com.example.testapp1;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.example.testapp1.editor.IntentEditorActivity;

public class BrowseAppsActivity extends Activity implements ExpandableListAdapter {

	ArrayList<DataSetObserver> mDataSetObservers = new ArrayList<DataSetObserver>();
	AppInfo mApps[] = null;
	boolean mSystemApps = false;
	ExpandableListView mList;
	boolean mListAdapterSet = false;
	TextView mMessage;

	enum PermissionFilter {
		PF_SHOW_WORLD_ACCESSIBLE,
		PF_SHOW_OBTAINABLE_PERMISSION,
		PF_SHOW_EXPORTED,
		PF_SHOW_ALL
	}

	PermissionFilter mPermissionFilter = PermissionFilter.PF_SHOW_WORLD_ACCESSIBLE;

	int mComponentTypeFilter = IntentEditorActivity.COMPONENT_TYPE_ACTIVITY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_apps);
        if (Build.VERSION.SDK_INT >= 11) {
        	onCreateAndroidSDK11AndUp();
        }

        mList = (ExpandableListView) findViewById(R.id.listView1);
        mMessage = (TextView) findViewById(R.id.message);
        updateList();
    }

    @TargetApi(11)
	public void onCreateAndroidSDK11AndUp() {
    	getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void updateList() {
    	new FetchAppsTask().execute((Object) null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_browse_apps, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	menu.findItem(R.id.system_apps).setVisible(!mSystemApps).setEnabled(!mSystemApps);
    	menu.findItem(R.id.user_apps).setVisible(mSystemApps).setEnabled(mSystemApps);
    	menu.findItem(
    			mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_ACTIVITY ? R.id.activities :
    			mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_BROADCAST? R.id.broadcasts :
				mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_SERVICE ? R.id.services : 0
			).setChecked(true); /* this boolean value is ignored for radio buttons - system always thinks it's true */
    	return super.onPrepareOptionsMenu(menu);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void safelyInvalidateOptionsMenu() {
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    		invalidateOptionsMenu();
    	}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.system_apps:
            case R.id.user_apps:
            	mSystemApps = itemId == R.id.system_apps;
            	safelyInvalidateOptionsMenu();
            	updateList();
            	return true;
            case R.id.activities:
            case R.id.broadcasts:
            case R.id.services:
            	mComponentTypeFilter =
            		itemId == R.id.activities ? IntentEditorActivity.COMPONENT_TYPE_ACTIVITY :
        			itemId == R.id.broadcasts ? IntentEditorActivity.COMPONENT_TYPE_BROADCAST :
        				IntentEditorActivity.COMPONENT_TYPE_SERVICE;
            	safelyInvalidateOptionsMenu();
            	updateList();
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.add(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.remove(observer);
	}

	@Override
	public int getGroupCount() {
		return mApps.length;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mApps[groupPosition].components.length;
	}

	@Override
	public Object getGroup(int groupPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getGroupId(int groupPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, parent, false);
		}
		AppInfo app = mApps[groupPosition];
		((TextView)convertView.findViewById(android.R.id.text1))
			.setText(app.appName);
		((TextView)convertView.findViewById(android.R.id.text2))
			.setText(app.packageName);
		return convertView;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
		}
		AppComponentInfo component = mApps[groupPosition].components[childPosition];
		((TextView)convertView.findViewById(android.R.id.text1))
			.setText(component.name);
		return convertView;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return mApps.length == 0;
	}

	@Override
	public void onGroupExpanded(int groupPosition) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onGroupCollapsed(int groupPosition) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getCombinedChildId(long groupId, long childId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCombinedGroupId(long groupId) {
		// TODO Auto-generated method stub
		return 0;
	}

	static class AppInfo {
		String appName;
		String packageName;
		AppComponentInfo components[];
	}

	static class AppComponentInfo {
		String name;
	}

	class FetchAppsTask extends AsyncTask</*Params*/Object, /*Progress*/Object, /*Result*/Object> {

		@Override
		protected void onPreExecute() {
			mList.setVisibility(View.GONE);
			mMessage.setVisibility(View.VISIBLE);
			mMessage.setText(R.string.loading_apps_list);
		}

		private boolean checkPermissionFilter(ComponentInfo cmp) {
			if (mPermissionFilter == PermissionFilter.PF_SHOW_ALL) {
				return true;
			}
			if (!cmp.exported) {
				return false;
			}
			switch(mPermissionFilter) {
			case PF_SHOW_WORLD_ACCESSIBLE:
				//return cmp.permission == null /*&& component.applicationInfo.permission == null*/;
			case PF_SHOW_OBTAINABLE_PERMISSION:

			}
			return true;
		}

		@Override
		protected Object doInBackground(Object... params) {
			PackageManager pm = getPackageManager();
	    	List<PackageInfo> allPackages =
	    			pm.getInstalledPackages(
	    					mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_ACTIVITY ? PackageManager.GET_ACTIVITIES :
        					mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_BROADCAST ? PackageManager.GET_RECEIVERS :
    						mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_SERVICE ? PackageManager.GET_SERVICES : 0

	    					);
	    	ArrayList<AppInfo> selectedApps = new ArrayList<AppInfo>();

	    	int requestedSystemFlagValue = mSystemApps ? ApplicationInfo.FLAG_SYSTEM : 0;
	    	for (PackageInfo pack: allPackages) {
	    		// Filter out non-applications
	    		if (pack.applicationInfo == null) {
	    			continue;
	    		}

	    		// System app filter
	    		if ((pack.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != requestedSystemFlagValue) {
	    			continue;
	    		}

	    		// Pick right components type
	    		ComponentInfo allComponents[] =
	    				mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_ACTIVITY ? pack.activities :
    					mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_BROADCAST ? pack.receivers :
						mComponentTypeFilter == IntentEditorActivity.COMPONENT_TYPE_SERVICE ? pack.services : null;

	    		// Skip apps not having any components of requested type
	    		if (!(allComponents != null && allComponents.length != 0)) {
    				continue;
    			}

	    		// Scan components
    			ArrayList<AppComponentInfo> components = new ArrayList<AppComponentInfo>();
    			for (ComponentInfo cmp: allComponents) {
    				if (!checkPermissionFilter(cmp)) {
    					continue;
    				}
    				AppComponentInfo component = new AppComponentInfo();
    				component.name = cmp.name;
    				components.add(component);
    			}

    			// Check again if we filtered out all components and skip whole app if so
    			if (components.isEmpty()) {
    				continue;
    			}

    			// Build and add app descriptor
    			AppInfo app = new AppInfo();
	    		app.appName = pack.applicationInfo.loadLabel(pm).toString();
	    		app.packageName = pack.packageName;
    			app.components = components.toArray(new AppComponentInfo[components.size()]);
    			selectedApps.add(app);
	    	}
	    	mApps = selectedApps.toArray(new AppInfo[selectedApps.size()]);
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			for (DataSetObserver observer: mDataSetObservers) {
	    		observer.onChanged();
	    	}

			if (!mListAdapterSet) {
				mList.setAdapter(BrowseAppsActivity.this);
				mListAdapterSet = true;
			}

			mMessage.setVisibility(View.GONE);
			mList.setVisibility(View.VISIBLE);
		}
	}
}
