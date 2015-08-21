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
import com.github.michalbednarski.intentslab.appinfo.MyComponentInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageInfo;
import com.github.michalbednarski.intentslab.appinfo.MyPackageManagerImpl;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;

/**
 * Fragment for displaying provider info
 *
 * Accepts same arguments as {@link com.github.michalbednarski.intentslab.browser.ComponentInfoFragment}
 */
public class ProviderInfoFragment extends Fragment {
    private static final String TAG = "ProviderInfoActivity";

    private String mPackageName, mComponentName;
    private MyComponentInfo mProviderInfo = null;

    private CharSequence mDescription;

    private TextView mTitleText;
    private TextView mComponentText;
    private ImageView mIconView;
    private TextView mDescriptionText;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPackageName = getArguments().getString(ComponentInfoFragment.ARG_PACKAGE_NAME);
        mComponentName = getArguments().getString(ComponentInfoFragment.ARG_COMPONENT_NAME);

        setHasOptionsMenu(true);

        MyPackageManagerImpl
                .getInstance(getActivity())
                .getPackageInfo(false, mPackageName)
                .then(new DoneCallback<MyPackageInfo>() {
                    @Override
                    public void onDone(MyPackageInfo result) {
                        mProviderInfo = result.getProviderByName(mComponentName);
                        if (mProviderInfo == null) {
                            onProviderMissing();
                            return;
                        }
                        buildDescriptionText();
                        fillView();
                    }
                })
                .fail(new FailCallback<Void>() {
                    @Override
                    public void onFail(Void result) {
                        onProviderMissing();
                    }
                });
    }

    private void buildDescriptionText() {
        ProviderInfo rawProviderInfo = mProviderInfo.getProviderInfo();

        // Start building description
        FormattedTextBuilder text = new FormattedTextBuilder();

        // Authority
        if (rawProviderInfo.authority != null) {
            text.appendValue("Authority", rawProviderInfo.authority);
        }

        // Permission/exported
        if (!mProviderInfo.isExported()) {
            text.appendHeader(getString(R.string.component_not_exported));
        } else {
            String readPermission = mProviderInfo.getPermission();
            String writePermission = mProviderInfo.getWritePermission();
            if (readPermission == null) {
                if (writePermission == null) {
                    text.appendHeader(getString(R.string.provider_rw_world_accessible));
                } else {
                    text.appendValue(getString(R.string.provider_w_only_permission), writePermission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
                }
            } else if (readPermission.equals(writePermission)) {
                text.appendValue(getString(R.string.provider_rw_permission), readPermission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
            } else {
                text.appendValue(getString(R.string.provider_r_permission), readPermission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
                if (writePermission == null) {
                    text.appendValuelessKeyContinuingGroup(getResources().getText(R.string.provider_no_w_permission));
                } else {
                    text.appendValue(getString(R.string.provider_w_permission), writePermission, false, FormattedTextBuilder.ValueSemantic.PERMISSION);
                }
            }
        }

        // Permission granting
        if (rawProviderInfo.grantUriPermissions) {
            if (rawProviderInfo.uriPermissionPatterns != null) {
                PatternMatcher[] uriPermissionPatterns = rawProviderInfo.uriPermissionPatterns;
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
        text.appendFormattedText(ComponentInfoFragment.dumpMetaData(getActivity(), mPackageName, rawProviderInfo.metaData));

        mDescription = text.getText();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_provider_info, container, false);

        mTitleText = ((TextView) view.findViewById(R.id.title));
        mComponentText = ((TextView) view.findViewById(R.id.component));
        mIconView = ((ImageView) view.findViewById(R.id.icon));

        mDescriptionText = (TextView) view.findViewById(R.id.description);
        mDescriptionText.setMovementMethod(LinkMovementMethod.getInstance());

        // Set button action
        view.findViewById(R.id.go_to_provider_lab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToProviderLab();
            }
        });

        // Fill view if contents are ready
        fillView();

        // Return view
        return view;
    }

    @Override
    public void onDestroyView() {
        mTitleText = null;
        mComponentText = null;
        mIconView = null;
        mDescriptionText = null;
        super.onDestroyView();
    }

    private void fillView() {
        // If we don't have information or views, don't fill
        if (mProviderInfo == null || mDescriptionText == null) {
            return;
        }

        PackageManager packageManager = getActivity().getPackageManager();

        // Header icon and text
        mTitleText.setText(
                mProviderInfo.getProviderInfo().loadLabel(packageManager) // TODO: loadLabel for MyComponentInfo
        );
        mComponentText.setText(
                new ComponentName(mPackageName, mComponentName).flattenToShortString()
        );
        mIconView.setImageDrawable(
                mProviderInfo.loadIcon(packageManager)
        );

        // Put description in TextView
        mDescriptionText.setText(mDescription);
    }

    private void onProviderMissing() {
        Toast.makeText(getActivity(), R.string.component_not_found, Toast.LENGTH_SHORT).show();
        //finish();
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
        if (mProviderInfo == null) {
            return;
        }
        String authority = mProviderInfo.getProviderInfo().authority;
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
