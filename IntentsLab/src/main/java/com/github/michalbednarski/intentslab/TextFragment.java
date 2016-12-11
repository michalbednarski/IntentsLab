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

package com.github.michalbednarski.intentslab;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base fragment for displaying long text
 */
public class TextFragment extends Fragment {
    private View mLoaderView;
    private ScrollView mScrollView;
    private TextView mTextView;

    private Spannable mText;

    private @ColorInt int mSearchHighlightColor;
    private String mLastSearchQuery;
    private List<BackgroundColorSpan> mSearchHighlights = new ArrayList<>();
    private boolean mIsPutTextPending;

    private int[] mHitLines;
    private WeakReference<Layout> mHitLinesGeneratedForLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSearchHighlightColor = getResources().getColor(R.color.search_highlight);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.xml_viewer, container, false);
        mLoaderView = view.findViewById(R.id.loader);
        mScrollView = (ScrollView) view.findViewById(R.id.xml_wrapper);
        mTextView = (TextView) view.findViewById(R.id.xml);
        mTextView.setMovementMethod(LinkMovementMethod.getInstance());
        if (mText != null) {
            putTextInView();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        mLoaderView = null;
        mScrollView = null;
        mTextView = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.text_viewer, menu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            if (mLastSearchQuery != null) {
                searchView.setQuery(mLastSearchQuery, false);
            }
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    InputMethodManager imm = (InputMethodManager)
                            getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                    doSearch(query, true);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return true;
                }
            });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean canSearch = mText != null;
        boolean canFindNext = canSearch && !mSearchHighlights.isEmpty();
        menu.findItem(R.id.action_search)
                .setVisible(canSearch)
                .setEnabled(canSearch);
        menu.findItem(R.id.action_find_next)
                .setVisible(canFindNext)
                .setEnabled(canFindNext);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search && !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)) {
            final EditText queryView = new EditText(getContext());
            queryView.setSingleLine();
            if (mLastSearchQuery != null) {
                queryView.setText(mLastSearchQuery);
            }
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.search)
                    .setView(queryView)
                    .setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doSearch(queryView.getText().toString(), true);
                        }
                    })
                    .show();
            return true;
        }
        if (item.getItemId() == R.id.action_find_next) {
            scrollToSearchHit(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && mIsPutTextPending) {
            mIsPutTextPending = false;
            putTextInView();
        }
    }

    private void doSearch(String query, boolean doScroll) {
        // Remember last query
        mLastSearchQuery = query;

        if (mText == null) {
            return;
        }

        // Clear highlights
        clearSearchHighlights();

        // Handle empty "query"
        if (query != null && query.length() != 0) {
            // Highlight all occurrences
            Matcher matcher =
                    Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE).matcher(mText);

            while (matcher.find()) {
                BackgroundColorSpan highlight = new BackgroundColorSpan(mSearchHighlightColor);
                mText.setSpan(highlight, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mSearchHighlights.add(highlight);
            }
        }

        if (doScroll) {
            scrollToSearchHit(false);
        }

        ActivityCompat.invalidateOptionsMenu(getActivity());
    }

    private void clearSearchHighlights()
    {
        for (BackgroundColorSpan searchHighlight : mSearchHighlights) {
            mText.removeSpan(searchHighlight);
        }
        mSearchHighlights.clear();
        mHitLines = null;
    }


    private void generateHitLinesIfNeeded()
    {
        Layout layout = mTextView.getLayout();

        if (
                mHitLines != null &&
                mHitLinesGeneratedForLayout != null &&
                mHitLinesGeneratedForLayout.get() == layout) {
            // Already up to date
            return;
        }

        ArrayList<Integer> hitLines = new ArrayList<>();
        int lastLine = -2;
        for (BackgroundColorSpan searchHighlight : mSearchHighlights) {
            int line = layout.getLineForOffset(mText.getSpanStart(searchHighlight));
            if (line != lastLine) {
                hitLines.add(line);
            }
            lastLine = line;
        }

        mHitLines = Utils.toIntArray(hitLines);
        mHitLinesGeneratedForLayout = new WeakReference<>(layout);
    }

    private void scrollToSearchHit(boolean next)
    {
        if (
                mText == null ||
                mTextView == null ||
                mTextView.getLayout() == null ||
                mSearchHighlights == null ||
                mSearchHighlights.isEmpty()
                ) {
            return;
        }

        generateHitLinesIfNeeded();
        int currentScroll = mScrollView.getScrollY();


        boolean isScrolledToEnd = mScrollView.getScrollY() >= mTextView.getHeight() - mScrollView.getHeight();

        int nextOccurrenceIndex;
        if (isScrolledToEnd) {
            nextOccurrenceIndex = 0;
        } else {
            int currentLine = mTextView.getLayout().getLineForVertical(currentScroll);
            nextOccurrenceIndex = Arrays.binarySearch(mHitLines, currentLine);
            if (nextOccurrenceIndex < 0) {
                nextOccurrenceIndex = ~nextOccurrenceIndex;
            } else if (next) {
                nextOccurrenceIndex++;
            }
        }
        if (nextOccurrenceIndex >= mHitLines.length) {
            nextOccurrenceIndex = 0;
        }

        Rect lineBounds = new Rect();
        mTextView.getLineBounds(mHitLines[nextOccurrenceIndex], lineBounds);
        mScrollView.smoothScrollTo(0, lineBounds.top);
    }

    protected void publishText(Spannable text) {
        mSearchHighlights.clear();
        mText = text;
        if (mLoaderView != null) {
            putTextInView();
        }
    }

    private void putTextInView() {
        if (!getUserVisibleHint()) {
            mIsPutTextPending = true;
            return;
        }
        ActivityCompat.invalidateOptionsMenu(getActivity());
        FormattedTextBuilder.putInTextView(mTextView, mText);
        mScrollView.setVisibility(View.VISIBLE);

        mLoaderView.setVisibility(View.GONE);
    }
}
