package com.example.testapp1.editor;

import android.content.Context;
import android.os.AsyncTask;
import com.example.testapp1.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
* Find extra names in res/xml/intent_extras.xml
*/
class NewExtraSuggesterByAction extends AsyncTask<Object, Object, Object> {
    private final Context mContext;
    private final String mExpectedStartTag; // "intent" or "reply-intent"
    private final String mExpectedAction;
    private final NewExtraPickerDialog mNewExtraDialog;

    private ArrayList<NewExtraPickerDialog.ExtraSuggestion> mExtraSuggestions = new ArrayList<NewExtraPickerDialog.ExtraSuggestion>();

    NewExtraSuggesterByAction(Context context, String intentAction, boolean isReplyIntent, NewExtraPickerDialog newExtraPickerDialog) {
        mContext = context;
        mExpectedAction = intentAction;
        mExpectedStartTag = isReplyIntent ? "reply-intent" : "intent";
        mNewExtraDialog = newExtraPickerDialog;
    }

    @Override
    protected Object doInBackground(Object... params) {
        XmlPullParser xml = mContext.getResources().getXml(R.xml.intent_extras);

        try {
            // Skip the root element (<intent-extras>)
            while (true) {
                if (xml.next() == XmlPullParser.START_TAG) break;
            }

            // Iterate over elements
            int token;
            while ((token = xml.next()) != XmlPullParser.END_DOCUMENT) {
                if (token == XmlPullParser.START_TAG) {
                    if (
                        mExpectedAction.equals(xml.getAttributeValue(null, "action")) &&
                        mExpectedStartTag.equals(xml.getName())
                    ) {
                        int depth = xml.getDepth();
                        do {
                            token = xml.next();
                            if (token == XmlPullParser.START_TAG) {
                                if (xml.getName().equals("extra")) {
                                    NewExtraPickerDialog.ExtraSuggestion extraSuggestion = new NewExtraPickerDialog.ExtraSuggestion();
                                    extraSuggestion.name = xml.getAttributeValue(null, "name");
                                    extraSuggestion.type = xml.getAttributeValue(null, "type");
                                    mExtraSuggestions.add(extraSuggestion);
                                }
                            }
                        } while (xml.getDepth() > depth);
                        // End scanning
                        return null;
                    } else { // Not element we were looking for
                        // Skip element
                        int depth = xml.getDepth();
                        do {
                            xml.next();
                        } while (xml.getDepth() > depth);
                    }
                }
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        mNewExtraDialog.onExtrasByActionReceived(mExtraSuggestions.toArray(new NewExtraPickerDialog.ExtraSuggestion[mExtraSuggestions.size()]));
    }
}
