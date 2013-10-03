package com.github.michalbednarski.intentslab.bindservice;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.sandbox.IAidlInterface;
import com.github.michalbednarski.intentslab.sandbox.SandboxedMethod;

/**
 * Created by mb on 30.09.13.
 */
public class AidlControlsFragment extends ListFragment {

    private BindServiceManager.Helper mServiceHelper;
    private IAidlInterface mAidlInterface;
    private BaseAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mServiceHelper = ((BoundServiceActivity) getActivity()).getBoundService();

        mServiceHelper.prepareAidlAndRunWhenReady(getActivity(), new Runnable() {
            @Override
            public void run() {
                mAidlInterface = mServiceHelper.mAidlInterface;
                if (mAidlInterface != null) {
                    createAdapter();
                } else {
                    // TODO: handle non-aidl or unknown interface binders
                    getActivity().finish();
                }
            }
        });



    }

    private void createAdapter() {
        try {
            // Get interface name and format it
            final String name = mAidlInterface.getInterfaceName();
            int lastNamePart = name.lastIndexOf('.') + 1;
            final SpannableString displayName = new SpannableString(name.substring(0, lastNamePart) + "\n" + name.substring(lastNamePart));
            displayName.setSpan(new RelativeSizeSpan(1.5f), lastNamePart + 1, name.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Get list of methods
            final SandboxedMethod[] sandboxedMethods = mAidlInterface.getMethods();

            mAdapter = new BaseAdapter() {
                @Override
                public int getCount() {
                    return 1 + sandboxedMethods.length;
                }

                @Override
                public Object getItem(int position) {
                    return null;
                }

                @Override
                public long getItemId(int position) {
                    return position - 1;
                }

                @Override
                public int getViewTypeCount() {
                    return 2;
                }

                @Override
                public int getItemViewType(int position) {
                    return position == 0 ? 1 : 0;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (position == 0) {
                        if (convertView == null) {
                            TextView headerText = new TextView(getActivity());
                            headerText.setText(displayName);
                            return headerText;
                        }
                    } else {
                        if (convertView == null) {
                            convertView = new Button(getActivity());
                            convertView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    (new InvokeAidlMethodDialog(mServiceHelper, (Integer) v.getTag()))
                                            .show(getActivity().getSupportFragmentManager(), "invokeAidlMethod");
                                }
                            });
                        }
                        ((Button) convertView).setText(sandboxedMethods[position - 1].name);
                        convertView.setTag(position - 1);
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
            try {
                setListAdapter(mAdapter);
            } catch (Exception ignored) {
                // List might have not been ready
            }
        } catch (RemoteException e) {
            // TODO: handle crashed sandbox
        }
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
    }


}
