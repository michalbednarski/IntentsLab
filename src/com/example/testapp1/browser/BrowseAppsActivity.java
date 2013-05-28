package com.example.testapp1.browser;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
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
import com.example.testapp1.Utils;
import com.example.testapp1.providerlab.ProviderInfoActivity;

public class BrowseAppsActivity extends Activity implements ExpandableListAdapter, OnChildClickListener {

    private final static int ITEM_ID_SPLIT_BASE = 1000;
    private static final String TAG = "BrowseAppsActivity";

    ArrayList<DataSetObserver> mDataSetObservers = new ArrayList<DataSetObserver>();
    AppInfo mApps[] = null;
    ExpandableListView mList;
    boolean mListAdapterSet = false;
    TextView mMessage;

    //private class CachedPackagesInfo {};

    AppsBrowserFilter filter = new AppsBrowserFilter();
    boolean useCustomFilter = false;

    private FetchAppsTask mTask = null;

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
        if (mTask != null) {
            mTask.cancel(true);
        }
        mTask = new FetchAppsTask();
        mTask.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_browse_apps, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (useCustomFilter) {
            //menu.findItem(R.id.simple_filter).setVisible(false).setEnabled(false);
            menu.findItem(R.id.system_apps).setVisible(false).setEnabled(false);
            menu.findItem(R.id.user_apps).setVisible(false).setEnabled(false);
            menu.findItem(R.id.simple_filter_component_type).setVisible(false).setEnabled(false);
            menu.findItem(R.id.simple_filter_permission).setVisible(false).setEnabled(false);
        } else {
            boolean selectedSystemApps = filter.appType != AppsBrowserFilter.APP_TYPE_USER;
            menu.findItem(R.id.system_apps).setVisible(!selectedSystemApps).setEnabled(!selectedSystemApps);
            menu.findItem(R.id.user_apps).setVisible(selectedSystemApps).setEnabled(selectedSystemApps);
            menu.findItem(
                    filter.type == AppsBrowserFilter.TYPE_ACTIVITY ? R.id.activities :
                    filter.type == AppsBrowserFilter.TYPE_RECEIVER ? R.id.broadcasts :
                    filter.type == AppsBrowserFilter.TYPE_SERVICE ? R.id.services :
                    filter.type == AppsBrowserFilter.TYPE_CONTENT_PROVIDER ? R.id.content_providers : R.id.activities
            ).setChecked(true); /* this boolean value is ignored for radio buttons - system always thinks it's true */
            menu.findItem(
                    filter.protection == AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE ? R.id.permission_filter_world_accessible :
                    filter.protection == AppsBrowserFilter.PROTECTION_ANY_OBTAINABLE ? R.id.permission_filter_obtainable :
                    filter.protection == AppsBrowserFilter.PROTECTION_ANY_EXPORTED ? R.id.permission_filter_exported :
                    filter.protection == AppsBrowserFilter.PROTECTION_ANY ? R.id.permission_filter_all : 0
            ).setChecked(true);
            menu.findItem(R.id.simple_filter_component_type).setVisible(true).setEnabled(true);
            menu.findItem(R.id.simple_filter_permission).setVisible(true).setEnabled(true);
        }
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
                filter.appType = ((itemId == R.id.system_apps) ? AppsBrowserFilter.APP_TYPE_SYSTEM : AppsBrowserFilter.APP_TYPE_USER);
                safelyInvalidateOptionsMenu();
                updateList();
                return true;
            case R.id.activities:
            case R.id.broadcasts:
            case R.id.services:
            case R.id.content_providers:
                filter.type =
                        itemId == R.id.activities ? AppsBrowserFilter.TYPE_ACTIVITY :
                        itemId == R.id.broadcasts ? AppsBrowserFilter.TYPE_RECEIVER :
                        itemId == R.id.services ? AppsBrowserFilter.TYPE_SERVICE :
                            AppsBrowserFilter.TYPE_CONTENT_PROVIDER;
                safelyInvalidateOptionsMenu();
                updateList();
                return true;
            case R.id.permission_filter_all:
            case R.id.permission_filter_exported:
            case R.id.permission_filter_obtainable:
            case R.id.permission_filter_world_accessible:
                filter.protection =
                        itemId == R.id.permission_filter_all ? AppsBrowserFilter.PROTECTION_ANY :
                        itemId == R.id.permission_filter_exported ? AppsBrowserFilter.PROTECTION_ANY_EXPORTED :
                        itemId == R.id.permission_filter_obtainable ? AppsBrowserFilter.PROTECTION_ANY_OBTAINABLE :
                            AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE;
                safelyInvalidateOptionsMenu();
                updateList();
                return true;
            case R.id.custom_filter:
                new EditFilterDialog(this).showDialog();
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
        ((TextView) convertView.findViewById(android.R.id.text1))
                .setText(app.appName);
        ((TextView) convertView.findViewById(android.R.id.text2))
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
        ((TextView) convertView.findViewById(android.R.id.text1))
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

