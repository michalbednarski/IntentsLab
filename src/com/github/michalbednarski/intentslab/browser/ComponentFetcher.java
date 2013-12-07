package com.github.michalbednarski.intentslab.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Parcel;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;
import com.github.michalbednarski.intentslab.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetcher for application components
 */
public class ComponentFetcher extends Fetcher {
    private static final String TAG = "ComponentFetcher";

    private static final boolean DEVELOPMENT_PERMISSIONS_SUPPORTED =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;


    /**
     * Type of components to get
     *
     * Combination of following flags:
     * <ul>
     *  <li> {@link PackageManager#GET_ACTIVITIES}
     *  <li> {@link PackageManager#GET_RECEIVERS}
     *  <li> {@link PackageManager#GET_SERVICES}
     *  <li> {@link PackageManager#GET_PROVIDERS}
     * </ul>
     */
    public int type = PackageManager.GET_ACTIVITIES;


    public static final int APP_TYPE_USER = 1;
    public static final int APP_TYPE_SYSTEM = 2;
    public int appType = APP_TYPE_USER;


    public static final int PROTECTION_WORLD_ACCESSIBLE = 1;
    public static final int PROTECTION_NORMAL = 2;
    public static final int PROTECTION_DANGEROUS = 4;
    public static final int PROTECTION_SIGNATURE = 8;
    public static final int PROTECTION_SYSTEM = 16;
    public static final int PROTECTION_DEVELOPMENT = 32;
    public static final int PROTECTION_UNEXPORTED = 64;
    public static final int PROTECTION_UNKNOWN = 128;

    public int protection = PROTECTION_WORLD_ACCESSIBLE;

    public static final int PROTECTION_ANY = 128 * 2 - 1;

    public static final int PROTECTION_ANY_OBTAINABLE =
            PROTECTION_WORLD_ACCESSIBLE |
            PROTECTION_NORMAL |
            PROTECTION_DANGEROUS |
            PROTECTION_DEVELOPMENT;

    public static final int PROTECTION_ANY_EXPORTED =
            PROTECTION_ANY & ~PROTECTION_UNEXPORTED;

    /**
     * Preset protection filters for displaying in Spinner in dialog
     */
    private static final int[] PROTECTION_PRESETS = new int[] {
            PROTECTION_ANY,
            PROTECTION_ANY_EXPORTED,
            PROTECTION_ANY_OBTAINABLE,
            PROTECTION_WORLD_ACCESSIBLE
    };

    /**
     * Preset protection filters for displaying in Spinner in dialog
     */
    private static final int[] PROTECTION_PRESETS_MENU_IDS = new int[] {
            R.id.permission_filter_all,
            R.id.permission_filter_exported,
            R.id.permission_filter_obtainable,
            R.id.permission_filter_world_accessible
    };

    /**
     * This is used for checking if we can skip looking up PermissionInfo because filtering result will be the same
     * no matter what protectionLevel is set
     */
    private static final int PROTECTION_ANY_PERMISSION =
            PROTECTION_ANY &~ (PROTECTION_WORLD_ACCESSIBLE | PROTECTION_UNEXPORTED);



    public String requireMetaDataSubstring = null;

    public boolean testWritePermissionForProviders = false;


    public ComponentFetcher() {}

