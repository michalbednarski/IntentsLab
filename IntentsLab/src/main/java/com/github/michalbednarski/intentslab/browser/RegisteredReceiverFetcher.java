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
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Parcel;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.runas.RemoteEntryPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Fetcher for receivers registered with {@link Context#registerReceiver(android.content.BroadcastReceiver, IntentFilter)}
 */
public class RegisteredReceiverFetcher extends AsyncTaskFetcher {

    private boolean mExcludeProtected = true;

    @Override
    Object getEntries(Context context) {
        try {
            final Categorizer<RegisteredReceiverInfo> categorizer = new ProcessCategorizer();
            (new RegisteredReceiversParser(mExcludeProtected) {
                @Override
                protected void onReceiverFound(RegisteredReceiverInfo receiverInfo) {
                    categorizer.add(receiverInfo);
                }
            }).parse(context);
            return categorizer.getResult();
        } catch (SecurityException e) {
            // Create message about error
            SpannableStringBuilder ssb = new SpannableStringBuilder(context.getString(R.string.registered_receivers_denied));
            ssb.append("\n");
            int commandStart = ssb.length();
            if (ComponentFetcher.DEVELOPMENT_PERMISSIONS_SUPPORTED) {
                ssb
                        .append("pm grant ")
                        .append(context.getPackageName())
                        .append(" android.permission.DUMP");
            } else {
                ssb
                        .append(RemoteEntryPoint.getScriptFile(context).getAbsolutePath());
            }
            int commandEnd = ssb.length();
            ssb.setSpan(new StyleSpan(Typeface.BOLD), commandStart, commandEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return new CustomError(ssb);
        } catch (Throwable e){
            e.printStackTrace();
            return new CustomError(Utils.describeException(e));
        }
    }

    private class ProcessCategorizer extends Categorizer<RegisteredReceiverInfo> {

        @Override
        void add(RegisteredReceiverInfo component) {
            addToCategory(component.processName, component);
        }

        @Override
        String getTitleForComponent(RegisteredReceiverInfo component) {
            String firstAction = null;
            int otherActionsCount = 0;
            for (IntentFilter intentFilter : component.intentFilters) {
                Iterator<String> iterator = intentFilter.actionsIterator();
                String action;
                while (iterator.hasNext()) {
                    action = iterator.next();
                    if (firstAction == null) {
                        firstAction = action;
                    } else {
                        otherActionsCount++;
                    }
                }
            }
            return firstAction + (otherActionsCount != 0 ? " [+" + otherActionsCount + "]" : "");
        }
    }



    @Override
    int getConfigurationLayout() {
        return R.layout.registered_receivers_filter;
    }

    @Override
    void initConfigurationForm(FetcherOptionsDialog dialog) {
        dialog.setBoxChecked(R.id.exclude_protected, mExcludeProtected);
    }

    @Override
    void updateFromConfigurationForm(FetcherOptionsDialog dialog) {
        mExcludeProtected = dialog.isBoxChecked(R.id.exclude_protected);
    }


    @Override
    boolean isExcludingEverything() {
        return false;
    }



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public static final Creator<RegisteredReceiverFetcher> CREATOR = new Creator<RegisteredReceiverFetcher>() {
        @Override
        public RegisteredReceiverFetcher createFromParcel(Parcel source) {
            return new RegisteredReceiverFetcher();
        }

        @Override
        public RegisteredReceiverFetcher[] newArray(int size) {
            return new RegisteredReceiverFetcher[size];
        }
    };

    // JSON serialization & name
    static final Descriptor DESCRIPTOR = new Descriptor(RegisteredReceiverFetcher.class, "registered-receivers", R.string.registered_receivers) {
        @Override
        Fetcher unserializeFromJSON(JSONObject jsonObject) throws JSONException {
            RegisteredReceiverFetcher fetcher = new RegisteredReceiverFetcher();
            fetcher.mExcludeProtected = jsonObject.getBoolean("excludeProtected");
            return fetcher;
        }
    };

    @Override
    JSONObject serializeToJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("excludeProtected", mExcludeProtected);
        return jsonObject;
    }
}
