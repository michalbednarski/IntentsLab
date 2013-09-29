package com.github.michalbednarski.intentslab;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.*;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.michalbednarski.intentslab.browser.ComponentInfoActivity;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mb on 14.07.13.
 */
public class PermissionInfoActivity extends ListActivity implements AdapterView.OnItemClickListener {
    public static final String EXTRA_PERMISSION_NAME = "PermissionInfo.NAME";

    private TextView mDetailsBeforeListView;
    private PackageManager mPm;
    private LayoutInflater mInflater;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get system services
        mPm = getPackageManager();
        mInflater = getLayoutInflater();

        final String permissionName = getIntent().getStringExtra(EXTRA_PERMISSION_NAME);

        // Get information about permission itself
        FormattedTextBuilder headerText = new FormattedTextBuilder();
        headerText.appendGlobalHeader(permissionName);
        PackageInfo declaringPackage = null;
        try {
            final PermissionInfo permissionInfo = mPm.getPermissionInfo(permissionName, 0);

            headerText.appendGlobalHeader(String.valueOf(permissionInfo.loadLabel(mPm)));
            headerText.appendValue(getString(R.string.description), String.valueOf(permissionInfo.loadDescription(mPm)));

            try {
                if (permissionInfo.group != null) {
                    headerText.appendValue(getString(R.string.permission_group_name), permissionInfo.group);
                    final PermissionGroupInfo permissionGroupInfo = mPm.getPermissionGroupInfo(permissionInfo.group, 0);
                    headerText.appendValueNoNewLine(getString(R.string.permission_group_label), String.valueOf(permissionGroupInfo.loadLabel(mPm)));
                    headerText.appendValueNoNewLine(getString(R.string.permission_group_description), String.valueOf(permissionGroupInfo.loadDescription(mPm)));
                }
            } catch (PackageManager.NameNotFoundException ignored) {}


            headerText.appendValue(getString(R.string.permission_protection_level), String.valueOf(permissionInfo.protectionLevel));
            declaringPackage = mPm.getPackageInfo(permissionInfo.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // Undeclared permission
            e.printStackTrace();
        }

        mDetailsBeforeListView = new TextView(this);
        mDetailsBeforeListView.setText(headerText.getText());

        // Lists of packages
        ArrayList<PackageInfo> packagesGrantedPermission = new ArrayList<PackageInfo>();
        ArrayList<PackageInfo> packagesDeniedPermission = new ArrayList<PackageInfo>();
        ArrayList<PackageInfo> packagesImplicitlyGrantedPermission = new ArrayList<PackageInfo>();

        // Scan packages
        List<PackageInfo> installedPackages = mPm.getInstalledPackages(
                PackageManager.GET_ACTIVITIES |
                PackageManager.GET_RECEIVERS |
                PackageManager.GET_SERVICES |
                PackageManager.GET_PROVIDERS |
                PackageManager.GET_PERMISSIONS
        );
        boolean workAroundSmallBinderBuffer = false;
        if (installedPackages.size() == 0) {
            installedPackages = mPm.getInstalledPackages(0);
            workAroundSmallBinderBuffer = true;
        }

        for (PackageInfo packageInfo : installedPackages) {
            if (workAroundSmallBinderBuffer) {
                try {
                    packageInfo = mPm.getPackageInfo(packageInfo.packageName,
                            PackageManager.GET_ACTIVITIES |
                            PackageManager.GET_RECEIVERS |
                            PackageManager.GET_SERVICES |
                            PackageManager.GET_PROVIDERS |
                            PackageManager.GET_PERMISSIONS
                    );
                } catch (PackageManager.NameNotFoundException e) {
                    // Shouldn't happen (package removed in meantime?)
                    e.printStackTrace();
                    continue;
                }
            }

            // Find components enforcing this permission
            if (packageInfo.activities != null) {
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    if (permissionName.equals(activityInfo.permission)) {
                        mEnforcingComponents.add(activityInfo);
                    }
                }
            }

            if (packageInfo.receivers != null) {
                for (ActivityInfo receiverInfo : packageInfo.receivers) {
                    if (permissionName.equals(receiverInfo.permission)) {
                        mEnforcingComponents.add(receiverInfo);
                    }
                }
            }

            if (packageInfo.services != null) {
                for (ServiceInfo serviceInfo : packageInfo.services) {
                    if (permissionName.equals(serviceInfo.permission)) {
                        mEnforcingComponents.add(serviceInfo);
                    }
                }
            }

            if (packageInfo.providers != null) {
                for (ProviderInfo providerInfo : packageInfo.providers) {
                    if (permissionName.equals(providerInfo.readPermission) || permissionName.equals(providerInfo.writePermission)) {
                        mEnforcingComponents.add(providerInfo);
                    }
                }
            }

            // Check if app requested/has permission
            if (
                    packageInfo.requestedPermissions != null &&
                    Arrays.asList(packageInfo.requestedPermissions).contains(permissionName)) {
                if (isPermissionGrantedTo(permissionName, packageInfo)) {
                    packagesGrantedPermission.add(packageInfo);
                } else {
                    packagesDeniedPermission.add(packageInfo);
                }
                continue;
            }
            if (isPermissionGrantedTo(permissionName, packageInfo)) {
                packagesImplicitlyGrantedPermission.add(packageInfo);
            }
        }