    void setCustomFilter(AppsBrowserFilter newFilter) {
        filter = newFilter;
        useCustomFilter = true;
        safelyInvalidateOptionsMenu();
        updateList();
    }

    static class AppInfo {
        String appName;
        String packageName;
        AppComponentInfo components[];
    }

    static class AppComponentInfo {
        String name;
        boolean isProvider = false;
    }

    class FetchAppsTask extends AsyncTask</*Params*/Object, /*Progress*/Object, /*Result*/Object> {

        @Override
        protected void onPreExecute() {
            mList.setVisibility(View.GONE);
            mMessage.setVisibility(View.VISIBLE);
            mMessage.setText(R.string.loading_apps_list);
        }

        private String getProviderPermissionName(ProviderInfo providerInfo) {
            return providerInfo.readPermission;
        }

        @SuppressLint("InlinedApi")
        private boolean checkPermissionFilter(ComponentInfo cmp) {

            if (!cmp.exported) {
                return (filter.protection & AppsBrowserFilter.PROTECTION_UNEXPORTED) != 0;
            }

            String permission =
                    cmp instanceof ServiceInfo ?
                        ((ServiceInfo) cmp).permission :
                    cmp instanceof ActivityInfo ?
                        ((ActivityInfo) cmp).permission :
                    cmp instanceof ProviderInfo ?
                        getProviderPermissionName((ProviderInfo) cmp) :
                    null;
                    ;

            if (permission == null) {
                return (filter.protection & AppsBrowserFilter.PROTECTION_WORLD_ACCESSIBLE) != 0;
            }

            if ((filter.protection & AppsBrowserFilter.PROTECTION_ANY_PERMISSION) == AppsBrowserFilter.PROTECTION_ANY_PERMISSION) {
                return true;
            }

            PermissionInfo permissionInfo;
            try {
                permissionInfo = getPackageManager().getPermissionInfo(permission, 0);
            } catch (NameNotFoundException e) {
                Log.v("PermissionFilter", "Unknown permission " + permission + " for " + cmp.name);
                return (filter.protection & AppsBrowserFilter.PROTECTION_UNKNOWN) != 0;
            }

            int protectionLevel = permissionInfo.protectionLevel;
            int protectionLevelBase = protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
            int protectionLevelFlags = protectionLevel & PermissionInfo.PROTECTION_MASK_FLAGS;
            return ((
                protectionLevel == PermissionInfo.PROTECTION_NORMAL ? AppsBrowserFilter.PROTECTION_NORMAL :
                protectionLevel == PermissionInfo.PROTECTION_DANGEROUS ? AppsBrowserFilter.PROTECTION_DANGEROUS :
                    (
                        ((protectionLevelBase == PermissionInfo.PROTECTION_SIGNATURE ||
                          protectionLevelBase == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM)
                            ? AppsBrowserFilter.PROTECTION_SIGNATURE : 0) |
                        ((protectionLevelBase == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM ||
                         (protectionLevelFlags & PermissionInfo.PROTECTION_FLAG_SYSTEM) != 0)
                            ? AppsBrowserFilter.PROTECTION_SYSTEM : 0) |
                        (((protectionLevelFlags & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0)
                            ? AppsBrowserFilter.PROTECTION_SYSTEM : 0)
                    )
            ) & filter.protection) != 0;
        }

        private boolean checkMetaDataFilter(ComponentInfo cmp) {
            if (!filter.requireMetaData) {
                return true;
            }
            if (cmp.metaData == null || cmp.metaData.isEmpty()) {
                return false;
            }
            if (Utils.stringEmptyOrNull(filter.requireMetaDataSubstring)) {
                return true;
            }
            for (String key : cmp.metaData.keySet()) {
                if (key.contains(filter.requireMetaDataSubstring)) {
                    return true;
                }
            }
            return false;
        }

        private void scanComponents(ComponentInfo[] components, ArrayList<AppComponentInfo> outList) {
            // Skip apps not having any components of requested type
            if (!(components != null && components.length != 0)) {
                return;
            }

            // Scan components
            for (ComponentInfo cmp : components) {
                if (!checkPermissionFilter(cmp)) {
                    continue;
                }
                if (!checkMetaDataFilter(cmp)) {
                    continue;
                }
                AppComponentInfo component = new AppComponentInfo();
                component.name = cmp.name;
                component.isProvider = cmp instanceof ProviderInfo;
                outList.add(component);
            }
        }

        @Override
        protected Object doInBackground(Object... params) {
            PackageManager pm = getPackageManager();
            int requestedPackageInfoFlags =
                    ((filter.type & AppsBrowserFilter.TYPE_ACTIVITY) != 0 ? PackageManager.GET_ACTIVITIES : 0) |
                    ((filter.type & AppsBrowserFilter.TYPE_RECEIVER) != 0 ? PackageManager.GET_RECEIVERS : 0) |
                    ((filter.type & AppsBrowserFilter.TYPE_SERVICE) != 0 ? PackageManager.GET_SERVICES : 0) |
                    ((filter.type & AppsBrowserFilter.TYPE_CONTENT_PROVIDER) != 0 ? PackageManager.GET_PROVIDERS : 0) |
                    (filter.requireMetaData ? PackageManager.GET_META_DATA : 0);

            List<PackageInfo> allPackages = pm.getInstalledPackages(requestedPackageInfoFlags);

            boolean workAroundSmallBinderBuffer = false;

            if (allPackages.isEmpty()) {
                workAroundSmallBinderBuffer = true;
                allPackages = pm.getInstalledPackages(0);
            }

            ArrayList<AppInfo> selectedApps = new ArrayList<AppInfo>();

            for (PackageInfo pack : allPackages) {
                // Filter out non-applications
                if (pack.applicationInfo == null) {
                    continue;
                }

                // System app filter
                if (((
                        (pack.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ?
                                AppsBrowserFilter.APP_TYPE_SYSTEM :
                                AppsBrowserFilter.APP_TYPE_USER)
                        & filter.appType) == 0) {
                    continue;
                }

                // Load component information separately if they were to big to send them all at once
                if (workAroundSmallBinderBuffer) {
                    try {
                        pack = pm.getPackageInfo(pack.packageName, requestedPackageInfoFlags);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "getPackageInfo() thrown NameNotFoundException for " + pack.packageName);
                        continue;
                    }
                }

                // Scan components

                ArrayList<AppComponentInfo> selectedComponents = new ArrayList<AppComponentInfo>();

                if ((filter.type & AppsBrowserFilter.TYPE_ACTIVITY) != 0) {
                    scanComponents(pack.activities, selectedComponents);
                }
                if ((filter.type & AppsBrowserFilter.TYPE_RECEIVER) != 0) {
                    scanComponents(pack.receivers, selectedComponents);
                }
                if ((filter.type & AppsBrowserFilter.TYPE_SERVICE) != 0) {
                    scanComponents(pack.services, selectedComponents);
                }
                if ((filter.type & AppsBrowserFilter.TYPE_CONTENT_PROVIDER) != 0) {
                    scanComponents(pack.providers, selectedComponents);
                }

                // Check if we filtered out all components and skip whole app if so
                if (selectedComponents.isEmpty()) {
                    continue;
                }

                // Build and add app descriptor
                AppInfo app = new AppInfo();
                app.appName = pack.applicationInfo.loadLabel(pm).toString();
                app.packageName = pack.packageName;
                app.components = selectedComponents.toArray(new AppComponentInfo[selectedComponents.size()]);
                selectedApps.add(app);
            }
            mApps = selectedApps.toArray(new AppInfo[selectedApps.size()]);
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            for (DataSetObserver observer : mDataSetObservers) {
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
        Intent intent = new Intent(this, (cmp.isProvider ? ProviderInfoActivity.class : ComponentInfoActivity.class));
        intent.putExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME, app.packageName);
        intent.putExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME, cmp.name);
        startActivity(intent);
        return true;
    }
}
