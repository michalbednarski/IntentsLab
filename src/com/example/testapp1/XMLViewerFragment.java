package com.example.testapp1;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;

public class XMLViewerFragment extends Fragment {
    public static final String ARG_PACKAGE_NAME = "packageName__Arg";
    public static final String ARG_RESOURCE_ID = "resIdArg";

    private View mLoaderView;
    private View mXmlWrapperView;
    private TextView mXmlTextView;
    private ReserializeXMLTask mTask = null;

    public static XMLViewerFragment create(String packageName, int resourceId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_PACKAGE_NAME, packageName);
        arguments.putInt(ARG_RESOURCE_ID, resourceId);
        final XMLViewerFragment fragment = new XMLViewerFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mTask != null) {
            mTask.cancel(true);
        }
        final View view = inflater.inflate(R.layout.xml_viewer, container, false);
        mLoaderView = view.findViewById(R.id.loader);
        mXmlWrapperView = view.findViewById(R.id.xml_wrapper);
        mXmlTextView = (TextView) view.findViewById(R.id.xml);
        mXmlTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mTask = new ReserializeXMLTask();
        mTask.execute();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }

    }

    private class ReserializeXMLTask extends AsyncTask<Object, Object, CharSequence> {
        private String mPackageName;
        private int mResourceId;

        @Override
        protected void onPreExecute() {
            final Bundle arguments = getArguments();
            mPackageName = arguments.getString(ARG_PACKAGE_NAME);
            mResourceId = arguments.getInt(ARG_RESOURCE_ID);
        }

        @Override
        protected CharSequence doInBackground(Object... args) {
            final Context context = getActivity();
            FormattedTextBuilder ftb = new FormattedTextBuilder();
            XmlPullParser parser;
            try {
                // Get resource xml parser
                Context scannedAppContext = context.createPackageContext(mPackageName, 0);
                if (mResourceId != 0) {
                    parser = scannedAppContext.getResources().getXml(mResourceId);
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
                    if (isCancelled()) {
                        return null;
                    }
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
                                                            new Intent(getActivity(), PermissionInfoActivity.class)
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
            mXmlTextView.setText(text);
            mLoaderView.setVisibility(View.GONE);
            mXmlWrapperView.setVisibility(View.VISIBLE);
            mTask = null;
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
    }
}
