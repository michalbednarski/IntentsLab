package com.example.testapp1;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Autocomplete adapter for action names etc.
 */
public class NameAutocompleteAdapter extends BaseAdapter implements Filterable {
    private static final String TAG = "NameAutocompleteAdapter";
    private final Context mContext;
    private final LayoutInflater mInflater;
    private String[] mNames = null;
    private int mNamesResource;
    private final Object mResourceLoadLock = new Object();
    private FilteredSuggestions mFilteredSuggestions = null;

    public NameAutocompleteAdapter(Context context, int namesResource) {
        mContext = context;
        mNamesResource = namesResource;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public NameAutocompleteAdapter(Context context, String names[]) {
        mContext = context;
        mNames = names;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mFilteredSuggestions == null ? 0 : mFilteredSuggestions.suggestions.length;
    }

    @Override
    public Object getItem(int position) {
        return mFilteredSuggestions.suggestions[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflate view
        if (convertView == null) {
            convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        // Get suggestion text
        String suggestion = mFilteredSuggestions.suggestions[position];
        final SpannableString formattedSuggestion = new SpannableString(suggestion);

        // Highlight searched parts
        suggestion = suggestion.toLowerCase();
        int endPos = 0;
        for (String highlightPart : mFilteredSuggestions.highlightParts) {
            int startPos = suggestion.indexOf(highlightPart, endPos);
            if (startPos == -1) {
                // Shouldn't happen
                Log.e(TAG, "Couldn't find part to highlight");
                break;
            }
            endPos = startPos + highlightPart.length();
            formattedSuggestion.setSpan(new StyleSpan(Typeface.BOLD), startPos, endPos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Set text and return
        ((TextView) convertView).setText(formattedSuggestion);
        return convertView;
    }

    private static class FilteredSuggestions {
        String suggestions[];
        String highlightParts[];
    }

    private class MyFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (TextUtils.isEmpty(constraint)) {
                // Return empty results
                FilterResults wrappedResults = new FilterResults();
                wrappedResults.values = null;
                wrappedResults.count = 0;
                return wrappedResults;
            }

            // Load names from resource
            if (mNames == null) { // Check first outside synchronized for performance since we don't lock it for reading
                synchronized (mResourceLoadLock) {
                    if (mNames == null) { // Check again to avoid race conditions
                        try {
                            final BufferedReader reader = new BufferedReader(new InputStreamReader(mContext.getResources().openRawResource(mNamesResource)));
                            ArrayList<String> nameList = new ArrayList<String>();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                line = line.split("#", 2)[0].trim();
                                if (!line.equals("")) {
                                    nameList.add(line);
                                }
                            }
                            mNames = nameList.toArray(new String[nameList.size()]);
                        } catch (IOException e) {
                            e.printStackTrace();
                            mNames = new String[0];
                        }
                    }
                }
            }

            // Build Pattern from constraint text
            String queryParts[] = constraint.toString().split("[^0-9A-Za-z]+");
            ArrayList<String> highlightParts = new ArrayList<String>(queryParts.length);
            String patternText = ".*";
            for (String queryPart : queryParts) {
                patternText += "(^|[^0-9A-Z])" + Pattern.quote(queryPart) + ".*";
                highlightParts.add(queryPart.toLowerCase());
            }
            final Pattern pattern = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);

            // Test all names
            ArrayList<String> suggestions = new ArrayList<String>();
            for (String name : mNames) {
                if (pattern.matcher(name).matches()) {
                    suggestions.add(name);
                }
            }

            // Return results
            FilteredSuggestions results = new FilteredSuggestions();
            results.suggestions = suggestions.toArray(new String[suggestions.size()]);
            results.highlightParts = highlightParts.toArray(new String[highlightParts.size()]);

            FilterResults wrappedResults = new FilterResults();
            wrappedResults.values = results;
            wrappedResults.count = suggestions.size();
            return wrappedResults;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredSuggestions = (FilteredSuggestions) results.values;
            notifyDataSetChanged();
        }
    }

    @Override
    public Filter getFilter() {
        return new MyFilter();
    }
}
