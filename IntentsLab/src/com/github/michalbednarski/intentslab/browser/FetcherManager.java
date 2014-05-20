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
            RegisteredReceiverFetcher.DESCRIPTOR,
            ApplicationFetcher.DESCRIPTOR,
            PermissionsFetcher.DESCRIPTOR
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
