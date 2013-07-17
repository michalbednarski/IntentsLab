package com.example.testapp1;

import android.content.Intent;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.*;
import android.view.View;

public class FormattedTextBuilder {
    private SpannableStringBuilder ssb = new SpannableStringBuilder();

    private void appendSpan(CharSequence text, Object style) {
        int baseLen = ssb.length();
        int textLen = text.length();
        ssb.append(text);
        ssb.setSpan(style, baseLen, baseLen + textLen, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public void appendHeader(String text) {
        ssb.append("\n\n");
        appendSpan(text, new StyleSpan(Typeface.BOLD));
    }

    public void appendGlobalHeader(String text) {
        if (ssb.length() != 0) {
            ssb.append("\n");
        }
        appendSpan(text, new RelativeSizeSpan(1.5f));
    }

    public void appendValue(String key, String value) {
        ssb.append("\n\n");
        appendSpan(key + ":", new StyleSpan(Typeface.BOLD));
        ssb.append(" " + value);
    }

    public void appendValueNoNewLine(CharSequence key, String value) {
        ssb.append("\n");
        if (value != null) {
            key += ":";
        }
        appendSpan(key, new StyleSpan(Typeface.BOLD));
        if (value != null) {
            ssb.append(" " + value);
        }
    }

    public void appendText(String text) {
        ssb.append("\n" + text);
    }

    public void appendList(String header, String[] items) {
        ssb.append("\n\n");
        appendSpan(header + ":", new StyleSpan(Typeface.BOLD));
        //Log.v("FTB-list-header", header);

        for (String item : items) {
            //Log.v("FTB-list-item", item);
            ssb.append("\n");
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
        ssb.append(' ');
        appendSpan(text, clickableSpan);
    }

    public CharSequence getText() {
        return ssb;
    }

    public void appendFormattedText(CharSequence text) {
        ssb.append(text);
    }


}