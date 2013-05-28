package com.example.testapp1.providerlab;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.testapp1.FormattedTextBuilder;
import com.example.testapp1.R;
import com.example.testapp1.browser.ComponentInfoActivity;

/**
 *
 */
public class ProviderInfoActivity extends Activity {
    String mPackageName, mComponentName;
    private ProviderInfo mProviderInfo = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_provider_info);

        mPackageName = getIntent().getStringExtra(ComponentInfoActivity.EXTRA_PACKAGE_NAME);
        mComponentName = getIntent().getStringExtra(ComponentInfoActivity.EXTRA_COMPONENT_NAME);

        // Fill mProviderInfo
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                fillProviderInfo();
            } else {
                fillProviderInfoLegacy();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.component_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PackageManager packageManager = getPackageManager();

        Toast.makeText(this, mProviderInfo.authority, Toast.LENGTH_LONG).show();

        // Header icon and text
        ((TextView) findViewById(R.id.title)).setText(
                mProviderInfo.loadLabel(packageManager)
        );
        ((TextView) findViewById(R.id.component)).setText(
                new ComponentName(mPackageName, mComponentName).flattenToShortString()
        );
        ((ImageView) findViewById(R.id.icon)).setImageDrawable(
                mProviderInfo.loadIcon(packageManager)
        );

        FormattedTextBuilder text = new FormattedTextBuilder();

        // Permissions
        // Description: permission/exported
        if (!mProviderInfo.exported) {
            text.appendHeader(getString(R.string.component_not_exported));
        } else {
            if (mProviderInfo.readPermission == null) {
                if (mProviderInfo.writePermission == null) {
                    text.appendHeader(getString(R.string.provider_rw_world_accessible));
                } else {
                    text.appendValue(getString(R.string.provider_w_only_permission), mProviderInfo.writePermission);
                }
            } else if (mProviderInfo.readPermission.equals(mProviderInfo.writePermission)) {
                text.appendValue(getString(R.string.provider_rw_permission), mProviderInfo.readPermission);
            } else {
                text.appendValue(getString(R.string.provider_r_permission), mProviderInfo.readPermission);
                if (mProviderInfo.writePermission == null) {
                    text.appendValueNoNewLine(getResources().getText(R.string.provider_no_w_permission), null);
                } else {
                    text.appendValueNoNewLine(getString(R.string.provider_w_permission), mProviderInfo.writePermission);
                }
            }
        }


        // <meta-data>
        text.appendFormattedText(ComponentInfoActivity.dumpMetaData(this, mPackageName, mProviderInfo.metaData));

        // Put text in TextView
        TextView textView = (TextView) findViewById(R.id.description);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(text.getText());
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void fillProviderInfo() throws PackageManager.NameNotFoundException {
        mProviderInfo = getPackageManager().getProviderInfo(new ComponentName(mPackageName, mComponentName),
                PackageManager.GET_DISABLED_COMPONENTS |
                PackageManager.GET_META_DATA
        );
    }
    private void fillProviderInfoLegacy() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = getPackageManager().getPackageInfo(mPackageName,
                PackageManager.GET_PROVIDERS |
                PackageManager.GET_DISABLED_COMPONENTS |
                PackageManager.GET_META_DATA
        );
        for (ProviderInfo provider : packageInfo.providers) {
            if (provider.name.equals(mComponentName)) {
                mProviderInfo = provider;
                return;
            }
        }
        throw new PackageManager.NameNotFoundException("No such provider (manual search in PackageInfo)");
    }
}