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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.appinfo.MyComponentInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageManagerImpl;
import com.github.michalbednarski.intentslab.appinfo.PermissionDetails;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;
import com.github.michalbednarski.intentslab.editor.IntentEditorConstants;
import com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment;
import com.github.michalbednarski.intentslab.uihelpers.CategorizedAdapter;

import org.jdeferred.DoneCallback;

/**
 * Created by mb on 14.07.13.
 */
public class PermissionInfoFragment extends ListFragment {
    public static final String ARG_PERMISSION_NAME = "PermissionInfo.NAME";

    private PackageManager mPm;

    private CharSequence mDetailsText;
    private MyPackageInfo mDefinedBy;

    private boolean mAppListsReady = false;
    private MyPackageInfo[] mGrantedTo;
    private MyPackageInfo[] mImplicitlyGrantedTo;
    private MyPackageInfo[] mDeniedTo;
    private MyComponentInfo[] mEnforcingComponents;




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
            // TODO: load this from MyPackageManager too?
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
        } catch (PackageManager.NameNotFoundException e) {
            // Undeclared permission
            e.printStackTrace();
        }

        mDetailsText = headerText.getText();

        MyPackageManagerImpl
                .getInstance(getActivity())
                .getPermissionDetails(permissionName)
                .then(new DoneCallback<PermissionDetails>() {
                    @Override
                    public void onDone(PermissionDetails result) {
                        // Export data to fields
                        mDefinedBy = result.permissionInfo.getOwnerPackage();
                        mGrantedTo = result.grantedTo;
                        mImplicitlyGrantedTo = result.implicitlyGrantedTo;
                        mDeniedTo = result.deniedTo;
                        mEnforcingComponents = result.enforcingComponents;

                        // Fill list
                        mAppListsReady = true;
                        mListAdapter.notifyDataSetChanged();
                    }
                });
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
        if (item instanceof MyComponentInfo) {
            // Component, jump to component info
            MyComponentInfo componentInfo = (MyComponentInfo) item;
            Intent intent = new Intent(getActivity(), SingleFragmentActivity.class);
            if (componentInfo.getType() == IntentEditorConstants.PROVIDER) {
                intent.putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ProviderInfoFragment.class.getName());
            } else {
                intent.putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName());
                intent.putExtra(ComponentInfoFragment.ARG_COMPONENT_TYPE, componentInfo.getType());
            }
            startActivity(
                    intent
                    .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, componentInfo.getOwnerPackage().getPackageName())
                    .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, componentInfo.getName())
            );
        } else {
            // Package, show our package info
            String packageName = ((MyPackageInfo) item).getPackageName();
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

        private View createItemViewForPackage(View convertView, ViewGroup parent, MyPackageInfo packageInfo) {
            return createItemView(
                    convertView, parent,
                    String.valueOf(packageInfo.loadLabel(mPm)),
                    packageInfo.getPackageName(),
                    packageInfo.loadIcon(mPm)
            );
        }

        private View createItemViewForComponent(View convertView, ViewGroup parent, MyComponentInfo componentInfo) {
            return createItemView(
                    convertView, parent,
                    String.valueOf(componentInfo.loadLabel(mPm)),
                    new ComponentName(componentInfo.getOwnerPackage().getPackageName(), componentInfo.getName()).flattenToShortString(),
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