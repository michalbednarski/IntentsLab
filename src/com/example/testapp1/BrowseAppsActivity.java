package com.example.testapp1;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

public class BrowseAppsActivity extends Activity implements ExpandableListAdapter {

	ArrayList<DataSetObserver> mDataSetObservers = new ArrayList<DataSetObserver>();
	ArrayList<PackageInfo> mPackages;
	boolean mSystemApps = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_apps);
        if (Build.VERSION.SDK_INT >= 11) {
        	onCreateAndroidSDK11AndUp();
        }
        updateList();
        ((ExpandableListView)findViewById(R.id.listView1)).setAdapter(this);
    }

    @TargetApi(11)
	public void onCreateAndroidSDK11AndUp() {
    	getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    public void updateList() {
    	PackageManager pm = getPackageManager();
    	List<PackageInfo> allPackages = pm.getInstalledPackages(0);
    	mPackages = new ArrayList<PackageInfo>();
    	int requestedSystemFlagValue = mSystemApps ? ApplicationInfo.FLAG_SYSTEM : 0;
    	for (PackageInfo pack: allPackages) {
    		if ((pack.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != requestedSystemFlagValue) {
    			for (ActivityInfo activity: pack.activities) {
    				//pm.
    				//ActivityManager am;
    				//am.
    				
    			}
    		}
    	}
    	for (DataSetObserver observer: mDataSetObservers) {
    		observer.onChanged();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_browse_apps, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	public void registerDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.add(observer);
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		mDataSetObservers.remove(observer);
	}

	public int getGroupCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getGroup(int groupPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getGroupId(int groupPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}

	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return null;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean areAllItemsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	public void onGroupExpanded(int groupPosition) {
		// TODO Auto-generated method stub
		
	}

	public void onGroupCollapsed(int groupPosition) {
		// TODO Auto-generated method stub
		
	}

	public long getCombinedChildId(long groupId, long childId) {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getCombinedGroupId(long groupId) {
		// TODO Auto-generated method stub
		return 0;
	}

}
