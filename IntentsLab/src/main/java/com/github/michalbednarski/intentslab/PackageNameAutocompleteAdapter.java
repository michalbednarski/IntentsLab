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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto complete adapter for package names
 */
public class PackageNameAutocompleteAdapter extends BaseAdapter implements Filterable {
    Context mContext;
    MyPackageInfo[] mAllPackages = null;
    MyPackageInfo[] mMatchingPackages = null;

    public PackageNameAutocompleteAdapter(Context context) {
        mContext = context;
    }

    @Override
    public int getCount() {
        return mMatchingPackages == null ? 0 : mMatchingPackages.length;
    }

    @Override
    public Object getItem(int position) {
        return mMatchingPackages[position].packageName;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        }
        MyPackageInfo aPackage = mMatchingPackages[position];
        ((TextView) convertView.findViewById(android.R.id.text1)).setText(aPackage.label);
        ((TextView) convertView.findViewById(android.R.id.text2)).setText(aPackage.packageName);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private Filter mFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (TextUtils.isEmpty(constraint)) {
                return new FilterResults(); // Return empty result
            }

            MyPackageInfo[] allPackages;

            // Prepare list of packages
            synchronized (PackageNameAutocompleteAdapter.this) {
                if (mAllPackages == null) {
                    PackageManager pm = mContext.getPackageManager();
                    List<PackageInfo> installedPackages = pm.getInstalledPackages(0);
                    ArrayList<MyPackageInfo> stagingAllPackages = new ArrayList<MyPackageInfo>();
                    for (PackageInfo packageInfo : installedPackages) {
                        if (packageInfo.applicationInfo != null) {
                            stagingAllPackages.add(new MyPackageInfo(packageInfo, pm));
                        }
                    }
                    allPackages = stagingAllPackages.toArray(new MyPackageInfo[stagingAllPackages.size()]);
                    mAllPackages = allPackages;
                } else {
                    allPackages = mAllPackages;
                }
            }

            // Filter it
            ArrayList<MyPackageInfo> matchingPackages = new ArrayList<MyPackageInfo>();
            for (MyPackageInfo myPackageInfo : allPackages) {
                if (myPackageInfo.matchesConstraint(constraint)) {
                    matchingPackages.add(myPackageInfo);
                }
            }

            // Post results
            FilterResults results = new FilterResults();
            results.count = matchingPackages.size();
            results.values = matchingPackages.toArray(new MyPackageInfo[matchingPackages.size()]);
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mMatchingPackages = (MyPackageInfo[]) results.values;
            notifyDataSetChanged();
        }
    };

    private static class MyPackageInfo {
        String label;
        String packageName;

        MyPackageInfo(PackageInfo packageInfo, PackageManager pm) {
            label = String.valueOf(packageInfo.applicationInfo.loadLabel(pm));
            packageName = packageInfo.packageName;
        }

        boolean matchesConstraint(CharSequence constraint) {
            return label.toUpperCase().contains(constraint.toString().toUpperCase()) || packageName.contains(constraint);
        }
    }
}
