package com.github.michalbednarski.intentslab.browser;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.github.michalbednarski.intentslab.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetcher for applications
 */
public class ApplicationFetcher extends Fetcher {
    private static final String TAG = "ApplicationFetcher";



    public static final int APP_TYPE_USER = 1;
    public static final int APP_TYPE_SYSTEM = 2;
    public int appType = APP_TYPE_USER;


    public String requireMetaDataSubstring = null;


    public ApplicationFetcher() {}

    // Fetching
    @Override
    Object getEntries(Context context) {
        PackageManager pm = context.getPackageManager();
        int requestedPackageInfoFlags =
                PackageManager.GET_DISABLED_COMPONENTS |
                (requireMetaDataSubstring != null ? PackageManager.GET_META_DATA : 0);

        List<PackageInfo> allPackages = pm.getInstalledPackages(requestedPackageInfoFlags);

        ArrayList<Component> selectedApps = new ArrayList<Component>();

        for (PackageInfo pack : allPackages) {
            ApplicationInfo applicationInfo = pack.applicationInfo;

            // Filter out non-applications
            if (applicationInfo == null) {
                continue;
            }

            // System app filter
            if (((
                    (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ?
                            APP_TYPE_SYSTEM :
                            APP_TYPE_USER)
                    & appType) == 0) {
                continue;
            }

            // Metadata filter
            if (!checkMetaDataFilter(applicationInfo)) {
                continue;
            }

            // Build and add app descriptor
            Component app = new Component();
            app.title = String.valueOf(applicationInfo.loadLabel(pm));
            app.subtitle = pack.packageName;
            app.componentInfo = applicationInfo;
            selectedApps.add(app);

            // Allow cancelling task
            if (Thread.interrupted()) {
                return null;
            }
        }
        return selectedApps.toArray(new Component[selectedApps.size()]);
    }

    private boolean checkMetaDataFilter(ApplicationInfo cmp) {
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


    // Configuration UI
    @Override
    int getConfigurationLayout() {
        return R.layout.apps_filter;
    }

    @Override
    void initConfigurationForm(final FetcherOptionsDialog dialog) {
        // Fill form
        dialog.setBoxChecked(R.id.system_apps, (appType & APP_TYPE_SYSTEM) != 0);
        dialog.setBoxChecked(R.id.user_apps, (appType & APP_TYPE_USER) != 0);

        dialog.setBoxChecked(R.id.metadata, requireMetaDataSubstring != null);
        dialog.setTextInField(R.id.metadata_substring, requireMetaDataSubstring);

        // Hiding of metadata section
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

        boolean requireMetaData = dialog.isBoxChecked(R.id.metadata);
        requireMetaDataSubstring =
                requireMetaData ?
                dialog.getTextFromField(R.id.metadata_substring) :
                null;
    }



    // Verification
    @Override
    boolean isExcludingEverything() {
        return
                appType == 0;
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
        dest.writeInt(appType);
        dest.writeString(requireMetaDataSubstring);
    }

    public static final Creator<ApplicationFetcher> CREATOR = new Creator<ApplicationFetcher>() {
        @Override
        public ApplicationFetcher createFromParcel(Parcel source) {
            ApplicationFetcher fetcher = new ApplicationFetcher();
            fetcher.appType = source.readInt();
            fetcher.requireMetaDataSubstring = source.readString();
            return fetcher;
        }

        @Override
        public ApplicationFetcher[] newArray(int size) {
            return new ApplicationFetcher[size];
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

        menu.findItem(R.id.applications).setChecked(true);
    }

    @Override
    boolean onOptionsItemSelected(int id) {
        switch (id) {
            case R.id.system_apps: appType = APP_TYPE_SYSTEM; return true;
            case R.id.user_apps:   appType = APP_TYPE_USER;   return true;
        }
        return false;
    }

    // JSON serialization & name
    static final Descriptor DESCRIPTOR = new Descriptor(ApplicationFetcher.class, "apps", R.string.applications) {
        @Override
        Fetcher unserializeFromJSON(JSONObject jsonObject) throws JSONException {
            ApplicationFetcher fetcher = new ApplicationFetcher();
            fetcher.appType = jsonObject.getInt("appType");
            fetcher.requireMetaDataSubstring = jsonObject.getString("metadataSubstring");
            return fetcher;
        }
    };

    @Override
    JSONObject serializeToJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("appType", appType);
        jsonObject.put("metadataSubstring", requireMetaDataSubstring);
        return jsonObject;
    }
}
