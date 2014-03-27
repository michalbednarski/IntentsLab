package com.github.michalbednarski.intentslab.clipboard;

import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.github.michalbednarski.intentslab.CategorizedAdapter;
import com.github.michalbednarski.intentslab.MasterDetailActivity;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;
import com.github.michalbednarski.intentslab.bindservice.AidlControlsFragment;
import com.github.michalbednarski.intentslab.bindservice.BaseServiceFragment;
import com.github.michalbednarski.intentslab.bindservice.callback.CallbackInterfacesManager;
import com.github.michalbednarski.intentslab.bindservice.manager.BindServiceManager;
import com.github.michalbednarski.intentslab.bindservice.manager.ServiceDescriptor;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncher;
import com.github.michalbednarski.intentslab.valueeditors.framework.EditorLauncherForMasterDetail;

import java.util.ArrayList;

/**
* Created by mb on 15.02.14.
*/
public class ClipboardItemsFragment extends ListFragment {
    static ArrayList<ClipboardItemsFragment> sObservers = new ArrayList<ClipboardItemsFragment>();

    public static void refreshAll() {
        for (ClipboardItemsFragment observer : sObservers) {
            observer.update();
        }
    }

    static final int CATEGORY_INTERFACES = 0;
    static final int CATEGORY_MY_INTERFACES = 1;
    static final int CATEGORY_OBJECTS = 2;

    private void update() {
        mBoundServices = BindServiceManager.getBoundServices();
        mAdapter.notifyDataSetChanged();
    }

    private ServiceDescriptor[] mBoundServices;
    private Adapter mAdapter = new Adapter();
    private EditorLauncher mEditorLauncher;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        CategorizedAdapter.ItemInfo itemInfo = mAdapter.getItemInfoForPosition(position);
        switch (itemInfo.category) {
            case CATEGORY_INTERFACES:
                // TODO: open as detail
                startActivity(
                        new Intent(getActivity(), SingleFragmentActivity.class)
                                .putExtra(SingleFragmentActivity.EXTRA_FRAGMENT, AidlControlsFragment.class.getName())
                                .putExtra(BaseServiceFragment.ARG_SERVICE_DESCRIPTOR, mBoundServices[itemInfo.positionInCategory])
                );
                break;
            case CATEGORY_MY_INTERFACES:
                CallbackInterfacesManager.openLogForCallbackAt((MasterDetailActivity) getActivity(), itemInfo.positionInCategory);
                break;
            case CATEGORY_OBJECTS:
                mEditorLauncher.launchEditorForSandboxedObject(
                        ClipboardService.sObjects.keyAt(itemInfo.positionInCategory),
                        ClipboardService.sObjects.keyAt(itemInfo.positionInCategory),
                        ClipboardService.sObjects.valueAt(itemInfo.positionInCategory)
                );
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity instanceof MasterDetailActivity) {
            mEditorLauncher = new EditorLauncherForMasterDetail((MasterDetailActivity) activity, "clipboardLauncher");
        } else {
            mEditorLauncher = new EditorLauncher(activity, "clipboardLauncherEmb");
        }
        update();
        sObservers.add(this);
    }

    @Override
    public void onDestroy() {
        sObservers.remove(this);
        super.onDestroy();
    }

    private class Adapter extends CategorizedAdapter {


        @Override
        protected int getCategoryCount() {
            return 3;
        }

        @Override
        protected int getCountInCategory(int category) {
            switch (category) {
                case CATEGORY_INTERFACES:
                    return mBoundServices.length;
                case CATEGORY_MY_INTERFACES:
                    return CallbackInterfacesManager.getCallbacksCount();
                case CATEGORY_OBJECTS:
                    return ClipboardService.sObjects.size();
            }
            return 0; // No uncategorized items
        }

        @Override
        protected String getCategoryName(int category) {
            return "category " + category; // TODO
        }

        @Override
        protected int getViewTypeInCategory(int category, int positionInCategory) {
            return 1;
        }

        @Override
        protected View getViewInCategory(int category, int positionInCategory, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            String text = null;
            switch (category) {
                case CATEGORY_INTERFACES:
                    text = mBoundServices[positionInCategory].getTitle();
                    break;
                case CATEGORY_OBJECTS:
                    text = ClipboardService.sObjects.keyAt(positionInCategory);
                    break;
            }
            ((TextView) convertView).setText(text);
            return convertView;
        }

        @Override
        protected boolean isItemInCategoryEnabled(int category, int positionInCategory) {
            return true;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            super.registerDataSetObserver(observer);
            CallbackInterfacesManager.registerCallbacksObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            super.unregisterDataSetObserver(observer);
            CallbackInterfacesManager.unregisterCallbacksObserver(observer);
        }
    }
}
