package com.example.testapp1.editor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.testapp1.R;

public class IntentGeneralWithExtrasFragment extends Fragment {
    public IntentGeneralWithExtrasFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intent_editor_general_with_extras, container, false);

        //}
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // NOTE: why split onCreateView and onCreate?:
        // http://stackoverflow.com/a/15421835

        if (savedInstanceState == null) {
            // And wrap in if:
            // http://stackoverflow.com/a/15421835
            getChildFragmentManager().beginTransaction()
                    .add(R.id.general, new IntentGeneralFragment())
                    .add(R.id.extras, new IntentExtrasFragment())
                    .commit();
        }
    }
}
