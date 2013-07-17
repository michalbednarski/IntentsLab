package com.example.testapp1;

import android.content.Intent;
import android.text.method.LinkMovementMethod;
import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 *
 */
public class XMLViewerActivity extends Activity {
    public static final String EXTRA_PACKAGE_NAME = "packageName__";
    public static final String EXTRA_RESOURCE_ID = "resId";

    private String packageName;
    private int resourceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        resourceId = getIntent().getIntExtra(EXTRA_RESOURCE_ID, 0);
        (new ReserializeXMLTask()).execute();
    }

    private String getPadding(XmlPullParser parser) {
        // http://stackoverflow.com/a/4903603
        try {
            return new String(new char[parser.getDepth() - 1]).replace("\0", "  ");
        } catch (NegativeArraySizeException e) { // Should not happen
            e.printStackTrace();
            return "";
        }
    }

    private class ReserializeXMLTask extends AsyncTask<Object, Object, CharSequence> {


        @Override
        protected CharSequence doInBackground(Object... args) {
            FormattedTextBuilder ftb = new FormattedTextBuilder();
            XmlPullParser parser;
            try {
                // Get resource xml parser
                Context scannedAppContext = createPackageContext(packageName, 0);
                if (resourceId != 0) {
                    parser = scannedAppContext.getResources().getXml(resourceId);
                } else {
                    parser = scannedAppContext.getAssets().openXmlResourceParser("AndroidManifest.xml");
                }

                // Get colors for formatting
                Resources myResources = getResources();
                int tagColor = myResources.getColor(R.color.xml_tag);
                int attributeNameColor = myResources.getColor(R.color.xml_attr_name);
                int attributeValueColor = myResources.getColor(R.color.xml_attr_value);

                // Parse and reserialize xml
                int token;
                boolean haveTagOpen = false;
                while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    switch (token) {
                        case XmlPullParser.START_TAG:
                            ftb.appendColoured(
                                    (haveTagOpen ? ">\n" : "") +
                                            getPadding(parser) +
                                            "<" + parser.getName(), tagColor);
                        {
                            int attrCount = parser.getAttributeCount();
                            String attrDelimiter = (attrCount < 2) ? " " : ("\n " + getPadding(parser));
                            for (int i = 0, j = parser.getAttributeCount(); i < attrCount; i++) {
                                ftb.appendColoured(
                                        attrDelimiter +
                                                parser.getAttributeName(i) + "=",
                                        attributeNameColor);
                                if (("uses-permission".equals(parser.getName()) || "permission".equals(parser.getName())) && "name".equals(parser.getAttributeName(i))) {
                                    final String permissionName = parser.getAttributeValue(i);
                                    ftb.appendColouredAndLinked(
                                            "\"" + parser.getAttributeValue(i) + "\"",
                                            attributeValueColor,
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    startActivity(
                                                            new Intent(XMLViewerActivity.this, PermissionInfoActivity.class)
                                                            .putExtra(PermissionInfoActivity.EXTRA_PERMISSION_NAME, permissionName)
                                                    );
                                                }
                                            }
                                    );
                                } else {
                                    ftb.appendColoured(
                                            "\"" + parser.getAttributeValue(i) + "\"",
                                            attributeValueColor);
                                }
                            }
                        }
                        haveTagOpen = true;
                        break;
                        case XmlPullParser.END_TAG:
                            ftb.appendColoured(
                                    (haveTagOpen ? "/>\n" : getPadding(parser) + "</" + parser.getName() + ">\n"),
                                    tagColor);
                            haveTagOpen = false;
                            break;
                        case XmlPullParser.TEXT:
                            if (haveTagOpen) {
                                ftb.appendColoured(">\n", tagColor);
                                haveTagOpen = false;
                            }
                            ftb.appendText(parser.getText());
                            break;
                    }
                }
                return ftb.getText();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(CharSequence text) {
            TextView textView = new TextView(XMLViewerActivity.this);
            ScrollView scrollView = new ScrollView(XMLViewerActivity.this);
            textView.setText(text);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            scrollView.addView(textView);
            setContentView(scrollView);
        }
    }
}
