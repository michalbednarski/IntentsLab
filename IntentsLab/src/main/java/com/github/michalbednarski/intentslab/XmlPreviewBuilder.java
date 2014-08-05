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
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Build formatted XML for display, may add clickable links to values
 *
 * Also can split text if it's to long to be displayed efficiently
 */
public class XmlPreviewBuilder {

    /**
     * Main text being build
     *
     * If we're splitting text for efficient display in ListView
     * then this is last chunk
     */
    private SpannableStringBuilder mSsb;




    private boolean mHaveNotEndedOpenTag = false;
    private boolean mTagHasMoreThanOneAttribute = false;

    /**
     * This is first attribute of tag as we don't know yet if it should be printed inline with tag or in separate line
     */
    private SpannableStringBuilder mPendingUnpaddedAttr = null;




    private final int mTagColor;
    private final int mAttributeNameColor;
    private final int mAttributeValueColor;


    private static final int CHUNK_LINES = 50;
    private ArrayList<SpannableStringBuilder> mTextChunks = null;
    private int mLinesToNextTextSplit = CHUNK_LINES;


    /**
     * If true next call to {@link #newLine()} will be ignored
     */
    private boolean mSkipNextNewLine = true;

    /**
     * Add \n to {@link #mSsb} or split it if needed
     */
    private void newLine() {
        if (mSkipNextNewLine) {
            mSkipNextNewLine = false;
            return;
        }
        boolean splitNow = (mTextChunks != null) && (mLinesToNextTextSplit-- == 0);
        if (splitNow) {
            mTextChunks.add(mSsb);
            mSsb = new SpannableStringBuilder();
            mLinesToNextTextSplit = CHUNK_LINES;
        } else {
            mSsb.append("\n");
        }
    }



    private int mDepth = 0;

    private String getPadding() {
        return getPadding(0);
    }

    private String getPadding(int extraPadding) {
        final int spacesAmount = 2 * mDepth + extraPadding;
        if (spacesAmount < 0) {
            return ""; // Shouldn't happen
        }
        // http://stackoverflow.com/a/4903603
        return new String(new char[spacesAmount]).replace('\0', ' ');
    }


