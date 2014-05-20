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

package com.github.michalbednarski.intentslab.providerlab;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Autocomplete for Uris for Intent's data and content providers
 */
public class UriAutocompleteAdapter extends BaseAdapter implements Filterable {
    private static final int LAYOUT_ID = android.R.layout.simple_list_item_1;

    private MyFilter mMyFilter = new MyFilter();
    private Context mContext;

    Suggestions mSuggestions = null;
    private IntentFilter[] mIntentFilters;

    private class Suggestions {
        private String[] suggestions;
        private String prefix;

        private Suggestions(String[] suggestions, String prefix) {
            this.suggestions = suggestions;
            this.prefix = prefix;
        }
    }


    public UriAutocompleteAdapter(Context context) {
        mContext = context;
    }

    public void setIntentFilters(IntentFilter[] filters) {
        if (filters == null || filters.length == 0) {
            mIntentFilters = null;
        } else {
            mIntentFilters = filters;
        }
    }

    @Override
    public int getCount() {
        return mSuggestions == null ? 0 : mSuggestions.suggestions.length;
    }

    @Override
    public Object getItem(int position) {
        return mSuggestions.prefix + mSuggestions.suggestions[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(LAYOUT_ID, parent, false);
        }
        ((TextView) convertView.findViewById(android.R.id.text1))
                .setText(mSuggestions.suggestions[position]);
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return mMyFilter;
    }

    private class MyFilter extends Filter {



        @Override
        protected FilterResults performFiltering(CharSequence constraintSeq) {
            if (constraintSeq == null) {
                return null;
            }
            String constraint = constraintSeq.toString();
            int pos = constraint.indexOf(':');
            if (pos != -1) {
                String scheme = constraint.substring(0, pos);
                if (scheme.equals("package") || scheme.equals("android.resource")) {
                    return generatePackageSuggestions(scheme, constraint);
                } else if (scheme.equals("content")) {
                    return generateContentProvidersSuggestions(constraint);
                } else if (mIntentFilters != null) {
                    return generateSuggestionsFromIntentFilter(constraint);
                }
            } else {
                if (mIntentFilters != null) {
                    return generateSuggestionsFromIntentFilter(constraint);
                }
            }
            return null;
        }

        /**
         * Generate suggestions based on <intent-filter>s attached to intent editor
         */
        private FilterResults generateSuggestionsFromIntentFilter(String constraint) {
            String prefix;
            HashSet<String> allSuggestions = new HashSet<String>();
            String keywords;

            // Check what part is missing and load suggestion set
            int schemeSeparator = constraint.indexOf(":");
            if (schemeSeparator == -1) { // Fill scheme
                prefix = "";
                keywords = constraint;
                for (IntentFilter filter : mIntentFilters) {
                    for (int i = 0, j = filter.countDataSchemes(); i < j; i++) {
                        allSuggestions.add(filter.getDataScheme(i) + "://");
                    }
                }

            } else {
                int pathSeparator = constraint.indexOf('/', schemeSeparator + 3);
                if (pathSeparator == -1) { // Path missing, fill authority (host)
                    keywords = constraint.substring(schemeSeparator + 1);
                    if (keywords.startsWith("//")) {
                        keywords = keywords.substring(2);
                    }
                    prefix = constraint.substring(0, schemeSeparator) + "://";
                    for (IntentFilter filter : mIntentFilters) {
                        for (int i = 0, j = filter.countDataAuthorities(); i < j; i++) {
                            IntentFilter.AuthorityEntry authority = filter.getDataAuthority(i);
                            String string = authority.getHost();
                            if (authority.getPort() != -1) {
                                string += ":" + authority.getPort();
                            }
                            allSuggestions.add(string);
                        }
                    }

                } else { // Fill path

                    prefix = constraint.substring(0, pathSeparator);
                    keywords = constraint.substring(pathSeparator + 1);
                    for (IntentFilter filter : mIntentFilters) {
                        for (int i = 0, j = filter.countDataPaths(); i < j; i++) {
                            String path = filter.getDataPath(i).getPath();
                            if (path.length() >= 2 && path.charAt(0) == '/') {
                                allSuggestions.add(path);
                            }
                        }
                    }
                }
            }

            // Now filter whats found
            String[] splittedKeywords = keywords.split(" ");
            ArrayList<String> selectedSuggestions = new ArrayList<String>();
            checkSuggestion:
            for (String suggestion : allSuggestions) {
                for (String keyword : splittedKeywords) {
                    if (!suggestion.contains(keyword)) {
                        continue checkSuggestion;
                    }
                }
                selectedSuggestions.add(suggestion);
            }

            Suggestions suggestions = new Suggestions(selectedSuggestions.toArray(new String[selectedSuggestions.size()]), prefix);
            FilterResults results = new FilterResults();
            results.values = suggestions;
            results.count = suggestions.suggestions.length;
            return results;
        }

