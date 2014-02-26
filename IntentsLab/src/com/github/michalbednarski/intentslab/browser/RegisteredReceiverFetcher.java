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
public class RegisteredReceiverFetcher extends Fetcher {

    @Override
    Object getEntries(Context context) {
        try {
            final Categorizer<RegisteredReceiverInfo> categorizer = new ProcessCategorizer();
            (new RegisteredReceiversParser() {
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
        // empty form
    }

    @Override
    void updateFromConfigurationForm(FetcherOptionsDialog dialog) {
        // empty form
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
            return fetcher;
        }
    };

    @Override
    JSONObject serializeToJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        return jsonObject;
    }
}
