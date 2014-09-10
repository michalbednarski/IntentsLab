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

package com.github.michalbednarski.intentslab.browser;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.AppInfoActivity;
import com.github.michalbednarski.intentslab.ReceiveBroadcastService;
import com.github.michalbednarski.intentslab.FormattedTextBuilder;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.XmlViewerFragment;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.editor.IntentEditorConstants;

/**
 * Fragment used for displaying component (activity, broadcast or service) info.
 *
 * For content providers use {@link com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment}
 *
 * Can be used in {@link com.github.michalbednarski.intentslab.SingleFragmentActivity}
 */
public class ComponentInfoFragment extends Fragment {
    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_COMPONENT_NAME = "component";

    /**
     * If this extra is true, "Go to intent editor" button will just finish activity
     */
    public static final String ARG_LAUNCHED_FROM_INTENT_EDITOR = "componentInfo.launchedFromIntentEditor";

    /**
     * If this extra is true, "App info" option will just finish activity
     */
    public static final String ARG_LAUNCHED_FROM_APP_INFO = "componentInfo.launchedFromAppInfo";

    private static final String TAG = "ComponentInfoFragment";

    private String mPackageName;
    private String mComponentName;

    private ExtendedPackageInfo.ExtendedComponentInfo mExtendedComponentInfo;


    // Views
    private TextView mTitleTextView;
    private TextView mComponentTextView;
    private ImageView mIconView;
    private TextView mDescriptionTextView;
    private View mReceiveBroadcastButton;

    // Contents
    private CharSequence mTitleText;
    private CharSequence mDescription;
    private boolean mShowReceiveBroadcast;

    static CharSequence dumpIntentFilter(IntentFilter filter, Resources res, boolean isBroadcast) {
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        int tagColor = res.getColor(R.color.xml_tag);
        int attributeNameColor = res.getColor(R.color.xml_attr_name);
        int attributeValueColor = res.getColor(R.color.xml_attr_value);
        int commentColor = res.getColor(R.color.xml_comment);
        final String protectedComment = " <!-- " + res.getString(R.string.broadcast_action_protected_comment) + " -->";

        ftb.appendColoured("\n<intent-filter>", tagColor);

        for (int i = 0, j = filter.countActions(); i < j; i++) {
            final String action = filter.getAction(i);
            ftb.appendColoured("\n  <action", tagColor);
            ftb.appendColoured(" a:name=", attributeNameColor);
            ftb.appendColoured("\"" + action + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);

            if (isBroadcast && Utils.isProtectedBroadcast(action)) {
                ftb.appendColoured(protectedComment, commentColor);
            }
        }

        for (int i = 0, j = filter.countCategories(); i < j; i++) {
            ftb.appendColoured("\n  <category", tagColor);
            ftb.appendColoured(" a:name=", attributeNameColor);
            ftb.appendColoured("\"" + filter.getCategory(i) + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);
        }

        for (int i = 0, j = filter.countDataSchemes(); i < j; i++) {
            ftb.appendColoured("\n  <data", tagColor);
            ftb.appendColoured(" a:scheme=", attributeNameColor);
            ftb.appendColoured("\"" + filter.getDataScheme(i) + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);
        }

        for (int i = 0, j = filter.countDataAuthorities(); i < j; i++) {
            IntentFilter.AuthorityEntry authority = filter.getDataAuthority(i);
            ftb.appendColoured("\n  <data", tagColor);
            ftb.appendColoured(" a:host=", attributeNameColor);
            ftb.appendColoured("\"" + authority.getHost() + "\"", attributeValueColor);
            if (authority.getPort() != -1) {
                ftb.appendColoured(" a:port=", attributeNameColor);
                ftb.appendColoured("\"" + authority.getPort() + "\"", attributeValueColor);
            }
            ftb.appendColoured(">", tagColor);
        }

        for (int i = 0, j = filter.countDataPaths(); i < j; i++) {
            PatternMatcher pathMatcher = filter.getDataPath(i);
            int type = pathMatcher.getType();
            ftb.appendColoured("\n  <data", tagColor);
            ftb.appendColoured(" a:path" + (
                    type == PatternMatcher.PATTERN_LITERAL ? "" :
                            type == PatternMatcher.PATTERN_PREFIX ? "Prefix" :
                                    type == PatternMatcher.PATTERN_SIMPLE_GLOB ? "Pattern" : "[unknown]"
            ) + "=", attributeNameColor);
            ftb.appendColoured("\"" + pathMatcher.getPath() + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);
        }

        for (int i = 0, j = filter.countDataTypes(); i < j; i++) {
            String dataType = filter.getDataType(i);
            if (!dataType.contains("/")) {
                // IntentFilter for partial types don't store "/*" at end
                // e.g. "image" instead of "image/*".
                // We display it in full form here
                dataType += "/*";
            }
            ftb.appendColoured("\n  <data", tagColor);
            ftb.appendColoured(" a:mimeType=", attributeNameColor);
            ftb.appendColoured("\"" + dataType + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);
        }

        ftb.appendColoured("\n</intent-filter>", tagColor);
        return ftb.getText();
    }