    // Fetching
    @Override
    Object getEntries(Context context) {
        PackageManager pm = context.getPackageManager();
        int requestedPackageInfoFlags =
                type |
                PackageManager.GET_DISABLED_COMPONENTS |
                (requireMetaDataSubstring != null ? PackageManager.GET_META_DATA : 0);

        boolean workAroundSmallBinderBuffer = false;
        List<PackageInfo> allPackages = null;
        try {
            allPackages = pm.getInstalledPackages(requestedPackageInfoFlags);
        } catch (Exception e) {
            Log.w(TAG, "Loading all apps at once failed, retrying separately", e);
        }

        if (allPackages == null || allPackages.isEmpty()) {
            workAroundSmallBinderBuffer = true;
            allPackages = pm.getInstalledPackages(0);
        }

        ArrayList<Category> selectedApps = new ArrayList<Category>();

        for (PackageInfo pack : allPackages) {
            // Filter out non-applications
            if (pack.applicationInfo == null) {
                continue;
            }

            // System app filter
            if (((
                    (pack.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ?
                            APP_TYPE_SYSTEM :
                            APP_TYPE_USER)
                    & appType) == 0) {
                continue;
            }

            // Load component information separately if they were to big to send them all at once
            if (workAroundSmallBinderBuffer) {
                try {
                    pack = pm.getPackageInfo(pack.packageName, requestedPackageInfoFlags);
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(TAG, "getPackageInfo() thrown NameNotFoundException for " + pack.packageName, e);
                    continue;
                }
            }

            // Scan components
            ArrayList<Component> selectedComponents = new ArrayList<Component>();

            if ((type & PackageManager.GET_ACTIVITIES) != 0) {
                scanComponents(pm, pack.activities, selectedComponents);
            }
            if ((type & PackageManager.GET_RECEIVERS) != 0) {
                scanComponents(pm, pack.receivers, selectedComponents);
            }
            if ((type & PackageManager.GET_SERVICES) != 0) {
                scanComponents(pm, pack.services, selectedComponents);
            }
            if ((type & PackageManager.GET_PROVIDERS) != 0) {
                scanComponents(pm, pack.providers, selectedComponents);
            }

            // Check if we filtered out all components and skip whole app if so
            if (selectedComponents.isEmpty()) {
                continue;
            }

            // Build and add app descriptor
            Category app = new Category();
            app.title = String.valueOf(pack.applicationInfo.loadLabel(pm));
            app.subtitle = pack.packageName;
            app.components = selectedComponents.toArray(new Component[selectedComponents.size()]);
            selectedApps.add(app);

            // Allow cancelling task
            if (Thread.interrupted()) {
                return null;
            }
        }
        return selectedApps.toArray(new Category[selectedApps.size()]);
    }

    private void scanComponents(PackageManager pm, ComponentInfo[] components, ArrayList<Component> outList) {
        // Skip apps not having any components of requested type
        if (!(components != null && components.length != 0)) {
            return;
        }

        // Scan components
        for (ComponentInfo cmp : components) {
            if (!checkMetaDataFilter(cmp)) {
                continue;
            }
            if (!checkPermissionFilter(pm, cmp)) {
                continue;
            }
            Component component = new Component();
            String name = cmp.name;
            String packageName = cmp.packageName;
            component.title = name.startsWith(packageName) ? name.substring(packageName.length()) : name;
            component.componentInfo = cmp;
            outList.add(component);
        }
    }

    private boolean checkMetaDataFilter(ComponentInfo cmp) {
        if (requireMetaDataSubstring == null) {
            return true;
        }
        if (cmp.metaData == null || cmp.metaData.isEmpty()) {
            return false;
        }
        if (requireMetaDataSubstring.length() == 0) {
            return true;
        }
        for (String key : cmp.metaData.keySet()) {
            if (key.contains(requireMetaDataSubstring)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("InlinedApi")
    private boolean checkPermissionFilter(PackageManager pm, ComponentInfo cmp) {
        // Not exported?
        if (!cmp.exported) {
            return (protection & PROTECTION_UNEXPORTED) != 0;
        }

        // Get checked permission
        String permission =
                cmp instanceof ServiceInfo ? ((ServiceInfo) cmp).permission :
                cmp instanceof ActivityInfo ? ((ActivityInfo) cmp).permission :
                cmp instanceof ProviderInfo ? (
                        testWritePermissionForProviders ?
                                ((ProviderInfo) cmp).writePermission :
                                ((ProviderInfo) cmp).readPermission
                ) : null;

        // World accessible
        if (permission == null) {
            return (protection & PROTECTION_WORLD_ACCESSIBLE) != 0;
        }

        // Skip checking protectionLevel if it doesn't matter
        if ((protection & PROTECTION_ANY_PERMISSION) == PROTECTION_ANY_PERMISSION) {
            return true;
        }
        if ((protection & PROTECTION_ANY_PERMISSION) == 0) {
            return false;
        }

        // Obtain PermissionInfo
        PermissionInfo permissionInfo;
        try {
            permissionInfo = pm.getPermissionInfo(permission, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.v("PermissionFilter", "Unknown permission " + permission + " for " + cmp.name, e);
            return (protection & PROTECTION_UNKNOWN) != 0;
        }

        // Test protectionLevel
        int protectionLevel = permissionInfo.protectionLevel;
        if (protectionLevel == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
            protectionLevel = PermissionInfo.PROTECTION_SIGNATURE | PermissionInfo.PROTECTION_FLAG_SYSTEM;
        }
        int protectionLevelBase = protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
        int protectionLevelFlags = protectionLevel & PermissionInfo.PROTECTION_MASK_FLAGS;

        // Match against our flags
        return ((
                protectionLevel == PermissionInfo.PROTECTION_NORMAL ? PROTECTION_NORMAL :
                protectionLevel == PermissionInfo.PROTECTION_DANGEROUS ? PROTECTION_DANGEROUS :
                (
                    ((protectionLevelBase == PermissionInfo.PROTECTION_SIGNATURE)
                        ? PROTECTION_SIGNATURE : 0) |
                    (((protectionLevelFlags & PermissionInfo.PROTECTION_FLAG_SYSTEM) != 0)
                        ? PROTECTION_SYSTEM : 0) |
                    (((protectionLevelFlags & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0)
                        ? PROTECTION_SYSTEM : 0)
                )
        ) & protection) != 0;
    }

    // Configuration UI
    @Override
    int getConfigurationLayout() {
        return R.layout.components_filter;
    }

    @Override
    void initConfigurationForm(final FetcherOptionsDialog dialog) {
        // Disable development permission checkbox if it's not available
        if (!DEVELOPMENT_PERMISSIONS_SUPPORTED) {
            dialog.findView(R.id.permission_filter_development).setEnabled(false);
        }

        // Prepare protection preset spinner
        {
            // Find current preset
            int currentPresetId = PROTECTION_PRESETS.length; // "Custom" if nothing found
            if (!testWritePermissionForProviders) {
                for (int i = 0; i < PROTECTION_PRESETS.length; i++) {
                    if (protection == PROTECTION_PRESETS[i]) {
                        currentPresetId = i;
                        dialog.findView(R.id.permission_filter_details).setVisibility(View.GONE);
                        break;
                    }
                }
            }

            // Fill spinner
            Spinner protectionPresetSpinner = (Spinner) dialog.findView(R.id.permission_filter_spinner);
            Activity activity = dialog.getActivity();
            protectionPresetSpinner.setAdapter(new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item,
                    new String[]{
                            activity.getString(R.string.permission_filter_show_all), // 0
                            activity.getString(R.string.permission_filter_show_exported), // 1
                            activity.getString(R.string.permission_filter_show_obtainable), // 2
                            activity.getString(R.string.permission_filter_world_accessible), // 3
                            activity.getString(R.string.filter_custom) // 4
                    }
            ));
            protectionPresetSpinner.setSelection(currentPresetId);

            // Set up spinner event
            protectionPresetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    boolean isCustom = position == PROTECTION_PRESETS.length;
                    if (!isCustom) {
                        int preset = PROTECTION_PRESETS[position];
                        dialog.setBoxChecked(R.id.permission_filter_world_accessible, (preset & PROTECTION_WORLD_ACCESSIBLE) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_normal, (preset & PROTECTION_NORMAL) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_dangerous, (preset & PROTECTION_DANGEROUS) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_signature, (preset & PROTECTION_SIGNATURE) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_system, (preset & PROTECTION_SYSTEM) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_development, (preset & PROTECTION_DEVELOPMENT) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_unexported, (preset & PROTECTION_UNEXPORTED) != 0);
                        dialog.setBoxChecked(R.id.permission_filter_unknown, (preset & PROTECTION_UNKNOWN) != 0);
                        dialog.setBoxChecked(R.id.read_permission, true);
                    }
                    dialog.findView(R.id.permission_filter_details).setVisibility(isCustom ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Spinner cannot have nothing selected
                }
            });
        }



        // Fill form
        dialog.setBoxChecked(R.id.system_apps, (appType & APP_TYPE_SYSTEM) != 0);
        dialog.setBoxChecked(R.id.user_apps, (appType & APP_TYPE_USER) != 0);

        dialog.setBoxChecked(R.id.activities, (type & PackageManager.GET_ACTIVITIES) != 0);
        dialog.setBoxChecked(R.id.receivers, (type & PackageManager.GET_RECEIVERS) != 0);
        dialog.setBoxChecked(R.id.services, (type & PackageManager.GET_SERVICES) != 0);
        dialog.setBoxChecked(R.id.content_providers, (type & PackageManager.GET_PROVIDERS) != 0);

        dialog.setBoxChecked(R.id.permission_filter_world_accessible, (protection & PROTECTION_WORLD_ACCESSIBLE) != 0);
        dialog.setBoxChecked(R.id.permission_filter_normal, (protection & PROTECTION_NORMAL) != 0);
        dialog.setBoxChecked(R.id.permission_filter_dangerous, (protection & PROTECTION_DANGEROUS) != 0);
        dialog.setBoxChecked(R.id.permission_filter_signature, (protection & PROTECTION_SIGNATURE) != 0);
        dialog.setBoxChecked(R.id.permission_filter_system, (protection & PROTECTION_SYSTEM) != 0);
        dialog.setBoxChecked(R.id.permission_filter_development, (protection & PROTECTION_DEVELOPMENT) != 0);
        dialog.setBoxChecked(R.id.permission_filter_unexported, (protection & PROTECTION_UNEXPORTED) != 0);
        dialog.setBoxChecked(R.id.permission_filter_unknown, (protection & PROTECTION_UNKNOWN) != 0);

        dialog.setBoxChecked(testWritePermissionForProviders ? R.id.write_permission : R.id.read_permission, true);

        dialog.setBoxChecked(R.id.metadata, requireMetaDataSubstring != null);
        dialog.setTextInField(R.id.metadata_substring, requireMetaDataSubstring);

        // Set up sections showing when their checkboxes are checked
        dialog.findView(R.id.content_provider_permission_type).setVisibility(testWritePermissionForProviders ? View.VISIBLE : View.GONE);
        ((CheckBox) dialog.findView(R.id.content_providers)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialog.findView(R.id.content_provider_permission_type).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (!isChecked) {
                    dialog.setBoxChecked(R.id.read_permission, true);
                }
            }
        });

        dialog.findView(R.id.metadata_details).setVisibility(requireMetaDataSubstring != null ? View.VISIBLE : View.GONE);
        ((CheckBox) dialog.findView(R.id.metadata)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialog.findView(R.id.metadata_details).setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (!isChecked) {
                    dialog.setTextInField(R.id.metadata_substring, "");
                }
            }
        });
    }

    @Override
    void updateFromConfigurationForm(FetcherOptionsDialog dialog) {
        appType =
                (dialog.isBoxChecked(R.id.system_apps) ? APP_TYPE_SYSTEM : 0) |
                (dialog.isBoxChecked(R.id.user_apps) ? APP_TYPE_USER : 0);

        type =
                (dialog.isBoxChecked(R.id.activities) ? PackageManager.GET_ACTIVITIES : 0) |
                (dialog.isBoxChecked(R.id.receivers) ? PackageManager.GET_RECEIVERS : 0) |
                (dialog.isBoxChecked(R.id.services) ? PackageManager.GET_SERVICES : 0) |
                (dialog.isBoxChecked(R.id.content_providers) ? PackageManager.GET_PROVIDERS : 0);

        protection =
                (dialog.isBoxChecked(R.id.permission_filter_world_accessible) ? PROTECTION_WORLD_ACCESSIBLE : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_normal) ? PROTECTION_NORMAL : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_dangerous) ? PROTECTION_DANGEROUS : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_signature) ? PROTECTION_SIGNATURE : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_system) ? PROTECTION_SYSTEM : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_development) ? PROTECTION_DEVELOPMENT : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_unexported) ? PROTECTION_UNEXPORTED : 0) |
                (dialog.isBoxChecked(R.id.permission_filter_unknown) ? PROTECTION_UNKNOWN : 0);

        boolean requireMetaData = dialog.isBoxChecked(R.id.metadata);
        requireMetaDataSubstring =
                requireMetaData ?
                dialog.getTextFromField(R.id.metadata_substring) :
                null;

        testWritePermissionForProviders = dialog.isBoxChecked(R.id.write_permission);
    }



    // Verification
    @Override
    boolean isExcludingEverything() {
        return
                appType == 0 ||
                type == 0 ||
                protection == 0;
    }

    //
    // Parcelable
    //
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(
                (testWritePermissionForProviders ? 2 : 0)
        );
        dest.writeInt(type);
        dest.writeInt(appType);
        dest.writeInt(protection);
        dest.writeString(requireMetaDataSubstring);
    }

    public static final Creator<ComponentFetcher> CREATOR = new Creator<ComponentFetcher>() {
        @Override
        public ComponentFetcher createFromParcel(Parcel source) {
            int flags = source.readInt();
            ComponentFetcher fetcher = new ComponentFetcher();
            fetcher.type = source.readInt();
            fetcher.appType = source.readInt();
            fetcher.protection = source.readInt();
            fetcher.requireMetaDataSubstring = source.readString();
            fetcher.testWritePermissionForProviders = (flags & 2) != 0;
            return fetcher;
        }

        @Override
        public ComponentFetcher[] newArray(int size) {
            return new ComponentFetcher[size];
        }
    };

    // Options menu
    @Override
    void onPrepareOptionsMenu(Menu menu) {
        if (appType == APP_TYPE_USER) {
            menu.findItem(R.id.system_apps).setVisible(true);
        } else if (appType == APP_TYPE_SYSTEM) {
            menu.findItem(R.id.user_apps).setVisible(true);
        }

        if (type == PackageManager.GET_ACTIVITIES) {
            menu.findItem(R.id.activities).setChecked(true);
        } else if (type == PackageManager.GET_RECEIVERS) {
            menu.findItem(R.id.broadcasts).setChecked(true);
        } else if (type == PackageManager.GET_SERVICES) {
            menu.findItem(R.id.services).setChecked(true);
        } else if (type == PackageManager.GET_PROVIDERS) {
            menu.findItem(R.id.content_providers).setChecked(true);
        }

        menu.findItem(R.id.simple_filter_permission).setVisible(true);
        for (int i = 0; i < PROTECTION_PRESETS_MENU_IDS.length; i++) {
            if (protection == PROTECTION_PRESETS[i]) {
                menu.findItem(PROTECTION_PRESETS_MENU_IDS[i]).setChecked(true);
            }
        }
    }

    @Override
    boolean onOptionsItemSelected(int id) {
        switch (id) {
            case R.id.system_apps: appType = APP_TYPE_SYSTEM; return true;
            case R.id.user_apps:   appType = APP_TYPE_USER;   return true;

            case R.id.activities:        type = PackageManager.GET_ACTIVITIES; return true;
            case R.id.broadcasts:        type = PackageManager.GET_RECEIVERS;  return true;
            case R.id.services:          type = PackageManager.GET_SERVICES;   return true;
            case R.id.content_providers: type = PackageManager.GET_PROVIDERS;  return true;

            case R.id.permission_filter_all:              protection = PROTECTION_ANY;              return true;
            case R.id.permission_filter_exported:         protection = PROTECTION_ANY_EXPORTED;     return true;
            case R.id.permission_filter_obtainable:       protection = PROTECTION_ANY_OBTAINABLE;   return true;
            case R.id.permission_filter_world_accessible: protection = PROTECTION_WORLD_ACCESSIBLE; return true;
        }
        return false;
    }

    // JSON serialization & name
    static final Descriptor DESCRIPTOR = new Descriptor(ComponentFetcher.class, "components", R.string.components) {
        @Override
        Fetcher unserializeFromJSON(JSONObject jsonObject) throws JSONException {
            ComponentFetcher fetcher = new ComponentFetcher();
            fetcher.type = jsonObject.getInt("componentType");
            fetcher.appType = jsonObject.getInt("appType");
            fetcher.protection = jsonObject.getInt("protectionFilter");
            if ((fetcher.type & PackageManager.GET_PROVIDERS) != 0) {
                fetcher.testWritePermissionForProviders = jsonObject.getBoolean("testProviderWrite");
            }
            fetcher.requireMetaDataSubstring = jsonObject.getString("metadataSubstring");
            return fetcher;
        }
    };

    @Override
    JSONObject serializeToJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("componentType", type);
        jsonObject.put("appType", appType);
        jsonObject.put("protectionFilter", protection);
        if ((type & PackageManager.GET_PROVIDERS) != 0) {
            jsonObject.put("testProviderWrite", testWritePermissionForProviders);
        }
        jsonObject.put("metadataSubstring", requireMetaDataSubstring);
        return jsonObject;
    }
}
