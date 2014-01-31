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
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.bindservice.manager.ServiceDescriptor;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.InvokeMethodResult;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethodArguments;
import com.github.michalbednarski.intentslab.sandbox.SandboxedType;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.object.InlineValueEditor;
import com.github.michalbednarski.intentslab.valueeditors.object.InlineValueEditorsLayout;

/**
 * Created by mb on 03.10.13.
 */
public class InvokeAidlMethodDialog extends DialogFragment implements EditorLauncher.EditorLauncherCallback {
    private static final String ARG_SERVICE = "bound-service-descriptor";
    private static final String ARG_METHOD_NUMBER = "method-number";
    private static final String STATE_METHOD_ARGUMENTS = "method-arguments";

    private IAidlInterface mAidlInterface;
    private int mMethodNumber = -9999;
    private SandboxedMethodArguments mMethodArguments;
    private EditorLauncher mEditorLauncher;
    private InlineValueEditor[] mValueEditors;


    /**
     * Required by framework empty constructor
     */
    public InvokeAidlMethodDialog() {}

    public InvokeAidlMethodDialog(BindServiceManager.Helper serviceHelper, int methodNr) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_SERVICE, serviceHelper.getDescriptor());
        args.putInt(ARG_METHOD_NUMBER, methodNr);
        setArguments(args);

        mMethodNumber = methodNr;
        mAidlInterface = serviceHelper.getAidlIfAvailable();
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
        if (mAidlInterface == null && mMethodNumber == -9999) {
            Bundle args = getArguments();
            try {
                mAidlInterface = BindServiceManager.getBoundService(args.<ServiceDescriptor>getParcelable(ARG_SERVICE)).getAidlIfAvailable();
            } catch (Exception e) {
                mAidlInterface = null;
            }
            mMethodNumber = args.getInt(ARG_METHOD_NUMBER);
        }
        if (mAidlInterface == null || !mAidlInterface.asBinder().isBinderAlive()) {
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
            for (int i = 0; i < argumentCount; i++) {
                final Class<?> type = sandboxedMethod.argumentTypes[i].aClass;
                if (type == Boolean.TYPE) {
                    mMethodArguments.arguments[i] = false;
                } else if (type == Byte.TYPE) {
                    mMethodArguments.arguments[i] = (byte) 0;
                } else if (type == Character.TYPE) {
                    mMethodArguments.arguments[i] = (char) 0;
                } else if (type == Short.TYPE) {
                    mMethodArguments.arguments[i] = (short) 0;
                } else if (type == Integer.TYPE) {
                    mMethodArguments.arguments[i] = 0;
                } else if (type == Long.TYPE) {
                    mMethodArguments.arguments[i] = (long) 0;
                } else if (type == Float.TYPE) {
                    mMethodArguments.arguments[i] = (float) 0;
                } else if (type == Double.TYPE) {
                    mMethodArguments.arguments[i] = (double) 0;
                }
            }
        }
        mValueEditors = new InlineValueEditor[argumentCount];
        for (int ii = 0; ii < argumentCount; ii++) {
            SandboxedType type = sandboxedMethod.argumentTypes[ii];
            final int i = ii;
            mValueEditors[ii] = new InlineValueEditor(
                    type.aClass,
                    type.toString(),
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
                invokeAidlMethod();
            }
        });
        return builder.create();
    }

    private void invokeAidlMethod() {
        try {
            InvokeMethodResult result = mAidlInterface.invokeMethod(mMethodNumber, mMethodArguments);
            if (result.exception == null) { // True if there weren't error
                Toast.makeText(getActivity(), result.returnValueAsString, Toast.LENGTH_LONG).show();
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


    @Override
    public void onEditorResult(String key, Object newValue) {
        final int i = Integer.parseInt(key.substring(3));
        mMethodArguments.arguments[i] = newValue;
        mValueEditors[i].updateTextOnButton();
    }
}
