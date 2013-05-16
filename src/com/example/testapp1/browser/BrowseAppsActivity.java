package com.example.testapp1.browser;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.content.pm.ServiceInfo;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import com.example.testapp1.R;
import com.example.testapp1.editor.IntentEditorConstants;

public class BrowseAppsActivity extends Activity implements ExpandableListAdapter, OnChildClickListener {

	private final static int ITEM_ID_SPLIT_BASE = 1000;

	ArrayList<DataSetObserver> mDataSetObservers = new ArrayList<DataSetObserver>();
	AppInfo mApps[] = null;
	boolean mSystemApps = false;
	ExpandableListView mList;
	boolean mListAdapterSet = false;
	TextView mMessage;

	enum PermissionFilter {
		WORLD_ACCESSIBLE,
		OBTAINABLE_PERMISSION,
		EXPORTED,
		ALL
	}

	PermissionFilter mPermissionFilter = PermissionFilter.WORLD_ACCESSIBLE;

	int mComponentTypeFilter = IntentEditorConstants.ACTIVITY;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_apps);
        if (Build.VERSION.SDK_INT >= 11) {
        	onCreateAndroidSDK11AndUp();
        }

        mList = (ExpandableListView) findViewById(R.id.listView1);
        mMessage = (TextView) findViewById(R.id.message);
        mList.setOnChildClickListener(this);
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
    			mComponentTypeFilter == IntentEditorConstants.ACTIVITY ? R.id.activities :
    			mComponentTypeFilter == IntentEditorConstants.BROADCAST? R.id.broadcasts :
				mComponentTypeFilter == IntentEditorConstants.SERVICE  ? R.id.services   : 0
			).setChecked(true); /* this boolean value is ignored for radio buttons - system always thinks it's true */
    	menu.findItem(
    			mPermissionFilter == PermissionFilter.WORLD_ACCESSIBLE ? R.id.permission_filter_world_accessible :
				mPermissionFilter == PermissionFilter.OBTAINABLE_PERMISSION ? R.id.permission_filter_obtaiable :
				mPermissionFilter == PermissionFilter.EXPORTED ? R.id.permission_filter_exported :
				mPermissionFilter == PermissionFilter.ALL ? R.id.permission_filter_all : 0
    		).setChecked(true);
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
            		itemId == R.id.activities ? IntentEditorConstants.ACTIVITY :
        			itemId == R.id.broadcasts ? IntentEditorConstants.BROADCAST :
        				IntentEditorConstants.SERVICE;
            	safelyInvalidateOptionsMenu();
            	updateList();
            	return true;
            case R.id.permission_filter_all:
            case R.id.permission_filter_exported:
            case R.id.permission_filter_obtaiable:
            case R.id.permission_filter_world_accessible:
            	mPermissionFilter =
            		itemId == R.id.permission_filter_all ? PermissionFilter.ALL :
        			itemId == R.id.permission_filter_exported ? PermissionFilter.EXPORTED :
        			itemId == R.id.permission_filter_obtaiable ? PermissionFilter.OBTAINABLE_PERMISSION :
        				PermissionFilter.WORLD_ACCESSIBLE;
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
		return groupPosition;
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
		return groupId * ITEM_ID_SPLIT_BASE + childId;
	}

	@Override
	public long getCombinedGroupId(long groupId) {
		return groupId;
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

		@SuppressWarnings("incomplete-switch")
		private boolean checkPermissionFilter(ComponentInfo cmp) {
			if (mPermissionFilter == PermissionFilter.ALL) {
				return true;
			}
			if (!cmp.exported) {
				return false;
			}
			String permission = cmp instanceof ServiceInfo ?
					((ServiceInfo) cmp).permission:
					((ActivityInfo) cmp).permission;

			switch(mPermissionFilter) {
			case WORLD_ACCESSIBLE:
				return permission == null /*&& component.applicationInfo.permission == null*/;
			case OBTAINABLE_PERMISSION:
				{
					if (permission == null) {
						return true;
					}
					PermissionInfo permissionInfo;
					try {
						permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
					} catch (NameNotFoundException e) {
						Log.v("PermissionFilter", "Unknown permission " + permission + " for " + cmp.name);
						return false; // filter out
					}
					return permissionInfo.protectionLevel == PermissionInfo.PROTECTION_NORMAL |
					       permissionInfo.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS;
				}
			}
			return true; // mPermissionFilter = PermissionFilter.EXPORTED
		}

		@Override
		protected Object doInBackground(Object... params) {
			PackageManager pm = getPackageManager();
	    	List<PackageInfo> allPackages =
	    			pm.getInstalledPackages(
	    					mComponentTypeFilter == IntentEditorConstants.ACTIVITY ? PackageManager.GET_ACTIVITIES :
        					mComponentTypeFilter == IntentEditorConstants.BROADCAST ? PackageManager.GET_RECEIVERS :
    						mComponentTypeFilter == IntentEditorConstants.SERVICE ? PackageManager.GET_SERVICES : 0

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
	    				mComponentTypeFilter == IntentEditorConstants.ACTIVITY ? pack.activities :
    					mComponentTypeFilter == IntentEditorConstants.BROADCAST ? pack.receivers :
						mComponentTypeFilter == IntentEditorConstants.SERVICE ? pack.services : null;

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

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		AppInfo app = mApps[groupPosition];
		AppComponentInfo cmp = app.components[childPosition];
		Intent intent = new Intent(this, ComponentInfoActivity.class);
		intent.putExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME, app.packageName);
		intent.putExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME, cmp.name);
		startActivity(intent);
		return true;
	}
}
