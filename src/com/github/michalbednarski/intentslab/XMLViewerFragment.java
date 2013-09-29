package com.github.michalbednarski.intentslab;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;

public class XMLViewerFragment extends Fragment {
    public static final String ARG_PACKAGE_NAME = "packageName__Arg";
    public static final String ARG_RESOURCE_ID = "resIdArg";

    private View mLoaderView;
    private View mXmlWrapperView;
    private TextView mXmlTextView;
    private ListView mFakeLongText;
    private ReserializeXMLTask mTask = null;
    private XmlPreviewBuilder mXmlPreviewBuilder = null;

    public static XMLViewerFragment create(String packageName, int resourceId) {
        Bundle arguments = new Bundle();
        arguments.putString(ARG_PACKAGE_NAME, packageName);
        arguments.putInt(ARG_RESOURCE_ID, resourceId);
        final XMLViewerFragment fragment = new XMLViewerFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mTask = new ReserializeXMLTask();
        mTask.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.xml_viewer, container, false);
        mLoaderView = view.findViewById(R.id.loader);
        mXmlWrapperView = view.findViewById(R.id.xml_wrapper);
        mXmlTextView = (TextView) view.findViewById(R.id.xml);
        mXmlTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mFakeLongText = (ListView) view.findViewById(R.id.xml_fake_long_view);
        publishTextIfReady();
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    private class ReserializeXMLTask extends AsyncTask<Object, Object, CharSequence[]> {
        private String mPackageName;
        private int mResourceId;

        private Context mApplicationContext;

        @Override
        protected void onPreExecute() {
            final Bundle arguments = getArguments();
            mPackageName = arguments.getString(ARG_PACKAGE_NAME);
            mResourceId = arguments.getInt(ARG_RESOURCE_ID);

            mXmlPreviewBuilder = new XmlPreviewBuilder(getActivity().getResources(), true);
            mApplicationContext = getActivity().getApplicationContext();
        }


        @Override
        protected CharSequence[] doInBackground(Object... args) {

            XmlResourceParser parser;
            try {
                // Get resource xml parser
                Context scannedAppContext = mApplicationContext.createPackageContext(mPackageName, 0);
                if (mResourceId != 0) {
                    parser = scannedAppContext.getResources().getXml(mResourceId);
                } else {
                    parser = scannedAppContext.getAssets().openXmlResourceParser("AndroidManifest.xml");
                }

                // Parse and reserialize xml
                int token;
                while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (isCancelled()) {
                        return null;
                    }
                    switch (token) {
                        case XmlPullParser.START_TAG:
                            mXmlPreviewBuilder.openTag(parser.getName());

                            // Attributes
                            for (int i = 0, attrCount = parser.getAttributeCount(); i < attrCount; i++) {
                                mXmlPreviewBuilder.attrFromResourceParser(parser, i, scannedAppContext);
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            mXmlPreviewBuilder.endTag(parser.getName());
                            break;

                        case XmlPullParser.TEXT:
                            mXmlPreviewBuilder.text(parser.getText());
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mXmlPreviewBuilder.showException(e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(CharSequence[] text) {
            mTask = null;
            publishTextIfReady();
        }
    }

    private void publishTextIfReady() {
        if (mTask != null || mLoaderView == null) {
            return;
        }
        if (mXmlPreviewBuilder.shouldUseFakeListText()) {
            mFakeLongText.setAdapter(mXmlPreviewBuilder.createFakeTextListAdapter(getActivity()));
            mFakeLongText.setVisibility(View.VISIBLE);
        } else {
            mXmlTextView.setText(mXmlPreviewBuilder.getText());
            mXmlWrapperView.setVisibility(View.VISIBLE);
        }

        mLoaderView.setVisibility(View.GONE);
    }
}
