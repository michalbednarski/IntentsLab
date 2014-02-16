package com.github.michalbednarski.intentslab.clipboard;

import android.support.v4.app.Fragment;
import com.github.michalbednarski.intentslab.MasterDetailActivity;

/**
 * Created by mb on 15.02.14.
 */
public class ClipboardActivity extends MasterDetailActivity {

    @Override
    protected Fragment createMasterFragment() {
        return new ClipboardItemsFragment();
    }

}
