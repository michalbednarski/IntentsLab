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
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuggestionSpan;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FormattedTextBuilder {
    private StringBuilder sb = new StringBuilder();

    private List<SpanEntry> spans = new ArrayList<>();

    private void appendSpan(String text, Object style) {
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

    public void appendValuelessKeyContinuingGroup(String key) {
        sb.append("\n");
        appendSpan(key, new StyleSpan(Typeface.BOLD));
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

    public void appendColoured(String string, int color) {
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

    public CharSequence getText() {
        if (spans.size() < 50) {
            return getTextAsSystemSpannable();
        } else {
            return getTextAsMySpannable();
        }
    }

    Spannable getTextAsMySpannable() {
        // Make span array
        final SpanEntry[] spansArr = spans.toArray(new SpanEntry[spans.size()]);

        // Return spannable
        return new MySpannable(sb.toString(), spansArr);
    }

    Spannable getTextAsSystemSpannable() {
        final SpannableString systemSpannable = new SpannableString(sb.toString());
        for (SpanEntry entry : spans) {
            systemSpannable.setSpan(entry.span, entry.start, entry.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return systemSpannable;
    }

    public void appendFormattedText(CharSequence text) {
        //ssb.append(text);
        //
        int last = 0;

        if (text instanceof Spanned) {
            Spanned spannedText = (Spanned) text;
            for (Object span : spannedText.getSpans(0, text.length(), Object.class)) {
                // Get start and end and check them
                int start = spannedText.getSpanStart(span);
                int end = spannedText.getSpanEnd(span);
                if (BuildConfig.DEBUG) {
                    if (!(start <= end)) {
                        throw new AssertionError("Invalid span range");
                    }
                    if (!(start >= last)) {
                        throw new AssertionError("Nested spans are not supported");
                    }
                }
                last = end;

                // Append span
                addSpan(span, start, end);
            }
        }
        sb.append(text.toString());
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
     * Custom version of Spannable. pptimized for performance when using strings with many spans
     * Faster, but uses more memory than system implementation
     */
    private static class MySpannable implements Spannable {
        private final SpanEntry[] mSpansArr;
        private final String mString;
        private final Map<Object, SpanEntry> mSpanMap;

        MySpannable(String string, SpanEntry[] spansArr) {
            // Make span map
            final ArrayMap<Object, SpanEntry> spanMap = new ArrayMap<>();
            for (SpanEntry entry : spansArr) {
                spanMap.put(entry.span, entry);
            }

            // Put results in fields
            mSpansArr = spansArr;
            mString = string;
            mSpanMap = spanMap;
        }

        // For Spannable interface itself
        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            if (type == SuggestionSpan.class) {
                return (T[]) new SuggestionSpan[0];
            }

            if (mSpansArr.length == 0) {
                return (T[]) Array.newInstance(type, 0);
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
            List<T> result = new ArrayList<>();
            for (; mid < mSpansArr.length && mSpansArr[mid].start < end; mid++) {
                if (mSpansArr[mid].end > start && type.isInstance(mSpansArr[mid].span)) {
                    result.add((T) mSpansArr[mid].span);
                }
            }

            return result.toArray((T[]) Array.newInstance(type, result.size()));

        }

        @Override
        public int getSpanStart(Object tag) {
            SpanEntry spanEntry = mSpanMap.get(tag);
            return spanEntry == null ? -1 : spanEntry.start;
        }

        @Override
        public int getSpanEnd(Object tag) {
            SpanEntry spanEntry = mSpanMap.get(tag);
            return spanEntry == null ? -1 : spanEntry.end;
        }

        @Override
        public int getSpanFlags(Object tag) {
            return Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            // If there are no spans we won't be able to do binary search
            if (mSpansArr.length == 0) {
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

        // For CharSequence
        @Override
        public int length() {
            return mString.length();
        }

        @Override
        public char charAt(int index) {
            return mString.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return mString.subSequence(start, end);
        }

        @NonNull
        @Override
        public String toString() {
            return mString;
        }

        // For Spannable editing (not actually supported)
        @Override
        public void setSpan(Object what, int start, int end, int flags) {
        }

        @Override
        public void removeSpan(Object what) {
        }
    }
}
