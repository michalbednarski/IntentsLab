package com.github.michalbednarski.intentslab;

import android.content.Context;
import android.database.Cursor;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

public class MultipleCursorAdapter extends BaseAdapter implements AdapterView.OnItemClickListener, View.OnCreateContextMenuListener {
    private int PREVENT_ANDROID_R_AUTO_IMPORT = R.id.action;

    private int mTotalCount = 0;
    private Cursor[] mCursors;
    private int[] mCursorStarts;
    private String[] mHeaders;
    private OnCursorAdapterItemClickListener[] mOnClicks;
    private LayoutInflater mInflater;

    private boolean isQuerying = true;

    // Hide constructor, use Builder
    private MultipleCursorAdapter() {}

    public static class Builder {
        private ArrayList<Cursor> mCursors = new ArrayList<Cursor>();
        private ArrayList<String> mHeaders = new ArrayList<String>();
        private ArrayList<OnCursorAdapterItemClickListener> mOnClicks = new ArrayList<OnCursorAdapterItemClickListener>();


        public Builder addCursor(Cursor cursor, String header, OnCursorAdapterItemClickListener onClick) {
            mCursors.add(cursor);
            mHeaders.add(header);
            mOnClicks.add(onClick);
            return this;
        }

        public void buildAndAttach(ListView listView) {
            final MultipleCursorAdapter adapter = new MultipleCursorAdapter();
            adapter.mCursors = mCursors.toArray(new Cursor[mCursors.size()]);
            adapter.mCursorStarts = new int[mCursors.size() + 1];
            adapter.mHeaders = mHeaders.toArray(new String[mHeaders.size()]);
            adapter.mOnClicks = mOnClicks.toArray(new OnCursorAdapterItemClickListener[mOnClicks.size()]);
            adapter.mInflater = (LayoutInflater) listView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            adapter.reindexCursors();
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(adapter);
            listView.setOnCreateContextMenuListener(adapter);
        }
    }

    public void reindexCursors() {
        mTotalCount = 0;
        for (int i = 0, j = mCursors.length; i < j; i++) {
            mCursorStarts[i] = mTotalCount;
            int count = mCursors[i].getCount();
            if (count != 0) {
                mTotalCount += count + 1;
            }
        }
        mCursorStarts[mCursorStarts.length - 1] = mTotalCount;
    }

    @Override
    public int getCount() {
        return mTotalCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int cursorId = getCursorId(position);
        position -= mCursorStarts[cursorId] + 1;
        if (position == -1) {
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.preference_category, parent, false);
            }
            ((TextView) convertView).setText(mHeaders[cursorId]);
        } else {
            if (convertView == null) {
                convertView = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            Cursor cursor = mCursors[cursorId];
            cursor.moveToPosition(position);
            ((TextView) convertView).setText(cursor.getString(0));
        }
        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int cursorId = getCursorId(position);
        position -= mCursorStarts[cursorId] + 1;
        Cursor cursor = mCursors[cursorId];
        cursor.moveToPosition(position);
        mOnClicks[cursorId].onCursorAdapterItemClick(cursor);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        int cursorId = getCursorId(position);
        position -= mCursorStarts[cursorId] + 1;
        Cursor cursor = mCursors[cursorId];
        cursor.moveToPosition(position);
        mOnClicks[cursorId].onCursorAdapterCreateContextMenu(menu, cursor);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return Arrays.binarySearch(mCursorStarts, position) >= 0 ? 1 : 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return Arrays.binarySearch(mCursorStarts, position) < 0;
    }

    private int getCursorId(int position) {
        int pos = Arrays.binarySearch(mCursorStarts, position);
        if (pos < 0) {
            pos = ~pos - 1;
        }
        while (pos < mCursorStarts.length - 2 && mCursorStarts[pos] == mCursorStarts[pos + 1]) {
            pos++;
        }
        return pos;
    }

    public interface OnCursorAdapterItemClickListener {
        public void onCursorAdapterItemClick(Cursor cursor);
        void onCursorAdapterCreateContextMenu(ContextMenu menu, Cursor cursor);
    }
}
