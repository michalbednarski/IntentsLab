package com.github.michalbednarski.intentslab.bindservice;

import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;

/**
 * Created by mb on 16.10.13.
 */
public class UnrecognizedAidlFragment extends Fragment {

    private BindServiceManager.Helper mServiceHelper;
    String mMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mServiceHelper = ((BoundServiceActivity) getActivity()).getBoundService();


        final IBinder boundService = mServiceHelper.mBoundService;
        String interfaceDescriptor;
        try {
            interfaceDescriptor = boundService.getInterfaceDescriptor();
        } catch (DeadObjectException e) {
            Toast.makeText(getActivity(), "Service disconnected", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            mMessage = "queryInterfaceDescriptor: " + Utils.describeException(e);
            return;
        }

        mMessage = "No AIDL found\ngetInterfaceDescriptor() = " + interfaceDescriptor;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.unrecognized_aidl, container, false);
        ((TextView) view.findViewById(R.id.message)).setText(mMessage);
        view.findViewById(R.id.unbind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mServiceHelper.unbind();
                getActivity().finish();
            }
        });
        return view;
    }
}