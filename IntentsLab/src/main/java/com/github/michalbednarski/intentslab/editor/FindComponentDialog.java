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

package com.github.michalbednarski.intentslab.editor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.appinfo.MyComponentInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageManagerImpl;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;

import org.jdeferred.DoneCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Dialog for picking app by inexact intent filters.
 */
public class FindComponentDialog extends DialogFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener, DialogInterface.OnKeyListener {

    /**
     * ListAdapter for list of apps/components
     */
    private final AppListAdapter mAppListAdapter = new AppListAdapter();



    /**
     * Flag that scanning is finished.
     *
     * May be set back to false from true.
     * Must be modified only by {@link ListFiltersTask}
     */
    private boolean mIsScanningFinished = false;


    /**
     * Intent for which we look for IntentFilters
     */
    private Intent mIntent = null;

    /**
     * Flag for {@link #mFlags}
     * When scanning IntentFilters ignore case and match just substrings from values
     */
    private static final int FLAG_CASE_INSENSITIVE_AND_SUBSTRING = 1;
    private static final int FLAG_TEST_ACTION = 2;
    private static final int FLAG_TEST_CATEGORIES = 4;


    /**
     * Currently selected filtering flags.
     * To get used filtering options bit-and this value with {@link #mAvailableFlags}
     */
    private int mFlags =
            FLAG_CASE_INSENSITIVE_AND_SUBSTRING |
            FLAG_TEST_ACTION |
            FLAG_TEST_CATEGORIES;

    /**
     * Flags that can be used and shown to user in filter options
     */
    private int mAvailableFlags = 0;

    /**
     * Package from which we're displaying components or null if from all
     */
    private AppWithMatchingFilters mInPackage = null;

    private View mProgressView;
    private ListView mListView;
    private Parcelable mAllAppsListViewState;

    public FindComponentDialog() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable instance retaining, we'll continue scanning when configuration changes
        setRetainInstance(true);

        // Get intent
        final IntentEditorActivity intentEditor = (IntentEditorActivity) getActivity();
        mIntent = intentEditor.getEditedIntent(); // Before showing dialog IntentEditorActivity updates Intent

        // Calculate available flags
        if (mIntent.getAction() != null && !"".equals(mIntent.getAction())) {
            mAvailableFlags |= FLAG_TEST_ACTION | FLAG_CASE_INSENSITIVE_AND_SUBSTRING;
        }

        if (mIntent.getCategories() != null && !mIntent.getCategories().isEmpty()) {
            mAvailableFlags |= FLAG_TEST_CATEGORIES | FLAG_CASE_INSENSITIVE_AND_SUBSTRING;
        }

