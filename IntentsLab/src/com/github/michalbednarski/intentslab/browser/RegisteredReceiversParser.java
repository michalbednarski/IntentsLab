package com.github.michalbednarski.intentslab.browser;

import android.content.Context;
import android.content.IntentFilter;
import android.os.PatternMatcher;
import android.util.Log;
import com.github.michalbednarski.intentslab.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mb on 20.02.14.
 */
public abstract class RegisteredReceiversParser {
    private static final String TAG = "ReReParser";
    private static final boolean VERBOSE = false;

    /**
     * This method is called from {@link #parse(Context)} when receiver is found
     * Warning: This method will be called while activity manager lock is held.
     *          Don't use activity manager from it and avoid doing slow operations in it.
     */
    protected abstract void onReceiverFound(RegisteredReceiverInfo receiverInfo);

    // Expressions for dump parsing
    private static final Pattern PATTERN_RECEIVER_LIST_START =
            Pattern.compile("  \\* ReceiverList\\{.*\\}");
    private static final Pattern PATTERN_RECEIVER_LIST_SECOND_LINE =
            Pattern.compile("    app=ProcessRecord\\{[0-9a-f]+ \\d+:(.*)/[0-9au]+\\} pid=(\\d+) uid=(\\d+)");
    private static final Pattern PATTERN_RECEIVER_LIST_SECOND_LINE_V2 =
            Pattern.compile("    app=\\d+:(.*)/[0-9au]+ pid=(\\d+) uid=(\\d+)( .*)?");
    private static final Pattern PATTERN_RECEIVER_FILTER_START =
            Pattern.compile("    Filter #\\d+: BroadcastFilter\\{.*\\}");
    private static final Pattern PATTERN_RECEIVER_FILTER_ACTION =
            Pattern.compile("      Action: \"(.*)\"");
    private static final Pattern PATTERN_RECEIVER_FILTER_CATEGORY =
            Pattern.compile("      Category: \"(.*)\"");
    private static final Pattern PATTERN_RECEIVER_FILTER_SCHEME =
            Pattern.compile("      Scheme: \"(.*)\"");
    private static final Pattern PATTERN_RECEIVER_FILTER_AUTHORITY =
            Pattern.compile("      Authority: \"(.*)\": (-?\\d+)( WILD)?");
    private static final Pattern PATTERN_RECEIVER_FILTER_TYPE =
            Pattern.compile("      Type: \"(.*)\"");
    private static final Pattern PATTERN_RECEIVER_FILTER_PATH =
            Pattern.compile("      Path: \"PatternMatcher\\{(LITERAL|PREFIX|GLOB): (.*)\\}\"");
    private static final Pattern PATTERN_RECEIVER_FILTER_PRIORITY =
            Pattern.compile("      mPriority=(-?\\d+),.*");
    private static final Pattern PATTERN_RECEIVER_FILTER_PERMISSION =
            Pattern.compile("      requiredPermission=(.*)");
    private static final Pattern PATTERN_LIST_END =
            Pattern.compile("\\s*Receiver Resolver Table:");






