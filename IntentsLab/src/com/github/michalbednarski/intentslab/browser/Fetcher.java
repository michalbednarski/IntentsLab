package com.github.michalbednarski.intentslab.browser;

import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.os.Parcelable;
import android.view.Menu;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Category of data in components browser
 */
abstract class Fetcher implements Parcelable, Cloneable {
    static final class Category {
        String title;
        String subtitle;
        Component[] components;
    }

    static final class Component {
        String title;
        String subtitle; // Used only in non categorized mode
        PackageItemInfo componentInfo;
    }

    static abstract class Descriptor {
        /**
         * Name used in JSON serialization, must not be changed
         */
        final String internalName;

        /**
         * Resource id for user visible name
         */
        final int nameRes;

        final Class<? extends Fetcher> aClass;

        Descriptor(Class<? extends Fetcher> aClass, String internalName, int nameRes) {
            this.aClass = aClass;
            this.internalName = internalName;
            this.nameRes = nameRes;
        }

        abstract Fetcher unserializeFromJSON(JSONObject jsonObject) throws JSONException;
    }






    /**
     * Fetch data for display in {@link BrowseComponentsFragment}
     * This method will be invoked in background thread.
     *
     * @param context The application context
     *
     * @return {@link Category[]} or {@link Component[]}
     */
    abstract Object getEntries(Context context);






    /**
     * Get R.layout id for filter options
     */
    abstract int getConfigurationLayout();

    /**
     * Initialize configuration layout, that is set up events and fill form with current values
     */
    abstract void initConfigurationForm(FetcherOptionsDialog dialog);






    /**
     * Update current fetcher configuration based on data entered in form
     */
    abstract void updateFromConfigurationForm(FetcherOptionsDialog dialog);

    /**
     * True if fetcher is not valid and will always return empty array in {@link #getEntries(Context)}
     */
    abstract boolean isExcludingEverything();






    /**
     * Prepare options menu loaded from {@link com.github.michalbednarski.intentslab.R.menu#activity_browse_apps}
     *
     * At start all options will be hidden
     */
    void onPrepareOptionsMenu(Menu menu) {}

    /**
     * An option was chosen, update current fetcher
     *
     * @return true if option was recognized, false otherwise
     */
    boolean onOptionsItemSelected(int id) { return false; }





    /**
     * Serialize fetcher to JSON
     *
     * This method should be only used by {@link FetcherManager}.
     * Use {@link FetcherManager#serializeFetcher(Fetcher)} instead
     */
    abstract JSONObject serializeToJSON() throws JSONException;





    /**
     * Clone the fetcher, all fetchers must be cloneable
     */
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public Fetcher clone() {
        try {
            return (Fetcher) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
