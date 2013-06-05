package com.example.testapp1.browser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.AuthorityEntry;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testapp1.CatchBroadcastService;
import com.example.testapp1.FormattedTextBuilder;
import com.example.testapp1.R;
import com.example.testapp1.XMLViewerActivity;
import com.example.testapp1.browser.ExtendedPackageInfo.ExtendedComponentInfo;
import com.example.testapp1.editor.IntentEditorActivity;
import com.example.testapp1.editor.IntentEditorConstants;

public class ComponentInfoActivity extends Activity {
    public static final String EXTRA_PACKAGE_NAME = "package";
    public static final String EXTRA_COMPONENT_NAME = "component";
    private static final String TAG = "ComponentInfoActivity";

    private String mPackageName;
    private String mComponentName;

    private ExtendedComponentInfo mExtendedComponentInfo;

    static CharSequence dumpIntentFilter(IntentFilter filter, Resources res) {
        FormattedTextBuilder ftb = new FormattedTextBuilder();
        int tagColor = res.getColor(R.color.xml_tag);
        int attributeNameColor = res.getColor(R.color.xml_attr_name);
        int attributeValueColor = res.getColor(R.color.xml_attr_value);

        ftb.appendColoured("\n<intent-filter>", tagColor);

        for (int i = 0, j = filter.countActions(); i < j; i++) {
            ftb.appendColoured("\n  <action", tagColor);
            ftb.appendColoured(" a:name=", attributeNameColor);
            ftb.appendColoured("\"" + filter.getAction(i) + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);
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
            ftb.appendColoured("\n  <data", tagColor);
            ftb.appendColoured(" a:mimeType=", attributeNameColor);
            ftb.appendColoured("\"" + filter.getDataType(i) + "\"", attributeValueColor);
            ftb.appendColoured(">", tagColor);
        }

        for (int i = 0, j = filter.countDataAuthorities(); i < j; i++) {
            AuthorityEntry authority = filter.getDataAuthority(i);
            ftb.appendColoured("\n  <data", tagColor);
            ftb.appendColoured(" a:host=", attributeNameColor);
            ftb.appendColoured("\"" + authority.getHost() + "\"", attributeValueColor);
            if (authority.getPort() != -1) {
                ftb.appendColoured(" a:port=", attributeNameColor);
                ftb.appendColoured("\"" + authority.getPort() + "\"", attributeValueColor);
            }
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
                                            new Intent(context, XMLViewerActivity.class)
                                                    .putExtra(XMLViewerActivity.EXTRA_PACKAGE_NAME, packageName)
                                                    .putExtra(XMLViewerActivity.EXTRA_RESOURCE_ID, resId)
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        mComponentName = intent.getStringExtra(EXTRA_COMPONENT_NAME);


        final ExtendedPackageInfo epi = new ExtendedPackageInfo(this, mPackageName, PackageManager.GET_META_DATA);

        epi.runWhenReady(new Runnable() {

            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                // Get loaded component info
                mExtendedComponentInfo = epi.getComponentInfo(mComponentName);
                if (mExtendedComponentInfo == null) {
                    Log.e(TAG, "component not found in manifest");
                    Log.e(TAG, "packageName=" + mPackageName);
                    Log.e(TAG, "componentName=" + mComponentName);
                    Toast.makeText(ComponentInfoActivity.this, R.string.component_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                PackageManager packageManager = getPackageManager();

                setContentView(R.layout.activity_component_info);

                // Header icon and component name
                ((TextView) findViewById(R.id.title)).setText(
                        mExtendedComponentInfo.systemComponentInfo.loadLabel(packageManager)
                );

                ((TextView) findViewById(R.id.component)).setText(
                        new ComponentName(mPackageName, mComponentName).flattenToShortString()
                );

                ((ImageView) findViewById(R.id.icon)).setImageDrawable(
                        mExtendedComponentInfo.systemComponentInfo.loadIcon(packageManager)
                );

                // Description text
                FormattedTextBuilder text = new FormattedTextBuilder();

                // Description: permission/exported
                if (!mExtendedComponentInfo.systemComponentInfo.exported) {
                    text.appendHeader(getString(R.string.component_not_exported));
                } else {
                    String permission = mExtendedComponentInfo.getPermission();
                    if (permission != null) {
                        text.appendValue(getString(R.string.permission_required_title), permission);
                    }
                }

                // Description: <intent-filter>'s
                if (mExtendedComponentInfo.intentFilters == null) {
                    text.appendHeader(getString(R.string.unknown_intent_filters));
                } else if (mExtendedComponentInfo.intentFilters.length == 0) {
                    text.appendHeader(getString(R.string.no_intent_filters));
                } else {
                    text.appendHeader(getString(R.string.intent_filters));
                    for (IntentFilter filter : mExtendedComponentInfo.intentFilters) {
                        text.appendFormattedText(dumpIntentFilter(filter, getResources()));
                    }
                }

                text.appendFormattedText(dumpMetaData(ComponentInfoActivity.this, mPackageName, mExtendedComponentInfo.systemComponentInfo.metaData));

                // Put text in TextView
                TextView textView = (TextView) findViewById(R.id.description);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
                textView.setText(text.getText());

                // Show or hide "Receive broadcast" button
                {
                    final boolean showReceiveBroadcast =
                            mExtendedComponentInfo.componentType == IntentEditorConstants.BROADCAST &&
                            mExtendedComponentInfo.intentFilters != null &&
                            mExtendedComponentInfo.intentFilters.length != 0;
                    View receiveBroadcastButton = findViewById(R.id.receive_broadcast);
                    receiveBroadcastButton.setVisibility(showReceiveBroadcast ? View.VISIBLE : View.GONE);
                    receiveBroadcastButton.setEnabled(showReceiveBroadcast);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.component_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_manifest:
                startActivity(
                        new Intent(this, XMLViewerActivity.class)
                                .putExtra(XMLViewerActivity.EXTRA_PACKAGE_NAME, mPackageName)
                );
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void goToIntentEditor(View view) {
        startActivity(
                new Intent(this, IntentEditorActivity.class)
                        .putExtra("intent", new Intent().setClassName(mPackageName, mComponentName))
                        .putExtra(IntentEditorActivity.EXTRA_COMPONENT_TYPE, mExtendedComponentInfo.componentType)
                        .putExtra(IntentEditorActivity.EXTRA_INTENT_FILTERS, mExtendedComponentInfo.intentFilters)
        );
    }

    public void receiveBroadcast(View view) {
        startService(
                new Intent(this, CatchBroadcastService.class)
                        .putExtra("intentFilters", mExtendedComponentInfo.intentFilters)
        );
    }
}