    /**
     * Start the open tag
     * Use {@link #attr(String, String)} to add attributes
     */
    public void openTag(String name) {
        // Finish previous open tag
        finishTag(false);

        // New line and padding
        newLine();
        mSsb.append(getPadding());

        // Append text and color it
        int startTagStart = mSsb.length();
        mSsb.append("<").append(name);
        int startTagEnd = mSsb.length();
        mSsb.setSpan(new ForegroundColorSpan(mTagColor), startTagStart, startTagEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set flags for formatting next elements
        mHaveNotEndedOpenTag = true;
        mTagHasMoreThanOneAttribute = false;
        mDepth++;
    }


    /**
     * Helper for {@link #attr(String, String)}
     *
     * Returns SpannableStringBuilder that should be used to add attr
     * this function will care about adding new line and padding
     *
     * @see #mPendingUnpaddedAttr
     */
    private SpannableStringBuilder getBuilderForAttrAndAddPadding() {
        assert mHaveNotEndedOpenTag;

        if (mPendingUnpaddedAttr != null) {
            // Second attribute, flush the first one and fall through to next attribute
            newLine();
            mSsb.append(getPadding(-1)).append(mPendingUnpaddedAttr);
            mPendingUnpaddedAttr = null;
            mTagHasMoreThanOneAttribute = true;
        } else if (!mTagHasMoreThanOneAttribute) {
            // First attribute, may be inline if it's only attribute
            mPendingUnpaddedAttr = new SpannableStringBuilder();
            return mPendingUnpaddedAttr;
        }

        // Next attribute (Every but first)
        newLine();
        mSsb.append(getPadding(-1));
        return mSsb;
    }

    /**
     * Add attr from resource parser, resolving resources if needed
     */
    public void attrFromResourceParser(XmlResourceParser resParser, int attrIndex, Context xmlOwnerContext) {
        String name = resParser.getAttributeName(attrIndex);
        String value = resParser.getAttributeValue(attrIndex);

        // If string is empty or null we skip all following tests
        if ("".equals(value)) {
            attr(name, value);
            return;
        }

        // Check if this is reference
        if (value.charAt(0) == '@') {
            try {
                final int resourceId = resParser.getAttributeResourceValue(attrIndex, 0);
                if (resourceId != 0) {
                    TypedValue res = new TypedValue();
                    xmlOwnerContext.getResources().getValue(resourceId, res, true);

                    // String? (also raw/xml types are represented as strings)
                    if (res.type == TypedValue.TYPE_STRING) {
                        String resStringValue = res.string.toString();

                        // XML?
                        if (resStringValue.endsWith(".xml")) {
                            try {
                                xmlOwnerContext.getResources().getXml(resourceId).close();
                                final String attrOwnerPackageName = xmlOwnerContext.getPackageName();
                                attr(name, resStringValue, new ClickableSpan() {
                                    @Override
                                    public void onClick(View widget) {
                                        widget.getContext().startActivity(
                                                new Intent(widget.getContext(), SingleFragmentActivity.class)
                                                .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, XmlViewerFragment.class.getName())
                                                .putExtra(XmlViewerFragment.ARG_PACKAGE_NAME, attrOwnerPackageName)
                                                .putExtra(XmlViewerFragment.ARG_RESOURCE_ID, resourceId)
                                        );
                                    }
                                });
                                return;
                            } catch (Resources.NotFoundException ignored) {}
                        }

                        // Another file resource
                        {
                            try {
                                // Check if file can be accessed
                                xmlOwnerContext.getResources().getAssets().openNonAssetFd(resStringValue).close();

                                // Build an intent to view it
                                final Intent intent = new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(
                                                "content://" + AssetProvider.AUTHORITY +
                                                        "/" + xmlOwnerContext.getPackageName() + "/" + resStringValue
                                        )
                                );
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                                // Get mime type
                                intent.resolveType(xmlOwnerContext.getContentResolver());

                                // Check if any app can handle this intent and if so display value as link
                                if (!xmlOwnerContext.getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
                                    attr(name, resStringValue, new ClickableSpan() {
                                        @Override
                                        public void onClick(View widget) {
                                            widget.getContext().startActivity(intent);
                                        }
                                    });
                                    return;
                                } // Otherwise fall through
                            } catch (IOException ignored) {}
                        }

                        attr(name, resStringValue);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Nothing special was recognized
        attr(name, value);
    }

    /**
     * Add attr to last opened tag
     */
    public void attr(String name, String value) {
        attr(name, value, new ForegroundColorSpan(mAttributeValueColor));
    }

    /**
     * Add attr to last opened tag, using specified span (eg. ClickableSpan) for value
     */
    private void attr(String name, String value, Object valueSpan) {
        // Get builder and add padding
        SpannableStringBuilder builder = getBuilderForAttrAndAddPadding();

        // Append text
        int attrNameStart = builder.length();
        builder.append(name).append("=");
        int attrNameEndAndValueStart = builder.length();
        builder.append("\"").append(value).append("\"");
        int attrValueEnd = builder.length();

        // Color name
        builder.setSpan(new ForegroundColorSpan(mAttributeNameColor), attrNameStart, attrNameEndAndValueStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(valueSpan, attrNameEndAndValueStart, attrValueEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * End the last open tag
     */
    private void finishTag(boolean selfClose) {
        if (mHaveNotEndedOpenTag) {
            if (mPendingUnpaddedAttr != null) {
                mSsb.append(" ").append(mPendingUnpaddedAttr);
                mPendingUnpaddedAttr = null;
            }
            int start = mSsb.length();
            mSsb.append(selfClose ? " />" : ">");
            int end = mSsb.length();
            mSsb.setSpan(new ForegroundColorSpan(mTagColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            mHaveNotEndedOpenTag = false;
        }
    }

    /**
     * Add text in currently opened element
     */
    public void text(String s) {
        finishTag(false);
        mSsb.append("\n").append(s);
    }

    /**
     * Write an end tag or self-close last open tag if can
     */
    public void endTag(String name) {
        if (mHaveNotEndedOpenTag) {
            // Not finished open tag, use self closing tag (/>)
            finishTag(true);
            mDepth--;
        } else {
            // New line and padding
            mDepth--;
            newLine();
            mSsb.append(getPadding());

            // Text and it's color
            int endTagStart = mSsb.length();
            mSsb.append("</").append(name).append(">");
            int endTagEnd = mSsb.length();
            mSsb.setSpan(new ForegroundColorSpan(mTagColor), endTagStart, endTagEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Write an exception description to show it to user
     */
    public void showException(Exception e) {
        String description = Utils.describeException(e);
        newLine();
        newLine();
        int messageStart = mSsb.length();
        mSsb.append("[").append(description).append("]");
        int messageEnd = mSsb.length();
        mSsb.setSpan(new ForegroundColorSpan(Color.RED), messageStart, messageEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * Make sure that exactly one of {@link #mSsb} and {@link #mTextChunks} is null
     * After being called no further additions are allowed
     */
    private void normalizeChunks() {
        if (mSsb != null && mTextChunks != null) {
            if (mLinesToNextTextSplit > CHUNK_LINES / 2 && !mTextChunks.isEmpty()) {
                // Last chunk is smaller than half
                // Append to last chunk
                mTextChunks.get(mTextChunks.size() - 1).append("\n").append(mSsb);
            } else {
                // Last chunk larger than half or it's only chunk
                // Make it separate chunk
                mTextChunks.add(mSsb);
            }

            // Neuter mSsb
            mSsb = null;

            // Check if list now has only one entry
            if (mTextChunks.size() == 1) {
                mSsb = mTextChunks.get(0);
                mTextChunks = null;
            }
        }
    }

    /**
     * Create final possibly-chunked text for use in
     * {@link com.github.michalbednarski.intentslab.TextFragment#publishText(Object)}
     *
     * Do not modify text after calling this method
     */
    public Object getPossiblyChunkedText() {
        normalizeChunks();

        if (mTextChunks != null) {
            return mTextChunks.toArray(new CharSequence[mTextChunks.size()]);
        } else {
            return mSsb;
        }
    }



    private XmlPreviewBuilder(Resources myResources) {
        mTagColor = myResources.getColor(R.color.xml_tag);
        mAttributeNameColor = myResources.getColor(R.color.xml_attr_name);
        mAttributeValueColor = myResources.getColor(R.color.xml_attr_value);
    }

    public XmlPreviewBuilder(Resources resources, boolean splitForFakeListView) {
        this(resources);
        mSsb = new SpannableStringBuilder();
        if (splitForFakeListView) {
            mTextChunks = new ArrayList<SpannableStringBuilder>();
        }
    }

    public XmlPreviewBuilder(Resources resources, SpannableStringBuilder ssb) {
        this(resources);
        mSsb = ssb;
    }
}