    public static CharSequence dumpMetaData(final Context context, final String packageName, Bundle metaData) {
        FormattedTextBuilder text = new FormattedTextBuilder();
        Context foreignContext = null;
        try {
            foreignContext = context.createPackageContext(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (metaData != null && !metaData.isEmpty()) {
            text.appendHeader(context.getString(R.string.metadata_header));
            for (String key : metaData.keySet()) {
                Object value = metaData.get(key);
                if (value instanceof Integer) {
                    // TODO resource?
                    if (foreignContext != null) {
                        text.appendValueNoNewLine(key, value.toString());
                        // Integers can point to resources
                        final int resId = (Integer) value;
                        if (resId == 0) {
                            continue;
                        }

                        // [View as XML] link
                        try {
                            foreignContext.getResources().getXml(resId);
                            text.appendClickable(context.getString(R.string.view_as_xml_resource), new ClickableSpan() {
                                @Override
                                public void onClick(View widget) {
                                    context.startActivity(
                                            new Intent(context, SingleFragmentActivity.class)
                                                    .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, XmlViewerFragment.class.getName())
                                                    .putExtra(XmlViewerFragment.ARG_PACKAGE_NAME, packageName)
                                                    .putExtra(XmlViewerFragment.ARG_RESOURCE_ID, resId)
                                    );
                                }
                            });
                        } catch (Resources.NotFoundException ignored) {
                        }
                    }
                    // Other types
                } else if (value instanceof Float) {
                    text.appendValueNoNewLine(key, value.toString() + (((Float) value) % 1f == 0f ? "" : " (float)"));
                } else if (value instanceof Boolean) {
                    text.appendValueNoNewLine(key, value.toString());
                } else if (value instanceof String) {
                    text.appendValueNoNewLine(key, "\"" + value + "\"");
                }
            }
        }
        return text.getText();
    }

    @SuppressLint("InlinedApi")
    public static boolean isComponentDisabledState(int state, boolean defaultEnabled) {
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && !defaultEnabled);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle intent = getArguments();
        mPackageName = intent.getString(ARG_PACKAGE_NAME);
        mComponentName = intent.getString(ARG_COMPONENT_NAME);


        ExtendedPackageInfo.getExtendedPackageInfo(getActivity(), mPackageName, new ExtendedPackageInfo.Callback() {
            @Override
            public void onPackageInfoAvailable(ExtendedPackageInfo epi) {
                // Get loaded component info
                mExtendedComponentInfo = epi.getComponentInfo(mComponentName);
                if (mExtendedComponentInfo == null) {
                    Log.e(TAG, "component not found in manifest");
                    Log.e(TAG, "packageName=" + mPackageName);
                    Log.e(TAG, "componentName=" + mComponentName);
                    Toast.makeText(getActivity(), R.string.component_not_found, Toast.LENGTH_SHORT).show();
                    //finish(); // TODO: Show message directly in fragment instead of Toast if in tablet view
                    return;
                }

                PackageManager packageManager = getActivity().getPackageManager();

                // Header icon and component name
                mTitleText = mExtendedComponentInfo.systemComponentInfo.loadLabel(packageManager);



                // Description text
                FormattedTextBuilder text = new FormattedTextBuilder();

                // Description: disabled
                if (isComponentDisabledState(
                        packageManager.getComponentEnabledSetting(new ComponentName(mPackageName, mComponentName)),
                        mExtendedComponentInfo.systemComponentInfo.enabled
                        )) {
                    text.appendHeader(getString(R.string.component_disabled));
                } else if (!mExtendedComponentInfo.systemComponentInfo.applicationInfo.enabled) {
                    text.appendHeader(getString(R.string.component_in_disabled_application));
                }

                // Description: permission/exported
                if (!mExtendedComponentInfo.systemComponentInfo.exported) {
                    text.appendHeader(getString(R.string.component_not_exported));
                } else {
                    String permission = mExtendedComponentInfo.getPermission();
                    if (permission != null) {
                        text.appendValue(getString(R.string.permission_required_title), permission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
                    }
                }

                // Description: <intent-filter>'s
                final boolean isBroadcast = mExtendedComponentInfo.componentType == IntentEditorConstants.BROADCAST;
                if (mExtendedComponentInfo.intentFilters == null) {
                    text.appendHeader(getString(R.string.unknown_intent_filters));
                } else if (mExtendedComponentInfo.intentFilters.length == 0) {
                    text.appendHeader(getString(R.string.no_intent_filters));
                } else {
                    text.appendHeader(getString(R.string.intent_filters));
                    for (IntentFilter filter : mExtendedComponentInfo.intentFilters) {
                        text.appendFormattedText(dumpIntentFilter(filter, getResources(), isBroadcast));
                    }
                }

                text.appendFormattedText(dumpMetaData(getActivity(), mPackageName, mExtendedComponentInfo.systemComponentInfo.metaData));

                // Put text in TextView
                mDescription = text.getText();


                // Show or hide "Receive broadcast" button
                mShowReceiveBroadcast = isBroadcast &&
                        mExtendedComponentInfo.intentFilters != null &&
                        mExtendedComponentInfo.intentFilters.length != 0;

                if (haveView()) {
                    fillViews();
                }
            }
        });
    }

    private boolean haveView() {
        return mTitleTextView != null;
    }

    private boolean haveContentLoaded() {
        return mDescription != null;
    }

    private void fillViews() {
        assert haveView() && haveContentLoaded();

        mTitleTextView.setText(mTitleText);

        mComponentTextView.setText(
                new ComponentName(mPackageName, mComponentName).flattenToShortString()
        );

        mIconView.setImageDrawable(
                mExtendedComponentInfo.systemComponentInfo.loadIcon(getActivity().getPackageManager())
        );

        mDescriptionTextView.setText(mDescription);

        mReceiveBroadcastButton.setVisibility(mShowReceiveBroadcast ? View.VISIBLE : View.GONE);
        mReceiveBroadcastButton.setEnabled(mShowReceiveBroadcast);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.activity_component_info, container, false);

        // Header, icon, component name and description
        mTitleTextView = (TextView) v.findViewById(R.id.title);
        mComponentTextView = (TextView) v.findViewById(R.id.component);
        mIconView = (ImageView) v.findViewById(R.id.icon);
        mDescriptionTextView = (TextView) v.findViewById(R.id.description);
        mDescriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // Go to intent editor button
        v.findViewById(R.id.go_to_intent_editor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getArguments().getBoolean(ARG_LAUNCHED_FROM_INTENT_EDITOR, false)) {
                    getActivity().finish();
                    return;
                }
                startActivity(
                        new Intent(getActivity(), IntentEditorActivity.class)
                                .putExtra(IntentEditorActivity.EXTRA_INTENT, new Intent().setClassName(mPackageName, mComponentName))
                                .putExtra(IntentEditorActivity.EXTRA_COMPONENT_TYPE, mExtendedComponentInfo.componentType)
                                .putExtra(IntentEditorActivity.EXTRA_INTENT_FILTERS, mExtendedComponentInfo.intentFilters)
                );
            }
        });

        // Receive broadcast button
        mReceiveBroadcastButton = v.findViewById(R.id.receive_broadcast);
        mReceiveBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReceiveBroadcastService.startReceiving(getActivity(), mExtendedComponentInfo.intentFilters, false);
            }
        });
        mReceiveBroadcastButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ReceiveBroadcastService.startReceiving(getActivity(), mExtendedComponentInfo.intentFilters, true);
                return true;
            }
        });

        // Fill it if have content
        if (haveContentLoaded()) {
            fillViews();
        }

        return v;
    }

    @Override
    public void onDestroyView() {
        mTitleTextView = null;
        mComponentTextView = null;
        mIconView = null;
        mDescriptionTextView = null;
        mReceiveBroadcastButton = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.component_info, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.package_info:
                if (getArguments().getBoolean(ARG_LAUNCHED_FROM_APP_INFO, false)) {
                    getActivity().finish();
                } else {
                    startActivity(
                            new Intent(getActivity(), AppInfoActivity.class)
                                    .putExtra(AppInfoActivity.EXTRA_PACKAGE_NAME, mPackageName)
                    );
                }
                return true;
        }
        return false;
    }
}
