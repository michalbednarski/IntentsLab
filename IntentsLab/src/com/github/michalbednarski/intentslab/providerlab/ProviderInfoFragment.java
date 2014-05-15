package com.github.michalbednarski.intentslab.providerlab;

import android.annotation.TargetApi;
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
import android.os.PatternMatcher;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
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
import com.github.michalbednarski.intentslab.FormattedTextBuilder;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;

/**
 * Fragment for displaying provider info
 *
 * Accepts same arguments as {@link com.github.michalbednarski.intentslab.browser.ComponentInfoFragment}
 */
public class ProviderInfoFragment extends Fragment {
    private static final String TAG = "ProviderInfoActivity";

    String mPackageName, mComponentName;
    private ProviderInfo mProviderInfo = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_provider_info, container, false);

        mPackageName = getArguments().getString(ComponentInfoFragment.ARG_PACKAGE_NAME);
        mComponentName = getArguments().getString(ComponentInfoFragment.ARG_COMPONENT_NAME);

        // Fill mProviderInfo
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                fillProviderInfo();
            } else {
                fillProviderInfoLegacy();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.component_not_found, Toast.LENGTH_SHORT).show();
            //finish();
            return null;
        }

        PackageManager packageManager = getActivity().getPackageManager();

        // Header icon and text
        ((TextView) view.findViewById(R.id.title)).setText(
                mProviderInfo.loadLabel(packageManager)
        );
        ((TextView) view.findViewById(R.id.component)).setText(
                new ComponentName(mPackageName, mComponentName).flattenToShortString()
        );
        ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(
                mProviderInfo.loadIcon(packageManager)
        );

        // Start building description
        FormattedTextBuilder text = new FormattedTextBuilder();

        // Authority
        if (mProviderInfo.authority != null) {
            text.appendValue("Authority", mProviderInfo.authority);
        }

        // Permission/exported
        if (!mProviderInfo.exported) {
            text.appendHeader(getString(R.string.component_not_exported));
        } else {
            if (mProviderInfo.readPermission == null) {
                if (mProviderInfo.writePermission == null) {
                    text.appendHeader(getString(R.string.provider_rw_world_accessible));
                } else {
                    text.appendValue(getString(R.string.provider_w_only_permission), mProviderInfo.writePermission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
                }
            } else if (mProviderInfo.readPermission.equals(mProviderInfo.writePermission)) {
                text.appendValue(getString(R.string.provider_rw_permission), mProviderInfo.readPermission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
            } else {
                text.appendValue(getString(R.string.provider_r_permission), mProviderInfo.readPermission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
                if (mProviderInfo.writePermission == null) {
                    text.appendValuelessKeyContinuingGroup(getResources().getText(R.string.provider_no_w_permission));
                } else {
                    text.appendValue(getString(R.string.provider_w_permission), mProviderInfo.writePermission, false, FormattedTextBuilder.ValueSemantic.PERMISSION);
                }
            }
        }

        // Permission granting
        if (mProviderInfo.grantUriPermissions) {
            if (mProviderInfo.uriPermissionPatterns != null) {
                PatternMatcher[] uriPermissionPatterns = mProviderInfo.uriPermissionPatterns;
                String[] listItems = new String[uriPermissionPatterns.length];
                for (int i = 0; i < uriPermissionPatterns.length; i++) {
                    PatternMatcher pattern = uriPermissionPatterns[i];
                    listItems[i] = pattern.getPath() + (pattern.getType() == PatternMatcher.PATTERN_PREFIX ? "*" : "");
                }
                text.appendList(getString(R.string.provider_grant_uri_permission_for), listItems);
            } else {
                text.appendHeader(getString(R.string.provider_grant_uri_permission_for_all_paths));
            }
        }

        // <meta-data>
        text.appendFormattedText(ComponentInfoFragment.dumpMetaData(getActivity(), mPackageName, mProviderInfo.metaData));

        // Put description in TextView
        TextView textView = (TextView) view.findViewById(R.id.description);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(text.getText());

        // Set button action
        view.findViewById(R.id.go_to_provider_lab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToProviderLab();
            }
        });

        // Return view
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.component_info, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.package_info:
                if (getArguments().getBoolean(ComponentInfoFragment.ARG_LAUNCHED_FROM_APP_INFO, false)) {
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void fillProviderInfo() throws PackageManager.NameNotFoundException {
        mProviderInfo = getActivity().getPackageManager().getProviderInfo(
                new ComponentName(mPackageName, mComponentName),
                PackageManager.GET_DISABLED_COMPONENTS |
                        PackageManager.GET_META_DATA |
                        PackageManager.GET_URI_PERMISSION_PATTERNS
        );
    }

    private void fillProviderInfoLegacy() throws PackageManager.NameNotFoundException {
        PackageInfo packageInfo = getActivity().getPackageManager().getPackageInfo(
                mPackageName,
                PackageManager.GET_PROVIDERS |
                        PackageManager.GET_DISABLED_COMPONENTS |
                        PackageManager.GET_META_DATA |
                        PackageManager.GET_URI_PERMISSION_PATTERNS
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
    void openProviderLab(String authority) {
        startActivity(
                new Intent(getActivity(), AdvancedQueryActivity.class)
                .setData(Uri.parse("content://" + authority + "/"))
        );
    }

    /**
     * Open provider lab or prompt user to choose authority if there are multiple
     *
     * Triggered by provider lab button
     */
    void goToProviderLab() {
        String authority = mProviderInfo.authority;
        if (authority == null) {
            Log.e(TAG, "Missing authority");
            return;
        }
        final String[] authorities = authority.split(";");
        if (authorities.length == 1) {
            openProviderLab(authority);
        } else {
            new AlertDialog.Builder(getActivity())
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
