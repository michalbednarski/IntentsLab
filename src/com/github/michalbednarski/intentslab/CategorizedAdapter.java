package com.github.michalbednarski.intentslab;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Adapter managing categories
 *
 * Note: this adapter reserves view type 0 for headers
 *
 * Note: theres category -1 for uncategorized items
 *       if you don't want to use it return 0 from getCountInCategory(-1)
 */
public abstract class CategorizedAdapter extends BaseAdapter {

    public static class ItemInfo {
        public final int category;
        public final int positionInCategory;

        private ItemInfo(int category, int positionInCategory) {
            this.category = category;
            this.positionInCategory = positionInCategory;
        }
    }


    protected abstract int getCategoryCount();
    protected abstract int getCountInCategory(int category);
    protected abstract String getCategoryName(int category);
    protected Object getItemInCategory(int category, int positionInCategory) { return null; }
    protected abstract int getViewTypeInCategory(int category, int positionInCategory);
    protected abstract View getViewInCategory(int category, int positionInCategory, View convertView, ViewGroup parent);
    protected abstract boolean isItemInCategoryEnabled(int category, int positionInCategory);

    @Override
    public abstract int getViewTypeCount();

    /*
     *
     *
     * Implementation
     *
     *
     */

    private int mTotalCount = -1; // -1 means that we have to call createCategoryPositionIndex()
    private int[] mCategoryIndex; // dense category index => position in list
    private int[] mCategoryIndexMap; // dense category index => sparse category index

    /**
     * prepare mCategoryIndex(Map) and mTotalCount
     */
    private void createCategoryPositionIndex() {
        int countSoFar = getCountInCategory(-1);


        int sparseCategoryCount = getCategoryCount();
        int[] categoryIndex = new int[sparseCategoryCount];
        int[] categoryIndexMap = new int[sparseCategoryCount];
        int denseCategoryCount = 0;
        for (int i = 0; i < sparseCategoryCount; i++) {
            int countInCategory = getCountInCategory(i);
            if (countInCategory > 0) {
                categoryIndex[denseCategoryCount] = countSoFar;
                categoryIndexMap[denseCategoryCount] = i;
                countSoFar += 1 + countInCategory; // 1 for header
                denseCategoryCount++;
            }
        }

        mTotalCount = countSoFar;
        mCategoryIndex = Utils.shrinkIntArray(categoryIndex, denseCategoryCount);
        mCategoryIndexMap = Utils.shrinkIntArray(categoryIndexMap, denseCategoryCount);
    }

    private ItemInfo getItemInfoFromBinarySearchResult(int binarySearchResult, int position) {
        int denseCategory = ~binarySearchResult - 1;
        if (denseCategory == -1) {
            return new ItemInfo(-1, position);
        } else {
            return new ItemInfo(mCategoryIndexMap[denseCategory], position - mCategoryIndex[denseCategory] - 1);
        }
    }

    public ItemInfo getItemInfoForPosition(int position) {
        return getItemInfoFromBinarySearchResult(Arrays.binarySearch(mCategoryIndex, position), position);
    }

    @Override
    public void notifyDataSetChanged() {
        mTotalCount = -1;
        super.notifyDataSetChanged();
    }

    @Override
    public final int getCount() {
        if (mTotalCount == -1) {
            createCategoryPositionIndex();
        }
        return mTotalCount;
    }

    @Override
    public Object getItem(int position) {
        ItemInfo itemInfo = getItemInfoForPosition(position);
        if (itemInfo.positionInCategory == -1) {
            return null; // Header
        }
        return getItemInCategory(itemInfo.category, itemInfo.positionInCategory);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        int binarySearchResult = Arrays.binarySearch(mCategoryIndex, position);
        if (binarySearchResult >= 0) {
            return 0; // header
        }

        ItemInfo itemInfo = getItemInfoFromBinarySearchResult(binarySearchResult, position);
        return getViewTypeInCategory(itemInfo.category, itemInfo.positionInCategory);
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        int binarySearchResult = Arrays.binarySearch(mCategoryIndex, position);
        if (binarySearchResult >= 0) {
            // header
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.preference_category, parent, false);
            }
            ((TextView) convertView).setText(getCategoryName(mCategoryIndexMap[binarySearchResult]));
            return convertView;
        }

        ItemInfo itemInfo = getItemInfoFromBinarySearchResult(binarySearchResult, position);
        return getViewInCategory(itemInfo.category, itemInfo.positionInCategory, convertView, parent);
    }

    @Override
    public final boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public final boolean isEnabled(int position) {
        int binarySearchResult = Arrays.binarySearch(mCategoryIndex, position);
        if (binarySearchResult >= 0) {
            return false; // header
        }

        ItemInfo itemInfo = getItemInfoFromBinarySearchResult(binarySearchResult, position);
        return isItemInCategoryEnabled(itemInfo.category, itemInfo.positionInCategory);
    }
}
