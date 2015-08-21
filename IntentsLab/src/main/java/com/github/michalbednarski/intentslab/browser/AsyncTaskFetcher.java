package com.github.michalbednarski.intentslab.browser;

import android.content.Context;

import org.jdeferred.Promise;
import org.jdeferred.android.DeferredAsyncTask;

/**
 * Variation of Fetcher that wraps DeferredAsyncTask
 */
abstract class AsyncTaskFetcher extends Fetcher {

    /**
     * Fetch data for display in {@link BrowseComponentsFragment}
     * This method will be invoked in background thread.
     *
     * @param context The application context
     *
     * @return {@link Category[]}, {@link Component[]} or {@link CustomError}
     */
    abstract Object getEntries(Context context);


    @Override
    final Promise<Object, Throwable, Void> getEntriesAsync(final Context context) {
        DeferredAsyncTask<Void, Void, Object> deferredAsyncTask = new DeferredAsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackgroundSafe(Void... objects) throws Exception {
                return getEntries(context);
            }
        };
        deferredAsyncTask.execute();
        return deferredAsyncTask.promise();
    }
}