        // Start scanning
        (new ListFiltersTask()).execute();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.find);
        dialog.setOnKeyListener(this);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.attach_intent_filter, container, false);

        mProgressView = view.findViewById(R.id.progress);

        // Prepare ListView
        mListView = (ListView) view.findViewById(R.id.apps_list_view);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        Utils.fixListViewInDialogBackground(mListView);
        if (mIsScanningFinished) {
            hideProgressAndShowList();
        }

        // Hide unneeded button or attach events
        Button optionsButton = (Button) view.findViewById(R.id.options);
        if (mAvailableFlags == 0) {
            optionsButton.setVisibility(View.GONE);
        } else {
            optionsButton.setOnClickListener(this);
        }

        return view;
    }

    void hideProgressAndShowList() {
        if (mListView != null) {
            mProgressView.setVisibility(View.GONE);
            mListView.setVisibility(View.VISIBLE);
            mListView.setAdapter(mAppListAdapter);
        }
    }

    @Override
    public void onDestroyView() {
        mProgressView = null;
        mListView.setAdapter(null);
        mListView = null;

        try {
            // Work around dismiss on rotation after setRetainInstance(true)
            // http://stackoverflow.com/a/13596466
            getDialog().setOnDismissListener(null);
        } catch (Exception ignored) {}

        super.onDestroyView();
    }

    /**
     * The item was clicked.
     *
     * Set IntentFilters in activity and dismiss()
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAppListAdapter.getItem(position).handleClick();
    }

    /**
     * Item on list was long clicked.
     *
     * Show app details
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ListItem item = mAppListAdapter.getItem(position);
        if (item instanceof ComponentWithMatchingFilters) {
            MyComponentInfo info = ((ComponentWithMatchingFilters) item).componentInfo;
            startActivity(
                    new Intent(getActivity(), SingleFragmentActivity.class)
                            .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName())
                            .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, info.getOwnerPackage().getPackageName())
                            .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, info.getName())
                            .putExtra(ComponentInfoFragment.ARG_COMPONENT_TYPE, info.getType())
                            .putExtra(ComponentInfoFragment.ARG_LAUNCHED_FROM_INTENT_EDITOR, true)
            );
            return true;
        }
        return false;
    }

    /**
     * Filter options button was pressed
     */
    @Override
    public void onClick(View v) {
        (new OptionsAlertDialog()).show();
    }

    /**
     * Handle back key if we're in specific app
     */
    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mInPackage != null) {
            mInPackage = null;
            mAppListAdapter.notifyDataSetChanged();
            if (mAllAppsListViewState != null) {
                mListView.onRestoreInstanceState(mAllAppsListViewState);
            }
            return true;
        }
        return false;
    }


    private class OptionsAlertDialog implements DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnClickListener {
        ArrayList<String> mOptionNames = new ArrayList<String>();
        ArrayList<Integer> mOptionFlags = new ArrayList<Integer>();
        int mNewFlags = mFlags;

        OptionsAlertDialog() {
            // Add options if they are available
            if ((mAvailableFlags & FLAG_TEST_ACTION) != 0) {
                mOptionNames.add(getActivity().getString(R.string.filters_filter_test_action));
                mOptionFlags.add(FLAG_TEST_ACTION);
            }

            if ((mAvailableFlags & FLAG_TEST_CATEGORIES) != 0) {
                mOptionNames.add(getActivity().getString(R.string.filters_filter_test_categories));
                mOptionFlags.add(FLAG_TEST_CATEGORIES);
            }

            if ((mAvailableFlags & FLAG_CASE_INSENSITIVE_AND_SUBSTRING) != 0) {
                mOptionNames.add(getActivity().getString(R.string.filters_filter_substring_and_insensitive));
                mOptionFlags.add(FLAG_CASE_INSENSITIVE_AND_SUBSTRING);
            }
        }

        void show() {
            // Get current values of flags
            boolean[] values = new boolean[mOptionFlags.size()];
            int i = 0;
            for (Integer optionFlag : mOptionFlags) {
                values[i++] = (mNewFlags & optionFlag) != 0;
            }

            // Create and show dialog
            new AlertDialog.Builder(getActivity())
                    .setMultiChoiceItems(
                            mOptionNames.toArray(new CharSequence[mOptionNames.size()]),
                            values,
                            this
                    )
                    .setPositiveButton(android.R.string.ok, this)
                    .show();
        }

        /**
         * "OK" button in dialog was clicked
         */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mFlags == mNewFlags) {
                return;
            }
            mFlags = mNewFlags;

            // Restart scan
            (new ListFiltersTask()).execute();
        }

        /**
         * Filtering option was checked or unchecked.
         *
         * We apply new flags when user clicks OK
         */
        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            int flagValue = mOptionFlags.get(which);
            if (isChecked) {
                mNewFlags |= flagValue;
            } else {
                mNewFlags &= ~flagValue;
            }
        }
    }


    private interface ListItem {
        CharSequence getTitle(PackageManager pm);

        Drawable loadIcon(PackageManager pm);

        String getComponentName();

        void handleClick();
    }

    /**
     * Structure containing application or component with matched filters
     */
    private class AppWithMatchingFilters implements ListItem {
        final ComponentWithMatchingFilters[] matchingComponents;

        AppWithMatchingFilters(List<ComponentWithMatchingFilters> matchingComponents) {
            this.matchingComponents = matchingComponents.toArray(new ComponentWithMatchingFilters[matchingComponents.size()]);
        }

        @Override
        public CharSequence getTitle(PackageManager pm) {
            return matchingComponents[0].componentInfo.getOwnerPackage().loadLabel(pm);
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return matchingComponents[0].loadIcon(pm);
        }

        @Override
        public String getComponentName() {
            return matchingComponents[0].componentInfo.getOwnerPackage().getPackageName();
        }

        @Override
        public void handleClick() {
            mAllAppsListViewState = mListView.onSaveInstanceState();
            mInPackage = this;
            mAppListAdapter.notifyDataSetChanged();
        }
    }

    private class ComponentWithMatchingFilters implements ListItem {
        final MyComponentInfo componentInfo;

        ComponentWithMatchingFilters(MyComponentInfo componentInfo) {
            this.componentInfo = componentInfo;
        }

        @Override
        public CharSequence getTitle(PackageManager pm) {
            return componentInfo.loadLabel(pm);
        }

        @Override
        public Drawable loadIcon(PackageManager pm) {
            return componentInfo.loadIcon(pm);
        }

        @NonNull
        private ComponentName getComponentNameObject() {
            return new ComponentName(
                    componentInfo.getOwnerPackage().getPackageName(),
                    componentInfo.getName()
            );
        }

        @Override
        public String getComponentName() {
            ComponentName componentNameObject = getComponentNameObject();
            return componentNameObject.flattenToShortString();
        }



        @Override
        public void handleClick() {
            final IntentEditorActivity intentEditorActivity = (IntentEditorActivity) getActivity();
            intentEditorActivity.setComponentName(getComponentNameObject());
            dismiss();
        }
    }

    private ArrayList<AppWithMatchingFilters> mAppsWithMatchingFilters = new ArrayList<AppWithMatchingFilters>();

    /**
     * Adapter for list.
     * Automatically gets values, just call {@link #notifyDataSetChanged()}.
     */
    private class AppListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (!mIsScanningFinished) {
                return 0;
            }
            if (mInPackage != null) {
                return mInPackage.matchingComponents.length;
            }
            return mAppsWithMatchingFilters.size();
        }

        @Override
        public ListItem getItem(int position) {
            // Displaying just package items?
            if (mInPackage != null) {
                return mInPackage.matchingComponents[position];
            }

            // Get an app
            AppWithMatchingFilters app = mAppsWithMatchingFilters.get(position);

            // But if it has only one matching component display it directly
            if (app.matchingComponents.length == 1) {
                return app.matchingComponents[0];
            }
            return app;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get app info
            ListItem item = getItem(position);

            // Create view if needed
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.simple_list_item_2_with_icon, parent, false);
            }

            // Get package manager
            PackageManager pm = parent.getContext().getPackageManager();

            // Title (label)
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(item.getTitle(pm));

            // Package/component name (secondary text)
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(item.getComponentName());

            // Icon
            ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(item.loadIcon(pm));

            // Return view
            return convertView;
        }
    }


    /**
     * Task scanning components in apps and matches intent filters.
     *
     * To start or restart use following code:
     *   (new ListFiltersTask()).execute();
     */
    private class ListFiltersTask implements DoneCallback<Collection<MyPackageInfo>> {
        private int mComponentType;
        private String mAction;
        private Set<String> mCategories;

        boolean mInsensitiveAndSubstring;


        public void execute() {
            // Flag us as running and clear list
            mIsScanningFinished = false;


            // Get filtering flags
            int usedFlags = mFlags & mAvailableFlags;
            mInsensitiveAndSubstring = (usedFlags & FLAG_CASE_INSENSITIVE_AND_SUBSTRING) != 0;

            // Get requested component type
            final IntentEditorActivity intentEditor = (IntentEditorActivity) getActivity();

            mComponentType = intentEditor.getComponentType();

            // Set filtering constraints
            if ((usedFlags & FLAG_TEST_ACTION) != 0) {
                mAction = mIntent.getAction();
                if (mAction != null && "".equals(mAction)) {
                    mAction = null;
                }
            }

            if ((usedFlags & FLAG_TEST_CATEGORIES) != 0) {
                if (mInsensitiveAndSubstring) {
                    mCategories = new HashSet<String>();
                    for (String category : mIntent.getCategories()) {
                        if (mCategories != null) {
                            mCategories.add(category.toLowerCase());
                        }
                    }
                } else {
                    mCategories = mIntent.getCategories();
                }
            }

            // Prepare package info
            MyPackageManagerImpl
                    .getInstance(getActivity())
                    .getPackages(true)
                    .done(this);
        }


        @Override
        public void onDone(Collection<MyPackageInfo> result) {
            mAppsWithMatchingFilters.clear();

            // Temporary list holding currently scanned intent filters
            ArrayList<ComponentWithMatchingFilters> matchingComponents = new ArrayList<ComponentWithMatchingFilters>();

            // Iterate through packages
            for (MyPackageInfo extendedPackageInfo : result) {

                // Scan intent filters in package
                scanIntentFiltersInPackage(extendedPackageInfo, matchingComponents);

                // Add IntentFilters to list
                if (matchingComponents.size() != 0) {
                    // Add app to list and refresh
                    mAppsWithMatchingFilters.add(new AppWithMatchingFilters(matchingComponents));
                    matchingComponents.clear(); // Clear list so we can use it again for next app
                }
            }

            // Set flags that we're finished
            mIsScanningFinished = true;

            // Refresh list and/or remove scanning indicator
            hideProgressAndShowList();
            mAppListAdapter.notifyDataSetChanged();
        }

        private void scanIntentFiltersInPackage(MyPackageInfo extendedPackageInfo, ArrayList<ComponentWithMatchingFilters> matchingComponents) {

            // Get components list
            MyComponentInfo[] components = null;
            switch (mComponentType) {
                case IntentEditorConstants.ACTIVITY:
                    components = extendedPackageInfo.getActivities();
                    break;
                case IntentEditorConstants.BROADCAST:
                    components = extendedPackageInfo.getReceivers();
                    break;
                case IntentEditorConstants.SERVICE:
                    components = extendedPackageInfo.getServices();
                    break;
            }

            // Skip app if no components are found
            if (components == null) {
                return;
            }


            // Iterate over components
            for (MyComponentInfo component : components) {
                IntentFilter[] intentFilters = component.getIntentFilters();
                if (intentFilters != null) {
                    // Test the intent filters
                    for (IntentFilter intentFilter : intentFilters) {
                        if (testIntentFilter(intentFilter)) {

                            // Component matched, add to list
                            matchingComponents.add(
                                    new ComponentWithMatchingFilters(component)
                            );

                            // End scanning this component, scan next in package
                            break;
                        }
                    }
                }
            }
        }

        private boolean testIntentFilter(IntentFilter intentFilter) {
            // Check if IntentFilter is valid
            if (intentFilter.countActions() == 0) {
                return false;
            }

            // Perform tests
            if (mInsensitiveAndSubstring) {
                // Lax tests

                // Lax action test
                if (mAction != null) {
                    boolean foundAction = false;
                    for (final Iterator<String> actionsIterator = intentFilter.actionsIterator(); actionsIterator.hasNext();) {
                        if (actionsIterator.next().toLowerCase().contains(mAction)) {
                            foundAction = true;
                            break;
                        }
                    }
                    if (!foundAction) {
                        return false;
                    }
                }

                // Lax category test
                if (mCategories != null) {
                    if (intentFilter.countCategories() == 0) {
                        return false; // No categories but we require some
                    }

                    String[] categories = mCategories.toArray(new String[mCategories.size()]);
                    boolean[] foundCategories = new boolean[categories.length];

                    for (final Iterator<String> categoriesIterator = intentFilter.categoriesIterator(); categoriesIterator.hasNext();) {
                        final String filterCategory = categoriesIterator.next().toLowerCase();
                        for (int i = 0, categoriesLength = categories.length; i < categoriesLength; i++) {
                            String category = categories[i];
                            if (filterCategory.contains(category)) {
                                foundCategories[i] = true;
                            }
                        }
                    }
                    for (boolean foundCategory : foundCategories) {
                        if (!foundCategory) {
                            return false;
                        }
                    }
                }
            } else {
                // Strict tests
                if (mAction != null && !intentFilter.hasAction(mAction)) {
                    return false;
                }
                if (intentFilter.matchCategories(mCategories) != null) {
                    return false;
                }
            }

            // If we reached this point IntentFilter matches filtering criteria
            return true;
        }

    }
}
