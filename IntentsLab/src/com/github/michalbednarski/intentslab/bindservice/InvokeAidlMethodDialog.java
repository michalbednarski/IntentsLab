package com.github.michalbednarski.intentslab.bindservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.bindservice.manager.BaseServiceFragment;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.clipboard.ClipboardService;
import com.github.michalbednarski.intentslab.runas.IRemoteInterface;
import com.github.michalbednarski.intentslab.runas.RunAsManager;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedObject;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.methodcall.ArgumentsEditorHelper;
import com.github.michalbednarski.intentslab.valueeditors.object.InlineValueEditorsLayout;

/**
 * Created by mb on 03.10.13.
 */
public class InvokeAidlMethodDialog extends BaseServiceFragment implements BindServiceManager.AidlReadyCallback {
    static final String ARG_METHOD_NUMBER = "method-number";
    private static final String STATE_METHOD_ARGUMENTS = "method-arguments";
    private static final String STATE_EDITOR_LAUNCHER_TAG = "launcher-tag";

    private IAidlInterface mAidlInterface;
    private int mMethodNumber;
    private SandboxedObject[] mMethodArgumentsToRestore;
    private EditorLauncher mEditorLauncher;

    private InlineValueEditorsLayout mEditorsLayout;
    private ArgumentsEditorHelper mArgumentsEditorHelper;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (mArgumentsEditorHelper != null) {
            mArgumentsEditorHelper.setEditorLauncher(mEditorLauncher);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        // Prepare editor launcher
        mEditorLauncher = new EditorLauncher(
                getActivity(),
                savedInstanceState != null ?
                        savedInstanceState.getString(STATE_EDITOR_LAUNCHER_TAG) :
                        null
        );
        mEditorLauncher.setRetainFragmentInstance(true);

        // Restore method arguments from state
        if (savedInstanceState != null) {
            mMethodArgumentsToRestore = (SandboxedObject[]) Utils.deepCastArray
                    (savedInstanceState.getParcelableArray(STATE_METHOD_ARGUMENTS),
                    SandboxedObject[].class
            );
        }

        // Read arguments and continue to preparing aidl
        Bundle args = getArguments();
        mMethodNumber = args.getInt(ARG_METHOD_NUMBER);
        getServiceHelper().prepareAidlAndRunWhenReady(getActivity(), this);
    }

    @Override
    public void onAidlReady(IAidlInterface anInterface) {
        mAidlInterface = anInterface;
        if (mAidlInterface == null || !mAidlInterface.asBinder().isBinderAlive()) {
            getActivity().finish();
            return;
        }

        // Prepare method info
        SandboxedMethod sandboxedMethod;
        try {
            sandboxedMethod = mAidlInterface.getMethods()[mMethodNumber];
        } catch (RemoteException e) {
            e.printStackTrace();
            getActivity().finish();
            return;
        }

        // Prepare arguments editor helper
        mArgumentsEditorHelper = new ArgumentsEditorHelper(sandboxedMethod, true);
        mArgumentsEditorHelper.setEditorLauncher(mEditorLauncher);

        // Restore arguments
        if (mMethodArgumentsToRestore != null) {
            mArgumentsEditorHelper.setSandboxedArguments(mMethodArgumentsToRestore);
            mMethodArgumentsToRestore = null;
        }

        // Show value editors if their layout was ready first
        if (mEditorsLayout != null) {
            mArgumentsEditorHelper.fillEditorsLayout(mEditorsLayout);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray(
                STATE_METHOD_ARGUMENTS,
                mMethodArgumentsToRestore != null ? mMethodArgumentsToRestore : // Not fully restored
                mArgumentsEditorHelper != null ? mArgumentsEditorHelper.getSandboxedArguments() : null
        );
        outState.putString(STATE_EDITOR_LAUNCHER_TAG, mEditorLauncher.getTag());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mEditorsLayout = new InlineValueEditorsLayout(getActivity());
        if (mArgumentsEditorHelper != null) {
            mArgumentsEditorHelper.fillEditorsLayout(mEditorsLayout);
        }
        return mEditorsLayout;
    }

    @Override
    public void onDestroyView() {
        mEditorsLayout = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.aidl_method, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.execute:
                invokeAidlMethod();
                return true;
        }
        return false;
    }

    private void invokeAidlMethod() {
        try {
            InvokeMethodResult result;
            final IRemoteInterface runAs = RunAsManager.getSelectedRemoteInterface();
            if (runAs != null) {
                result = mAidlInterface.invokeMethodUsingBinder(runAs.createOneShotProxyBinder(getServiceHelper().getBinderIfAvailable()), mMethodNumber, mArgumentsEditorHelper.getSandboxedArguments());
            } else {
                result = mAidlInterface.invokeMethod(mMethodNumber, mArgumentsEditorHelper.getSandboxedArguments());
            }
            if (result.exception == null) { // True if there weren't error
                if (!"null".equals(result.returnValueAsString)) {
                    ResultDialog resultDialog = new ResultDialog();
                    Bundle args = new Bundle();
                    args.putString(ResultDialog.ARG_RESULT_AS_STRING, result.returnValueAsString);
                    args.putParcelable(ResultDialog.ARG_RESULT, result.sandboxedReturnValue);
                    resultDialog.setArguments(args);
                    resultDialog.show(getFragmentManager(), "ResultOf" + getTag());
                } else {
                    Toast.makeText(getActivity(), result.returnValueAsString, Toast.LENGTH_LONG).show();
                }
                return;
            } else {
                Toast.makeText(getActivity(), result.exception, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            // Fall through
        }
        Toast.makeText(getActivity(), "Something went wrong...", Toast.LENGTH_SHORT).show(); // Should never happen
    }

    public static class ResultDialog extends DialogFragment {
        public static final String ARG_RESULT = "invokeAidl.ResultDialog.TheResult";
        public static final String ARG_RESULT_AS_STRING = "invokeAidl.ResultDialog.TheResultAsString";

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final SandboxedObject result = getArguments().getParcelable(ARG_RESULT);

            final String string = getArguments().getString(ARG_RESULT_AS_STRING);
            return new AlertDialog.Builder(getActivity())
                    .setMessage(string)
                    .setPositiveButton(getString(R.string.edit_or_add_to_clipboard), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ClipboardService.saveSandboxedObject(string, result);
                        }
                    })
                    .setNegativeButton(getString(R.string.dismiss), null)
                    .create();
        }
    }
}
