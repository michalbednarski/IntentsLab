package com.github.michalbednarski.intentslab.browser;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.AppInfoActivity;
import com.github.michalbednarski.intentslab.FormattedTextBuilder;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.ReceiveBroadcastService;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.editor.IntentEditorConstants;

/**
 * Fragment used for displaying component (activity, broadcast or service) info.
 *
 * For content providers use {@link com.github.michalbednarski.intentslab.providerlab.ProviderInfoFragment}
 *
 * Can be used in {@link com.github.michalbednarski.intentslab.SingleFragmentActivity}
 */
public class RegisteredReceiverInfoFragment extends Fragment {
    public static final String ARG_REGISTERED_RECEIVER = "RegisteredReceiverInfoFragment.data";

    private static final String TAG = "RegisteredReceiverInfoFragment";


    // Contents
    private RegisteredReceiverInfo mReceiverInfo;
    private String mPackageName;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        mReceiverInfo = getArguments().getParcelable(ARG_REGISTERED_RECEIVER);

        PackageManager packageManager = getActivity().getPackageManager();
        try {
            packageManager.getPackageInfo(mReceiverInfo.processName, 0);
            mPackageName = mReceiverInfo.processName;
        } catch (PackageManager.NameNotFoundException e) {
            String[] packagesForUid = packageManager.getPackagesForUid(mReceiverInfo.uid);
            if (packagesForUid.length == 0) {
                mPackageName = null;
            } else {
                mPackageName = packagesForUid[0];
            }
        }


    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_component_info, container, false);

        // Find views: header, icon, component name and description
        TextView titleTextView = (TextView) v.findViewById(R.id.title);
        TextView componentTextView = (TextView) v.findViewById(R.id.component);
        ImageView iconView = (ImageView) v.findViewById(R.id.icon);
        TextView descriptionTextView = (TextView) v.findViewById(R.id.description);
        descriptionTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // Fill header
        titleTextView.setText(R.string.registered_receiver);
        componentTextView.setText(mReceiverInfo.processName);
        /*mIconView.setImageDrawable(
                mExtendedComponentInfo.systemComponentInfo.loadIcon(getActivity().getPackageManager())
        );*/
        iconView.setImageDrawable(null); // TODO


        // Description text
        FormattedTextBuilder text = new FormattedTextBuilder();


        // Description: permission
        boolean hasOverallPermission = false;
        String permission = null;
        try {
            permission = mReceiverInfo.getOverallPermission();
            hasOverallPermission = true;
        } catch (RegisteredReceiverInfo.MixedPermissionsException ignored) {}
        if (permission != null) {
            text.appendValue(getString(R.string.permission_required_title), permission, true, FormattedTextBuilder.ValueSemantic.PERMISSION);
        }

        // Description: <intent-filter>'s
        text.appendHeader(getString(R.string.intent_filters));
        IntentFilter[] intentFilters = mReceiverInfo.intentFilters;
        for (int i = 0, j = intentFilters.length; i < j; i++) {
            IntentFilter filter = intentFilters[i];
            boolean hasSpecificPermission = !hasOverallPermission && mReceiverInfo.filterPermissions[i] != null;
            if (hasSpecificPermission) {
                text.appendColoured( // TODO: link
                        "\n\n<!-- " + getString(R.string.permission_required_title) + ": " + mReceiverInfo.filterPermissions[i] + " -->",
                        getResources().getColor(R.color.xml_comment)
                );
            }
            text.appendFormattedText(ComponentInfoFragment.dumpIntentFilter(filter, getResources(), true));
            if (hasSpecificPermission) {
                text.appendRaw("\n");
            }
        }

        // Put text in TextView
        descriptionTextView.setText(text.getText());

        // Go to intent editor button
        v.findViewById(R.id.go_to_intent_editor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getArguments().getBoolean(ComponentInfoFragment.ARG_LAUNCHED_FROM_INTENT_EDITOR, false)) {
                    getActivity().finish();
                    return;
                }
                startActivity(
                        new Intent(getActivity(), IntentEditorActivity.class)
                                .putExtra("intent", new Intent())
                                .putExtra(IntentEditorActivity.EXTRA_COMPONENT_TYPE, IntentEditorConstants.BROADCAST)
                                .putExtra(IntentEditorActivity.EXTRA_INTENT_FILTERS, mReceiverInfo.intentFilters)
                );
            }
        });

        // Receive broadcast button
        View receiveBroadcastButton = v.findViewById(R.id.receive_broadcast);
        receiveBroadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReceiveBroadcastService.startReceiving(getActivity(), mReceiverInfo.intentFilters, false);
            }
        });
        receiveBroadcastButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ReceiveBroadcastService.startReceiving(getActivity(), mReceiverInfo.intentFilters, true);
                return true;
            }
        });
        receiveBroadcastButton.setVisibility(View.VISIBLE);
        receiveBroadcastButton.setEnabled(true);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.component_info, menu);
        MenuItem packageInfoOption = menu.findItem(R.id.package_info);
        boolean packageInfoAvailable = mPackageName != null;
        packageInfoOption.setVisible(packageInfoAvailable);
        packageInfoOption.setEnabled(packageInfoAvailable);
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
}
