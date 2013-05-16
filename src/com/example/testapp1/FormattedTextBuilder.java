package com.example.testapp1;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

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

	public void appendValue(String key, String value) {
		ssb.append("\n\n");
		appendSpan(key + ":", new StyleSpan(Typeface.BOLD));
		ssb.append(" " + value);
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

	public CharSequence getText() {
		return ssb;
	}

	public void appendFormattedText(CharSequence text) {
		ssb.append(text);
	}


}