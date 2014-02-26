package com.github.michalbednarski.intentslab;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;


public class ReceiveBroadcastDialog extends DialogFragment implements OnClickListener, OnCancelListener {

    private AutoCompleteTextView mActionTextView;
    private CheckBox mMultipleCheckBox;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_receive_broadcast, null);
        mActionTextView = (AutoCompleteTextView) view.findViewById(R.id.action);
        mMultipleCheckBox = (CheckBox) view.findViewById(R.id.receive_multiple_broadcasts);
        mActionTextView.setText(PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString("lastcatchbroadcastaction", "")
        );
        mActionTextView.setAdapter(new NameAutocompleteAdapter(context, R.raw.broadcast_actions));
        builder.setView(view);
        builder.setTitle(R.string.receive_broadcast);
        builder.setPositiveButton(R.string.register_receiver, this);
        if (ReceiveBroadcastService.sIsRunning) {
            builder.setNegativeButton(R.string.unregister_receiver, this);
        }
        builder.setOnCancelListener(this);
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Note: this is not DRY, but we cannot use onDismiss because we support older Android versions
        FragmentActivity activity = getActivity();
        if (activity instanceof WrapperActivity) {
            activity.finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_NEGATIVE) {
            stopReceiving();
        } else {
            startReceiving();
        }
        FragmentActivity activity = getActivity();
        if (activity instanceof WrapperActivity) {
            activity.finish();
        }
    }

    void startReceiving() {
        String action = mActionTextView.getText().toString();
        Utils.applyOrCommitPrefs(
                PreferenceManager
                        .getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString("lastcatchbroadcastaction", action)
        );
        ReceiveBroadcastService.startReceiving(getActivity(), action, mMultipleCheckBox.isChecked());
    }

    void stopReceiving() {
        getActivity().stopService(new Intent(getActivity(), ReceiveBroadcastService.class));
    }

    public static class WrapperActivity extends FragmentActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            new ReceiveBroadcastDialog()
                    .show(getSupportFragmentManager(), "dialogInWrapperActivity");
        }
    }


}