    public void parse(Context context) throws Throwable {
        InputStream inputStream = null;
        try {
            // Define variables
            RegisteredReceiverInfo receiverInfo = null;
            ArrayList<IntentFilter> filters = new ArrayList<IntentFilter>();
            ArrayList<String> filterPermissions = new ArrayList<String>();
            IntentFilter currentFilter = new IntentFilter();

            // Start dumping receivers
            if (android.os.Build.VERSION.SDK_INT >= 8) {
                inputStream = Utils.dumpSystemService(context, "activity", new String[]{"broadcasts"});
            } else {
                // Legacy syntax
                inputStream = Utils.dumpSystemService(context, "activity.broadcasts", new String[0]);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            // Find start of receiver list
            String line;
            do {
                line = reader.readLine().trim();
            } while (!line.contains("Registered Receivers:"));

            // Start parsing
            while ((line = reader.readLine()) != null) {
                if (VERBOSE) {
                    Log.v(TAG, "Parsing: " + line);
                }

                // Match receiver start
                Matcher matcher = PATTERN_RECEIVER_LIST_START.matcher(line);
                if (matcher.find()) {
                    line = reader.readLine();
                    matcher = PATTERN_RECEIVER_LIST_SECOND_LINE.matcher(line);
                    if (!matcher.find()) {
                        // Try again using different syntax
                        matcher = PATTERN_RECEIVER_LIST_SECOND_LINE_V2.matcher(line);
                        if (!matcher.find()) {
                            throw new Exception("Unexpected second line");
                        }
                    }

                    // Finish previous info // this code is also below
                    if (receiverInfo != null) {
                        receiverInfo.intentFilters = filters.toArray(new IntentFilter[filters.size()]);
                        receiverInfo.filterPermissions = filterPermissions.toArray(new String[filterPermissions.size()]);
                        filters.clear();
                        filterPermissions.clear();
                        onReceiverFound(receiverInfo);
                    }
                    currentFilter = null;

                    // Prepare new info
                    receiverInfo = new RegisteredReceiverInfo();
                    receiverInfo.processName = matcher.group(1);
                    receiverInfo.pid = Integer.parseInt(matcher.group(2));
                    receiverInfo.uid = Integer.parseInt(matcher.group(3));

                    continue;
                }

                // Match filter start
                matcher = PATTERN_RECEIVER_FILTER_START.matcher(line);
                if (matcher.find()) {
                    currentFilter = new IntentFilter();
                    filters.add(currentFilter);
                    filterPermissions.add(null); // Will replace if we find
                    continue;
                }

                // Match filter components
                matcher = PATTERN_RECEIVER_FILTER_ACTION.matcher(line);
                if (matcher.find()) {
                    currentFilter.addAction(matcher.group(1));
                    continue;
                }

                matcher = PATTERN_RECEIVER_FILTER_CATEGORY.matcher(line);
                if (matcher.find()) {
                    currentFilter.addCategory(matcher.group(1));
                    continue;
                }

                matcher = PATTERN_RECEIVER_FILTER_SCHEME.matcher(line);
                if (matcher.find()) {
                    currentFilter.addDataScheme(matcher.group(1));
                    continue;
                }

                matcher = PATTERN_RECEIVER_FILTER_AUTHORITY.matcher(line);
                if (matcher.find()) {
                    String host = matcher.group(1);
                    if (matcher.group(3) != null) { // " WILD" at end of parsed line
                        host = "*" + host; // Prepend wildcard
                    }
                    currentFilter.addDataAuthority(host, matcher.group(2));
                    continue;
                }

                matcher = PATTERN_RECEIVER_FILTER_PATH.matcher(line);
                if (matcher.find()) {
                    switch (matcher.group(1).charAt(0)) {
                        case 'L': // LITERAL
                            currentFilter.addDataPath(matcher.group(2), PatternMatcher.PATTERN_LITERAL);
                            break;
                        case 'P': // PREFIX
                            currentFilter.addDataPath(matcher.group(2), PatternMatcher.PATTERN_PREFIX);
                            break;
                        case 'G': // GLOB
                            currentFilter.addDataPath(matcher.group(2), PatternMatcher.PATTERN_SIMPLE_GLOB);
                            break;
                    }
                    continue;
                }

                matcher = PATTERN_RECEIVER_FILTER_TYPE.matcher(line);
                if (matcher.find()) {
                    String type = matcher.group(1);
                    try {
                        if (type.contains("/")) {
                            currentFilter.addDataType(type);
                        } else {
                            currentFilter.addDataType(type + "/*");
                        }
                    } catch (IntentFilter.MalformedMimeTypeException e) {
                        Log.w(TAG, "Malformed type: " + type);
                        e.printStackTrace();
                    }
                    continue;
                }

                // Match filter priority
                matcher = PATTERN_RECEIVER_FILTER_PRIORITY.matcher(line);
                if (matcher.find()) {
                    currentFilter.setPriority(Integer.parseInt(matcher.group(1)));
                    continue;
                }

                // Match filter permission
                matcher = PATTERN_RECEIVER_FILTER_PERMISSION.matcher(line);
                if (matcher.find()) {
                    filterPermissions.set(filterPermissions.size() - 1, matcher.group(1));
                    continue;
                }

                // Match list end
                if (PATTERN_LIST_END.matcher(line).find()) {
                    break;
                }

                if (VERBOSE) {
                    Log.v(TAG, "No matches!");
                }
            }

            if (VERBOSE) {
                Log.v(TAG, "Finished");
            }

            // Finish last info // TODO: this duplicates "Finish previous info" above
            if (receiverInfo != null) {
                receiverInfo.intentFilters = filters.toArray(new IntentFilter[filters.size()]);
                receiverInfo.filterPermissions = filterPermissions.toArray(new String[filterPermissions.size()]);
                onReceiverFound(receiverInfo);
            }
        } finally {
            try {
                inputStream.close();
            } catch (Exception ignored) {}
        }
    }
}
