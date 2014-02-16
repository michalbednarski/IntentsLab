package com.github.michalbednarski.intentslab.valueeditors.object;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TableLayout;

/**
 * Created by mb on 05.10.13.
 */
public class InlineValueEditorsLayout extends ScrollView {
    private final TableLayout mTableLayout;
    private InlineValueEditor[] mValueEditors;
    private View[] mEditorViews;

    public InlineValueEditorsLayout(Context context) {
        this(context, null);
    }

    public InlineValueEditorsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTableLayout = new TableLayout(context);
        addView(mTableLayout);
    }

    /**
     * Put value editors in view, this method must be called only once
     */
    public void setValueEditors(InlineValueEditor[] valueEditors) {
        // Ensure this is called only once
        assert mValueEditors == null && valueEditors != null;

        // Save editors list
        mValueEditors = valueEditors;

        // Get views and add to layout
        mEditorViews = new View[valueEditors.length];
        for (int i = 0; i < valueEditors.length; i++) {
            final View view = valueEditors[i].createView(mTableLayout);
            mEditorViews[i] = view;
            mTableLayout.addView(view);
        }
    }

    /**
     * Add header or footer view depending on whether this method
     * was called before or after {@link #setValueEditors(InlineValueEditor[])}
     */
    public void addHeaderOrFooter(View headerOrFooter) {
        mTableLayout.addView(headerOrFooter);
    }

    /**
     * Show or hide value editors
     */
    public void setHiddenEditorsVisible(boolean visible) {
        for (int i = 0; i < mValueEditors.length; i++) {
            if (mValueEditors[i].mHidden) {
                mEditorViews[i].setVisibility(visible ? VISIBLE : GONE);
            }
        }
    }
}
