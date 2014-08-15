/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab.editor;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.Utils;
import com.github.michalbednarski.intentslab.browser.ComponentInfoFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for choosing component for intent
 *
 * Must be in {@link IntentEditorActivity}
 * and have target fragment set to {@link IntentGeneralFragment}
 */
public class ComponentPickerDialogFragment extends DialogFragment implements OnItemClickListener, OnItemLongClickListener {

    private ResolveInfo[] mChoices;
    private int mEnabledChoicesCount;

    public ComponentPickerDialogFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Get edited intent
        IntentEditorActivity intentEditor = (IntentEditorActivity) getActivity();
        Intent intent = new Intent(intentEditor.getEditedIntent());
        intent.setComponent(null);

        // Get components
        PackageManager pm = intentEditor.getPackageManager();
        List<ResolveInfo> ri = null;

        switch (intentEditor.getComponentType()) {
            case IntentEditorConstants.ACTIVITY:
                ri = pm.queryIntentActivities(intent, PackageManager.GET_DISABLED_COMPONENTS);
                break;
            case IntentEditorConstants.BROADCAST:
                ri = pm.queryBroadcastReceivers(intent, PackageManager.GET_DISABLED_COMPONENTS);
                break;
            case IntentEditorConstants.SERVICE:
                ri = pm.queryIntentServices(intent, PackageManager.GET_DISABLED_COMPONENTS);
                break;
        }

        // Cancel if no components
        if (ri.isEmpty()) {
            Toast.makeText(getActivity(), R.string.no_matching_components_found, Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        // Split enabled and disabled choices
        ArrayList<ResolveInfo> choices = new ArrayList<ResolveInfo>();
        ArrayList<ResolveInfo> disabledChoices = new ArrayList<ResolveInfo>();
        for (ResolveInfo resolveInfo : ri) {
            (isComponentEnabled(pm, resolveInfo) ? choices : disabledChoices)
                    .add(resolveInfo);
        }

        mEnabledChoicesCount = choices.size();
        choices.addAll(disabledChoices);
        mChoices = choices.toArray(new ResolveInfo[choices.size()]);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Abort if nothing found, onCreate called dismiss()
        if (mChoices == null) {
            return null;
        }

        // Create list
        ListView lv = new ListView(getActivity(), null);
        lv.setId(android.R.id.list);
        lv.setAdapter(new Adapter());
        Utils.fixListViewInDialogBackground(lv);
        lv.setOnItemClickListener(this);
        lv.setOnItemLongClickListener(this);
        return lv;
    }

	private String getComponentName(ResolveInfo info) {
		boolean isService = info.activityInfo == null;
		return new ComponentName(
				isService ? info.serviceInfo.packageName : info.activityInfo.packageName,
				isService ? info.serviceInfo.name : info.activityInfo.name
			).flattenToShortString();
	}

    private static boolean isComponentEnabled(PackageManager pm, ResolveInfo info) {
        final boolean defaultEnabled;
        final ComponentName componentName;

        ActivityInfo activityInfo = info.activityInfo;
        if (activityInfo != null) {
            if (!activityInfo.applicationInfo.enabled) {
                return false;
            }
            defaultEnabled = activityInfo.enabled;
            componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
        } else {
            ServiceInfo serviceInfo = info.serviceInfo;
            if (!serviceInfo.applicationInfo.enabled) {
                return false;
            }
            defaultEnabled = serviceInfo.enabled;
            componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
        }

        int enabledSetting = pm.getComponentEnabledSetting(componentName);
        if (enabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            return defaultEnabled;
        } else {
            return enabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        }
    }


    private class Adapter extends ArrayAdapter<ResolveInfo> {
        private LayoutInflater mInflater;
        private PackageManager mPm;

        Adapter() {
            super(getActivity(), 0, mChoices);
            FragmentActivity activity = getActivity();
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPm = activity.getPackageManager();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.simple_list_item_2_with_icon, parent, false);
            }

            ResolveInfo info = getItem(position);
            ((TextView)convertView.findViewById(android.R.id.text1))
                .setText(info.loadLabel(mPm));
            ((TextView)convertView.findViewById(android.R.id.text2))
                .setText(getComponentName(info));

            ((ImageView)convertView.findViewById(R.id.app_icon)).setImageDrawable(info.loadIcon(mPm));

            setTextViewsEnabled(convertView, position < mEnabledChoicesCount);

            return convertView;
        }
    }

    /**
     * Fill component name field on item click
     */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ((IntentGeneralFragment) getTargetFragment()).setComponentText(getComponentName(mChoices[position]));
		dismiss();
	}

    /**
     * Show item info on long click
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ResolveInfo info = mChoices[position];
        boolean isService = info.activityInfo == null;
        startActivity(
                new Intent(getActivity(), SingleFragmentActivity.class)
                .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, ComponentInfoFragment.class.getName())
                .putExtra(ComponentInfoFragment.ARG_PACKAGE_NAME, isService ? info.serviceInfo.packageName : info.activityInfo.packageName)
                .putExtra(ComponentInfoFragment.ARG_COMPONENT_NAME, isService ? info.serviceInfo.name : info.activityInfo.name)
                .putExtra(ComponentInfoFragment.ARG_LAUNCHED_FROM_INTENT_EDITOR, true)
        );
        return true;
    }

    private static void setTextViewsEnabled(View v, boolean enabled) {
        if (v instanceof TextView) {
            v.setEnabled(enabled);
        }

        if (v instanceof ViewGroup) {
            final ViewGroup vg = (ViewGroup) v;
            for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                setTextViewsEnabled(vg.getChildAt(i), enabled);
            }
        }
    }
}
