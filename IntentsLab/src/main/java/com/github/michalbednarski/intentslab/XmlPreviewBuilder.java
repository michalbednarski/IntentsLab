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
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.View;

import java.io.IOException;

/**
 * Build formatted XML for display, may add clickable links to values
 *
 * Also can split text if it's to long to be displayed efficiently
 */
public class XmlPreviewBuilder {

    /**
     * Main text being build
     */
    private FormattedTextBuilder mFtb = new FormattedTextBuilder();

    private boolean mEnableColoring;

    private boolean mHaveNotEndedOpenTag = false;

    private @ColorInt final int mTagColor;
    private @ColorInt final int mAttributeNameColor;
    private @ColorInt final int mAttributeValueColor;

    private int mDepth = 0;

    /**
     * If true next call to {@link #newLine()} will be ignored
     */
    private boolean mSkipNextNewLine = true;

    private void appendColoured(String text, @ColorInt int color) {
        if (mEnableColoring) {
            mFtb.appendColoured(text, color);
        } else {
            mFtb.appendRaw(text);
        }
    }

    /**
     * Add \n to {@link #mFtb}
     */
    private void newLine() {
        if (mSkipNextNewLine) {
            mSkipNextNewLine = false;
            return;
        }
        mFtb.appendRaw("\n");
    }


    private String getPadding() {
        return getPadding(0);
    }

    private String getPadding(int extraPadding) {
        final int spacesAmount = 2 * mDepth + extraPadding;
        if (spacesAmount < 0) {
            return ""; // Shouldn't happen
        }
        // http://stackoverflow.com/a/4903603
        return new String(new char[spacesAmount]).replace('\0', '\u00a0');
    }


    /**
     * Start the open tag
     * Use {@link #attr(String, String, boolean)} to add attributes
     */
    public void openTag(String name) {
        // Finish previous open tag
        finishTag(false);

        // New line and padding
        newLine();
        mFtb.appendRaw(getPadding());

        // Append text and color it
        appendColoured("<" + name, mTagColor);

        // Set flags for formatting next elements
        mHaveNotEndedOpenTag = true;
        mDepth++;
    }


    /**
     * Add attr from resource parser, resolving resources if needed
     */
    public void attrFromResourceParser(XmlResourceParser resParser, int attrIndex, Context xmlOwnerContext, boolean inline) {
        String name = resParser.getAttributeName(attrIndex);
        String value = resParser.getAttributeValue(attrIndex);

        // If string is empty or null we skip all following tests
        if ("".equals(value)) {
            attr(name, value, inline);
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
                                }, inline);
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
                                    }, inline);
                                    return;
                                } // Otherwise fall through
                            } catch (IOException ignored) {}
                        }

                        attr(name, resStringValue, inline);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Nothing special was recognized
        attr(name, value, inline);
    }

    /**
     * Add attr to last opened tag
     */
    public void attr(String name, String value, boolean inline) {
        attr(name, value, mEnableColoring ? new ForegroundColorSpan(mAttributeValueColor) : null, inline);
    }

    /**
     * Add attr to last opened tag, using specified span (eg. ClickableSpan) for value
     */
    private void attr(String name, String value, Object valueSpan, boolean inline) {
        if (BuildConfig.DEBUG && !mHaveNotEndedOpenTag) {
            throw new AssertionError("attr() not inside tag");
        }

        // Add padding
        if (inline) {
            mFtb.appendRaw(" ");
        } else {
            newLine();
            mFtb.appendRaw(getPadding(-1));
        }

        // Attribute name and value
        appendColoured(name + "=", mAttributeNameColor);
        String valueText = "\"" + value + "\"";
        if (valueSpan != null) {
            mFtb.appendSpan(valueText, valueSpan);
        } else {
            mFtb.appendRaw(valueText);
        }
    }

    /**
     * End the last open tag
     */
    private void finishTag(boolean selfClose) {
        if (mHaveNotEndedOpenTag) {
            appendColoured(selfClose ? " />" : ">", mTagColor);
            mHaveNotEndedOpenTag = false;
        }
    }

    /**
     * Add text in currently opened element
     */
    public void text(String s) {
        finishTag(false);
        mFtb.appendRaw("\n");
        mFtb.appendRaw(s);
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
            mFtb.appendRaw(getPadding());

            // Text and it's color
            appendColoured("</" + name + ">", mTagColor);
        }
    }

    /**
     * Write an exception description to show it to user
     */
    public void showException(Exception e) {
        String description = Utils.describeException(e);
        newLine();
        newLine();
        appendColoured("[" + description + "]", Color.RED);
    }

    /**
     * Create final text to be passed to
     * {@link com.github.michalbednarski.intentslab.TextFragment#publishText(Spannable)}
     *
     * Do not modify text after calling this method
     */
    public Spannable getText() {
        return mFtb.getText();
    }



    public XmlPreviewBuilder(Context myContext) {
        Resources myResources = myContext.getResources();
        mTagColor = myResources.getColor(R.color.xml_tag);
        mAttributeNameColor = myResources.getColor(R.color.xml_attr_name);
        mAttributeValueColor = myResources.getColor(R.color.xml_attr_value);
        mEnableColoring =
                PreferenceManager.getDefaultSharedPreferences(myContext)
                        .getBoolean("color-xml", true);
    }
}
