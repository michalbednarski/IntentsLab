package com.github.michalbednarski.intentslab.bindservice;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethodArguments;

/**
 * Created by mb on 03.10.13.
 */
public class InvokeAidlMethodDialog extends DialogFragment {
    private static final String ARG_SERVICE = "bound-service-descriptor";
    private static final String ARG_METHOD_NUMBER = "method-number";
    private static final String STATE_METHOD_ARGUMENTS = "method-arguments";

    private IAidlInterface mAidlInterface;
    private final int mMethodNumber;
    private SandboxedMethodArguments mMethodArguments;


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
        if (savedInstanceState != null) {
            mMethodArguments = savedInstanceState.getParcelable(STATE_METHOD_ARGUMENTS);
            if (mMethodArguments.arguments.length != sandboxedMethod.argumentTypes.length) {
                Log.e("InvokeAidlMethodDialog", "Arguments count changed unexpectedly");
                dismissAllowingStateLoss();
                return;
            }
        } else {
            mMethodArguments = new SandboxedMethodArguments(sandboxedMethod.argumentTypes.length);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_METHOD_ARGUMENTS, mMethodArguments);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        ListView listView = new ListView(getActivity());
        listView.setAdapter(mAdapter);
        builder.setView(listView);
        builder.setPositiveButton("Invoke", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getActivity(), "TODO", Toast.LENGTH_SHORT).show(); // TODO
            }
        });
        return builder.create();
    }



    private final BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return mMethodArguments.arguments.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.strucutre_editor_row_with_button, parent, false);
            }
            return convertView;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }
    };
}
