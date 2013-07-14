package com.example.testapp1.providerlab;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Scroller;
import com.example.testapp1.R;

import java.util.ArrayList;

/**
 * Created by mb on 12.06.13.
 */
public class DataGridView extends LinearLayout {

    // Row sizes
    private static final int BORDER_SIZE = 1;

    // Text paddings
    private static final int PADDING_LEFT = 2;
    private static final int HORIZONTAL_PADDING = PADDING_LEFT + 6;

    // Minimal and maximal column widths
    private static final int MIN_COLUMN_WIDTH = 50; // enforced
    private static final int MAX_COLUMN_WIDTH = 500; // for automatic detection, user can expand further

    // More = further flings
    private static final int HORIZONTAL_FLING_SPEED_FACTOR = 1000;

    // Amount of rows scanned for determining default column widths
    private static final int SCAN_ROW_COUNT_FOR_COLUMN_WIDTH = 5;

    // Special value for RowView#mRowId indicating that this is header row
    private static final int ROW_ID_HEADER = -1;


    private RowView mHeadersView;
    private ListView mListView;
    private Cursor mCursor = null;
    private ListAdapter mListAdapter = new ListAdapter();
    private ColumnInfo[] mColumns;
    private Paint mPaint;
    private Paint mHeadersPaint;
    private Paint mSpecialValuePaint;
    private int mRowHeight;
    private int mFontAscent;

    private ArrayList<RowView> mDisplayedRows = new ArrayList<RowView>();

    private int mMaxScroll = 0;