        private FilterResults generateContentProvidersSuggestions(String constraint) {
            // Trim scheme away
            if (constraint.startsWith("content://")) {
                constraint = constraint.substring(10);
            } else if (constraint.startsWith("content:")) {
                constraint = constraint.substring(8);
            }

            String[] tokens = null;
            if (!constraint.equals("")) {
                tokens = constraint.split(" ");
            }

            ArrayList<String> a = new ArrayList<String>();

            // Scan and filter providers
            PackageManager packageManager = mContext.getPackageManager();
            for (PackageInfo packageInfo : packageManager.getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                if (packageInfo.providers != null) {
                    scanProviders:
                    for (ProviderInfo provider : packageInfo.providers) {
                        if (provider.authority != null) {
                            for (String authority : provider.authority.split(";")) {
                                if (tokens != null) {
                                    for (String token : tokens) {
                                        if (!authority.contains(token)) {
                                            continue scanProviders;
                                        }
                                    }
                                }
                                a.add(authority);
                            }
                        }
                    }
                }
            }

            // Return results
            Suggestions suggestions = new Suggestions(a.toArray(new String[a.size()]), "content://");
            FilterResults results = new FilterResults();
            results.values = suggestions;
            results.count = a.size();
            return results;
        }


        /**
         * Generate suggestions based on list of installed packages
         * for package: and similar schemes
         */
        private FilterResults generatePackageSuggestions(String scheme, String constraint) {
            PackageManager packageManager = mContext.getPackageManager();
            List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);


            // Trim constraints and get search tokens
            if (constraint.startsWith(scheme + ":")) {
                constraint = constraint.substring(scheme.length() + 1);
                if (constraint.startsWith("//")) {
                    constraint = constraint.substring(2);
                }
            }
            String[] tokens = null;
            if (!constraint.equals("")) {
                tokens = constraint.split(" ");
            }

            // Iterate over packages and filter matching
            ArrayList<String> packageNames = new ArrayList<String>();
            packages:
            for (PackageInfo packageInfo : installedPackages) {
                if (tokens != null) {
                    String packageKeywords = packageInfo.packageName;
                    if (packageInfo.applicationInfo != null) {
                        packageKeywords += " " + packageInfo.applicationInfo.loadDescription(packageManager);
                    }
                    for (String token : tokens) {
                        if (!packageKeywords.contains(token)) {
                            continue packages;
                        }
                    }
                }
                packageNames.add(packageInfo.packageName);
            }

            // Return results
            Suggestions suggestions = new Suggestions(packageNames.toArray(new String[packageNames.size()]), scheme + ":");
            FilterResults results = new FilterResults();
            results.values = suggestions;
            results.count = packageNames.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results == null || results.count == 0) {
                if (mSuggestions == null) {
                    return;
                }
                mSuggestions = null;
            } else {
                mSuggestions = (Suggestions) results.values;
            }
            notifyDataSetChanged();
        }
    }
}
