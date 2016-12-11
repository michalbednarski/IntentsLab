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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pools;
import android.text.GetChars;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.ParagraphStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.text.style.TabStopSpan;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FormattedTextBuilder {
    private StringBuilder sb = new StringBuilder();

    private List<SpanEntry> spans = new ArrayList<>();

    public void appendSpan(String text, Object style) {
        int baseLen = sb.length();
        int textLen = text.length();
        sb.append(text);
        addSpan(style, baseLen, baseLen + textLen);
    }

    private void addSpan(Object style, int start, int end) {
        SpanEntry spanEntry = new SpanEntry(start, end, style);
        spans.add(spanEntry);
    }

    public void appendHeader(String text) {
        sb.append("\n\n");
        appendSpan(text, new StyleSpan(Typeface.BOLD));
    }

    public void appendGlobalHeader(String text) {
        if (sb.length() != 0) {
            sb.append("\n");
        }
        appendSpan(text, new RelativeSizeSpan(1.5f));
    }

    public void appendValue(String key, String value) {
        appendValue(key, value, true, ValueSemantic.NONE);
    }

    public void appendValueNoNewLine(String key, String value) {
        appendValue(key, value, false, ValueSemantic.NONE);
    }

    public void appendValuelessKeyContinuingGroup(CharSequence key) {
        sb.append("\n");
        int offset = sb.length();
        appendSpan(key.toString(), new StyleSpan(Typeface.BOLD));
        applyFormatting(key, offset);
    }

    public void appendRaw(String text) {
        sb.append(text);
    }

    public enum ValueSemantic {
        NONE,
        ERROR,
        PERMISSION
    }

    public void appendValue(String key, String value, boolean startGroup, ValueSemantic valueSemantic) {
        final String originalValue = value;
        Object span = null;
        switch (valueSemantic) {
            case ERROR:
                span = new ForegroundColorSpan(Color.RED);
                value = "[" + value + "]";
                break;
            case PERMISSION:
                span = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        Context context = widget.getContext();
                        context.startActivity(
                                new Intent(context, SingleFragmentActivity.class)
                                .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, PermissionInfoFragment.class.getName())
                                .putExtra(PermissionInfoFragment.ARG_PERMISSION_NAME, originalValue)
                        );
                    }
                };
                break;
        }

        sb.append(startGroup ? "\n\n" : "\n");
        appendSpan(key + ":", new StyleSpan(Typeface.BOLD));
        sb.append(" ");
        if (span != null) {
            appendSpan(value, span);
        } else {
            sb.append(value);
        }

    }

    public void appendText(String text) {
        sb.append("\n" + text);
    }

    public void appendList(String header, String[] items) {
        sb.append("\n\n");
        appendSpan(header + ":", new StyleSpan(Typeface.BOLD));
        //Log.v("FTB-list-header", header);

        for (String item : items) {
            //Log.v("FTB-list-item", item);
            sb.append("\n");
            appendSpan(item, new BulletSpan());
        }
    }

    public void appendColoured(String string, @ColorInt int color) {
        appendSpan(string, new ForegroundColorSpan(color));
    }

    public void appendColouredAndLinked(String string, final int color, final Runnable action) {
        appendSpan(string, new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                action.run();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(color);
                ds.setUnderlineText(true);
            }
        });
    }

    public void appendClickable(String text, ClickableSpan clickableSpan) {
        sb.append(' ');
        appendSpan(text, clickableSpan);
    }

    public Spannable getText() {
        if (spans.size() < 50) {
            return getTextAsSystemSpannable();
        } else {
            return getTextAsMySpannable();
        }
    }

    @VisibleForTesting
    Spannable getTextAsMySpannable() {
        return new MySpannable(sb.toString(), spans);
    }

    @VisibleForTesting
    Spannable getTextAsSystemSpannable() {
        final SpannableString systemSpannable = new SpannableString(sb.toString());
        for (SpanEntry entry : spans) {
            systemSpannable.setSpan(entry.span, entry.start, entry.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return systemSpannable;
    }

    private void applyFormatting(CharSequence originalText, int offset) {
        if (originalText instanceof Spanned) {
            Spanned spannedText = (Spanned) originalText;
            for (Object span : spannedText.getSpans(0, originalText.length(), Object.class)) {
                // Get start and end and check them
                int start = spannedText.getSpanStart(span) + offset;
                int end = spannedText.getSpanEnd(span) + offset;
                if (BuildConfig.DEBUG) {
                    if (!(start <= end)) {
                        throw new AssertionError("Invalid span range");
                    }
                }

                // Append span
                addSpan(span, start, end);
            }
        }
    }

    public static void putInTextView(TextView textView, CharSequence text) {
        textView.setSpannableFactory(new Spannable.Factory() {
            @Override
            public Spannable newSpannable(CharSequence source) {
                if (source instanceof Spannable) {
                    return (Spannable) source;
                } else {
                    return new SpannableString(source.toString());
                }
            }
        });

        textView.setText(text, TextView.BufferType.NORMAL);
    }


    private static class SpanEntry {
        int start, end;

        Object span;

        SpanEntry(int start, int end, Object span) {
            this.start = start;
            this.end = end;
            this.span = span;
        }
    }

    /**
     * Custom version of Spannable. optimized for performance when using strings with many spans
     * Faster, but uses more memory than system implementation
     */
    private static class MySpannable implements Spannable, GetChars {
        public static final MetricAffectingSpan[] EMPTY_METRIC_AFFECTING_SPAN_ARRAY = new MetricAffectingSpan[0];
        public static final ReplacementSpan[] EMPTY_REPLACEMENT_SPAN_ARRAY = new ReplacementSpan[0];
        public static final LeadingMarginSpan[] EMPTY_LEADING_MARGIN_SPAN_ARRAY = new LeadingMarginSpan[0];
        public static final LineHeightSpan[] EMPTY_LINE_HEIGHT_SPAN_ARRAY = new LineHeightSpan[0];
        public static final TabStopSpan[] EMPTY_TAB_STOP_SPAN_ARRAY = new TabStopSpan[0];
        /**
         * Base SpannableString used for text and unoptimized spans
         */
        private final SpannableString mSpannableString;
        private final SpanEntry[] mSpansArr;
        private final Map<Object, SpanEntry> mSpanMap;

        private static final List<Class<?>> EXCLUDED_SPAN_TYPES;

        private boolean mHasMetricAffectingSpan;
        private boolean mHasReplacementSpan;
        private boolean mHasParagraphStyle;

        private static final Pools.Pool<List> LIST_POOL = new Pools.SimplePool<List>(1) {
            @Override
            public List acquire() {
                List list = super.acquire();
                if (list == null) {
                    list = new ArrayList();
                }
                return list;
            }

            @Override
            public boolean release(List instance) {
                instance.clear();
                return super.release(instance);
            }
        };

        static {
            EXCLUDED_SPAN_TYPES = new ArrayList<>();
            EXCLUDED_SPAN_TYPES.add(SpanWatcher.class);
            try {
                EXCLUDED_SPAN_TYPES.add(Class.forName("android.text.style.SuggestionSpan"));
            } catch (ClassNotFoundException ignored) {
            }
        }


        MySpannable(String string, List<SpanEntry> spans) {
            mSpannableString = new SpannableString(string);

            // Make span map
            final ArrayMap<Object, SpanEntry> spanMap = new ArrayMap<>();
            int lastIndex = 0;
            for (Iterator<SpanEntry> iterator = spans.iterator(); iterator.hasNext(); ) {
                SpanEntry entry = iterator.next();
                if (BuildConfig.DEBUG && entry.end < entry.start) {
                    throw new AssertionError("SpanEntry with end < start");
                }
                onSpanAdded(entry.span);
                boolean isOptimizedEntry = entry.start >= lastIndex && !isExcludedSpanType(entry.span.getClass());
                if (isOptimizedEntry) {
                    spanMap.put(entry.span, entry);
                    lastIndex = entry.end;
                } else {
                    mSpannableString.setSpan(entry.span, entry.start, entry.end, SPAN_EXCLUSIVE_EXCLUSIVE);
                    iterator.remove();
                }
            }

            // Put results in fields
            mSpansArr = spans.toArray(new SpanEntry[spans.size()]);
            mSpanMap = spanMap;
        }

        // For Spannable interface itself
        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            // Fast path for common time-critical spans that aren't there
            if (type == MetricAffectingSpan.class && !mHasMetricAffectingSpan) {
                return (T[]) EMPTY_METRIC_AFFECTING_SPAN_ARRAY;
            }
            if (type == ReplacementSpan.class && !mHasReplacementSpan) {
                return (T[]) EMPTY_REPLACEMENT_SPAN_ARRAY;
            }
            if (!mHasParagraphStyle) {
                if (type == LeadingMarginSpan.class) {
                    return (T[]) EMPTY_LEADING_MARGIN_SPAN_ARRAY;
                }
                if (type == LineHeightSpan.class) {
                    return (T[]) EMPTY_LINE_HEIGHT_SPAN_ARRAY;
                }
                if (type == TabStopSpan.class) {
                    return (T[]) EMPTY_TAB_STOP_SPAN_ARRAY;
                }
            }

            T[] spansFromSuperclass = mSpannableString.getSpans(start, end, type);

            if (
                    mSpansArr.length == 0 || // We have no optimized spans
                    isExcludedSpanType(type)) { // Query is about unoptimized span
                return spansFromSuperclass;
            }

            // Based on Arrays.binarySearch()
            int lo = 0;
            int hi = mSpansArr.length - 1;
            int mid = -2;

            while (lo <= hi) {
                mid = (lo + hi) >>> 1;
                int midVal = mSpansArr[mid].end;

                if (midVal < start) {
                    lo = mid + 1;
                } else if (midVal > start) {
                    hi = mid - 1;
                } else {
                    break;
                }
            }

            // Iterate over spans in range
            List<T> result = null;
            for (; mid < mSpansArr.length && mSpansArr[mid].start < end; mid++) {
                if (mSpansArr[mid].end > start && type.isInstance(mSpansArr[mid].span)) {
                    if (result == null) {
                        result = LIST_POOL.acquire();
                        if (spansFromSuperclass.length != 0) {
                            result.addAll(Arrays.asList(spansFromSuperclass));
                        }
                    }
                    result.add((T) mSpansArr[mid].span);
                }
            }

            // If we have list then make array and pass to superclass
            if (result == null) {
                return spansFromSuperclass;
            } else {
                T[] resultArray = result.toArray((T[]) Array.newInstance(type, result.size()));
                LIST_POOL.release(result);
                return resultArray;
            }
        }

        @Override
        public int getSpanStart(Object tag) {
            SpanEntry spanEntry = mSpanMap.get(tag);
            return spanEntry == null ? mSpannableString.getSpanStart(tag) : spanEntry.start;
        }

        @Override
        public int getSpanEnd(Object tag) {
            SpanEntry spanEntry = mSpanMap.get(tag);
            return spanEntry == null ? mSpannableString.getSpanEnd(tag) : spanEntry.end;
        }

        @Override
        public int getSpanFlags(Object tag) {
            SpanEntry spanEntry = mSpanMap.get(tag);
            return spanEntry == null ? mSpannableString.getSpanFlags(tag) : SPAN_EXCLUSIVE_EXCLUSIVE;
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            // Fast path for common time-critical spans that aren't there
            if (type == MetricAffectingSpan.class && !mHasMetricAffectingSpan) {
                return limit;
            }

            // Let superclass find it's boundary
            limit = mSpannableString.nextSpanTransition(start, limit, type);

            if (
                    mSpansArr.length == 0 || // We have no optimized spans
                    isExcludedSpanType(type)) { // Query is about unoptimized span
                return limit;
            }

            // Based on Arrays.binarySearch()
            int lo = 0;
            int hi = mSpansArr.length - 1;
            int mid = -2;

            while (lo <= hi) {
                mid = (lo + hi) >>> 1;
                int midVal = mSpansArr[mid].end;

                if (midVal < start) {
                    lo = mid + 1;
                } else if (midVal > start) {
                    hi = mid - 1;
                } else {
                    break;
                }
            }

            for (; mid < mSpansArr.length && mSpansArr[mid].start <= limit; mid++) {
                SpanEntry entry = mSpansArr[mid];
                if (entry.start > start && entry.start < limit && type.isInstance(entry.span)) {
                    return entry.start;
                }
                if (entry.end > start && entry.end < limit && type.isInstance(entry.span)) {
                    return entry.end;
                }
            }
            return limit;
        }

        private static boolean isExcludedSpanType(Class<?> type)
        {
            for (Class<?> excludedSpanType : EXCLUDED_SPAN_TYPES) {
                if (excludedSpanType.isAssignableFrom(type)) {
                    return true;
                }
            }
            return false;
        }

        private void onSpanAdded(Object span) {
            if (span instanceof MetricAffectingSpan) {
                mHasMetricAffectingSpan = true;
            }
            if (span instanceof ReplacementSpan) {
                mHasReplacementSpan = true;
            }
            if (span instanceof ParagraphStyle) {
                mHasParagraphStyle = true;
            }
        }

        // Directly delegated methods
        @Override
        public void setSpan(Object what, int start, int end, int flags) {
            onSpanAdded(what);
            mSpannableString.setSpan(what, start, end, flags);
        }

        @Override
        public void removeSpan(Object what) {
            mSpannableString.removeSpan(what);
        }

        @Override
        public int length() {
            return mSpannableString.length();
        }

        @Override
        public char charAt(int index) {
            return mSpannableString.charAt(index);
        }

        @NonNull
        @Override
        public String toString() {
            return mSpannableString.toString();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            // TODO: Retain "optimized" spans
            // if that turns out to be needed
            return mSpannableString.subSequence(start, end);
        }

        @Override
        public void getChars(int start, int end, char[] dest, int destoff) {
            mSpannableString.getChars(start, end, dest, destoff);
        }
    }
}
