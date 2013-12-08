package com.github.michalbednarski.intentslab.bindservice;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.github.michalbednarski.intentslab.R;

public class BoundServicesListActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new BoundServicesFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bound_services_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            //
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class BoundServicesFragment extends ListFragment {
        private BindServiceManager.Helper[] mHelpers;
        private MyAdapter mAdapter = new MyAdapter();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mHelpers = BindServiceManager.getBoundServices();

        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setListAdapter(mAdapter);
            getListView().setOnItemClickListener(mAdapter);
        }

        private class MyAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
            @Override
            public int getCount() {
                return mHelpers.length;
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
                    LayoutInflater inflater = (LayoutInflater)
                            parent.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                    convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ((TextView) convertView).setText(mHelpers[position].getTitle());
                return convertView;
            }

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(
                        new Intent(getActivity(), BoundServiceActivity.class)
                        .putExtra(BoundServiceActivity.EXTRA_SERVICE, mHelpers[position].mDescriptor)
                );
            }
        }
    }

}
