package com.github.michalbednarski.intentslab;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

/**
 * Activity displaying single fragment
 */
public class SingleFragmentActivity extends FragmentActivity {

    public static final String EXTRA_FRAGMENT = "singleFragmentActivity.theFragmentClass";

    private static final Class<? extends Fragment>[] WHITE_LIST = new Class[] {
            XMLViewerFragment.class
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            String fragmentName = getIntent().getStringExtra(EXTRA_FRAGMENT);
            for (Class<? extends Fragment> fragmentClass : WHITE_LIST) {
                if (fragmentClass.getName().equals(fragmentName)) {

                    Fragment fragment;
                    try {
                        fragment = fragmentClass.newInstance();
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                    fragment.setArguments(getIntent().getExtras());
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, fragment)
                            .commit();

                    return;
                }
            }

            Toast.makeText(this, "Fragment not whitelisted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
