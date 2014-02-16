package com.github.michalbednarski.intentslab;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mb on 14.07.13.
 */
public class PermissionInfoFragment extends ListFragment {
    public static final String ARG_PERMISSION_NAME = "PermissionInfo.NAME";

    private PackageManager mPm;

    private CharSequence mDetailsText;
    private PackageInfo mDefinedBy;

    private boolean mAppListsReady = false;
    private PackageInfo[] mGrantedTo;
    private PackageInfo[] mImplicitlyGrantedTo;
    private PackageInfo[] mDeniedTo;
    private ComponentInfo[] mEnforcingComponents;




    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // Get system services
        mPm = getActivity().getApplicationContext().getPackageManager();

        final String permissionName = getArguments().getString(ARG_PERMISSION_NAME);

        // Get information about permission itself
        FormattedTextBuilder headerText = new FormattedTextBuilder();
        headerText.appendGlobalHeader(permissionName);
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


            headerText.appendValue(getString(R.string.permission_protection_level), protectionLevelToString(permissionInfo.protectionLevel));
            mDefinedBy = mPm.getPackageInfo(permissionInfo.packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            // Undeclared permission
            e.printStackTrace();
        }

        mDetailsText = headerText.getText();

        (new ScanUsingAppsTask()).execute(permissionName);
    }

    class ScanUsingAppsTask extends AsyncTask<String, Object, Object> {
        @Override
        protected Object doInBackground(String... params) {
            String permissionName = params[0];

            // Lists of packages
            ArrayList<PackageInfo> packagesGrantedPermission = new ArrayList<PackageInfo>();
            ArrayList<PackageInfo> packagesDeniedPermission = new ArrayList<PackageInfo>();
            ArrayList<PackageInfo> packagesImplicitlyGrantedPermission = new ArrayList<PackageInfo>();
            ArrayList<ComponentInfo> enforcingComponents = new ArrayList<ComponentInfo>();

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
                            enforcingComponents.add(activityInfo);
                        }
                    }
                }

                if (packageInfo.receivers != null) {
                    for (ActivityInfo receiverInfo : packageInfo.receivers) {
                        if (permissionName.equals(receiverInfo.permission)) {
                            enforcingComponents.add(receiverInfo);
                        }
                    }
                }

                if (packageInfo.services != null) {
                    for (ServiceInfo serviceInfo : packageInfo.services) {
                        if (permissionName.equals(serviceInfo.permission)) {
                            enforcingComponents.add(serviceInfo);
                        }
                    }
                }

                if (packageInfo.providers != null) {
                    for (ProviderInfo providerInfo : packageInfo.providers) {
                        if (permissionName.equals(providerInfo.readPermission) || permissionName.equals(providerInfo.writePermission)) {
                            enforcingComponents.add(providerInfo);
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
            mGrantedTo = packagesGrantedPermission.toArray(new PackageInfo[packagesGrantedPermission.size()]);
            mImplicitlyGrantedTo = packagesImplicitlyGrantedPermission.toArray(new PackageInfo[packagesImplicitlyGrantedPermission.size()]);
            mDeniedTo = packagesDeniedPermission.toArray(new PackageInfo[packagesDeniedPermission.size()]);
            mEnforcingComponents = enforcingComponents.toArray(new ComponentInfo[enforcingComponents.size()]);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            mAppListsReady = true;
            mListAdapter.notifyDataSetChanged();
        }
    }

    private boolean isPermissionGrantedTo(String permissionName, PackageInfo packageInfo) {
        return mPm.checkPermission(permissionName, packageInfo.packageName) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("InlinedApi")
    private static String protectionLevelToString(int protectionLevel) {
        int base = protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
        int flags = protectionLevel & PermissionInfo.PROTECTION_MASK_FLAGS;

        // Base
        StringBuilder builder = new StringBuilder(
                base == PermissionInfo.PROTECTION_NORMAL ? "normal" :
                base == PermissionInfo.PROTECTION_DANGEROUS ? "dangerous" :
                base == PermissionInfo.PROTECTION_SIGNATURE ? "signature" :
                base == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM ? "signatureOrSystem" :
                        String.valueOf(base) // If none matched
        );

        // Flags
        if ((flags & PermissionInfo.PROTECTION_FLAG_SYSTEM) != 0) {
            builder.append("|system");
        }
        if ((flags & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0) {
            builder.append("|development");
        }

        // Unrecognized flags
        int unknownFlags = flags & ~(
                PermissionInfo.PROTECTION_FLAG_SYSTEM |
                PermissionInfo.PROTECTION_FLAG_DEVELOPMENT
        );
        if (unknownFlags != 0) {
            builder.append("|");
            builder.append(unknownFlags);
        }
        return builder.toString();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListShownNoAnimation(true);
        setListAdapter(mListAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Object item = mListAdapter.getItem(position);
        if (item instanceof ComponentInfo) {
            // Component, jump to component info
            ComponentInfo componentInfo = (ComponentInfo) item;
            Intent intent = new Intent(getActivity(), SingleFragmentActivity.class);
            if (componentInfo instanceof ProviderInfo) {
                intent.putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ProviderInfoFragment.class.getName());
            } else {
                intent.putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName());
            }
            startActivity(
                    intent
                    .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, componentInfo.packageName)
                    .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, componentInfo.name)
            );
        } else {
            // Package, show our package info
            String packageName = ((PackageInfo) item).packageName;
            startActivity(
                    new Intent(getActivity(), AppInfoActivity.class)
                    .putExtra(AppInfoActivity.EXTRA_PACKAGE_NAME, packageName)
            );
        }
    }

    private final int CATEGORY_DEFINED_BY = 0;
    private final int CATEGORY_LOADING_INDICATOR = 1;
    private final int CATEGORY_GRANTED_TO = 2;
    private final int CATEGORY_IMPLICITLY_GRANTED_TO = 3;
    private final int CATEGORY_DENIED_TO = 4;
    private final int CATEGORY_ENFORCED_BY = 5;

    private CategorizedAdapter mListAdapter = new CategorizedAdapter() {
        @Override
        protected int getCategoryCount() {
            return 6;
        }

        @Override
        protected int getCountInCategory(int category) {
            switch (category) {
                case -1:
                    return 1;
                case CATEGORY_DEFINED_BY:
                    return mDefinedBy != null ? 1 : 0;
                case CATEGORY_LOADING_INDICATOR:
                    return mAppListsReady ? 0 : 1;
                case CATEGORY_GRANTED_TO:
                    return mAppListsReady ? mGrantedTo.length : 0;
                case CATEGORY_IMPLICITLY_GRANTED_TO:
                    return mAppListsReady ? mImplicitlyGrantedTo.length : 0;
                case CATEGORY_DENIED_TO:
                    return mAppListsReady ? mDeniedTo.length : 0;
                case CATEGORY_ENFORCED_BY:
                    return mAppListsReady ? mEnforcingComponents.length : 0;
            }
            return 0;
        }

        @Override
        protected String getCategoryName(int category) {
            return getString(
                    category == CATEGORY_DEFINED_BY ? R.string.permission_defined_by :
                    category == CATEGORY_LOADING_INDICATOR ? R.string.loading :
                    category == CATEGORY_GRANTED_TO ? R.string.permission_granted_to :
                    category == CATEGORY_IMPLICITLY_GRANTED_TO ? R.string.permission_implicitly_granted_to :
                    category == CATEGORY_DENIED_TO ? R.string.permission_denied_to :
                    category == CATEGORY_ENFORCED_BY ? R.string.permission_enforcing_components : 0
            );
        }

        @Override
        protected int getViewTypeInCategory(int category, int positionInCategory) {
            return category == -1 ? 2 :
                    category == CATEGORY_LOADING_INDICATOR ? 3 :1;
        }

        @Override
        protected View getViewInCategory(int category, int positionInCategory, View convertView, ViewGroup parent) {
            switch (category) {
                case -1:
                    if (convertView == null) {
                        TextView detailsTextView = new TextView(parent.getContext());
                        detailsTextView.setText(mDetailsText);
                        return detailsTextView;
                    } else {
                        return convertView;
                    }
                case CATEGORY_LOADING_INDICATOR:
                    if (convertView == null) {
                        return LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_loading2, parent, false);
                    } else {
                        return convertView;
                    }
                case CATEGORY_DEFINED_BY:
                    return createItemViewForPackage(convertView, parent, mDefinedBy);
                case CATEGORY_GRANTED_TO:
                    return createItemViewForPackage(convertView, parent, mGrantedTo[positionInCategory]);
                case CATEGORY_IMPLICITLY_GRANTED_TO:
                    return createItemViewForPackage(convertView, parent, mImplicitlyGrantedTo[positionInCategory]);
                case CATEGORY_DENIED_TO:
                    return createItemViewForPackage(convertView, parent, mDeniedTo[positionInCategory]);
                case CATEGORY_ENFORCED_BY:
                    return createItemViewForComponent(convertView, parent, mEnforcingComponents[positionInCategory]);
            }
            return null;
        }

        @Override
        protected boolean isItemInCategoryEnabled(int category, int positionInCategory) {
            return category != -1 && category != CATEGORY_LOADING_INDICATOR;
        }

        private View createItemView(View convertView, ViewGroup parent, String title, String subtitle, Drawable icon) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_list_item_2_with_icon, parent, false);
            }

            ((TextView) convertView.findViewById(android.R.id.text1)).setText(title);
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(subtitle);
            ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(icon);
            return convertView;
        }

        private View createItemViewForPackage(View convertView, ViewGroup parent, PackageInfo packageInfo) {
            return createItemView(
                    convertView, parent,
                    String.valueOf(packageInfo.applicationInfo.loadLabel(mPm)),
                    packageInfo.packageName,
                    packageInfo.applicationInfo.loadIcon(mPm)
            );
        }

        private View createItemViewForComponent(View convertView, ViewGroup parent, ComponentInfo componentInfo) {
            return createItemView(
                    convertView, parent,
                    String.valueOf(componentInfo.loadLabel(mPm)),
                    new ComponentName(componentInfo.packageName, componentInfo.name).flattenToShortString(),
                    componentInfo.loadIcon(mPm)
            );
        }

        @Override
        protected Object getItemInCategory(int category, int positionInCategory) {
            switch (category) {
                case CATEGORY_DEFINED_BY:
                    return mDefinedBy;
                case CATEGORY_GRANTED_TO:
                    return mGrantedTo[positionInCategory];
                case CATEGORY_IMPLICITLY_GRANTED_TO:
                    return mImplicitlyGrantedTo[positionInCategory];
                case CATEGORY_DENIED_TO:
                    return mDeniedTo[positionInCategory];
                case CATEGORY_ENFORCED_BY:
                    return mEnforcingComponents[positionInCategory];
            }
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 4; // category header + details header + 2 line item with icon + loading indicator
        }
    };
}