package com.github.michalbednarski.intentslab.sandbox;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by mb on 01.10.13.
 */
public class SandboxInstallRequestDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage("Sandbox install needed") // TODO text+link
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }
}
