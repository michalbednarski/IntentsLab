package com.github.michalbednarski.intentslab.bindservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethodArguments;
import com.github.michalbednarski.intentslab.valueeditors.InlineValueEditor;
import com.github.michalbednarski.intentslab.valueeditors.InlineValueEditorsLayout;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;

/**
 * Created by mb on 03.10.13.
 */
public class InvokeAidlMethodDialog extends DialogFragment implements EditorLauncher.EditorLauncherCallback {
    private static final String ARG_SERVICE = "bound-service-descriptor";
    private static final String ARG_METHOD_NUMBER = "method-number";
    private static final String STATE_METHOD_ARGUMENTS = "method-arguments";

    private IAidlInterface mAidlInterface;
    private final int mMethodNumber;
    private SandboxedMethodArguments mMethodArguments;
    private EditorLauncher mEditorLauncher;
    private InlineValueEditor[] mValueEditors;


    /**
     * Required by framework empty constructor
     */
    public InvokeAidlMethodDialog() {
        Bundle args = getArguments();
        try {
            mAidlInterface = BindServiceManager.getBoundService(args.<BindServiceDescriptor>getParcelable(ARG_SERVICE)).mAidlInterface;
        } catch (Exception e) {
            mAidlInterface = null;
        }
        mMethodNumber = args.getInt(ARG_METHOD_NUMBER);
    }

    public InvokeAidlMethodDialog(BindServiceManager.Helper serviceHelper, int methodNr) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_SERVICE, serviceHelper.mDescriptor);
        args.putInt(ARG_METHOD_NUMBER, methodNr);
        setArguments(args);

        mMethodNumber = methodNr;
        mAidlInterface = serviceHelper.mAidlInterface;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mEditorLauncher = new EditorLauncher(getActivity(), "");
        mEditorLauncher.setCallback(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mAidlInterface == null) {
            dismissAllowingStateLoss();
            return;
        }
        SandboxedMethod sandboxedMethod;
        try {
            sandboxedMethod = mAidlInterface.getMethods()[mMethodNumber];
        } catch (RemoteException e) {
            e.printStackTrace();
            dismissAllowingStateLoss();
            return;
        }
        final int argumentCount = sandboxedMethod.argumentTypes.length;
        if (savedInstanceState != null) {
            mMethodArguments = savedInstanceState.getParcelable(STATE_METHOD_ARGUMENTS);
            if (mMethodArguments.arguments.length != argumentCount) {
                Log.e("InvokeAidlMethodDialog", "Arguments count changed unexpectedly");
                dismissAllowingStateLoss();
                return;
            }
        } else {
            mMethodArguments = new SandboxedMethodArguments(argumentCount);
        }
        mValueEditors = new InlineValueEditor[argumentCount];
        for (int ii = 0; ii < argumentCount; ii++) {
            Class<?> type = sandboxedMethod.argumentTypes[ii];
            final int i = ii;
            mValueEditors[ii] = new InlineValueEditor(
                    type,
                    type.getName(),
                    new InlineValueEditor.ValueAccessors() {
                        @Override
                        public Object getValue() {
                            return mMethodArguments.arguments[i];
                        }

                        @Override
                        public void setValue(Object newValue) {
                            mMethodArguments.arguments[i] = newValue;
                        }

                        @Override
                        public void startEditor() {
                            mEditorLauncher.launchEditor("arg" + i, getValue());
                        }
                    }
            );
        }


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_METHOD_ARGUMENTS, mMethodArguments);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        InlineValueEditorsLayout editorsLayout = new InlineValueEditorsLayout(getActivity());
        editorsLayout.setValueEditors(mValueEditors);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(editorsLayout);
        builder.setPositiveButton("Invoke", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show(); // TODO
            }
        });
        return builder.create();
    }



    @Override
    public void onEditorResult(String key, Object newValue) {
        final int i = Integer.parseInt(key.substring(3));
        mMethodArguments.arguments[i] = newValue;
        mValueEditors[i].updateTextOnButton();
    }
}
