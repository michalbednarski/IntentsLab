package com.example.testapp1;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

public class XMLViewerFragment extends Fragment {
    public static final String ARG_PACKAGE_NAME = "packageName__Arg";
    public static final String ARG_RESOURCE_ID = "resIdArg";

    private View mLoaderView;
    private View mXmlWrapperView;
    private TextView mXmlTextView;
    private ListView mFakeLongText;
    private ReserializeXMLTask mTask = null;
    private CharSequence[] mTextChunks = null;

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
        if (mTextChunks != null) {
            publishTextChunks();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
    }

    private static final int LONG_TEXT_SPLIT_LINES = 50;

    private class ReserializeXMLTask extends AsyncTask<Object, Object, CharSequence[]> {
        private String mPackageName;
        private int mResourceId;
        private int mTagColor;
        private int mAttributeNameColor;
        private int mAttributeValueColor;

        @Override
        protected void onPreExecute() {
            final Bundle arguments = getArguments();
            mPackageName = arguments.getString(ARG_PACKAGE_NAME);
            mResourceId = arguments.getInt(ARG_RESOURCE_ID);

            // Get colors for formatting
            Resources myResources = getResources();
            mTagColor = myResources.getColor(R.color.xml_tag);
            mAttributeNameColor = myResources.getColor(R.color.xml_attr_name);
            mAttributeValueColor = myResources.getColor(R.color.xml_attr_value);
        }

        private int mChunkLinesLeft = LONG_TEXT_SPLIT_LINES;
        private FormattedTextBuilder mChunkTextBuilder = new FormattedTextBuilder();
        private ArrayList<CharSequence> mStagingTextChunks = new ArrayList<CharSequence>();

        /**
         * Append part of text, splitting it for list if necessary
         *
         * @param text Text to be appended
         * @param color Color of text or 0 for default
         */
        private void appendText(String text, int color) {
            int lastIndex = -1;
            while ((lastIndex = text.indexOf('\n', lastIndex + 1)) != -1) {
                if (--mChunkLinesLeft == 0) {
                    mChunkLinesLeft = LONG_TEXT_SPLIT_LINES;
                    if (color != 0) {
                        mChunkTextBuilder.appendColoured(text.substring(0, lastIndex), color);
                    } else {
                        mChunkTextBuilder.appendRaw(text.substring(0, lastIndex));
                    }
                    mStagingTextChunks.add(mChunkTextBuilder.getText());
                    mChunkTextBuilder = new FormattedTextBuilder();
                    text = text.substring(lastIndex + 1);
                    lastIndex = -1;
                }
            }

            if (color != 0) {
                mChunkTextBuilder.appendColoured(text, color);
            } else {
                mChunkTextBuilder.appendRaw(text);
            }
        }

        @Override
        protected CharSequence[] doInBackground(Object... args) {
            final Context context = getActivity().getApplicationContext();
            XmlPullParser parser;
            Exception exception = null;
            try {
                // Get resource xml parser
                Context scannedAppContext = context.createPackageContext(mPackageName, 0);
                if (mResourceId != 0) {
                    parser = scannedAppContext.getResources().getXml(mResourceId);
                } else {
                    parser = scannedAppContext.getAssets().openXmlResourceParser("AndroidManifest.xml");
                }

                // Parse and reserialize xml
                int token;
                boolean haveTagOpen = false;
                while ((token = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (isCancelled()) {
                        return null;
                    }
                    switch (token) {
                        case XmlPullParser.START_TAG:
                        {
                            appendText(
                                    // End previous tag "<"
                                    (haveTagOpen ? ">\n" : "") +
                                    // Start new open tag "<tag"
                                    getPadding(parser) +
                                    "<" + parser.getName(),
                                mTagColor);

                            // Attributes
                            int attrCount = parser.getAttributeCount();
                            String attrDelimiter = (attrCount < 2) ? " " : ("\n " + getPadding(parser));
                            int linesPerAttrDelimiter = (attrCount < 2) ? 0 : 1;
                            for (int i = 0, j = parser.getAttributeCount(); i < attrCount; i++) {
                                appendText(
                                        attrDelimiter +
                                                parser.getAttributeName(i) + "=",
                                        mAttributeNameColor);
                                appendText(
                                            "\"" + parser.getAttributeValue(i) + "\"",
                                            mAttributeValueColor);
                            }
                            haveTagOpen = true;
                            break;
                        }

                        case XmlPullParser.END_TAG:
                            appendText(
                                    // Close element, either full "</tag>" or just "/>" for unclosed tag
                                    (haveTagOpen ? "/>\n" : getPadding(parser) + "</" + parser.getName() + ">\n"),
                                    mTagColor);
                            haveTagOpen = false;
                            break;
                        case XmlPullParser.TEXT:
                            if (haveTagOpen) {
                                // End tag ">" if needed
                                appendText(">\n", mTagColor);
                                haveTagOpen = false;
                            }
                            // Show indented text value
                            appendText(parser.getText(), Color.BLACK);
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                exception = e;
            }

            // End text chunk if started
            if (mChunkTextBuilder.getText().length() != 0) {
                mStagingTextChunks.add(mChunkTextBuilder.getText());
            }

            // Show visible exception if something went wrong
            if (exception != null) {
                String exceptionDescription = Utils.describeException(exception);
                final SpannableStringBuilder newFirstChunk = new SpannableStringBuilder(exceptionDescription);
                newFirstChunk.setSpan(new ForegroundColorSpan(Color.RED), 0, exceptionDescription.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (mStagingTextChunks.size() != 0) {
                    newFirstChunk.append("\n\n").append(mStagingTextChunks.get(0));
                    mStagingTextChunks.set(0, newFirstChunk);
                } else {
                    mStagingTextChunks.add(newFirstChunk);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(CharSequence[] text) {
            mTextChunks = mStagingTextChunks.toArray(new CharSequence[mStagingTextChunks.size()]);
            publishTextChunks();
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

    private void publishTextChunks() {
        if (mTextChunks.length <= 1) {
            mXmlTextView.setText(mTextChunks[0]);
            mXmlWrapperView.setVisibility(View.VISIBLE);
        } else {
            mFakeLongText.setAdapter(new BaseAdapter() {
                @Override
                public int getCount() {
                    return mTextChunks.length;
                }

                @Override
                public Object getItem(int position) {
                    return null;
                }

                @Override
                public long getItemId(int position) {
                    return position;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView textView;
                    if (convertView == null) {
                        textView = new TextView(getActivity());
                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                    } else {
                        textView = (TextView) convertView;
                    }
                    textView.setText(mTextChunks[position]);
                    return textView;
                }
            });
            mFakeLongText.setVisibility(View.VISIBLE);
        }

        mLoaderView.setVisibility(View.GONE);

    }
}
