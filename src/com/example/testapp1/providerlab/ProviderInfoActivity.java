package com.example.testapp1.providerlab;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
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
    private static final String TAG = "ProviderInfoActivity";

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

        // Authority
        text.appendValue("Authority", mProviderInfo.authority);

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

    /**
     * Open provider lab for trying given authority
     */
    public void openProviderLab(String authority) {
        startActivity(
                new Intent(this, AdvancedQueryActivity.class)
                .setData(Uri.parse("content://" + authority + "/"))
        );
    }

    /**
     * Open provider lab or prompt user to choose authority if there are multiple
     *
     * Triggered by provider lab button
     */
    public void goToProviderLab(View view) {
        String authority = mProviderInfo.authority;
        if (authority == null) {
            Log.e(TAG, "Missing authority");
            return;
        }
        final String[] authorities = authority.split(";");
        if (authorities.length == 1) {
            openProviderLab(authority);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Choose authority")
                    .setItems(authorities, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            openProviderLab(authorities[which]);
                        }
                    })
                    .show();
        }
    }
}