        // Prepare ListView displaying all these lists
        new MultiListBuilder()
                .add(getString(R.string.permission_defined_by), declaringPackage == null ? null : new PackageInfo[]{ declaringPackage })
                .add(getString(R.string.permission_granted_to), packagesGrantedPermission)
                .add(getString(R.string.permission_implicitly_granted_to), packagesImplicitlyGrantedPermission)
                .add(getString(R.string.permission_denied_to), packagesDeniedPermission)
                .build();


    }

    private boolean isPermissionGrantedTo(String permissionName, PackageInfo packageInfo) {
        return mPm.checkPermission(permissionName, packageInfo.packageName) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int listId = ~Arrays.binarySearch(mListBoundaries, position) - 1;
        int positionInSubList = position - mListBoundaries[listId] - 1;
        if (mListEntries[listId] == null) {
            // Component, jump to component info
            ComponentInfo componentInfo = mEnforcingComponents.get(positionInSubList);
            Intent intent;
            if (componentInfo instanceof ProviderInfo) {
                intent = new Intent(this, ProviderInfoActivity.class);
            } else {
                intent = new Intent(this, ComponentInfoActivity.class);
            }
            startActivity(
                    intent
                    .putExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME, componentInfo.packageName)
                    .putExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME, componentInfo.name)
            );
        } else {
            // Package, show our package info
            String packageName = mListEntries[listId][positionInSubList].packageName;
            startActivity(
                    new Intent(this, AppInfoActivity.class)
                    .putExtra(AppInfoActivity.EXTRA_PACKAGE_NAME, packageName)
            );
        }
    }

    private class MultiListBuilder {
        private int mItemsSoFar = 1;
        private ArrayList<String> mBuilderListHeaders = new ArrayList<String>();
        private ArrayList<PackageInfo[]> mBuilderListEntries = new ArrayList<PackageInfo[]>();
        private ArrayList<Integer> mBuilderListBoundaries = new ArrayList<Integer>();




        MultiListBuilder add(String header, PackageInfo[] packageInfos) {
            if (packageInfos != null && packageInfos.length != 0) {
                mBuilderListHeaders.add(header);
                mBuilderListEntries.add(packageInfos);
                mBuilderListBoundaries.add(mItemsSoFar);
                mItemsSoFar += 1 + packageInfos.length;
            }
            return this;
        }

        MultiListBuilder add(String header, ArrayList<PackageInfo> packageInfos) {
            if (packageInfos.size() != 0) {
                add(header, packageInfos.toArray(new PackageInfo[packageInfos.size()]));
            }
            return this;
        }

        void build() {
            // Add list of components
            mBuilderListHeaders.add(getString(R.string.permission_enforcing_components));
            mBuilderListEntries.add(null);
            mBuilderListBoundaries.add(mItemsSoFar);

            // Convert local array lists to real arrays
            mListHeaders = mBuilderListHeaders.toArray(new String[mBuilderListHeaders.size()]);
            mListEntries = mBuilderListEntries.toArray(new PackageInfo[mBuilderListHeaders.size()][]);
            mListBoundaries = new int[mBuilderListHeaders.size()];
            int i = 0;
            for (Integer boundary : mBuilderListBoundaries) {
                mListBoundaries[i++] = boundary;
            }
            mListItemsCount = mItemsSoFar;

            // Set list adapter and events
            setListAdapter(mListAdapter);
            getListView().setOnItemClickListener(PermissionInfoActivity.this);
        }
    };

    private String[] mListHeaders;
    private PackageInfo[][] mListEntries;
    private int[] mListBoundaries;
    private int mListItemsCount;

    private ArrayList<ComponentInfo> mEnforcingComponents = new ArrayList<ComponentInfo>();

    private ListAdapter mListAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mListItemsCount + (mEnforcingComponents.size() != 0 ? 1 + mEnforcingComponents.size() : 0);
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return 2;
            }
            return Arrays.binarySearch(mListBoundaries, position) >= 0 ? 1 : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                return mDetailsBeforeListView;
            }
            int listId = Arrays.binarySearch(mListBoundaries, position);
            if (listId >= 0) {
                // Header
                if (convertView == null) {
                    convertView = mInflater.inflate(android.R.layout.preference_category, parent, false);
                }
                ((TextView) convertView).setText(mListHeaders[listId]);
            } else {
                // List item
                listId = ~listId - 1;
                if (convertView == null) {
                    convertView = mInflater.inflate(R.layout.simple_list_item_2_with_icon, parent, false);
                }
                if (mListEntries[listId] == null) {
                    // Component
                    final ComponentInfo componentInfo = mEnforcingComponents.get(position - mListBoundaries[listId] - 1);
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(componentInfo.loadLabel(mPm));
                    ((TextView) convertView.findViewById(android.R.id.text2)).setText(new ComponentName(componentInfo.packageName, componentInfo.name).flattenToShortString());
                    ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(componentInfo.loadIcon(mPm));
                } else {
                    // Package
                    PackageInfo packageInfo = mListEntries[listId][position - mListBoundaries[listId] - 1];
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(packageInfo.applicationInfo.loadLabel(mPm));
                    ((TextView) convertView.findViewById(android.R.id.text2)).setText(packageInfo.packageName);
                    ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(packageInfo.applicationInfo.loadIcon(mPm));
                }
            }
            return convertView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return position != 0 && Arrays.binarySearch(mListBoundaries, position) < 0;
        }
    };
}