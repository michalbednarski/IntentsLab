package com.github.michalbednarski.intentslab.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.browser.ExtendedPackageInfo;

import java.util.*;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * Dialog for picking app to use it's intent filters.
 */
public class AttachIntentFilterDialog extends DialogFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, View.OnClickListener {
    private LayoutInflater mInflater;
    private PackageManager mPm;

    /**
     * ViewGroup containing all dialog contents
     *
     * Set to null if we don't show anything but "Wait" title
     */
    private View mView;

    /**
     * "Filter options" button
     *
     * Disabled if {@link #mSkipFiltersCheckBox} is available and checked
     */
    private Button mOptionsButton;

    /**
     * Skip filters checkbox, available only if {@link #mScanComponentsInPackage} isn't null,
     * otherwise it will have visibility set to GONE and must be ignored
     */
    private CheckBox mSkipFiltersCheckBox;

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
     * Flag that "use all" option was selected and we're waiting for all components to be scanned
     */
    private boolean mIsUseAllScheduled = false;



    /**
     * Currently executed scanning task
     * Must be modified only by {@link ListFiltersTask}
     */
    private ListFiltersTask mTask;

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
            FLAG_TEST_ACTION |
            FLAG_TEST_CATEGORIES;

    /**
     * Flags that can be used and shown to user in filter options
     */
    private int mAvailableFlags = 0;

    /**
     * Use "Find" mode instead of "attach intent filter" mode, that is change title
     * and use inexact search by default
     *
     * @see #enableFindMode()
     */
    private boolean mFindMode = false;

    /**
     * Name of package which we scan for components or null if we're scanning all packages
     */
    private String mScanComponentsInPackage = null;

    /**
     * Name of boolean {@link SharedPreferences SharedPreference} reflecting checked state of {@link #mSkipFiltersCheckBox}
     */
    private static final String PREF_SKIP_FILTERS_IN_COMPONENTS = "attachIntentFilter.skipFilters";

    public AttachIntentFilterDialog() {
    }

