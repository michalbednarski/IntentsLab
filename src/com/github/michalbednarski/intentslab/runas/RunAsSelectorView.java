package com.github.michalbednarski.intentslab.runas;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * View for selecting uid to run as
 */
public class RunAsSelectorView extends LinearLayout {

    private final DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            setVisibility(RunAsManager.getSelectorAdapter().getCount() >= 2 ? VISIBLE : GONE);
        }
    };

    private static final AdapterView.OnItemSelectedListener mOnItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            RunAsManager.sSelectedId = id;
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Spinner cannot have nothing selected
        }
    };

    private Spinner mSpinner;

    public RunAsSelectorView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(HORIZONTAL);

        final TextView label = new TextView(context);
        label.setText("Run as:");

        mSpinner = new Spinner(context);
        mSpinner.setLayoutParams(new LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        ));

        addView(label);
        addView(mSpinner);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSpinner.setAdapter(RunAsManager.getSelectorAdapter());
        mSpinner.setSelection(RunAsManager.getSelectedSpinnerPosition());
        RunAsManager.getSelectorAdapter().registerDataSetObserver(mObserver);
        mSpinner.setOnItemSelectedListener(mOnItemSelectedListener);
        mObserver.onChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        RunAsManager.getSelectorAdapter().unregisterDataSetObserver(mObserver);
        mSpinner.setOnItemSelectedListener(null);
        mSpinner.setAdapter(null);
        super.onDetachedFromWindow();
    }
}
