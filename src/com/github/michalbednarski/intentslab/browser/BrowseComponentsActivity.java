package com.github.michalbednarski.intentslab.browser;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import com.github.michalbednarski.intentslab.R;
import com.github.michalbednarski.intentslab.SingleFragmentActivity;

public class BrowseComponentsActivity extends FragmentActivity {

    public static final String EXTRA_FETCHER = "BrowseComponentsActivity.fetcher";
    public static final String EXTRA_SERIALIZED_FETCHER = "BrowseComponentsActivity.fetcher.serialized";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 11) {
            onCreateAndroidSDK11AndUp();
        }

        setContentView(R.layout.master_detail_base);

        boolean showDetail = false;

        if (savedInstanceState == null) {
            Bundle args = new Bundle();
            Fetcher fetcher;
            if (getIntent().hasExtra(EXTRA_SERIALIZED_FETCHER)) {
                fetcher = FetcherManager.unserializeFetcher(getIntent().getStringExtra(EXTRA_SERIALIZED_FETCHER));
            } else {
                fetcher = getIntent().getParcelableExtra(EXTRA_FETCHER);
            }
            args.putParcelable(BrowseComponentsFragment.ARG_FETCHER, fetcher);
            BrowseComponentsFragment fragment = new BrowseComponentsFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().add(R.id.master, fragment).commit();
        } else {
            showDetail = haveDetail();
        }
        findViewById(R.id.detail).setVisibility(showDetail ? View.VISIBLE : View.GONE);
    }

    @TargetApi(11)
    private void onCreateAndroidSDK11AndUp() {
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean haveDetail() {
        return getSupportFragmentManager().findFragmentById(R.id.detail) != null;
    }

    private boolean usingTabletView() {
        return getResources().getBoolean(R.bool.use_master_detail);
    }

    /**
     * Open given fragment in detail panel or new activity
     */
    public void openFragment(Class<? extends Fragment> fragmentClass, Bundle arguments) {
        if (usingTabletView()) {
            Fragment fragment;
            try {
                fragment = fragmentClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            fragment.setArguments(arguments);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.detail, fragment)
                    .commit();
            findViewById(R.id.detail).setVisibility(View.VISIBLE);
        } else {
            arguments.putString(SingleFragmentActivity.EXTRA_FRAGMENT, fragmentClass.getName());
            startActivity(new Intent(this, SingleFragmentActivity.class).replaceExtras(arguments));
        }
    }
}