    /**
     * Switch to "find" mode
     */
    public AttachIntentFilterDialog enableFindMode() {
        mFindMode = true;
        mFlags |= FLAG_CASE_INSENSITIVE_AND_SUBSTRING;
        return this;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Get services
        mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPm = activity.getPackageManager();

        // If we're here first time
        if (mIntent == null) {

            // Get intent
            final IntentEditorActivity intentEditor = (IntentEditorActivity) activity;
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

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(mFindMode ? R.string.find : R.string.attach_intentfilter);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.attach_intent_filter, container, false);

        // Prepare ListView
        ListView appsListView = (ListView) mView.findViewById(R.id.apps_list_view);
        appsListView.setAdapter(mAppListAdapter);
        appsListView.setOnItemClickListener(this);
        appsListView.setOnItemLongClickListener(this);

        // Find fields related to filtering
        View filteringViewGroup = mView.findViewById(R.id.filtering);
        mOptionsButton = (Button) mView.findViewById(R.id.options);
        mSkipFiltersCheckBox = (CheckBox) mView.findViewById(R.id.skip_filters_checkbox);

        // Hide unneeded buttons and attach events
        if (mAvailableFlags == 0) {
            filteringViewGroup.setVisibility(View.GONE);
        } else {
            mOptionsButton.setOnClickListener(this);
        }
        if (mScanComponentsInPackage == null) {
            mSkipFiltersCheckBox.setVisibility(View.GONE);
        }

        // Init "skip filters" CheckBox
        mSkipFiltersCheckBox.setChecked(getDefaultSharedPreferences(getActivity()).getBoolean(PREF_SKIP_FILTERS_IN_COMPONENTS, false));
        mSkipFiltersCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Persist value
                Utils.applyOrCommitPrefs(
                        getDefaultSharedPreferences(getActivity()).edit()
                                .putBoolean(PREF_SKIP_FILTERS_IN_COMPONENTS, isChecked)
                );

                // Enable or disable filter options button
                mOptionsButton.setEnabled(!isChecked);

                // Restart scan
                (new ListFiltersTask()).execute();
            }
        });

        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Enable instance retaining, we'll continue scanning when configuration changes
        setRetainInstance(true);

        // If user selected all restore "Wait" message
        if (mIsUseAllScheduled) {
            setFiltersFromAllApps();
        }
    }

    @Override
    public void onDestroyView() {
        mView = null;

        try {
            // Work around dismiss on rotation after setRetainInstance(true)
            // http://stackoverflow.com/a/13596466
            getDialog().setOnDismissListener(null);
        } catch (Exception ignored) {}

        super.onDestroyView();
    }

    /**
     * User just pressed "All" on list or {@link #mIsUseAllScheduled} is true and we can pass results or must restore UI
     */
    private void setFiltersFromAllApps() {
        if (mIsScanningFinished) {
            if (getActivity() == null) {
                // We currently don't have an activity
                mIsUseAllScheduled = true;
                return;
            }

            // Merge all intent filters
            ArrayList<IntentFilter> allIntentFilters = new ArrayList<IntentFilter>();
            for (AppWithMatchingFilters app : mAppsWithMatchingFilters) {
                for (IntentFilter filter : app.filters) {
                    allIntentFilters.add(filter);
                }
            }

            // Pass to activity
            if (allIntentFilters.size() != 0) {
                ((IntentEditorActivity) getActivity()).setAttachedIntentFilters(allIntentFilters.toArray(new IntentFilter[allIntentFilters.size()]));
            } else {
                // Nothing has been found
                Toast.makeText(getActivity(), R.string.no_intent_filters_found, Toast.LENGTH_SHORT).show();
            }

            dismiss();
        } else {
            // Set flag to call this method when list is ready
            mIsUseAllScheduled = true;

            // Replace dialog with wait message
            if (mView != null) {
                mView.setVisibility(View.GONE);
                mView = null;
            }
            getDialog().setTitle(getActivity().getString(R.string.wait));
        }
    }


    /**
     * The item was clicked.
     *
     * Set IntentFilters in activity and dismiss()
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            setFiltersFromAllApps();
        } else {
            final IntentEditorActivity intentEditorActivity = (IntentEditorActivity) getActivity();
            final AppWithMatchingFilters app = mAppsWithMatchingFilters.get(position - 1);
            if (app.componentInfo != null) {
                intentEditorActivity.setComponentName(new ComponentName(app.packageName, app.componentInfo.name));
            }
            intentEditorActivity.setAttachedIntentFilters(app.filters);
            dismiss();
        }
    }

    /**
     * Item on list was long clicked.
     *
     * Enter into app components or return to all apps
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mScanComponentsInPackage == null && position != 0 && position - 1 < mAppsWithMatchingFilters.size()) {
            // App on list, go to components of app
            // Update UI
            mSkipFiltersCheckBox.setVisibility(View.VISIBLE);
            mOptionsButton.setEnabled(!mSkipFiltersCheckBox.isChecked());

            // Set scanned app name
            mScanComponentsInPackage = mAppsWithMatchingFilters.get(position - 1).packageName;

            // Restart scan
            (new ListFiltersTask()).execute();

            return true;
        } else if (mScanComponentsInPackage != null && position == 0) {
            // "All" on list of components of app, return to all apps
            // Update UI
            mSkipFiltersCheckBox.setVisibility(View.GONE);
            mOptionsButton.setEnabled(true);

            // Unset scanned app name
            mScanComponentsInPackage = null;

            // Restart scan
            (new ListFiltersTask()).execute();

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

    /**
     * Structure containing application or component with matched filters
     */
    private static class AppWithMatchingFilters {
        final IntentFilter[] filters;
        final String packageName;
        final ComponentInfo componentInfo; // Null if whole app

        AppWithMatchingFilters(ArrayList<IntentFilter> filters, String packageName, ComponentInfo componentInfo) {
            this.filters = filters.toArray(new IntentFilter[filters.size()]);
            this.packageName = packageName;
            this.componentInfo = componentInfo;
        }

        AppWithMatchingFilters(ArrayList<IntentFilter> filters, String packageName) {
            this(filters, packageName, null);
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
            if (mAppsWithMatchingFilters.size() == 0 && mIsScanningFinished) {
                return 0;
            }
            return 1 + mAppsWithMatchingFilters.size() + (mIsScanningFinished ? 0 : 1);
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
            return 3; // "All", apps and "Scanning..."
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 1 :
                    position == mAppsWithMatchingFilters.size() + 1 ? 2 : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // "All"
            if (position == 0) {
                if (convertView != null) {
                    return convertView;
                }
                TextView v = (TextView) mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                v.setText(getActivity().getString(R.string.attach_intent_filter_all));
                return v;
            }

            // "Scanning..."
            if (position == mAppsWithMatchingFilters.size() + 1) {
                if (convertView != null) {
                    return convertView;
                }
                return mInflater.inflate(R.layout.list_item_loading, parent, false);
            }

            // Get app info
            AppWithMatchingFilters app = mAppsWithMatchingFilters.get(position - 1);

            // Skip if this is already right app
            if (convertView != null && convertView.getTag() == app) {
                return convertView;
            }

            // Create view if needed
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.simple_list_item_2_with_icon, parent, false);
            }

            // Set tag to skip it if not needed in future
            convertView.setTag(app);

            // Set texts and value
            // Package name (secondary text)
            if (app.componentInfo == null) {
                // App
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(app.packageName);

                try {
                    final ApplicationInfo applicationInfo = mPm.getApplicationInfo(app.packageName, 0);

                    // Title (label)
                    final CharSequence applicationLabel = applicationInfo.loadLabel(mPm);
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(applicationLabel);

                    // Icon
                    ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(applicationInfo.loadIcon(mPm));
                } catch (PackageManager.NameNotFoundException ignored) {
                    // In case of app being removed in meantime
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText("?");
                    ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(null);
                }
            } else {
                // Specific component

                // Title (label)
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                        app.componentInfo.loadLabel(mPm)
                );

                // Subtitle (ComponentName)
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(
                        new ComponentName(app.packageName, app.componentInfo.name).flattenToShortString()
                );

                // Icon
                ((ImageView) convertView.findViewById(R.id.app_icon)).setImageDrawable(
                        app.componentInfo.loadIcon(mPm)
                );
            }

            // Return view
            return convertView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return position != mAppsWithMatchingFilters.size() + 1; // Not "Scanning..."
        }
    }


    /**
     * Task scanning components in apps and matches intent filters.
     *
     * To start or restart use following code:
     *   (new ListFiltersTask()).execute();
     */
    private class ListFiltersTask extends AsyncTask<String, AppWithMatchingFilters, Object> {
        private int mComponentTypeFlag;
        private String mAction;
        private Set<String> mCategories;

        boolean mInsensitiveAndSubstring;

        @Override
        protected void onPreExecute() {
            // Cancel previous task if running
            if (mTask != null) {
                mTask.cancel(false);
            }
            mTask = this;

            // Flag us as running and clear list
            mIsScanningFinished = false;
            mAppsWithMatchingFilters.clear();
            mAppListAdapter.notifyDataSetChanged();

            // Get filtering flags
            int usedFlags;
            if (mScanComponentsInPackage != null && mSkipFiltersCheckBox.isChecked()) {
                usedFlags = 0;
            } else {
                usedFlags = mFlags & mAvailableFlags;
            }
            mInsensitiveAndSubstring = (usedFlags & FLAG_CASE_INSENSITIVE_AND_SUBSTRING) != 0;

            // Get requested component type
            final IntentEditorActivity intentEditor = (IntentEditorActivity) getActivity();

            int componentType = intentEditor.getComponentType();
            mComponentTypeFlag =
                    componentType == IntentEditorConstants.ACTIVITY ? PackageManager.GET_ACTIVITIES :
                    componentType == IntentEditorConstants.BROADCAST ? PackageManager.GET_RECEIVERS :
                    componentType == IntentEditorConstants.SERVICE ? PackageManager.GET_SERVICES : 0;

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
        }

        @Override
        protected Object doInBackground(String... params) {
            if (mComponentTypeFlag == 0) {
                return null;
            }

            // If we're requested to scan just one app, scan one app
            if (mScanComponentsInPackage != null) {
                try {
                    final PackageInfo packageInfo = mPm.getPackageInfo(mScanComponentsInPackage, mComponentTypeFlag);
                    scanIntentFiltersInPackage(packageInfo, null);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }

            // Get list of packages
            List<PackageInfo> installedPackages = mPm.getInstalledPackages(mComponentTypeFlag);
            boolean workAroundSmallBinderBuffer = false;
            if (installedPackages == null || installedPackages.size() == 0) {
                installedPackages = mPm.getInstalledPackages(0);
                workAroundSmallBinderBuffer = true;
            }

            // Temporary list holding currently scanned intent filters
            ArrayList<IntentFilter> matchingIntentFilters = new ArrayList<IntentFilter>();

            // Iterate through packages
            for (PackageInfo packageInfo : installedPackages) {
                // End if we're cancelled
                if (isCancelled()) {
                    return null;
                }

                // Get package details it they haven't fit in buffer earlier
                if (workAroundSmallBinderBuffer) {
                    try {
                        packageInfo = mPm.getPackageInfo(packageInfo.packageName, mComponentTypeFlag);
                    } catch (PackageManager.NameNotFoundException e) {
                        // Shouldn't happen
                        e.printStackTrace();
                        continue; // Skip app
                    }
                }

                // Scan intent filters in package
                scanIntentFiltersInPackage(packageInfo, matchingIntentFilters);

                // Add IntentFilters to list
                if (matchingIntentFilters.size() != 0) {
                    publishProgress(new AppWithMatchingFilters(matchingIntentFilters, packageInfo.packageName));
                    matchingIntentFilters.clear(); // Clear list so we can use it again for next app
                }
            }
            return null;
        }

        private void scanIntentFiltersInPackage(PackageInfo packageInfo, ArrayList<IntentFilter> matchingIntentFilters) {
            // Synchronously scan IntentFilters
            ExtendedPackageInfo extendedPackageInfo = new ExtendedPackageInfo(getActivity(), packageInfo, true);

            // Get components list and skip app if it's null
            final ComponentInfo[] components =
                    mComponentTypeFlag == PackageManager.GET_ACTIVITIES ? packageInfo.activities :
                    mComponentTypeFlag == PackageManager.GET_RECEIVERS ? packageInfo.receivers : packageInfo.services;
            if (components == null) {
                return;
            }

            // Init matchingIntentFilters if we're using it only locally
            boolean publishComponents = matchingIntentFilters == null;
            if (publishComponents) {
                matchingIntentFilters = new ArrayList<IntentFilter>();
            }


            // Iterate over components
            for (ComponentInfo component : components) {
                final ExtendedPackageInfo.ExtendedComponentInfo extendedComponentInfo = extendedPackageInfo.getComponentInfo(component.name);
                if (extendedComponentInfo == null) {
                    // Shouldn't happen
                    continue;
                }
                IntentFilter[] intentFilters = extendedComponentInfo.intentFilters;
                if (intentFilters != null) {
                    for (IntentFilter intentFilter : intentFilters) {
                        if (testIntentFilter(intentFilter)) {
                            matchingIntentFilters.add(intentFilter);
                        }
                    }
                }

                // If we're scanning components of one app send results now
                if (publishComponents && matchingIntentFilters.size() != 0) {
                    publishProgress(new AppWithMatchingFilters(matchingIntentFilters, packageInfo.packageName, component));
                    matchingIntentFilters.clear();
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

        /**
         * Called on main thread to add item to app list
         */
        @Override
        protected void onProgressUpdate(AppWithMatchingFilters... values) {
            // Return if we're cancelled
            // System shouldn't call this method anyway, but checks this only in background thread and that's racy
            if (isCancelled()) {
                return;
            }

            // Add app to list and refresh
            mAppsWithMatchingFilters.add(values[0]);
            if (mAppListAdapter != null) {
                mAppListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            // Return if we're cancelled
            if (isCancelled()) {
                return;
            }

            // Set flags that we're finished
            mTask = null;
            mIsScanningFinished = true;

            // If we have scheduled use all, do it now
            if (mIsUseAllScheduled) {
                setFiltersFromAllApps();

            // Refresh list (we're removing scanning indicator)
            } else if (mAppListAdapter != null) {
                mAppListAdapter.notifyDataSetChanged();
            }
        }
    }
}