    public DataGridView(Context context) {
        super(context);

        // Create headers view and list
        mHeadersView = new RowView();
        mHeadersView.mRowId = ROW_ID_HEADER;
        mListView = new ListView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent ev) {
                boolean handledByListView = super.onTouchEvent(ev);
                DataGridView.this.onTouchEvent(ev);
                return handledByListView;

            }
        };
        mListView.setAdapter(mListAdapter);
        mListView.setDivider(null);

        // Lay everything out
        setOrientation(VERTICAL);
        mHeadersView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // width
                ViewGroup.LayoutParams.WRAP_CONTENT, // height
                0 // weight
        ));
        mListView.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // width
                0, // height
                1 // weight
        ));

        // Prepare fonts
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setTextSize(context.getResources().getDimension(R.dimen.text_size));
        final Paint.FontMetricsInt fontMetricsInt = mPaint.getFontMetricsInt();
        mRowHeight = -fontMetricsInt.ascent + fontMetricsInt.descent;
        mFontAscent = fontMetricsInt.ascent;
        mHeadersPaint = new Paint(mPaint);
        mHeadersPaint.setFakeBoldText(true);
        mSpecialValuePaint = new Paint(mPaint); // TODO
        mSpecialValuePaint.setTextSkewX(-0.25f);

        // Init gesture detector
        mScroller = new Scroller(context);

        // Add views
        addView(mHeadersView);
        addView(mListView);
    }

    void setCursor(Cursor cursor) {
        mCursor = cursor;

        // init column names
        int columnCount = cursor.getColumnCount();
        mColumns = new ColumnInfo[columnCount];
        for (int i = 0; i < columnCount; i++) {
            ColumnInfo column = new ColumnInfo();
            column.name = cursor.getColumnName(i);
            column.width = measureCellWidth(i, true, MIN_COLUMN_WIDTH); // measure header
            mColumns[i] = column;
        }

        // Shouldn't do anything but apparently moveToPosition below sometimes doesn't work
        // e.g. with TalkBack StatusProvider (content://com.google.android.marvin.talkback.StatusProvider/)
        mCursor.moveToFirst();

        // Measure first SCAN_ROW_COUNT_FOR_COLUMN_WIDTH rows for column widths
        for (int row = 0, rowCount = Math.min(cursor.getCount(), SCAN_ROW_COUNT_FOR_COLUMN_WIDTH); row < rowCount; row++) {
            mCursor.moveToPosition(row);
            for (int i = 0; i < columnCount; i++) {
                mColumns[i].width = measureCellWidth(i, false, mColumns[i].width);
            }
        }

        // Calculate max scroll
        calculateMaxScroll();

        // Update list
        mHeadersView.invalidate();
        mListAdapter.notifyDataSetChanged();
    }

    private void calculateMaxScroll() {
        int widthSum = 0;
        for (int i = 0; i < mColumns.length; i++) {
            widthSum += mColumns[i].width;
        }
        mMaxScroll = widthSum - getWidth();
        if (mMaxScroll < 0) {
            mMaxScroll = 0;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        calculateMaxScroll();
    }

    // Gesture detection
    private int mEditedColumn = -1;
    private int mColumnExtraSpace;

    Scroller mScroller;
    VelocityTracker mVelocityTracker = null;
    int mLastXPosition = -1; // for scrolling

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Init VelocityTracker for flinging
                if (mVelocityTracker == null) {
                    mVelocityTracker = VelocityTracker.obtain();
                }
                mVelocityTracker.addMovement(event);

                // Stop fling
                mScroller.forceFinished(true);

                // Prepare scrolling
                mLastXPosition = (int) event.getX();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Cancel fling
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                // End scroll
                mLastXPosition = -1;

                // Start column resizing
                if (event.getPointerCount() == 2) {
                    int pos = ((int) Math.min(event.getX(0), event.getX(1))) + mScroller.getCurrX();
                    mEditedColumn = -1;
                    for (int i = 0, columnCount = mColumns.length; i < columnCount; i++) {
                        int columnWidth = mColumns[i].width;
                        if (pos < columnWidth) {
                            mEditedColumn = i;
                            break;
                        }
                        pos -= columnWidth;
                    }
                    if (mEditedColumn != -1) {
                        mColumnExtraSpace = mColumns[mEditedColumn].width - (Math.abs((int) (event.getX(0) - event.getX(1))));
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // Resize column
                if (mEditedColumn != -1) {
                    if (event.getPointerCount() == 2) {
                        // If we are scrolled to right edge keep that position
                        boolean stickToRight = mMaxScroll > 5 && mScroller.getCurrX() >= mMaxScroll;

                        // Actually resize column and recalculate space
                        mColumns[mEditedColumn].width = Math.max(Math.abs((int)(event.getX(0) - event.getX(1))) + mColumnExtraSpace, MIN_COLUMN_WIDTH);
                        calculateMaxScroll();

                        // Keep position
                        if (stickToRight) {
                            mScroller.setFinalX(mMaxScroll);
                            mScroller.abortAnimation();
                        }

                        // Do redraw
                        invalidateAllRows();
                    } else {
                        // End resizing
                        mEditedColumn = -1;
                    }
                }

                // Calculate velocity for flinging
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);
                }

                // Scroll
                if (mLastXPosition != -1 && event.getPointerCount() == 1) {
                    int touchX = (int) event.getX();
                    int scrollX = mScroller.getCurrX() - (touchX - mLastXPosition);
                    if (scrollX < 0) {
                        scrollX = 0;
                    } else if (scrollX > mMaxScroll) {
                        scrollX = mMaxScroll;
                    }
                    mScroller.setFinalX(scrollX);
                    mScroller.abortAnimation();
                    mLastXPosition = touchX;
                    invalidateAllRows();
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // End resizing column
                mEditedColumn = -1;
                break;

            case MotionEvent.ACTION_UP:
                // Fling
                if (mVelocityTracker != null) {
                    mVelocityTracker.computeCurrentVelocity(HORIZONTAL_FLING_SPEED_FACTOR);
                    mScroller.fling(mScroller.getCurrX(), 0, -(int) mVelocityTracker.getXVelocity(), 0, 0, mMaxScroll, 0, 0);
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    invalidateAllRows();
                }
                mLastXPosition = -1;
                break;

            case MotionEvent.ACTION_CANCEL:
                // End scrolling without fling
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                mLastXPosition = -1;
        }
        return true;
    }

    // /Gesture detection


    // Triggering redraw
    private void invalidateAllRows() {
        for (RowView displayedRow : mDisplayedRows) {
            displayedRow.invalidate();
        }
    }
    private Runnable mInvalidateRunnable = new Runnable() {
        @Override
        public void run() {
            invalidateAllRows();
        }
    };

    // Values
    private Paint getCellTextPaint(int columnId, boolean isHeader) {
        if (isHeader) {
            return mHeadersPaint;
        }
        final int type = getTypeOfCellValue(columnId);
        return (type == Cursor.FIELD_TYPE_NULL || type == Cursor.FIELD_TYPE_BLOB) ? mSpecialValuePaint : mPaint;
    }

    private int measureCellWidth(int columnId, boolean isHeader, int previousWidth) {
        if (previousWidth == MAX_COLUMN_WIDTH) {
            return MAX_COLUMN_WIDTH;
        }
        Paint paint = getCellTextPaint(columnId, isHeader);
        String text = getCellText(columnId, isHeader);
        final int textWidth = ((int) paint.measureText(text)) + HORIZONTAL_PADDING;
        return Math.max(previousWidth, Math.min(textWidth, MAX_COLUMN_WIDTH));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private int getTypeOfCellValue(int columnId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return mCursor.getType(columnId);
        } else if (mCursor.isNull(columnId)) {
            return Cursor.FIELD_TYPE_NULL;
        } else {
            return -1; // Unknown, not any of Cursor.FIELD_TYPE_* values
        }
    }

    private String getCellText(int columnId, boolean isHeader) {
        if (isHeader) {
            return mCursor.getColumnName(columnId);
        } else if (getTypeOfCellValue(columnId) == Cursor.FIELD_TYPE_BLOB) {
            return "BLOB";
        } else {
            String text;
            try {
                text = mCursor.getString(columnId);
            } catch (Exception e) {
                if (e != null && e.getMessage() != null && e.getMessage().contains("Unable to convert BLOB to string")) {
                    text = "BLOB";
                } else {
                    text = "Unknown";
                }
            }

            if (text == null) {
                text = "null";
            }
            return text;
        }
    }

    /**
     * View displaying a row or headers, possibly scrolled horizontally
     * (Vertical scrolling is handled by ListView)
     */
    private class RowView extends View {
        private int mRowId;

        public RowView() {
            super(DataGridView.this.getContext());
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(getDefaultSize(0, widthMeasureSpec), mRowHeight + BORDER_SIZE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (mRowId != ROW_ID_HEADER) {
                // Not header, move cursor to current row
                mCursor.moveToPosition(mRowId);
            } else {
                // Header, recalculate scroll offset and request next animation frame
                if (mScroller.computeScrollOffset()) {
                    post(mInvalidateRunnable);
                }
            }

            // Draw cells
            int x = -mScroller.getCurrX();
            for (int i = 0, columnsCount = mColumns.length; i < columnsCount; i++) {
                String text = getCellText(i, mRowId == ROW_ID_HEADER);
                Paint textPaint = getCellTextPaint(i, mRowId == ROW_ID_HEADER);
                /*if (mRowId == ROW_ID_HEADER) {
                    text = mCursor.getColumnName(i);
                    textPaint = mHeadersPaint;
                } else {
                    text = mCursor.getString(i);
                    if (text == null) {
                        text = "null";
                        textPaint = mSpecialValuePaint;
                    }
                }*/

                ColumnInfo column = mColumns[i];
                canvas.save();
                canvas.clipRect(x, 0, x + column.width, mRowHeight);
                canvas.drawText(text, PADDING_LEFT + x, -mFontAscent, textPaint);
                canvas.restore();
                canvas.drawRect(x + column.width, 0, x + column.width + BORDER_SIZE, mRowHeight, mPaint);
                x += column.width + BORDER_SIZE;
            }

            // Draw separator line at bottom
            canvas.drawRect(0, mRowHeight, getWidth(), mRowHeight + BORDER_SIZE, mPaint);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mDisplayedRows.add(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mDisplayedRows.remove(this);
        }
    }

    private class ListAdapter extends BaseAdapter {

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return mCursor == null ? 0 : mCursor.getCount();
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
            RowView rowView =
                convertView != null ?
                        (RowView) convertView :
                        new RowView();
            rowView.mRowId = position;
            return rowView;
        }
    }

    private static class ColumnInfo {
        String name;
        int width;
    }
}
