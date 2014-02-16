package com.github.michalbednarski.intentslab.bindservice;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.bindservice.manager.SystemServiceDescriptor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * List of services registered in android.os.ServiceManager
 */
public class SystemServicesDialog extends DialogFragment implements AdapterView.OnItemClickListener {
    private String[] mServices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            mServices = (String[]) serviceManager.getMethod("listServices").invoke(null);
            if (mServices[0] == null) {
                throw new Exception("Invalid service list");
            }
        } catch (Exception e) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"service", "list"});
                process.waitFor();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                ArrayList<String> services = new ArrayList<String>();
                Pattern pattern = Pattern.compile("\\d+\\s+(.*): \\[.*");
                while ((line = bufferedReader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        services.add(matcher.group(1));
                    }
                }
                mServices = services.toArray(new String[services.size()]);
            } catch (Exception e1) {
                Utils.toastException(getActivity(), e); // Toast first exception
                dismiss();
                return;
            }
        }
        Arrays.sort(mServices);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(getActivity().getString(R.string.system_services));
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ListView listView = new ListView(getActivity());
        Utils.fixListViewInDialogBackground(listView);
        listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, mServices));
        listView.setOnItemClickListener(this);
        return listView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BindServiceManager.prepareBinderAndShowUI(getActivity(), new SystemServiceDescriptor(mServices[position]));
    }
}
