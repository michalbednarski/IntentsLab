package com.github.michalbednarski.intentslab.browser;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Helper functions for managing fetchers
 */
class FetcherManager {
    private static final Fetcher.Descriptor[] FETCHER_REGISTRY = new Fetcher.Descriptor[] {
            ComponentFetcher.DESCRIPTOR,
            ApplicationFetcher.DESCRIPTOR
    };

    private FetcherManager() {}

    static String[] getFetcherNames(Context context) {
        int count = FETCHER_REGISTRY.length;
        String[] fetcherNames = new String[count];
        for (int i = 0; i < count; i++) {
            fetcherNames[i] = context.getString(FETCHER_REGISTRY[i].nameRes);
        }
        return fetcherNames;
    }

    static int getFetcherIndex(Fetcher fetcher) {
        for (int i = 0; i < FETCHER_REGISTRY.length; i++) {
            if (FETCHER_REGISTRY[i].aClass.isInstance(fetcher)) {
                return i;
            }
        }
        throw new RuntimeException("Unknown fetcher");
    }

    static Fetcher createNewFetcherByIndex(int index) {
        try {
            return FETCHER_REGISTRY[index].aClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String serializeFetcher(Fetcher fetcher) {
        try {
            String type = null;
            for (Fetcher.Descriptor descriptor : FETCHER_REGISTRY) {
                if (descriptor.aClass.isInstance(fetcher)) {
                    type = descriptor.internalName;
                    break;
                }
            }
            assert type != null;
            JSONObject jsonObject = fetcher.serializeToJSON();
            jsonObject.put("_type", type);
            return jsonObject.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    static Fetcher unserializeFetcher(String serialized) {
        JSONTokener tokenizer = new JSONTokener(serialized);
        try {
            JSONObject jsonObject = (JSONObject) tokenizer.nextValue();
            String type = jsonObject.getString("_type");
            for (Fetcher.Descriptor descriptor : FETCHER_REGISTRY) {
                if (descriptor.internalName.equals(type)) {
                    return descriptor.unserializeFromJSON(jsonObject);
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Unknown _type");
    }
}
